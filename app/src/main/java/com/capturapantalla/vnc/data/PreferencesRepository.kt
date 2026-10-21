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
            buildEncryptedPrefs(context)
        } catch (_: Exception) {
            try {
                // Si el Keystore o el archivo de preferencias está corrupto/inválido, purgar y recrear de forma segura
                context.deleteSharedPreferences(PREFS_NAME)
                buildEncryptedPrefs(context)
            } catch (_: Exception) {
                // Bajo ningún concepto guardar contraseñas en texto plano en disco (MODE_PRIVATE)
                // Usar almacenamiento seguro en memoria volátil si el hardware Keystore no responde
                InMemorySharedPreferences()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun buildEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadConfig(): VncConfig {
        val defaultConfig = VncConfig()
        val host = prefs.getString(KEY_HOST, defaultConfig.host) ?: defaultConfig.host
        val port = prefs.getInt(KEY_PORT, defaultConfig.port)
        // Omitir contraseñas por defecto predeterminadas para evitar la exposición de credenciales
        val password = prefs.getString(KEY_PASSWORD, defaultConfig.password) ?: defaultConfig.password
        val timeout = prefs.getFloat(KEY_TIMEOUT, defaultConfig.timeoutSeconds.toFloat()).toDouble()
        val formatName = prefs.getString(KEY_FORMAT, defaultConfig.saveFormat.name) ?: defaultConfig.saveFormat.name
        val quality = prefs.getInt(KEY_QUALITY, defaultConfig.quality)

        val saveFormat = try {
            SaveFormat.valueOf(formatName)
        } catch (_: Exception) {
            defaultConfig.saveFormat
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

/**
 * Almacenamiento volátil seguro en memoria utilizado únicamente en caso de fallo crítico
 * irrecuperable en el Keystore de Android, garantizando que nunca se guarden contraseñas
 * en texto plano en el almacenamiento secundario.
 */
private class InMemorySharedPreferences : SharedPreferences {
    private val data = java.util.concurrent.ConcurrentHashMap<String, Any>()

    override fun getAll(): Map<String, *> = HashMap(data)
    override fun getString(key: String?, defValue: String?): String? = (data[key] as? String) ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        (data[key] as? Set<String>) ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = (data[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (data[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (data[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (data[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = EditorImpl()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        private val modifications = HashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = value
        }

        override fun putStringSet(key: String?, values: Set<String>?): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            if (key != null) modifications[key] = null
        }

        override fun clear(): SharedPreferences.Editor = apply { clear = true }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clear) data.clear()
            for ((k, v) in modifications) {
                if (v == null) data.remove(k) else data[k] = v
            }
        }
    }
}
