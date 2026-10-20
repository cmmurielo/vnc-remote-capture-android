package com.capturapantalla.vnc.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VncConfigTest {

    @Test
    fun defaultVncConfig_hasEmptyPassword() {
        val config = VncConfig()
        // Ensure default password is empty to avoid embedding hardcoded credentials
        assertEquals("", config.password)
    }

    @Test
    fun defaultVncConfig_hasEmptyHost() {
        val config = VncConfig()
        // Ensure default host is empty to avoid embedding hardcoded IP addresses or internal networks
        assertEquals("", config.host)
    }
}
