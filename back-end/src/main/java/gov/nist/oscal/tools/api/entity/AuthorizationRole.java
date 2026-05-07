package gov.nist.oscal.tools.api.entity;

/**
 * Per-authorization role granted to a user.
 * <ul>
 *   <li>{@code OWNER} — full control: edit, share, delete.</li>
 *   <li>{@code EDITOR} — edit content, conditions, signature; cannot manage grants or delete.</li>
 *   <li>{@code CONTRIBUTOR} — upload ConMon / Documents; cannot edit core authorization.</li>
 *   <li>{@code VIEWER} — read-only.</li>
 * </ul>
 *
 * The creator (Authorization.authorizedBy) is implicitly OWNER. Org admins and
 * SUPER_ADMINs bypass this enum and are treated as effective OWNER.
 */
public enum AuthorizationRole {
    OWNER,
    EDITOR,
    CONTRIBUTOR,
    VIEWER;

    /**
     * Roles that may be assigned via the share-with-org convenience setting.
     * OWNER is intentionally excluded — see V1.7 CHECK constraint.
     */
    public static boolean isAssignableAsShareDefault(AuthorizationRole role) {
        return role == VIEWER || role == CONTRIBUTOR || role == EDITOR;
    }
}
