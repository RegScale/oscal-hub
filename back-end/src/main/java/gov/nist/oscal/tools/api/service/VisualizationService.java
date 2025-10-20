package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class VisualizationService {

    private static final Logger logger = LoggerFactory.getLogger(VisualizationService.class);

    // Control family names mapping
    private static final Map<String, String> CONTROL_FAMILIES = new HashMap<String, String>() {{
        put("ac", "Access Control");
        put("at", "Awareness and Training");
        put("au", "Audit and Accountability");
        put("ca", "Assessment, Authorization, and Monitoring");
        put("cm", "Configuration Management");
        put("cp", "Contingency Planning");
        put("ia", "Identification and Authentication");
        put("ir", "Incident Response");
        put("ma", "Maintenance");
        put("mp", "Media Protection");
        put("pe", "Physical and Environmental Protection");
        put("pl", "Planning");
        put("pm", "Program Management");
        put("ps", "Personnel Security");
        put("pt", "PII Processing and Transparency");
        put("ra", "Risk Assessment");
        put("sa", "System and Services Acquisition");
        put("sc", "System and Communications Protection");
        put("si", "System and Information Integrity");
        put("sr", "Supply Chain Risk Management");
    }};

    public VisualizationService() {
    }

    public SspVisualizationResult analyzeSSP(SspVisualizationRequest request, String username) {
        SspVisualizationResult result = new SspVisualizationResult();

        try {
            logger.info("Analyzing SSP - Format: {}, Content length: {}", request.getFormat(), request.getContent().length());

            // Parse the content as JSON (converting if necessary)
            JsonNode sspNode = parseContent(request.getContent(), request.getFormat());

            logger.info("Parsed SSP node - isNull: {}, fieldNames: {}",
                sspNode == null,
                sspNode != null ? sspNode.fieldNames().hasNext() ? "has fields" : "no fields" : "null");

            if (sspNode != null) {
                // Log all field names
                StringBuilder fields = new StringBuilder();
                sspNode.fieldNames().forEachRemaining(name -> fields.append(name).append(", "));
                logger.info("Root node fields: [{}]", fields.toString());
            }

            // For XML, the root element is the document container, so we need to check differently
            JsonNode ssp;
            if (sspNode.has("system-security-plan")) {
                // JSON/YAML format: root has "system-security-plan" field
                ssp = sspNode.get("system-security-plan");
                logger.info("Found SSP in JSON/YAML format");
            } else if (sspNode.has("system-characteristics")) {
                // XML format: root node IS the SSP (no wrapper field)
                ssp = sspNode;
                logger.info("Found SSP in XML format (root node is SSP)");
            } else {
                result.setSuccess(false);
                result.setMessage("Invalid SSP document: missing required fields");

                // Log available fields for debugging
                if (sspNode != null) {
                    StringBuilder availableFields = new StringBuilder();
                    sspNode.fieldNames().forEachRemaining(name -> availableFields.append(name).append(", "));
                    logger.error("Invalid document - Available fields: [{}]", availableFields.toString());
                }
                return result;
            }

            logger.info("SSP node extracted - has system-characteristics: {}", ssp.has("system-characteristics"));

            // Extract various sections
            extractSystemInfo(ssp, result);
            extractCategorization(ssp, result);
            extractInformationTypes(ssp, result);
            extractPersonnelRoles(ssp, result);
            extractControlStatus(ssp, result);
            extractAssets(ssp, result);

            result.setSuccess(true);
            result.setMessage("SSP analyzed successfully");
            logger.info("Successfully analyzed SSP document");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Failed to analyze SSP: " + e.getMessage());
            logger.error("SSP analysis failed: {}", e.getMessage(), e);
        }

        return result;
    }

    public ProfileVisualizationResult analyzeProfile(ProfileVisualizationRequest request, String username) {
        ProfileVisualizationResult result = new ProfileVisualizationResult();

        try {
            logger.info("Analyzing Profile - Format: {}, Content length: {}", request.getFormat(), request.getContent().length());

            // Parse the content as JSON (converting if necessary)
            JsonNode profileNode = parseContent(request.getContent(), request.getFormat());

            if (profileNode == null) {
                result.setSuccess(false);
                result.setMessage("Failed to parse profile document");
                return result;
            }

            // Handle different format structures
            JsonNode profile;
            if (profileNode.has("profile")) {
                // JSON/YAML format: root has "profile" field
                profile = profileNode.get("profile");
                logger.info("Found Profile in JSON/YAML format");
            } else if (profileNode.has("metadata")) {
                // XML format: root node IS the profile (no wrapper field)
                profile = profileNode;
                logger.info("Found Profile in XML format (root node is Profile)");
            } else {
                result.setSuccess(false);
                result.setMessage("Invalid Profile document: missing required fields");
                return result;
            }

            // Extract profile information
            extractProfileInfo(profile, result);
            extractImports(profile, result);
            extractModifications(profile, result);
            calculateControlSummary(result);

            result.setSuccess(true);
            result.setMessage("Profile analyzed successfully");
            logger.info("Successfully analyzed Profile document");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Failed to analyze Profile: " + e.getMessage());
            logger.error("Profile analysis failed: {}", e.getMessage(), e);
        }

        return result;
    }

    private void extractProfileInfo(JsonNode profile, ProfileVisualizationResult result) {
        ProfileVisualizationResult.ProfileInfo profileInfo = new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid(getString(profile, "uuid"));

        JsonNode metadata = profile.get("metadata");
        if (metadata != null) {
            profileInfo.setTitle(getString(metadata, "title"));
            profileInfo.setVersion(getString(metadata, "version"));
            profileInfo.setOscalVersion(getString(metadata, "oscal-version"));
            profileInfo.setLastModified(getString(metadata, "last-modified"));
            profileInfo.setPublished(getString(metadata, "published"));
        }

        result.setProfileInfo(profileInfo);
    }

    private void extractImports(JsonNode profile, ProfileVisualizationResult result) {
        // Check for both plural (JSON/YAML) and singular (XML) forms
        JsonNode imports = profile.get("imports");
        if (imports == null) {
            imports = profile.get("import");
        }

        if (imports == null) {
            logger.warn("No imports found in profile");
            return;
        }

        List<ProfileVisualizationResult.ImportInfo> importList = new ArrayList<>();

        for (JsonNode importNode : imports) {
            ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
            importInfo.setHref(getString(importNode, "href"));

            // Extract include-all
            JsonNode includeAll = importNode.get("include-all");
            if (includeAll != null) {
                // If include-all is present, it means all controls from the catalog
                // We'll set a special marker
                importInfo.getIncludeAllIds().add("*");
            }

            // Extract include-controls
            JsonNode includeControls = importNode.get("include-controls");
            if (includeControls == null) {
                includeControls = importNode.get("include-control");
            }
            if (includeControls != null) {
                for (JsonNode withId : includeControls) {
                    JsonNode withIds = withId.get("with-ids");
                    if (withIds == null) {
                        withIds = withId.get("with-id");
                    }
                    if (withIds != null) {
                        for (JsonNode id : withIds) {
                            importInfo.getIncludeAllIds().add(id.asText());
                        }
                    }
                }
            }

            // Extract exclude-controls
            JsonNode excludeControls = importNode.get("exclude-controls");
            if (excludeControls == null) {
                excludeControls = importNode.get("exclude-control");
            }
            if (excludeControls != null) {
                for (JsonNode withId : excludeControls) {
                    JsonNode withIds = withId.get("with-ids");
                    if (withIds == null) {
                        withIds = withId.get("with-id");
                    }
                    if (withIds != null) {
                        for (JsonNode id : withIds) {
                            importInfo.getExcludeIds().add(id.asText());
                        }
                    }
                }
            }

            importList.add(importInfo);
        }

        result.setImports(importList);
    }

    private void extractModifications(JsonNode profile, ProfileVisualizationResult result) {
        JsonNode modify = profile.get("modify");
        if (modify == null) {
            return;
        }

        ProfileVisualizationResult.ModificationSummary modSummary = new ProfileVisualizationResult.ModificationSummary();

        // Extract set-parameters
        JsonNode setParams = modify.get("set-parameters");
        if (setParams == null) {
            setParams = modify.get("set-parameter");
        }
        if (setParams != null && setParams.isArray()) {
            modSummary.setTotalSetsParameters(setParams.size());
        }

        // Extract alters
        JsonNode alters = modify.get("alters");
        if (alters == null) {
            alters = modify.get("alter");
        }
        if (alters != null && alters.isArray()) {
            modSummary.setTotalAlters(alters.size());
            List<String> modifiedIds = new ArrayList<>();
            for (JsonNode alter : alters) {
                String controlId = getString(alter, "control-id");
                if (!controlId.isEmpty()) {
                    modifiedIds.add(controlId);
                }
            }
            modSummary.setModifiedControlIds(modifiedIds);
        }

        result.setModificationSummary(modSummary);
    }

    private void calculateControlSummary(ProfileVisualizationResult result) {
        ProfileVisualizationResult.ControlSummary summary = new ProfileVisualizationResult.ControlSummary();

        // Count controls from imports
        int totalIncluded = 0;
        int totalExcluded = 0;
        Map<String, ProfileVisualizationResult.ControlFamilyInfo> familyMap = new HashMap<>();

        for (ProfileVisualizationResult.ImportInfo importInfo : result.getImports()) {
            // Process included controls
            for (String controlId : importInfo.getIncludeAllIds()) {
                if ("*".equals(controlId)) {
                    // Special marker for include-all
                    continue;
                }
                totalIncluded++;
                String familyId = extractFamilyId(controlId.toLowerCase());
                String familyName = CONTROL_FAMILIES.getOrDefault(familyId, familyId.toUpperCase());

                ProfileVisualizationResult.ControlFamilyInfo familyInfo = familyMap.computeIfAbsent(familyId, key -> {
                    ProfileVisualizationResult.ControlFamilyInfo fi = new ProfileVisualizationResult.ControlFamilyInfo();
                    fi.setFamilyId(familyId);
                    fi.setFamilyName(familyName);
                    fi.setIncludedCount(0);
                    fi.setExcludedCount(0);
                    return fi;
                });

                familyInfo.setIncludedCount(familyInfo.getIncludedCount() + 1);
                familyInfo.getIncludedControls().add(controlId);
            }

            // Process excluded controls
            for (String controlId : importInfo.getExcludeIds()) {
                totalExcluded++;
                String familyId = extractFamilyId(controlId.toLowerCase());
                String familyName = CONTROL_FAMILIES.getOrDefault(familyId, familyId.toUpperCase());

                ProfileVisualizationResult.ControlFamilyInfo familyInfo = familyMap.computeIfAbsent(familyId, key -> {
                    ProfileVisualizationResult.ControlFamilyInfo fi = new ProfileVisualizationResult.ControlFamilyInfo();
                    fi.setFamilyId(familyId);
                    fi.setFamilyName(familyName);
                    fi.setIncludedCount(0);
                    fi.setExcludedCount(0);
                    return fi;
                });

                familyInfo.setExcludedCount(familyInfo.getExcludedCount() + 1);
                familyInfo.getExcludedControls().add(controlId);
            }
        }

        summary.setTotalIncludedControls(totalIncluded);
        summary.setTotalExcludedControls(totalExcluded);
        summary.setUniqueFamilies(familyMap.size());

        if (result.getModificationSummary() != null) {
            summary.setTotalModifications(
                result.getModificationSummary().getTotalSetsParameters() +
                result.getModificationSummary().getTotalAlters()
            );
        }

        result.setControlSummary(summary);
        result.setControlsByFamily(familyMap);
    }

    private JsonNode parseContent(String content, OscalFormat format) throws Exception {
        ObjectMapper mapper;
        switch (format) {
            case JSON:
                mapper = new ObjectMapper();
                break;
            case YAML:
                mapper = new ObjectMapper(new YAMLFactory());
                break;
            case XML:
                XmlMapper xmlMapper = new XmlMapper();
                // Configure to handle XML namespaces properly
                xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper = xmlMapper;
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return mapper.readTree(content);
    }

    private void extractSystemInfo(JsonNode ssp, SspVisualizationResult result) {
        logger.info("Extracting system info - SSP node has uuid: {}, has system-characteristics: {}",
            ssp.has("uuid"), ssp.has("system-characteristics"));

        SspVisualizationResult.SystemInfo systemInfo = new SspVisualizationResult.SystemInfo();

        systemInfo.setUuid(getString(ssp, "uuid"));
        logger.info("UUID extracted: {}", systemInfo.getUuid());

        JsonNode sysChars = ssp.get("system-characteristics");
        if (sysChars != null) {
            systemInfo.setName(getString(sysChars, "system-name"));
            systemInfo.setShortName(getString(sysChars, "system-name-short"));
            systemInfo.setDescription(getString(sysChars, "description"));

            logger.info("System info extracted - name: {}, shortName: {}, description length: {}",
                systemInfo.getName(), systemInfo.getShortName(),
                systemInfo.getDescription() != null ? systemInfo.getDescription().length() : 0);

            JsonNode status = sysChars.get("status");
            if (status != null) {
                systemInfo.setStatus(getString(status, "state"));
            }

            JsonNode systemIds = sysChars.get("system-ids");
            if (systemIds != null && systemIds.isArray()) {
                List<SspVisualizationResult.SystemInfo.SystemId> ids = new ArrayList<>();
                for (JsonNode sidNode : systemIds) {
                    ids.add(new SspVisualizationResult.SystemInfo.SystemId(
                        getString(sidNode, "identifier-type"),
                        getString(sidNode, "id")
                    ));
                }
                systemInfo.setSystemIds(ids);
            }
        }

        result.setSystemInfo(systemInfo);
    }

    private void extractCategorization(JsonNode ssp, SspVisualizationResult result) {
        JsonNode sysChars = ssp.get("system-characteristics");
        if (sysChars != null) {
            JsonNode impactLevel = sysChars.get("security-impact-level");
            if (impactLevel != null) {
                SspVisualizationResult.SecurityCategorization cat = new SspVisualizationResult.SecurityCategorization();
                cat.setConfidentiality(getString(impactLevel, "security-objective-confidentiality"));
                cat.setIntegrity(getString(impactLevel, "security-objective-integrity"));
                cat.setAvailability(getString(impactLevel, "security-objective-availability"));
                cat.setOverall(getString(sysChars, "security-sensitivity-level"));
                result.setCategorization(cat);
            }
        }
    }

    private void extractInformationTypes(JsonNode ssp, SspVisualizationResult result) {
        JsonNode sysChars = ssp.get("system-characteristics");
        if (sysChars != null) {
            JsonNode sysInfo = sysChars.get("system-information");
            if (sysInfo != null) {
                JsonNode infoTypes = sysInfo.get("information-types");
                if (infoTypes != null && infoTypes.isArray()) {
                    List<SspVisualizationResult.InformationType> types = new ArrayList<>();
                    for (JsonNode itNode : infoTypes) {
                        SspVisualizationResult.InformationType infoType = new SspVisualizationResult.InformationType();
                        infoType.setUuid(getString(itNode, "uuid"));
                        infoType.setTitle(getString(itNode, "title"));
                        infoType.setDescription(getString(itNode, "description"));

                        // Get categorizations
                        JsonNode cats = itNode.get("categorizations");
                        if (cats != null && cats.isArray()) {
                            List<String> catList = new ArrayList<>();
                            for (JsonNode catNode : cats) {
                                JsonNode idList = catNode.get("information-type-ids");
                                if (idList != null && idList.isArray()) {
                                    for (JsonNode id : idList) {
                                        catList.add(id.asText());
                                    }
                                }
                            }
                            infoType.setCategorizations(catList);
                        }

                        // Get impact levels
                        JsonNode confImpact = itNode.get("confidentiality-impact");
                        if (confImpact != null) {
                            infoType.setConfidentiality(new SspVisualizationResult.InformationType.ImpactLevel(
                                getString(confImpact, "base"),
                                getString(confImpact, "selected", getString(confImpact, "base"))
                            ));
                        }

                        JsonNode intImpact = itNode.get("integrity-impact");
                        if (intImpact != null) {
                            infoType.setIntegrity(new SspVisualizationResult.InformationType.ImpactLevel(
                                getString(intImpact, "base"),
                                getString(intImpact, "selected", getString(intImpact, "base"))
                            ));
                        }

                        JsonNode availImpact = itNode.get("availability-impact");
                        if (availImpact != null) {
                            infoType.setAvailability(new SspVisualizationResult.InformationType.ImpactLevel(
                                getString(availImpact, "base"),
                                getString(availImpact, "selected", getString(availImpact, "base"))
                            ));
                        }

                        types.add(infoType);
                    }
                    result.setInformationTypes(types);
                }
            }
        }
    }

    private void extractPersonnelRoles(JsonNode ssp, SspVisualizationResult result) {
        JsonNode metadata = ssp.get("metadata");
        if (metadata == null) return;

        // Build party map
        Map<String, JsonNode> partyMap = new HashMap<>();
        JsonNode parties = metadata.get("parties");
        if (parties != null && parties.isArray()) {
            for (JsonNode party : parties) {
                partyMap.put(getString(party, "uuid"), party);
            }
        }

        // Build role to party mapping
        Map<String, List<String>> roleToPartyMap = new HashMap<>();
        JsonNode respParties = metadata.get("responsible-parties");
        if (respParties != null && respParties.isArray()) {
            for (JsonNode rp : respParties) {
                String roleId = getString(rp, "role-id");
                List<String> partyUuids = new ArrayList<>();
                JsonNode partyUuidArray = rp.get("party-uuids");
                if (partyUuidArray != null && partyUuidArray.isArray()) {
                    for (JsonNode uuid : partyUuidArray) {
                        partyUuids.add(uuid.asText());
                    }
                }
                roleToPartyMap.put(roleId, partyUuids);
            }
        }

        // Build personnel roles
        List<SspVisualizationResult.PersonnelRole> personnel = new ArrayList<>();
        JsonNode roles = metadata.get("roles");
        if (roles != null && roles.isArray()) {
            for (JsonNode roleNode : roles) {
                SspVisualizationResult.PersonnelRole personnelRole = new SspVisualizationResult.PersonnelRole();
                String roleId = getString(roleNode, "id");
                personnelRole.setRoleId(roleId);
                personnelRole.setRoleTitle(getString(roleNode, "title"));
                personnelRole.setRoleShortName(getString(roleNode, "short-name"));

                // Get assigned personnel
                List<SspVisualizationResult.PersonnelRole.Person> assignedPersonnel = new ArrayList<>();
                List<String> assignedUuids = roleToPartyMap.getOrDefault(roleId, new ArrayList<>());
                for (String uuid : assignedUuids) {
                    JsonNode party = partyMap.get(uuid);
                    if (party != null) {
                        String jobTitle = "";
                        JsonNode props = party.get("props");
                        if (props != null && props.isArray()) {
                            for (JsonNode prop : props) {
                                if ("job-title".equals(getString(prop, "name"))) {
                                    jobTitle = getString(prop, "value");
                                    break;
                                }
                            }
                        }

                        assignedPersonnel.add(new SspVisualizationResult.PersonnelRole.Person(
                            uuid,
                            getString(party, "name"),
                            jobTitle,
                            getString(party, "type")
                        ));
                    }
                }

                personnelRole.setAssignedPersonnel(assignedPersonnel);
                personnel.add(personnelRole);
            }
        }

        result.setPersonnel(personnel);
    }

    private void extractControlStatus(JsonNode ssp, SspVisualizationResult result) {
        logger.info("Extracting control status - SSP has control-implementation: {}", ssp.has("control-implementation"));

        JsonNode controlImpl = ssp.get("control-implementation");
        if (controlImpl == null) {
            logger.warn("No control-implementation node found");
            // Log all available fields at SSP level
            StringBuilder sspFields = new StringBuilder();
            ssp.fieldNames().forEachRemaining(name -> sspFields.append(name).append(", "));
            logger.info("Available SSP fields: [{}]", sspFields.toString());
            return;
        }

        logger.info("Control implementation node found - has implemented-requirements: {}", controlImpl.has("implemented-requirements"));

        // Check for both plural (JSON/YAML) and singular (XML) forms
        JsonNode impReqs = controlImpl.get("implemented-requirements");
        if (impReqs == null) {
            impReqs = controlImpl.get("implemented-requirement");
            if (impReqs != null) {
                logger.info("Found implemented-requirement (XML singular form)");
            }
        } else {
            logger.info("Found implemented-requirements (JSON/YAML plural form)");
        }

        if (impReqs == null) {
            logger.warn("No implemented-requirements or implemented-requirement found");
            // Log all available fields in control-implementation
            StringBuilder ciFields = new StringBuilder();
            controlImpl.fieldNames().forEachRemaining(name -> ciFields.append(name).append(", "));
            logger.info("Available control-implementation fields: [{}]", ciFields.toString());
            return;
        }

        // Handle both single object and array
        if (!impReqs.isArray()) {
            logger.info("Converting single implemented-requirement to array");
            // Wrap single object in array-like processing
        }

        logger.info("Found {} implemented requirements", impReqs.size());

        Map<String, SspVisualizationResult.ControlFamilyStatus> familyMap = new HashMap<>();

        for (JsonNode req : impReqs) {
            String controlId = getString(req, "control-id", "").toLowerCase();
            String familyId = extractFamilyId(controlId);
            String familyName = CONTROL_FAMILIES.getOrDefault(familyId, familyId.toUpperCase());

            // Get or create family status
            SspVisualizationResult.ControlFamilyStatus familyStatus = familyMap.computeIfAbsent(familyId, key -> {
                SspVisualizationResult.ControlFamilyStatus fs = new SspVisualizationResult.ControlFamilyStatus();
                fs.setFamilyId(familyId);
                fs.setFamilyName(familyName);
                fs.setTotalControls(0);
                fs.setStatusCounts(new HashMap<>());
                fs.setControls(new ArrayList<>());
                return fs;
            });

            // Extract implementation status
            String implementationStatus = "unknown";
            String controlOrigination = "unknown";

            // Check for both plural (JSON/YAML) and singular (XML) forms
            JsonNode props = req.get("props");
            if (props == null) {
                props = req.get("prop");
                if (props != null) {
                    logger.info("Found 'prop' field for control {}", getString(req, "control-id"));
                }
            } else {
                logger.info("Found 'props' field for control {}", getString(req, "control-id"));
            }

            if (props != null) {
                logger.info("Props node isArray: {}, isObject: {}, for control {}",
                    props.isArray(), props.isObject(), getString(req, "control-id"));

                if (props.isArray()) {
                    logger.info("Processing {} props for control {}", props.size(), getString(req, "control-id"));
                    for (JsonNode prop : props) {
                        String propName = getString(prop, "name");
                        String propValue = getString(prop, "value");
                        logger.info("Control {}: prop name='{}', value='{}'",
                            getString(req, "control-id"), propName, propValue);

                        if ("implementation-status".equals(propName)) {
                            implementationStatus = propValue;
                        } else if ("control-origination".equals(propName)) {
                            controlOrigination = propValue;
                        }
                    }
                }
            } else {
                logger.warn("No props found for control {}", getString(req, "control-id"));
            }

            // Add control status
            familyStatus.getControls().add(new SspVisualizationResult.ControlFamilyStatus.ControlStatus(
                getString(req, "control-id"),
                implementationStatus,
                controlOrigination
            ));

            // Update counts
            familyStatus.setTotalControls(familyStatus.getTotalControls() + 1);
            familyStatus.getStatusCounts().merge(implementationStatus, 1, Integer::sum);
        }

        result.setControlsByFamily(familyMap);
    }

    private void extractAssets(JsonNode ssp, SspVisualizationResult result) {
        logger.info("Extracting assets - SSP has system-implementation: {}", ssp.has("system-implementation"));

        JsonNode sysImpl = ssp.get("system-implementation");
        if (sysImpl == null) {
            logger.warn("No system-implementation node found");
            // Log all available fields at SSP level
            StringBuilder sspFields = new StringBuilder();
            ssp.fieldNames().forEachRemaining(name -> sspFields.append(name).append(", "));
            logger.info("Available SSP fields for assets: [{}]", sspFields.toString());
            return;
        }

        logger.info("System implementation node found - has inventory-items: {}", sysImpl.has("inventory-items"));

        // Check for both plural (JSON/YAML) and singular (XML) forms
        JsonNode invItems = sysImpl.get("inventory-items");
        if (invItems == null) {
            invItems = sysImpl.get("inventory-item");
            if (invItems != null) {
                logger.info("Found inventory-item (XML singular form)");
            }
        } else {
            logger.info("Found inventory-items (JSON/YAML plural form)");
        }

        if (invItems == null) {
            logger.warn("No inventory-items or inventory-item found");
            // Log all available fields in system-implementation
            StringBuilder siFields = new StringBuilder();
            sysImpl.fieldNames().forEachRemaining(name -> siFields.append(name).append(", "));
            logger.info("Available system-implementation fields: [{}]", siFields.toString());
            return;
        }

        // Handle both single object and array
        if (!invItems.isArray()) {
            logger.info("Converting single inventory-item to array");
            // Wrap single object in array-like processing
        }

        logger.info("Found {} inventory items", invItems.size());

        List<SspVisualizationResult.Asset> assets = new ArrayList<>();
        for (JsonNode item : invItems) {
            SspVisualizationResult.Asset asset = new SspVisualizationResult.Asset();
            asset.setUuid(getString(item, "uuid"));
            asset.setDescription(getString(item, "description"));

            // Check for both plural (JSON/YAML) and singular (XML) forms
            JsonNode props = item.get("props");
            if (props == null) {
                props = item.get("prop");
            }

            if (props != null && props.isArray()) {
                for (JsonNode prop : props) {
                    String name = getString(prop, "name");
                    String value = getString(prop, "value");

                    switch (name) {
                        case "asset-type":
                            asset.setAssetType(value);
                            break;
                        case "function":
                            asset.setFunction(value);
                            break;
                        case "fqdn":
                            asset.setFqdn(value);
                            break;
                        case "ipv4-address":
                            asset.setIpv4Address(value);
                            break;
                        case "ipv6-address":
                            asset.setIpv6Address(value);
                            break;
                        case "mac-address":
                            asset.setMacAddress(value);
                            break;
                        case "virtual":
                            asset.setVirtual("yes".equalsIgnoreCase(value));
                            break;
                        case "public":
                            asset.setPublicAccess("yes".equalsIgnoreCase(value));
                            break;
                        case "software-name":
                            asset.setSoftwareName(value);
                            break;
                        case "software-version":
                            asset.setSoftwareVersion(value);
                            break;
                        case "vendor-name":
                            asset.setVendorName(value);
                            break;
                        case "is-scanned":
                            asset.setScanned("yes".equalsIgnoreCase(value));
                            break;
                    }
                }
            }

            assets.add(asset);
        }

        result.setAssets(assets);
    }

    private String extractFamilyId(String controlId) {
        // Extract family identifier from control ID
        // Supports multiple formats:
        // - Standard NIST format: "ac-1" -> "ac"
        // - CMMC format: "AC.L1-3.1.1" -> "ac"
        // - Dotted format: "ac.1" -> "ac"

        String lowerControlId = controlId.toLowerCase();

        // Check for dot (CMMC format like "AC.L1-3.1.1")
        int dotIndex = lowerControlId.indexOf('.');
        if (dotIndex > 0) {
            return lowerControlId.substring(0, dotIndex);
        }

        // Check for dash (standard format like "ac-1")
        int dashIndex = lowerControlId.indexOf('-');
        if (dashIndex > 0) {
            return lowerControlId.substring(0, dashIndex);
        }

        // For control IDs without a separator, take the first two characters
        return lowerControlId.length() >= 2 ? lowerControlId.substring(0, 2) : lowerControlId;
    }

    private String getString(JsonNode node, String field) {
        return getString(node, field, "");
    }

    public SarVisualizationResult analyzeSAR(SarVisualizationRequest request, String username) {
        SarVisualizationResult result = new SarVisualizationResult();

        try {
            logger.info("Analyzing SAR - Format: {}, Content length: {}", request.getFormat(), request.getContent().length());

            // Parse the content as JSON (converting if necessary)
            JsonNode sarNode = parseContent(request.getContent(), request.getFormat());

            if (sarNode == null) {
                result.setSuccess(false);
                result.setMessage("Failed to parse SAR document");
                return result;
            }

            // Handle different format structures
            JsonNode sar;
            if (sarNode.has("assessment-results")) {
                // JSON/YAML format: root has "assessment-results" field
                sar = sarNode.get("assessment-results");
                logger.info("Found SAR in JSON/YAML format");
            } else if (sarNode.has("metadata")) {
                // XML format: root node IS the assessment-results (no wrapper field)
                sar = sarNode;
                logger.info("Found SAR in XML format (root node is assessment-results)");
            } else {
                result.setSuccess(false);
                result.setMessage("Invalid SAR document: missing required fields");

                // Log available fields for debugging
                if (sarNode != null) {
                    StringBuilder availableFields = new StringBuilder();
                    sarNode.fieldNames().forEachRemaining(name -> availableFields.append(name).append(", "));
                    logger.error("Invalid document - Available fields: [{}]", availableFields.toString());
                }
                return result;
            }

            // Extract various sections
            extractAssessmentInfo(sar, result);
            extractResults(sar, result);
            calculateAssessmentSummary(result);

            result.setSuccess(true);
            result.setMessage("SAR analyzed successfully");
            logger.info("Successfully analyzed SAR document");

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Failed to analyze SAR: " + e.getMessage());
            logger.error("SAR analysis failed: {}", e.getMessage(), e);
        }

        return result;
    }

    private void extractAssessmentInfo(JsonNode sar, SarVisualizationResult result) {
        SarVisualizationResult.AssessmentInfo assessmentInfo = new SarVisualizationResult.AssessmentInfo();

        assessmentInfo.setUuid(getString(sar, "uuid"));

        JsonNode metadata = sar.get("metadata");
        if (metadata != null) {
            assessmentInfo.setTitle(getString(metadata, "title"));
            assessmentInfo.setVersion(getString(metadata, "version"));
            assessmentInfo.setOscalVersion(getString(metadata, "oscal-version"));
            assessmentInfo.setLastModified(getString(metadata, "last-modified"));
            assessmentInfo.setPublished(getString(metadata, "published"));
        }

        // Extract import-ssp href
        JsonNode importSsp = sar.get("import-ssp");
        if (importSsp != null) {
            assessmentInfo.setSspImportHref(getString(importSsp, "href"));
        }

        result.setAssessmentInfo(assessmentInfo);
    }

    private void extractResults(JsonNode sar, SarVisualizationResult result) {
        // Check for both plural (JSON/YAML) and singular (XML) forms
        JsonNode results = sar.get("results");
        if (results == null) {
            results = sar.get("result");
        }

        if (results == null) {
            logger.warn("No results found in SAR");
            return;
        }

        // Results can be an array, iterate through them
        for (JsonNode resultNode : results) {
            // Extract reviewed-controls
            extractReviewedControls(resultNode, result);

            // Extract observations
            extractObservations(resultNode, result);

            // Extract findings
            extractFindings(resultNode, result);

            // Extract risks
            extractRisks(resultNode, result);
        }
    }

    private void extractReviewedControls(JsonNode resultNode, SarVisualizationResult result) {
        JsonNode reviewedControls = resultNode.get("reviewed-controls");
        if (reviewedControls == null) {
            reviewedControls = resultNode.get("reviewed-control");
        }

        if (reviewedControls == null) {
            return;
        }

        // Get control-selections
        JsonNode controlSelections = reviewedControls.get("control-selections");
        if (controlSelections == null) {
            controlSelections = reviewedControls.get("control-selection");
        }

        if (controlSelections == null) {
            return;
        }

        Map<String, SarVisualizationResult.ControlFamilyAssessment> familyMap = new HashMap<>();

        for (JsonNode selection : controlSelections) {
            // Get include-controls
            JsonNode includeControls = selection.get("include-controls");
            if (includeControls == null) {
                includeControls = selection.get("include-control");
            }

            if (includeControls != null) {
                for (JsonNode includeControl : includeControls) {
                    // Check for statement-ids (SAR format) or with-ids (other formats)
                    JsonNode ids = includeControl.get("statement-ids");
                    if (ids == null) {
                        ids = includeControl.get("statement-id");
                    }
                    if (ids == null) {
                        ids = includeControl.get("with-ids");
                    }
                    if (ids == null) {
                        ids = includeControl.get("with-id");
                    }

                    if (ids != null) {
                        for (JsonNode idNode : ids) {
                            String controlId = idNode.asText().toLowerCase();
                            String familyId = extractFamilyId(controlId);
                            String familyName = CONTROL_FAMILIES.getOrDefault(familyId, familyId.toUpperCase());

                            // Get or create family assessment
                            SarVisualizationResult.ControlFamilyAssessment familyAssessment = familyMap.computeIfAbsent(familyId, key -> {
                                SarVisualizationResult.ControlFamilyAssessment fa = new SarVisualizationResult.ControlFamilyAssessment();
                                fa.setFamilyId(familyId);
                                fa.setFamilyName(familyName);
                                fa.setTotalControlsAssessed(0);
                                fa.setTotalFindings(0);
                                fa.setTotalObservations(0);
                                fa.setAssessedControls(new ArrayList<>());
                                return fa;
                            });

                            // Add assessed control
                            familyAssessment.setTotalControlsAssessed(familyAssessment.getTotalControlsAssessed() + 1);
                            familyAssessment.getAssessedControls().add(new SarVisualizationResult.ControlFamilyAssessment.AssessedControl(
                                controlId,
                                0,  // Will be updated when processing findings
                                0,  // Will be updated when processing observations
                                "assessed"
                            ));
                        }
                    }
                }
            }
        }

        result.setControlsByFamily(familyMap);
    }

    private void extractObservations(JsonNode resultNode, SarVisualizationResult result) {
        JsonNode observations = resultNode.get("observations");
        if (observations == null) {
            observations = resultNode.get("observation");
        }

        if (observations == null) {
            return;
        }

        List<SarVisualizationResult.Observation> observationList = new ArrayList<>();

        for (JsonNode obsNode : observations) {
            SarVisualizationResult.Observation observation = new SarVisualizationResult.Observation();
            observation.setUuid(getString(obsNode, "uuid"));
            observation.setTitle(getString(obsNode, "title"));
            observation.setDescription(getString(obsNode, "description"));

            // Extract observation type from types array (OSCAL 1.1+ format)
            JsonNode types = obsNode.get("types");
            if (types == null) {
                types = obsNode.get("type");
            }

            if (types != null && types.isArray() && types.size() > 0) {
                // Use the first type from the array
                observation.setObservationType(types.get(0).asText());
            } else {
                // Fallback: try to find type in props (older format)
                JsonNode props = obsNode.get("props");
                if (props == null) {
                    props = obsNode.get("prop");
                }

                if (props != null && props.isArray()) {
                    for (JsonNode prop : props) {
                        if ("type".equals(getString(prop, "name"))) {
                            observation.setObservationType(getString(prop, "value"));
                            break;
                        }
                    }
                }
            }

            // Extract related controls and scores from props
            JsonNode props = obsNode.get("props");
            if (props == null) {
                props = obsNode.get("prop");
            }

            if (props != null && props.isArray()) {
                for (JsonNode prop : props) {
                    String propName = getString(prop, "name");
                    String propValue = getString(prop, "value");

                    if ("control-id".equals(propName)) {
                        if (propValue != null && !propValue.isEmpty()) {
                            observation.getRelatedControls().add(propValue);
                        }
                    } else if ("overall-score".equals(propName)) {
                        try {
                            observation.setOverallScore(Double.parseDouble(propValue));
                        } catch (NumberFormatException e) {
                            logger.warn("Failed to parse overall-score '{}' for observation {}", propValue, observation.getUuid());
                        }
                    } else if ("quality-score".equals(propName)) {
                        try {
                            observation.setQualityScore(Double.parseDouble(propValue));
                        } catch (NumberFormatException e) {
                            logger.warn("Failed to parse quality-score '{}' for observation {}", propValue, observation.getUuid());
                        }
                    } else if ("completeness-score".equals(propName)) {
                        try {
                            observation.setCompletenessScore(Double.parseDouble(propValue));
                        } catch (NumberFormatException e) {
                            logger.warn("Failed to parse completeness-score '{}' for observation {}", propValue, observation.getUuid());
                        }
                    }
                }
            }

            // Also check relevant-evidence for control references
            JsonNode relevantEvidence = obsNode.get("relevant-evidence");
            if (relevantEvidence != null && relevantEvidence.isArray()) {
                for (JsonNode evidence : relevantEvidence) {
                    String href = getString(evidence, "href");
                    if (href != null && href.startsWith("#")) {
                        observation.getRelatedControls().add(href.substring(1));
                    }
                }
            }

            // Update control family counts
            for (String controlId : observation.getRelatedControls()) {
                updateControlObservationCount(result, controlId);
            }

            observationList.add(observation);
        }

        result.setObservations(observationList);
    }

    private void extractFindings(JsonNode resultNode, SarVisualizationResult result) {
        JsonNode findings = resultNode.get("findings");
        if (findings == null) {
            findings = resultNode.get("finding");
        }

        if (findings == null) {
            return;
        }

        // Build maps of observation UUID -> scores from already extracted observations
        Map<String, Double> observationScores = new HashMap<>();
        Map<String, Double> observationQualityScores = new HashMap<>();
        Map<String, Double> observationCompletenessScores = new HashMap<>();
        for (SarVisualizationResult.Observation obs : result.getObservations()) {
            if (obs.getOverallScore() != null) {
                observationScores.put(obs.getUuid(), obs.getOverallScore());
            }
            if (obs.getQualityScore() != null) {
                observationQualityScores.put(obs.getUuid(), obs.getQualityScore());
            }
            if (obs.getCompletenessScore() != null) {
                observationCompletenessScores.put(obs.getUuid(), obs.getCompletenessScore());
            }
        }

        List<SarVisualizationResult.Finding> findingList = new ArrayList<>();

        for (JsonNode findingNode : findings) {
            SarVisualizationResult.Finding finding = new SarVisualizationResult.Finding();
            finding.setUuid(getString(findingNode, "uuid"));
            finding.setTitle(getString(findingNode, "title"));
            finding.setDescription(getString(findingNode, "description"));

            // Extract related controls
            JsonNode relatedObservations = findingNode.get("related-observations");
            if (relatedObservations == null) {
                relatedObservations = findingNode.get("related-observation");
            }

            List<Double> relatedScores = new ArrayList<>();
            List<Double> relatedQualityScores = new ArrayList<>();
            List<Double> relatedCompletenessScores = new ArrayList<>();
            if (relatedObservations != null && relatedObservations.isArray()) {
                for (JsonNode relObs : relatedObservations) {
                    String observationUuid = getString(relObs, "observation-uuid");
                    if (observationUuid != null) {
                        finding.getRelatedObservations().add(observationUuid);

                        // Collect scores from related observation
                        if (observationScores.containsKey(observationUuid)) {
                            relatedScores.add(observationScores.get(observationUuid));
                        }
                        if (observationQualityScores.containsKey(observationUuid)) {
                            relatedQualityScores.add(observationQualityScores.get(observationUuid));
                        }
                        if (observationCompletenessScores.containsKey(observationUuid)) {
                            relatedCompletenessScores.add(observationCompletenessScores.get(observationUuid));
                        }
                    }
                }
            }

            // Calculate average scores from related observations
            if (!relatedScores.isEmpty()) {
                double sum = 0.0;
                for (Double score : relatedScores) {
                    sum += score;
                }
                finding.setScore(sum / relatedScores.size());
            }
            if (!relatedQualityScores.isEmpty()) {
                double sum = 0.0;
                for (Double score : relatedQualityScores) {
                    sum += score;
                }
                finding.setQualityScore(sum / relatedQualityScores.size());
            }
            if (!relatedCompletenessScores.isEmpty()) {
                double sum = 0.0;
                for (Double score : relatedCompletenessScores) {
                    sum += score;
                }
                finding.setCompletenessScore(sum / relatedCompletenessScores.size());
            }

            // Extract target controls
            JsonNode target = findingNode.get("target");
            if (target != null) {
                String targetType = getString(target, "type");
                if ("objective-id".equals(targetType)) {
                    JsonNode targetId = target.get("target-id");
                    if (targetId != null) {
                        String controlId = targetId.asText();
                        finding.getRelatedControls().add(controlId);
                        updateControlFindingCount(result, controlId);
                    }
                }
            }

            findingList.add(finding);
        }

        result.setFindings(findingList);
    }

    private void extractRisks(JsonNode resultNode, SarVisualizationResult result) {
        JsonNode risks = resultNode.get("risks");
        if (risks == null) {
            risks = resultNode.get("risk");
        }

        if (risks == null) {
            return;
        }

        List<SarVisualizationResult.Risk> riskList = new ArrayList<>();

        for (JsonNode riskNode : risks) {
            SarVisualizationResult.Risk risk = new SarVisualizationResult.Risk();
            risk.setUuid(getString(riskNode, "uuid"));
            risk.setTitle(getString(riskNode, "title"));
            risk.setDescription(getString(riskNode, "description"));
            risk.setStatus(getString(riskNode, "status"));

            // Extract related controls from characterization
            JsonNode characterization = riskNode.get("characterization");
            if (characterization != null) {
                JsonNode facets = characterization.get("facets");
                if (facets == null) {
                    facets = characterization.get("facet");
                }

                if (facets != null && facets.isArray()) {
                    for (JsonNode facet : facets) {
                        if ("control-id".equals(getString(facet, "name"))) {
                            String controlId = getString(facet, "value");
                            if (controlId != null) {
                                risk.getRelatedControls().add(controlId);
                            }
                        }
                    }
                }
            }

            riskList.add(risk);
        }

        result.setRisks(riskList);
    }

    private void updateControlObservationCount(SarVisualizationResult result, String controlId) {
        String familyId = extractFamilyId(controlId.toLowerCase());
        SarVisualizationResult.ControlFamilyAssessment familyAssessment = result.getControlsByFamily().get(familyId);

        if (familyAssessment != null) {
            familyAssessment.setTotalObservations(familyAssessment.getTotalObservations() + 1);

            for (SarVisualizationResult.ControlFamilyAssessment.AssessedControl control : familyAssessment.getAssessedControls()) {
                if (control.getControlId().equalsIgnoreCase(controlId)) {
                    control.setObservationsCount(control.getObservationsCount() + 1);
                    break;
                }
            }
        }
    }

    private void updateControlFindingCount(SarVisualizationResult result, String controlId) {
        String familyId = extractFamilyId(controlId.toLowerCase());
        SarVisualizationResult.ControlFamilyAssessment familyAssessment = result.getControlsByFamily().get(familyId);

        if (familyAssessment != null) {
            familyAssessment.setTotalFindings(familyAssessment.getTotalFindings() + 1);

            for (SarVisualizationResult.ControlFamilyAssessment.AssessedControl control : familyAssessment.getAssessedControls()) {
                if (control.getControlId().equalsIgnoreCase(controlId)) {
                    control.setFindingsCount(control.getFindingsCount() + 1);
                    break;
                }
            }
        }
    }

    private void calculateAssessmentSummary(SarVisualizationResult result) {
        SarVisualizationResult.AssessmentSummary summary = new SarVisualizationResult.AssessmentSummary();

        // Count total controls assessed
        int totalControls = 0;
        for (SarVisualizationResult.ControlFamilyAssessment family : result.getControlsByFamily().values()) {
            totalControls += family.getTotalControlsAssessed();
        }
        summary.setTotalControlsAssessed(totalControls);

        // Set counts from collected data
        summary.setTotalFindings(result.getFindings().size());
        summary.setTotalObservations(result.getObservations().size());
        summary.setTotalRisks(result.getRisks().size());
        summary.setUniqueFamiliesAssessed(result.getControlsByFamily().size());

        // Calculate findings by severity (simplified - would need to extract from props)
        summary.getFindingsBySeverity().put("total", result.getFindings().size());

        // Calculate observations by type
        Map<String, Integer> obsByType = new HashMap<>();
        for (SarVisualizationResult.Observation obs : result.getObservations()) {
            String type = obs.getObservationType() != null ? obs.getObservationType() : "unknown";
            obsByType.merge(type, 1, Integer::sum);
        }
        summary.setObservationsByType(obsByType);

        // Calculate score distribution in 10-point buckets (0-10, 10-20, etc.)
        Map<String, Integer> scoreDistribution = new HashMap<>();
        // Initialize all buckets to 0
        scoreDistribution.put("0-10", 0);
        scoreDistribution.put("10-20", 0);
        scoreDistribution.put("20-30", 0);
        scoreDistribution.put("30-40", 0);
        scoreDistribution.put("40-50", 0);
        scoreDistribution.put("50-60", 0);
        scoreDistribution.put("60-70", 0);
        scoreDistribution.put("70-80", 0);
        scoreDistribution.put("80-90", 0);
        scoreDistribution.put("90-100", 0);

        for (SarVisualizationResult.Observation obs : result.getObservations()) {
            if (obs.getOverallScore() != null) {
                double score = obs.getOverallScore();
                String bucket;

                if (score < 10) bucket = "0-10";
                else if (score < 20) bucket = "10-20";
                else if (score < 30) bucket = "20-30";
                else if (score < 40) bucket = "30-40";
                else if (score < 50) bucket = "40-50";
                else if (score < 60) bucket = "50-60";
                else if (score < 70) bucket = "60-70";
                else if (score < 80) bucket = "70-80";
                else if (score < 90) bucket = "80-90";
                else bucket = "90-100";

                scoreDistribution.merge(bucket, 1, Integer::sum);
            }
        }
        summary.setScoreDistribution(scoreDistribution);

        // Calculate risks by severity
        Map<String, Integer> risksBySev = new HashMap<>();
        for (SarVisualizationResult.Risk risk : result.getRisks()) {
            String status = risk.getStatus() != null ? risk.getStatus() : "unknown";
            risksBySev.merge(status, 1, Integer::sum);
        }
        summary.setRisksBySeverity(risksBySev);

        result.setAssessmentSummary(summary);
    }

    private String getString(JsonNode node, String field, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null) return defaultValue;
        return fieldNode.asText(defaultValue);
    }
}
