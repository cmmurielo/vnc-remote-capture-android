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

    @Test
    fun defaultVncConfig_hasDefaultTimeoutSeconds() {
        val config = VncConfig()
        assertEquals(15.0, config.timeoutSeconds, 0.0)
    }

    @Test
    fun defaultVncConfig_hasDefaultPortAndFormatAndQuality() {
        val config = VncConfig()
        assertEquals(5900, config.port)
        assertEquals(SaveFormat.JPEG, config.saveFormat)
        assertEquals(85, config.quality)
    }
}
