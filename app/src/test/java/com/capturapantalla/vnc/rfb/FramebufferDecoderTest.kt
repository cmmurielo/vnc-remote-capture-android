package com.capturapantalla.vnc.rfb

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FramebufferDecoderTest {

    @Test
    fun validDimensions_initializesSuccessfully() {
        val decoder = FramebufferDecoder(100, 100)
        assertNotNull(decoder)
        assertTrue(decoder.isCompletelyBlack())
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroWidth_throwsIllegalArgumentException() {
        FramebufferDecoder(0, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeHeight_throwsIllegalArgumentException() {
        FramebufferDecoder(100, -5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun excessiveDimensions_throwsIllegalArgumentException() {
        FramebufferDecoder(8193, 100)
    }

    @Test
    fun decodeRawRect_withTruncatedData_doesNotCrash() {
        val decoder = FramebufferDecoder(10, 10)
        // Rectangle requires 2 * 2 * 4 = 16 bytes, but we only supply 2 bytes
        val truncatedData = byteArrayOf(0x01, 0x02)
        decoder.decodeRawRect(0, 0, 2, 2, truncatedData)
        // Should handle gracefully without throwing ArrayIndexOutOfBoundsException
        assertTrue(decoder.isCompletelyBlack())
    }

    @Test
    fun decodeRawRect_withOutOfBoundsCoordinates_doesNotCrash() {
        val decoder = FramebufferDecoder(10, 10)
        val data = ByteArray(100)
        decoder.decodeRawRect(-5, -5, 10, 10, data)
        decoder.decodeRawRect(15, 15, 10, 10, data)
        assertTrue(decoder.isCompletelyBlack())
    }

    @Test
    fun decodeRawRect_withValidData_decodesPixels() {
        val decoder = FramebufferDecoder(2, 2)
        // 1 pixel (b=0xFF, g=0x00, r=0x00, a=0x00 in Little Endian ARGB)
        val data = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0x00)
        decoder.decodeRawRect(0, 0, 1, 1, data)
        assertFalse(decoder.isCompletelyBlack())
    }
}
