/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LibraryService#canRead(LibraryItem, User)}.
 * <p>
 * canRead is a pure predicate (no injected deps used), so we instantiate the
 * service directly with a no-args constructor — the @Autowired fields stay null
 * but they are not exercised by canRead.
 */
class LibraryServiceCanReadTest {

    private final LibraryService svc = new LibraryService();

    private LibraryItem item(Visibility v, User creator, Organization org) {
        LibraryItem it = new LibraryItem();
        it.setVisibility(v);
        it.setCreatedBy(creator);
        it.setOrganization(org);
        return it;
    }

    /**
     * Build a User whose primary ACTIVE org membership points at org with id
     * {@code orgId}. If orgId is null, the user has no memberships.
     */
    private User user(long id, String username, Long orgId) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        Set<OrganizationMembership> memberships = new HashSet<>();
        if (orgId != null) {
            Organization o = new Organization();
            o.setId(orgId);
            OrganizationMembership m = new OrganizationMembership();
            m.setUser(u);
            m.setOrganization(o);
            m.setStatus(MembershipStatus.ACTIVE);
            memberships.add(m);
        }
        u.setOrganizationMemberships(memberships);
        return u;
    }

    private Organization org(long id) {
        Organization o = new Organization();
        o.setId(id);
        return o;
    }

    @Test
    void publicItemReadableByAnyone() {
        LibraryItem item = item(Visibility.PUBLIC, user(1, "alice", 10L), null);
        assertThat(svc.canRead(item, null)).isTrue();              // anonymous
        assertThat(svc.canRead(item, user(2, "bob", 20L))).isTrue(); // unrelated user
        assertThat(svc.canRead(item, user(99, "no-org", null))).isTrue(); // user with no org
    }

    @Test
    void privateItemReadableOnlyByCreator() {
        User alice = user(1, "alice", 10L);
        LibraryItem item = item(Visibility.PRIVATE, alice, null);
        assertThat(svc.canRead(item, null)).isFalse();              // anonymous
        assertThat(svc.canRead(item, alice)).isTrue();              // creator
        assertThat(svc.canRead(item, user(2, "bob", 10L))).isFalse(); // same org, NOT enough
        assertThat(svc.canRead(item, user(3, "carol", 99L))).isFalse(); // different org
    }

    @Test
    void organizationItemReadableByOrgMembers() {
        User alice = user(1, "alice", 10L);
        Organization acme = org(10L);
        LibraryItem item = item(Visibility.ORGANIZATION, alice, acme);
        assertThat(svc.canRead(item, null)).isFalse();                  // anonymous
        assertThat(svc.canRead(item, alice)).isTrue();                  // creator
        assertThat(svc.canRead(item, user(2, "bob", 10L))).isTrue();    // same org
        assertThat(svc.canRead(item, user(3, "carol", 99L))).isFalse(); // different org
        assertThat(svc.canRead(item, user(4, "no-org", null))).isFalse(); // user with no org
    }
}
