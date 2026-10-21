package com.capturapantalla.vnc.rfb

import android.graphics.Bitmap

/**
 * Decodificador de rectángulos de framebuffer VNC (RAW encoding 32-bit ARGB).
 */
class FramebufferDecoder(
    val width: Int,
    val height: Int
) {
    init {
        require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) {
            "Dimensiones inválidas para FramebufferDecoder: $width x $height"
        }
        val totalPixelsLong = width.toLong() * height.toLong()
        require(totalPixelsLong <= MAX_PIXELS) {
            "El número de píxeles ($totalPixelsLong) supera el límite seguro permitido"
        }
    }

    private val pixelBuffer = IntArray(width * height)

    /**
     * Escribe un rectángulo de píxeles RAW little-endian en el buffer principal.
     */
    fun decodeRawRect(
        rectX: Int,
        rectY: Int,
        rectW: Int,
        rectH: Int,
        data: ByteArray,
        offset: Int = 0
    ) {
        if (rectX < 0 || rectY < 0 || rectW <= 0 || rectH <= 0 || offset < 0) return
        if (rectX >= width || rectY >= height) return

        var byteIdx = offset
        for (y in 0 until rectH) {
            val destY = rectY + y
            if (destY >= height) break
            val rowOffset = destY * width

            for (x in 0 until rectW) {
                val destX = rectX + x
                if (destX >= width) {
                    byteIdx += (rectW - x) * 4
                    break
                }

                if (byteIdx + 2 >= data.size) {
                    return
                }

                val b = data[byteIdx].toInt() and 0xFF
                val g = data[byteIdx + 1].toInt() and 0xFF
                val r = data[byteIdx + 2].toInt() and 0xFF

                val pixelIndex = rowOffset + destX
                if (pixelIndex in pixelBuffer.indices) {
                    pixelBuffer[pixelIndex] = -0x1000000 or (r shl 16) or (g shl 8) or b
                }
                byteIdx += 4
            }
        }
    }

    /**
     * Comprueba si el frame actual es completamente negro (valores RGB 0).
     * Usado para detectar buffers iniciales vacíos en servidores VNC como UltraVNC / TightVNC.
     */
    fun isCompletelyBlack(): Boolean {
        for (i in pixelBuffer.indices) {
            if ((pixelBuffer[i] and 0x00FFFFFF) != 0) {
                return false
            }
        }
        return true
    }

    /**
     * Convierte el buffer de píxeles en un Bitmap inmutable de Android.
     */
    fun toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixelBuffer, 0, width, 0, 0, width, height)
        return bitmap
    }

    companion object {
        const val MAX_DIMENSION = 8192
        const val MAX_PIXELS = 8192L * 8192L // 67.1M pixels (~268MB buffer)
    }
}
