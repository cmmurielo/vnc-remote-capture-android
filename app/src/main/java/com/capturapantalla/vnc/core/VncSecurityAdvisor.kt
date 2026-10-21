package com.capturapantalla.vnc.core

/**
 * Asesor de seguridad para el transporte de red VNC.
 * Evalúa si la dirección de destino opera en redes cifradas (Tailscale / VPN)
 * o locales vs. redes públicas no cifradas.
 */
object VncSecurityAdvisor {

    enum class NetworkSecurityLevel {
        SECURE_OR_LOCAL,
        UNPROTECTED_WAN
    }

    fun evaluateHostSecurity(host: String): NetworkSecurityLevel {
        val cleanHost = host.trim().lowercase()
        if (cleanHost.isEmpty() || cleanHost == "localhost" || cleanHost == "127.0.0.1" || cleanHost == "::1") {
            return NetworkSecurityLevel.SECURE_OR_LOCAL
        }
        if (cleanHost.endsWith(".local") || cleanHost.endsWith(".lan")) {
            return NetworkSecurityLevel.SECURE_OR_LOCAL
        }

        val parts = cleanHost.split(".")
        if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
            val p0 = parts[0].toInt()
            val p1 = parts[1].toInt()

            // 10.0.0.0/8 (Red privada)
            if (p0 == 10) return NetworkSecurityLevel.SECURE_OR_LOCAL
            // 192.168.0.0/16 (Red privada doméstica/oficina)
            if (p0 == 192 && p1 == 168) return NetworkSecurityLevel.SECURE_OR_LOCAL
            // 172.16.0.0/12 (Red privada corporativa 172.16.x.x - 172.31.x.x)
            if (p0 == 172 && p1 in 16..31) return NetworkSecurityLevel.SECURE_OR_LOCAL
            // 100.64.0.0/10 (CGNAT utilizado por Tailscale / WireGuard privado)
            if (p0 == 100 && p1 in 64..127) return NetworkSecurityLevel.SECURE_OR_LOCAL

            // Cualquier otra IPv4 es WAN pública
            return NetworkSecurityLevel.UNPROTECTED_WAN
        }

        return NetworkSecurityLevel.SECURE_OR_LOCAL
    }
}
