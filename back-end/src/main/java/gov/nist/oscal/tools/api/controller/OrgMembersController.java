package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.model.OrgMemberResponse;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.service.AuthorizationOrgContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization Members", description = "Read-only access to the current user's org members for pickers")
public class OrgMembersController {

    private final AuthorizationOrgContext orgContext;
    private final OrganizationMembershipRepository membershipRepository;

    public OrgMembersController(AuthorizationOrgContext orgContext,
                                OrganizationMembershipRepository membershipRepository) {
        this.orgContext = orgContext;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/me/members")
    public ResponseEntity<List<OrgMemberResponse>> listMyOrgMembers(Principal principal) {
        Organization org = orgContext.requirePrimaryOrganization(principal.getName());
        List<OrgMemberResponse> members = membershipRepository
                .findByOrganizationAndStatusWithUser(org, MembershipStatus.ACTIVE)
                .stream()
                .map(OrganizationMembership::getUser)
                .map(OrgMemberResponse::new)
                .toList();
        return ResponseEntity.ok(members);
    }
}
