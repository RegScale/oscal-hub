/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.crm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrmSyncListenerTest {

    @Mock private CrmService crmService;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;

    @InjectMocks
    private CrmSyncListener listener;

    private User user() {
        User u = new User();
        u.setId(4L);
        u.setUsername("someone");
        u.setEmail("someone@example.com");
        return u;
    }

    private Organization org() {
        Organization o = new Organization();
        o.setId(11L);
        o.setName("Acme");
        return o;
    }

    @Test
    void contactEventReloadsUserAndSyncs() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user()));

        listener.onContactRegistered(new CrmEvents.ContactRegistered(4L, "invitation"));

        verify(crmService).syncContact(any(User.class), eq("invitation"));
    }

    @Test
    void missingUserIsANoOp() {
        when(userRepository.findById(4L)).thenReturn(Optional.empty());

        listener.onContactRegistered(new CrmEvents.ContactRegistered(4L, "invitation"));

        verify(crmService, never()).syncContact(any(), any());
    }

    @Test
    void organizationEventSyncsCompanyWithOwner() {
        when(organizationRepository.findById(11L)).thenReturn(Optional.of(org()));
        when(userRepository.findById(4L)).thenReturn(Optional.of(user()));

        listener.onOrganizationSignedUp(new CrmEvents.OrganizationSignedUp(11L, 4L));

        verify(crmService).syncOrganization(any(Organization.class), any(User.class));
    }

    @Test
    void transientFailureIsRetriedOnceThenSwallowed() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user()));
        doThrow(new RuntimeException("hubspot 502"))
                .doNothing()
                .when(crmService).syncContact(any(User.class), any());

        listener.onContactRegistered(new CrmEvents.ContactRegistered(4L, "self_serve_registration"));

        verify(crmService, times(2)).syncContact(any(User.class), any());
    }

    @Test
    void persistentFailureNeverPropagates() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(user()));
        doThrow(new RuntimeException("hubspot down"))
                .when(crmService).syncContact(any(User.class), any());

        // Must not throw — signup already committed; losing a lead is acceptable,
        // breaking onboarding is not.
        listener.onContactRegistered(new CrmEvents.ContactRegistered(4L, "self_serve_registration"));

        verify(crmService, times(2)).syncContact(any(User.class), any());
    }
}
