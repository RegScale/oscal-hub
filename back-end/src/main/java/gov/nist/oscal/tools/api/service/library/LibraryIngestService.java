package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryTagRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates "save to library" from any builder source.
 * <p>
 * Idempotent on {@code (creator, sourceType, sourceId)}: the first save creates a
 * {@link LibraryItem} plus version 1; subsequent saves append a new
 * {@link LibraryVersion} to the existing item.
 */
@Service
public class LibraryIngestService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryIngestService.class);

    private final LibraryItemRepository itemRepo;
    private final LibraryVersionRepository versionRepo;
    private final LibraryStorageService libraryStorage;
    private final UserRepository userRepo;
    private final OrganizationRepository organizationRepo;
    private final LibraryTagRepository libraryTagRepo;
    private final Map<SourceType, SourceContentResolver> resolvers;

    @Autowired
    public LibraryIngestService(LibraryItemRepository itemRepo,
                                LibraryVersionRepository versionRepo,
                                LibraryStorageService libraryStorage,
                                List<SourceContentResolver> resolverList,
                                UserRepository userRepo,
                                OrganizationRepository organizationRepo,
                                LibraryTagRepository libraryTagRepo) {
        this.itemRepo = itemRepo;
        this.versionRepo = versionRepo;
        this.libraryStorage = libraryStorage;
        this.userRepo = userRepo;
        this.organizationRepo = organizationRepo;
        this.libraryTagRepo = libraryTagRepo;

        Map<SourceType, SourceContentResolver> map = new EnumMap<>(SourceType.class);
        for (SourceContentResolver r : resolverList) {
            for (SourceType t : r.supportedTypes()) {
                map.put(t, r);
            }
        }
        this.resolvers = Collections.unmodifiableMap(map);
    }

    /**
     * Save the contents of a builder row into the library, creating a new item or
     * appending a new version to an existing one based on {@code (creator, sourceType, sourceId)}.
     *
     * @return the saved {@link LibraryItem} with its current version pointer set
     */
    @Transactional
    public LibraryItem saveToLibrary(SourceType sourceType,
                                     Long builderRowId,
                                     String title,
                                     String description,
                                     Set<String> tagNames,
                                     Visibility visibility,
                                     Long organizationId,
                                     String callerUsername) {
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
        SourceContentResolver resolver = resolvers.get(sourceType);
        if (resolver == null) {
            throw new IllegalArgumentException("no resolver for source type: " + sourceType);
        }

        SourceContent content = resolver.resolve(builderRowId, callerUsername);

        User caller = userRepo.findByUsername(callerUsername)
                .orElseThrow(() -> new IllegalArgumentException("caller not found: " + callerUsername));

        Visibility effective = visibility != null ? visibility : Visibility.PRIVATE;
        if (effective == Visibility.ORGANIZATION && organizationId == null) {
            throw new IllegalArgumentException("organizationId required when visibility=ORGANIZATION");
        }

        Optional<LibraryItem> existing = itemRepo.findByCreatedBy_IdAndSourceTypeAndSourceId(
                caller.getId(), sourceType, content.sourceId());

        LibraryItem item;
        int nextVersion;
        boolean isAppend = existing.isPresent();

        if (isAppend) {
            item = existing.get();
            LibraryVersion currentVersion = item.getCurrentVersion();
            int currentNum = currentVersion != null && currentVersion.getVersionNumber() != null
                    ? currentVersion.getVersionNumber()
                    : 0;
            nextVersion = currentNum + 1;

            if (title != null) {
                item.setTitle(title);
            }
            if (description != null) {
                item.setDescription(description);
            }
            if (tagNames != null) {
                item.setTags(resolveTags(tagNames));
            }
        } else {
            String resolvedTitle = title != null ? title : content.defaultTitle();
            item = new LibraryItem(
                    UUID.randomUUID().toString(),
                    resolvedTitle,
                    description,
                    content.oscalType(),
                    caller);
            item.setSourceType(sourceType);
            item.setSourceId(content.sourceId());
            item.setVisibility(effective);
            if (effective == Visibility.ORGANIZATION) {
                Organization org = organizationRepo.findById(organizationId)
                        .orElseThrow(() -> new IllegalArgumentException("unknown organizationId: " + organizationId));
                item.setOrganization(org);
            }
            if (effective == Visibility.PUBLIC) {
                LocalDateTime now = LocalDateTime.now();
                item.setPublishedAt(now);
                item.setLastPublishedAt(now);
            }
            if (tagNames != null) {
                item.setTags(resolveTags(tagNames));
            }
            nextVersion = 1;
        }
        item = itemRepo.save(item);

        // Write blob to library storage. The storage service expects String content
        // (it converts to UTF-8 bytes internally) — keep the raw resolver bytes round-tripping
        // through the same encoding.
        String versionId = UUID.randomUUID().toString();
        byte[] bytes = content.bytes() != null ? content.bytes() : new byte[0];
        String fileContent = new String(bytes, StandardCharsets.UTF_8);
        String blobPath = libraryStorage.buildBlobPath(item.getItemId(), versionId, content.filename());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("itemId", item.getItemId());
        metadata.put("versionId", versionId);
        metadata.put("versionNumber", Integer.toString(nextVersion));
        metadata.put("oscalType", content.oscalType());
        metadata.put("format", content.format());
        metadata.put("uploadedBy", callerUsername);
        metadata.put("sourceType", sourceType.name());
        metadata.put("sourceId", content.sourceId() != null ? content.sourceId().toString() : "");

        libraryStorage.saveLibraryFile(fileContent, blobPath, metadata);

        LibraryVersion version = new LibraryVersion(
                versionId,
                item,
                nextVersion,
                content.filename(),
                content.format(),
                (long) bytes.length,
                blobPath,
                caller,
                isAppend ? "Saved from " + sourceType.name() + " (v" + nextVersion + ")"
                         : "Initial version (saved from " + sourceType.name() + ")"
        );
        version = versionRepo.save(version);

        item.setCurrentVersion(version);
        if (isAppend && item.getVisibility() == Visibility.PUBLIC) {
            item.setLastPublishedAt(LocalDateTime.now());
        }
        item = itemRepo.save(item);

        logger.info("Saved to library: itemId={} version={} sourceType={} sourceId={} caller={}",
                item.getItemId(), nextVersion, sourceType, content.sourceId(), callerUsername);
        return item;
    }

    private Set<LibraryTag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new HashSet<>();
        }
        Set<LibraryTag> result = new HashSet<>();
        for (String raw : names) {
            if (raw == null) continue;
            String normalized = raw.toLowerCase().trim();
            if (normalized.isEmpty()) continue;
            LibraryTag tag = libraryTagRepo.findByName(normalized)
                    .orElseGet(() -> libraryTagRepo.save(new LibraryTag(normalized)));
            result.add(tag);
        }
        return result;
    }
}
