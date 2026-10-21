package com.capturapantalla.vnc.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.capturapantalla.vnc.core.CapturedImage
import com.capturapantalla.vnc.core.Result
import com.capturapantalla.vnc.core.SaveFormat
import com.capturapantalla.vnc.core.VncError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedFileResult(
    val uri: Uri,
    val sizeBytes: Long,
    val relativePath: String
)

class ScreenshotStorage(
    private val context: Context? = null,
    private val cacheDirectoryProvider: () -> File = {
        context?.cacheDir?.let { File(it, "capturas") } ?: File("build/tmp/capturas")
    }
) {

    internal constructor(cacheDir: File) : this(
        context = null,
        cacheDirectoryProvider = { cacheDir }
    )

    private val dateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Guarda la captura de pantalla directamente en la Galería del teléfono (MediaStore).
     * En Android 10+ (API 29+) no requiere permisos de almacenamiento.
     */
    suspend fun saveToGallery(
        image: CapturedImage,
        format: SaveFormat,
        quality: Int
    ): Result<SavedFileResult, VncError> = withContext(Dispatchers.IO) {
        val timestamp = dateFormatter.format(Date(image.timestampMillis))
        val filename = "captura_$timestamp.${format.extension}"

        val compressFormat = when (format) {
            SaveFormat.JPEG -> Bitmap.CompressFormat.JPEG
            SaveFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            SaveFormat.PNG -> Bitmap.CompressFormat.PNG
        }

        val ctx = context ?: return@withContext Result.Failure(VncError.StorageError("Contexto no disponible"))

        try {
            val contentResolver = ctx.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                put(MediaStore.Images.Media.WIDTH, image.width)
                put(MediaStore.Images.Media.HEIGHT, image.height)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CapturasRemotas")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.Failure(VncError.StorageError("No se pudo crear entrada en MediaStore"))

            var bytesWritten = 0L
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                image.bitmap.compress(compressFormat, quality, outputStream)
                outputStream.flush()
            } ?: return@withContext Result.Failure(VncError.StorageError("No se pudo abrir stream de salida en MediaStore"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }

            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                bytesWritten = pfd.statSize
            }

            Result.Success(
                SavedFileResult(
                    uri = uri,
                    sizeBytes = bytesWritten,
                    relativePath = "Pictures/CapturasRemotas/$filename"
                )
            )
        } catch (e: Exception) {
            Result.Failure(VncError.StorageError("Error al guardar en galería: ${e.localizedMessage}"))
        }
    }

    /**
     * Genera un Uri accesible vía FileProvider para compartir la captura con otras apps.
     */
    suspend fun getShareIntent(
        image: CapturedImage,
        format: SaveFormat,
        quality: Int
    ): Result<Intent, VncError> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = cacheDirectoryProvider()
            if (!cacheDir.exists()) cacheDir.mkdirs()
            clearTempCache()

            val timestamp = dateFormatter.format(Date(image.timestampMillis))
            val tempFile = File(cacheDir, "compartir_$timestamp.${format.extension}")

            val compressFormat = if (format == SaveFormat.PNG) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }

            FileOutputStream(tempFile).use { out ->
                image.bitmap.compress(compressFormat, quality, out)
                out.flush()
            }

            val ctx = context ?: return@withContext Result.Failure(VncError.StorageError("Contexto no disponible"))

            val contentUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                tempFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Result.Success(Intent.createChooser(shareIntent, "Compartir Captura VNC"))
        } catch (e: Exception) {
            Result.Failure(VncError.StorageError("Error al preparar compartir: ${e.localizedMessage}"))
        }
    }

    /**
     * Purga capturas temporales residuales en caché para evitar la acumulación
     * de imágenes con información confidencial en el almacenamiento interno de la app.
     */
    fun clearTempCache(olderThanMillis: Long = CACHE_EXPIRATION_MILLIS): Int {
        val cacheDir = cacheDirectoryProvider()
        if (!cacheDir.exists()) return 0
        val cutoff = System.currentTimeMillis() - olderThanMillis
        var deletedCount = 0
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }
        return deletedCount
    }

    companion object {
        const val CACHE_EXPIRATION_MILLIS = 30 * 60 * 1000L // 30 minutos
    }
}
