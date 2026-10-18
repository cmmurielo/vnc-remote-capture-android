package com.capturapantalla.vnc.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.capturapantalla.vnc.core.SaveFormat
import com.capturapantalla.vnc.core.VncConfig

/**
 * Repositorio de configuración y persistencia cifrada de credenciales VNC.
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = createEncryptedSharedPreferences(context)

    @Suppress("DEPRECATION")
    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun loadConfig(): VncConfig {
        val host = prefs.getString(KEY_HOST, "100.105.103.8") ?: "100.105.103.8"
        val port = prefs.getInt(KEY_PORT, 5900)
        // Omitir contraseñas por defecto predeterminadas para evitar la exposición de credenciales
        val password = prefs.getString(KEY_PASSWORD, "") ?: ""
        val timeout = prefs.getFloat(KEY_TIMEOUT, 15f).toDouble()
        val formatName = prefs.getString(KEY_FORMAT, SaveFormat.JPEG.name) ?: SaveFormat.JPEG.name
        val quality = prefs.getInt(KEY_QUALITY, 85)

        val saveFormat = try {
            SaveFormat.valueOf(formatName)
        } catch (_: Exception) {
            SaveFormat.JPEG
        }

        return VncConfig(
            host = host,
            port = port,
            password = password,
            timeoutSeconds = timeout,
            saveFormat = saveFormat,
            quality = quality
        )
    }

    fun saveConfig(config: VncConfig) {
        prefs.edit()
            .putString(KEY_HOST, config.host)
            .putInt(KEY_PORT, config.port)
            .putString(KEY_PASSWORD, config.password)
            .putFloat(KEY_TIMEOUT, config.timeoutSeconds.toFloat())
            .putString(KEY_FORMAT, config.saveFormat.name)
            .putInt(KEY_QUALITY, config.quality)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "vnc_capture_encrypted_preferences"
        private const val KEY_HOST = "key_host"
        private const val KEY_PORT = "key_port"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_TIMEOUT = "key_timeout"
        private const val KEY_FORMAT = "key_format"
        private const val KEY_QUALITY = "key_quality"
    }
}
