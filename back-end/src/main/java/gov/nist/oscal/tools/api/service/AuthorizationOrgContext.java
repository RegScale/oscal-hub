package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.NoActiveOrganizationException;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Resolves the "primary" organization for a user. The primary organization
 * is the lowest-id ACTIVE OrganizationMembership. Authorizations created
 * by a user are scoped to that user's primary organization.
 */
@Service
public class AuthorizationOrgContext {

    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public AuthorizationOrgContext(OrganizationMembershipRepository membershipRepository,
                                   UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    public Organization requirePrimaryOrganization(User user) {
        List<OrganizationMembership> memberships =
                membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
        return memberships.stream()
                .min(Comparator.comparing(OrganizationMembership::getId))
                .map(OrganizationMembership::getOrganization)
                .orElseThrow(() -> new NoActiveOrganizationException(user.getUsername()));
    }

    public Organization requirePrimaryOrganization(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User '" + username + "' not found."));
        return requirePrimaryOrganization(user);
    }
}
