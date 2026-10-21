package com.capturapantalla.vnc.rfb

import com.capturapantalla.vnc.core.Result
import com.capturapantalla.vnc.core.VncConfig
import com.capturapantalla.vnc.core.VncError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class VncClientValidationTest {

    private val client = VncClient()

    @Test
    fun capture_withBlankHost_returnsInvalidConfiguration() = runBlocking {
        val config = VncConfig(host = "   ")
        val result = client.capture(config)
        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue(failure.error is VncError.InvalidConfiguration)
    }

    @Test
    fun capture_withInvalidPortZero_returnsInvalidConfiguration() = runBlocking {
        val config = VncConfig(host = "192.168.1.10", port = 0)
        val result = client.capture(config)
        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue(failure.error is VncError.InvalidConfiguration)
    }

    @Test
    fun capture_withPortAbove65535_returnsInvalidConfiguration() = runBlocking {
        val config = VncConfig(host = "192.168.1.10", port = 70000)
        val result = client.capture(config)
        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue(failure.error is VncError.InvalidConfiguration)
    }

    @Test
    fun capture_withNegativePort_returnsInvalidConfiguration() = runBlocking {
        val config = VncConfig(host = "192.168.1.10", port = -1)
        val result = client.capture(config)
        assertTrue(result is Result.Failure)
        val failure = result as Result.Failure
        assertTrue(failure.error is VncError.InvalidConfiguration)
    }
}
