/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /api/admin/organizations must include each organization's ACTIVE member
 * count (shown as the "Users" column on /admin/organizations). Previously the
 * response hardcoded memberCount to 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOrganizationsMemberCountTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrganizationRepository orgRepo;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationMembershipRepository memRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private Organization makeOrg(String name) {
        Organization o = new Organization();
        o.setName(name);
        o.setActive(true);
        o.setCreatedAt(LocalDateTime.now());
        return orgRepo.save(o);
    }

    private User makeUser(String prefix) {
        User u = new User();
        u.setUsername(prefix + "-" + System.nanoTime());
        u.setEmail(prefix + "-" + System.nanoTime() + "@example.com");
        u.setPassword(passwordEncoder.encode("CorrectH0rse!Batt"));
        u.setEnabled(true);
        return userRepo.save(u);
    }

    private void addMember(Organization org, MembershipStatus status) {
        OrganizationMembership m = new OrganizationMembership(makeUser("member"), org, OrganizationRole.USER);
        m.setStatus(status);
        memRepo.save(m);
    }

    @Test
    @WithMockUser(username = "super", roles = {"SUPER_ADMIN"})
    void listIncludesActiveMemberCountPerOrganization() throws Exception {
        long suffix = System.nanoTime();
        Organization staffed = makeOrg("Staffed-" + suffix);
        addMember(staffed, MembershipStatus.ACTIVE);
        addMember(staffed, MembershipStatus.ACTIVE);
        // DEACTIVATED members are not "users of the org" — must not count
        addMember(staffed, MembershipStatus.DEACTIVATED);

        Organization empty = makeOrg("Empty-" + suffix);

        mockMvc.perform(get("/api/admin/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.name == 'Staffed-" + suffix + "')].memberCount").value(2))
                .andExpect(jsonPath(
                        "$[?(@.name == 'Empty-" + suffix + "')].memberCount").value(0));
    }
}
