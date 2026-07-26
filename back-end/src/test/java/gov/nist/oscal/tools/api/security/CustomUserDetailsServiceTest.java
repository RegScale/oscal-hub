/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    private User user(String username) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPassword("hash");
        u.setEnabled(true);
        return u;
    }

    @Test
    void exactMatchWinsWithoutFallback() {
        when(userRepository.findByUsername("iorga")).thenReturn(Optional.of(user("iorga")));

        UserDetails details = service.loadUserByUsername("iorga");

        assertEquals("iorga", details.getUsername());
        verify(userRepository, never()).findAllByUsernameIgnoreCase("iorga");
    }

    @Test
    void uniqueCaseInsensitiveMatchFallsBack() {
        // User registered as "iorga" but types "Iorga" at login
        when(userRepository.findByUsername("Iorga")).thenReturn(Optional.empty());
        when(userRepository.findAllByUsernameIgnoreCase("Iorga"))
                .thenReturn(List.of(user("iorga")));

        UserDetails details = service.loadUserByUsername("Iorga");

        assertEquals("iorga", details.getUsername());
    }

    @Test
    void ambiguousCaseInsensitiveMatchIsNotGuessed() {
        // Legacy case-duplicate accounts: exact form required
        when(userRepository.findByUsername("Bob")).thenReturn(Optional.empty());
        when(userRepository.findAllByUsernameIgnoreCase("Bob"))
                .thenReturn(List.of(user("bob"), user("BOB")));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("Bob"));
    }
}
