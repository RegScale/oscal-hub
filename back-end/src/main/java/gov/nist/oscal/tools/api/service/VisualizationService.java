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
        // Extract family identifier from control ID (e.g., "ac-1" -> "ac")
        int dashIndex = controlId.indexOf('-');
        if (dashIndex > 0) {
            return controlId.substring(0, dashIndex);
        }
        // For control IDs without a dash, take the first two characters
        return controlId.length() >= 2 ? controlId.substring(0, 2) : controlId;
    }

    private String getString(JsonNode node, String field) {
        return getString(node, field, "");
    }

    private String getString(JsonNode node, String field, String defaultValue) {
        if (node == null) return defaultValue;
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null) return defaultValue;
        return fieldNode.asText(defaultValue);
    }
}
