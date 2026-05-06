package gov.nist.oscal.tools.api.model.library;

import java.util.List;

/**
 * Wire shape for GET /api/public/catalog/top-contributors.
 * Two leaderboards: individual users and organizations. Both are scoped
 * to PUBLIC items only.
 */
public record PublicCatalogTopContributors(
        List<UserContributor> users,
        List<OrgContributor> organizations) {

    public record UserContributor(
            Long userId,
            String username,
            String displayName,
            long uploadCount,
            long totalDownloads) {}

    public record OrgContributor(
            Long organizationId,
            String name,
            String logoUrl,
            long uploadCount,
            long totalDownloads) {}
}
