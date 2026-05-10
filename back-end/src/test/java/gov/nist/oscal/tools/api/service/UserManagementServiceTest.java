package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.User.GlobalRole;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserManagementService — covers organization-admin lock,
 * unlock, deactivate, reactivate, and password reset flows. Particular
 * focus on:
 *   1. Self-service prevention: an admin must not be able to lock or
 *      reset their own account from the admin panel.
 *   2. State-machine guards: cannot unlock an already-active member,
 *      cannot reactivate an already-active member, cannot unlock a
 *      deactivated user (must reactivate instead).
 *   3. Password reset secrecy: when notify=false the plaintext lands in
 *      the response; when notify=true it is dispatched via email and
 *      omitted from the response.
 */
class UserManagementServiceTest {

    private final UserRepository userRepo = mock(UserRepository.class);
    private final OrganizationRepository orgRepo = mock(OrganizationRepository.class);
    private final OrganizationMembershipRepository membershipRepo = mock(OrganizationMembershipRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final EmailService email = mock(EmailService.class);
    private final PasswordValidationService passwordValidator = mock(PasswordValidationService.class);

    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService();
        ReflectionTestUtils.setField(service, "userRepository", userRepo);
        ReflectionTestUtils.setField(service, "organizationRepository", orgRepo);
        ReflectionTestUtils.setField(service, "membershipRepository", membershipRepo);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "emailService", email);
        ReflectionTestUtils.setField(service, "passwordValidationService", passwordValidator);
    }

    // ---------- lockUser ----------

    @Test
    void lockUser_setsStatusToLocked_andSaves() {
        OrganizationMembership m = setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.lockUser(1L, 10L, 2L);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.LOCKED);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(membershipRepo).save(m);
    }

    @Test
    void lockUser_alreadyLocked_throws() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.LOCKED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.lockUser(1L, 10L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already locked");
    }

    @Test
    void lockUser_adminWithoutOrgAdminRole_isRejected_evenIfMember() {
        // Regression guard: a user with only OrganizationRole.USER inside the
        // org must not be able to lock peers. Previously this was a permission
        // bypass when the role-check expression was misread.
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.USER);

        assertThatThrownBy(() -> service.lockUser(1L, 10L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ORG_ADMIN");
    }

    @Test
    void lockUser_globalSuperAdmin_canActWithoutOrgAdminRole() {
        // Cross-org operations: SUPER_ADMIN bypasses the per-org role check
        // because they need to be able to manage any organization.
        OrganizationMembership target = setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.USER);
        // Promote admin to SUPER_ADMIN
        target.getOrganization(); // touch to silence unused
        User admin = userRepo.findById(2L).orElseThrow();
        admin.setGlobalRole(GlobalRole.SUPER_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.lockUser(1L, 10L, 2L);
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.LOCKED);
    }

    @Test
    void lockUser_selfTarget_isRejected() {
        // Admins cannot lock themselves out of their own org.
        setupMemberAndAdmin(2L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.ORG_ADMIN, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.lockUser(2L, 10L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("own account");
    }

    @Test
    void lockUser_userNotMemberOfOrg_throwsHelpfulError() {
        // The user exists, the admin exists, but the user is not a member
        // of this organization — should fail at membership lookup.
        User user = stubUser(1L, "alice");
        User admin = stubUser(2L, "bob");
        Organization org = stubOrg(10L);
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));
        when(orgRepo.findById(10L)).thenReturn(Optional.of(org));
        when(membershipRepo.findByUserAndOrganization(user, org)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lockUser(1L, 10L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not a member");
    }

    // ---------- unlockUser ----------

    @Test
    void unlockUser_locked_becomesActive() {
        OrganizationMembership m = setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.LOCKED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.unlockUser(1L, 10L, 2L);
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void unlockUser_alreadyActive_throws() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.unlockUser(1L, 10L, 2L))
                .hasMessageContaining("already active");
    }

    @Test
    void unlockUser_deactivated_redirectsToReactivate() {
        // Distinct guard: deactivated is a different lifecycle state than locked,
        // so the message must steer the admin toward the right action.
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.DEACTIVATED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.unlockUser(1L, 10L, 2L))
                .hasMessageContaining("reactivate");
    }

    // ---------- deactivateUser ----------

    @Test
    void deactivateUser_active_becomesDeactivated() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.deactivateUser(1L, 10L, 2L);
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.DEACTIVATED);
    }

    @Test
    void deactivateUser_alreadyDeactivated_throws() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.DEACTIVATED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.deactivateUser(1L, 10L, 2L))
                .hasMessageContaining("already deactivated");
    }

    // ---------- reactivateUser ----------

    @Test
    void reactivateUser_locked_becomesActive() {
        // The service allows reactivating from any non-active state.
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.LOCKED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.reactivateUser(1L, 10L, 2L);
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void reactivateUser_deactivated_becomesActive() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.DEACTIVATED, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        when(membershipRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrganizationMembership result = service.reactivateUser(1L, 10L, 2L);
        assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    void reactivateUser_alreadyActive_throws() {
        setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);

        assertThatThrownBy(() -> service.reactivateUser(1L, 10L, 2L))
                .hasMessageContaining("already active");
    }

    // ---------- resetPassword (org-scoped) ----------

    @Test
    void resetPassword_orgScoped_sendsEmail_doesNotReturnPassword() {
        OrganizationMembership m = setupMemberAndAdmin(1L, 2L, 10L,
                MembershipStatus.ACTIVE, OrganizationRole.USER, OrganizationRole.ORG_ADMIN);
        m.getUser().setEmail("alice@example.com");
        when(encoder.encode(anyString())).thenReturn("HASHED");

        Map<String, String> result = service.resetPassword(1L, 10L, 2L);

        assertThat(result).containsKeys("username", "email");
        assertThat(result).doesNotContainKey("password");
        // Email service was called with a non-null temp password
        verify(email, times(1)).sendPasswordReset(any(User.class), anyString(), any(User.class));
        // User must change password on next login
        assertThat(m.getUser().getMustChangePassword()).isTrue();
    }

    // ---------- resetPasswordByAdmin (super admin, no org scope) ----------

    @Test
    void resetPasswordByAdmin_notifyTrue_emailsAndOmitsPasswordFromResponse() {
        User target = stubUser(1L, "alice");
        target.setEmail("alice@example.com");
        User admin = stubUser(2L, "super");
        when(userRepo.findById(1L)).thenReturn(Optional.of(target));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));
        when(encoder.encode(anyString())).thenReturn("HASHED");

        Map<String, String> result = service.resetPasswordByAdmin(1L, 2L, null, true);

        assertThat(result).containsKeys("username", "email");
        assertThat(result).doesNotContainKey("password");
        verify(email, times(1)).sendPasswordReset(any(User.class), anyString(), any(User.class));
        assertThat(target.getMustChangePassword()).isTrue();
    }

    @Test
    void resetPasswordByAdmin_notifyFalse_returnsPlaintextAndDoesNotEmail() {
        // Out-of-band delivery: plaintext goes to the admin caller, not via email,
        // so the admin can hand it to the user securely. Email service must
        // NOT have been called in this branch.
        User target = stubUser(1L, "alice");
        target.setEmail("alice@example.com");
        User admin = stubUser(2L, "super");
        when(userRepo.findById(1L)).thenReturn(Optional.of(target));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));
        when(encoder.encode(anyString())).thenReturn("HASHED");

        Map<String, String> result = service.resetPasswordByAdmin(1L, 2L, null, false);

        assertThat(result).containsKey("password");
        assertThat(result.get("password")).isNotBlank();
        // The password we hand back must not be the encoded form
        assertThat(result.get("password")).isNotEqualTo("HASHED");
        verify(email, never()).sendPasswordReset(any(), any(), any());
    }

    @Test
    void resetPasswordByAdmin_customPassword_isValidated_thenUsedVerbatim() {
        User target = stubUser(1L, "alice");
        target.setEmail("alice@example.com");
        User admin = stubUser(2L, "super");
        when(userRepo.findById(1L)).thenReturn(Optional.of(target));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));
        when(encoder.encode("MyChosenPwd!1")).thenReturn("HASHED-CUSTOM");

        Map<String, String> result = service.resetPasswordByAdmin(1L, 2L, "MyChosenPwd!1", false);

        verify(passwordValidator).validatePassword("MyChosenPwd!1", "alice");
        assertThat(target.getPassword()).isEqualTo("HASHED-CUSTOM");
        assertThat(result.get("password")).isEqualTo("MyChosenPwd!1");
    }

    @Test
    void resetPasswordByAdmin_blankCustomPassword_fallsBackToGenerated() {
        // Empty/whitespace-only customPassword shouldn't sneak through — generate
        // a strong one instead so we never set blank passwords.
        User target = stubUser(1L, "alice");
        target.setEmail("alice@example.com");
        User admin = stubUser(2L, "super");
        when(userRepo.findById(1L)).thenReturn(Optional.of(target));
        when(userRepo.findById(2L)).thenReturn(Optional.of(admin));
        when(encoder.encode(anyString())).thenReturn("HASHED");

        Map<String, String> result = service.resetPasswordByAdmin(1L, 2L, "   ", false);

        // No validation call because we treated it as null
        verify(passwordValidator, never()).validatePassword(anyString(), anyString());
        assertThat(result.get("password")).isNotBlank().hasSizeGreaterThanOrEqualTo(12);
    }

    @Test
    void resetPasswordByAdmin_selfTarget_isRejected() {
        // Admins cannot reset their own password from the admin panel — that
        // path is for managing other people's accounts. Regression guard.
        User self = stubUser(2L, "super");
        when(userRepo.findById(2L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.resetPasswordByAdmin(2L, 2L, null, true))
                .hasMessageContaining("own password");
    }

    @Test
    void resetPasswordByAdmin_unknownUser_throws() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPasswordByAdmin(99L, 2L, null, true))
                .hasMessageContaining("User not found");
    }

    // ---------- helpers ----------

    private OrganizationMembership setupMemberAndAdmin(long userId, long adminId, long orgId,
                                                       MembershipStatus status,
                                                       OrganizationRole userRole,
                                                       OrganizationRole adminRole) {
        User user = stubUser(userId, "user-" + userId);
        User admin = stubUser(adminId, "admin-" + adminId);
        Organization org = stubOrg(orgId);

        OrganizationMembership userMem = new OrganizationMembership(user, org, userRole);
        userMem.setStatus(status);

        OrganizationMembership adminMem = new OrganizationMembership(admin, org, adminRole);
        adminMem.setStatus(MembershipStatus.ACTIVE);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(userRepo.findById(adminId)).thenReturn(Optional.of(admin));
        when(orgRepo.findById(orgId)).thenReturn(Optional.of(org));
        when(membershipRepo.findByUserAndOrganization(user, org)).thenReturn(Optional.of(userMem));
        when(membershipRepo.findByUserAndOrganization(admin, org)).thenReturn(Optional.of(adminMem));
        return userMem;
    }

    private static User stubUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setGlobalRole(GlobalRole.USER);
        return u;
    }

    private static Organization stubOrg(long id) {
        Organization o = new Organization();
        o.setId(id);
        o.setName("org-" + id);
        return o;
    }
}
