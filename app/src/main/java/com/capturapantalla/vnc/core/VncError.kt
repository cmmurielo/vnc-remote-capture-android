package com.capturapantalla.vnc.core

/**
 * Dominio cerrado de errores modelados algebraicamente para el cliente VNC.
 */
sealed interface VncError {
    data class InvalidConfiguration(val reason: String) : VncError
    data class ConnectionFailed(val host: String, val port: Int, val message: String) : VncError
    data class Timeout(val seconds: Double) : VncError
    data class AuthenticationFailed(val details: String = "Contraseña VNC incorrecta o rechazada por el servidor") : VncError
    data class ProtocolError(val stage: String, val message: String) : VncError
    data class FramebufferEmpty(val retries: Int = 3) : VncError
    data class StorageError(val message: String) : VncError

    fun userFriendlyMessage(): String = when (this) {
        is InvalidConfiguration -> "Configuración inválida: $reason"
        is ConnectionFailed -> "No se pudo conectar a $host:$port ($message). Verifica que Tailscale/VPN o la red local esté activa."
        is Timeout -> "Tiempo de espera agotado (${seconds}s). El servidor no respondió a tiempo."
        is AuthenticationFailed -> details
        is ProtocolError -> "Error en protocolo VNC ($stage): $message"
        is FramebufferEmpty -> "El servidor entregó un fotograma negro tras $retries intentos."
        is StorageError -> "Error de almacenamiento: $message"
    }
}
