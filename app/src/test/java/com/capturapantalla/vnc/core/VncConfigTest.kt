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
}
