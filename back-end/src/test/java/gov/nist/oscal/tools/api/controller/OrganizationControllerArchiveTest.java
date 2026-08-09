package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ServiceAccountTokenRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationControllerArchiveTest {

    @Mock private UserRepository userRepository;
    @Mock private ServiceAccountTokenRepository serviceAccountTokenRepository;
    @InjectMocks private OrganizationController controller;

    /**
     * Archiving disables the account, but a service account token it issued is
     * a separate credential — without this the token keeps working for years.
     */
    @Test
    void archivingAUserRevokesTheirServiceAccountTokens() {
        User target = new User();
        target.setId(9L);
        target.setUsername("bob");
        target.setEnabled(true);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        controller.archiveUser(9L);

        verify(serviceAccountTokenRepository)
                .revokeAllForUser(eq(9L), any(LocalDateTime.class), anyString());
    }

    /** Reinstating an account does not resurrect credentials already killed. */
    @Test
    void unarchivingAUserDoesNotRestoreRevokedTokens() {
        User target = new User();
        target.setId(9L);
        target.setUsername("bob");
        target.setEnabled(false);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        controller.unarchiveUser(9L);

        verify(serviceAccountTokenRepository, never()).revokeAllForUser(anyLong(), any(), anyString());
        verifyNoMoreInteractions(serviceAccountTokenRepository);
    }
}
