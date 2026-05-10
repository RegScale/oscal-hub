package gov.nist.oscal.tools.api.entity;

/**
 * Enumerates the OSCAL model types that share the unified {@link OscalDocument}
 * storage. Catalog, Profile, and Component Definition each have their own
 * dedicated entities and are intentionally not part of this enum.
 */
public enum OscalModelType {
    SYSTEM_SECURITY_PLAN,
    ASSESSMENT_PLAN,
    ASSESSMENT_RESULTS,
    PLAN_OF_ACTION_AND_MILESTONES;

    /** Lowercase, hyphenated name used in URLs and JSON. */
    public String slug() {
        switch (this) {
            case SYSTEM_SECURITY_PLAN: return "system-security-plan";
            case ASSESSMENT_PLAN: return "assessment-plan";
            case ASSESSMENT_RESULTS: return "assessment-results";
            case PLAN_OF_ACTION_AND_MILESTONES: return "plan-of-action-and-milestones";
            default: throw new IllegalStateException("Unknown model type: " + this);
        }
    }

    public static OscalModelType fromSlug(String slug) {
        if (slug == null) throw new IllegalArgumentException("modelType is required");
        switch (slug.toLowerCase()) {
            case "system-security-plan":
            case "ssp":
                return SYSTEM_SECURITY_PLAN;
            case "assessment-plan":
            case "ap":
                return ASSESSMENT_PLAN;
            case "assessment-results":
            case "ar":
                return ASSESSMENT_RESULTS;
            case "plan-of-action-and-milestones":
            case "poam":
            case "poa-and-m":
                return PLAN_OF_ACTION_AND_MILESTONES;
            default:
                throw new IllegalArgumentException("Unknown OSCAL model type: " + slug);
        }
    }
}
