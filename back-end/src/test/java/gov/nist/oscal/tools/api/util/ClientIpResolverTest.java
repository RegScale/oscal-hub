/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    private MockHttpServletRequest request(String xff, String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        req.setRemoteAddr(remoteAddr);
        return req;
    }

    @Test
    void takesRightmostEntryBehindOneTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        // Cloud Run appends the real client after anything the client supplied
        assertEquals("198.51.100.7",
                resolver.resolve(request("198.51.100.7", "10.0.0.1")));
        assertEquals("198.51.100.7",
                resolver.resolve(request("1.2.3.4, 198.51.100.7", "10.0.0.1")));
    }

    @Test
    void spoofedFirstEntryIsIgnored() {
        // The old implementation returned the FIRST entry ("6.6.6.6" here),
        // letting attackers rotate per-IP rate buckets and evade IP lockouts.
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertEquals("198.51.100.7",
                resolver.resolve(request("6.6.6.6, 6.6.6.7, 198.51.100.7", "10.0.0.1")));
    }

    @Test
    void twoTrustedHopsTakesSecondFromRight() {
        // External HTTPS LB in front of Cloud Run: "..., client, lb"
        ClientIpResolver resolver = new ClientIpResolver(2);
        assertEquals("198.51.100.7",
                resolver.resolve(request("6.6.6.6, 198.51.100.7, 35.0.0.9", "10.0.0.1")));
    }

    @Test
    void zeroHopsIgnoresHeaderEntirely() {
        ClientIpResolver resolver = new ClientIpResolver(0);
        assertEquals("192.0.2.50",
                resolver.resolve(request("6.6.6.6", "192.0.2.50")));
    }

    @Test
    void missingHeaderFallsBackToRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertEquals("192.0.2.50", resolver.resolve(request(null, "192.0.2.50")));
    }

    @Test
    void shortHeaderClampsInsteadOfFailing() {
        // hops=2 but only one entry present — take what exists
        ClientIpResolver resolver = new ClientIpResolver(2);
        assertEquals("198.51.100.7",
                resolver.resolve(request("198.51.100.7", "10.0.0.1")));
    }

    @Test
    void nullRequestAndOutsideRequestContextAreUnknown() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertEquals("unknown", resolver.resolve(null));
        // No request bound to this thread (async executor / scheduled job)
        assertEquals("unknown", resolver.resolveCurrent());
    }
}
