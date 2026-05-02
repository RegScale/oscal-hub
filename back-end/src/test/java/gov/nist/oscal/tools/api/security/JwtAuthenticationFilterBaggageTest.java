package gov.nist.oscal.tools.api.security;

import io.opentelemetry.api.baggage.Baggage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterBaggageTest {

    private JwtUtil jwtUtil;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = mock(JwtUtil.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter();
        var f1 = JwtAuthenticationFilter.class.getDeclaredField("jwtUtil");
        f1.setAccessible(true); f1.set(filter, jwtUtil);
        var f2 = JwtAuthenticationFilter.class.getDeclaredField("userDetailsService");
        f2.setAccessible(true); f2.set(filter, userDetailsService);
    }

    @Test
    void baggageIsPopulatedFromValidJwtAndIsActiveDuringDownstreamChain() throws Exception {
        String jwt = "fake.jwt.value";
        UserDetails user = new User("alice", "x", Collections.emptyList());
        when(jwtUtil.extractUsername(jwt)).thenReturn("alice");
        when(jwtUtil.validateToken(jwt, user)).thenReturn(true);
        when(jwtUtil.extractUserId(jwt)).thenReturn(456L);
        when(jwtUtil.extractOrganizationId(jwt)).thenReturn(123L);
        when(jwtUtil.extractGlobalRole(jwt)).thenReturn("USER");
        when(jwtUtil.extractOrganizationRole(jwt)).thenReturn("ORG_ADMIN");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<Baggage> captured = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest r, ServletResponse s) throws IOException, ServletException {
                captured.set(Baggage.current());
            }
        };

        filter.doFilterInternal(req, resp, chain);

        Baggage b = captured.get();
        assertEquals("456", b.getEntryValue("user.id"));
        assertEquals("123", b.getEntryValue("org.id"));
        assertEquals("USER", b.getEntryValue("user.role.global"));
        assertEquals("ORG_ADMIN", b.getEntryValue("user.role.org"));
    }

    @Test
    void noBaggageEntriesWhenAuthHeaderAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<Baggage> captured = new AtomicReference<>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest r, ServletResponse s) throws IOException, ServletException {
                captured.set(Baggage.current());
            }
        };

        filter.doFilterInternal(req, resp, chain);

        Baggage b = captured.get();
        assertEquals(0, b.size());
    }
}
