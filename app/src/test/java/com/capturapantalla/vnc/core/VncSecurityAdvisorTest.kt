package com.capturapantalla.vnc.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VncSecurityAdvisorTest {

    @Test
    fun evaluateHostSecurity_withLocalhost_returnsSecureOrLocal() {
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("localhost")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("127.0.0.1")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("::1")
        )
    }

    @Test
    fun evaluateHostSecurity_withTailscaleCGNAT_returnsSecureOrLocal() {
        // Tailscale CGNAT addresses are 100.64.0.0/10 (100.64.0.0 - 100.127.255.255)
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("100.105.103.8")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("100.64.0.1")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("100.127.255.254")
        )
    }

    @Test
    fun evaluateHostSecurity_withPrivateLanRFC1918_returnsSecureOrLocal() {
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("192.168.1.100")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("10.0.1.5")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.SECURE_OR_LOCAL,
            VncSecurityAdvisor.evaluateHostSecurity("172.16.50.1")
        )
    }

    @Test
    fun evaluateHostSecurity_withPublicWanIp_returnsUnprotectedWan() {
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.UNPROTECTED_WAN,
            VncSecurityAdvisor.evaluateHostSecurity("203.0.113.195")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.UNPROTECTED_WAN,
            VncSecurityAdvisor.evaluateHostSecurity("8.8.8.8")
        )
        assertEquals(
            VncSecurityAdvisor.NetworkSecurityLevel.UNPROTECTED_WAN,
            VncSecurityAdvisor.evaluateHostSecurity("1.1.1.1")
        )
    }
}
