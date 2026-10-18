package com.capturapantalla.vnc.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.capturapantalla.vnc.core.CapturedImage
import com.capturapantalla.vnc.core.Result
import com.capturapantalla.vnc.core.VncConfig
import com.capturapantalla.vnc.core.VncError
import com.capturapantalla.vnc.core.onFailure
import com.capturapantalla.vnc.core.onSuccess
import com.capturapantalla.vnc.data.PreferencesRepository
import com.capturapantalla.vnc.data.ScreenshotStorage
import com.capturapantalla.vnc.rfb.VncClient

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val config: VncConfig = VncConfig(),
    val isLoading: Boolean = false,
    val progressMessage: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentImage: CapturedImage? = null,
    val isSavedInGallery: Boolean = false
)

class MainViewModel(
    private val vncClient: VncClient,
    private val preferencesRepo: PreferencesRepository,
    private val storage: ScreenshotStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        val savedConfig = preferencesRepo.loadConfig()
        _uiState.update { it.copy(config = savedConfig) }
    }

    fun updateConfig(newConfig: VncConfig) {
        _uiState.update { it.copy(config = newConfig) }
        preferencesRepo.saveConfig(newConfig)
    }

    fun captureScreenshot() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                progressMessage = "Iniciando captura...",
                errorMessage = null,
                successMessage = null,
                isSavedInGallery = false
            )
        }

        viewModelScope.launch {
            val result = vncClient.capture(currentState.config) { progress ->
                _uiState.update { it.copy(progressMessage = progress) }
            }

            result.onSuccess { captured ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentImage = captured,
                        progressMessage = "",
                        successMessage = "Captura obtenida (${captured.resolutionText})"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progressMessage = "",
                        errorMessage = error.userFriendlyMessage()
                    )
                }
            }
        }
    }

    fun saveToGallery() {
        val image = _uiState.value.currentImage ?: return
        val config = _uiState.value.config

        viewModelScope.launch {
            _uiState.update { it.copy(progressMessage = "Guardando en galería...") }
            val saveResult = storage.saveToGallery(image, config.saveFormat, config.quality)

            saveResult.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        isSavedInGallery = true,
                        successMessage = "Guardada en galería: ${saved.relativePath} (${saved.sizeBytes / 1024} KB)"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.userFriendlyMessage())
                }
            }
        }
    }

    fun shareScreenshot(onIntentReady: (Intent) -> Unit) {
        val image = _uiState.value.currentImage ?: return
        val config = _uiState.value.config

        viewModelScope.launch {
            val shareResult = storage.getShareIntent(image, config.saveFormat, config.quality)
            shareResult.onSuccess { intent ->
                onIntentReady(intent)
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.userFriendlyMessage()) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    return MainViewModel(
                        vncClient = VncClient(),
                        preferencesRepo = PreferencesRepository(appContext),
                        storage = ScreenshotStorage(appContext)
                    ) as T
                }
            }
    }
}
