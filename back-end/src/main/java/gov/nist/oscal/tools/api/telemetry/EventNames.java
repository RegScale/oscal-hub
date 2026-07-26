package gov.nist.oscal.tools.api.telemetry;

public final class EventNames {
    private EventNames() {}

    public static final String AUTH_LOGIN_SUCCEEDED       = "auth.login_succeeded";
    public static final String AUTH_LOGIN_FAILED          = "auth.login_failed";
    public static final String AUTH_SESSION_STARTED       = "auth.session_started";
    public static final String AUTH_SESSION_ENDED         = "auth.session_ended";

    // Onboarding funnel (resilience plan, Phase 5): registration → invitation /
    // access request → membership. Emitted so drop-off between stages is measurable.
    public static final String AUTH_REGISTER_SUCCEEDED    = "auth.register_succeeded";
    public static final String INVITATION_CREATED         = "invitation.created";
    public static final String INVITATION_ACCEPTED        = "invitation.accepted";
    public static final String ACCESS_REQUEST_SUBMITTED   = "access_request.submitted";
    public static final String ACCESS_REQUEST_APPROVED    = "access_request.approved";

    public static final String FEATURE_VIEWED             = "feature.viewed";

    public static final String OSCAL_VALIDATE             = "oscal.validate";
    public static final String OSCAL_CONVERT              = "oscal.convert";
    public static final String OSCAL_RESOLVE              = "oscal.resolve";
    public static final String OSCAL_BATCH_SUBMITTED      = "oscal.batch_submitted";
    public static final String OSCAL_BATCH_COMPLETED      = "oscal.batch_completed";

    public static final String LIBRARY_ITEM_UPLOADED      = "library.item_uploaded";
    public static final String LIBRARY_ITEM_DOWNLOADED    = "library.item_downloaded";
    public static final String LIBRARY_ITEM_DELETED       = "library.item_deleted";

    public static final String AUTHORIZATION_CREATED      = "authorization.template_created";
    public static final String AUTHORIZATION_APPROVED     = "authorization.template_approved";
    public static final String AUTHORIZATION_REJECTED     = "authorization.template_rejected";

    public static final String ARTIFACT_UPLOADED          = "artifact.uploaded";
    public static final String ARTIFACT_DOWNLOADED        = "artifact.downloaded";

    public static final String ADMIN_USER_INVITED         = "admin.user_invited";
    public static final String ADMIN_USER_ROLE_CHANGED    = "admin.user_role_changed";
    public static final String ADMIN_USER_DEACTIVATED     = "admin.user_deactivated";

    public static final String ERROR_UNHANDLED            = "error.unhandled";
    public static final String ERROR_FRONTEND_JS          = "error.frontend_js";
}
