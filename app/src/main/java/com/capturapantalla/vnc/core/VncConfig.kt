package com.capturapantalla.vnc.core

import android.graphics.Bitmap
import android.net.Uri

/**
 * Value Object inmutable para la configuración de conexión VNC.
 */
data class VncConfig(
    val host: String = "",
    val port: Int = 5900,
    val password: String = "",
    val timeoutSeconds: Double = 15.0,
    val viewOnly: Boolean = true,
    val shared: Boolean = true,
    val saveFormat: SaveFormat = SaveFormat.JPEG,
    val quality: Int = 85
)

enum class SaveFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
    PNG("png", "image/png")
}

/**
 * Representación inmutable de una captura de pantalla remota exitosa.
 */
data class CapturedImage(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val timestampMillis: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val localUri: Uri? = null
) {
    val resolutionText: String
        get() = "${width}x${height} px"

    val sizeKbText: String
        get() = String.format("%.2f KB", sizeBytes / 1024.0)
}
