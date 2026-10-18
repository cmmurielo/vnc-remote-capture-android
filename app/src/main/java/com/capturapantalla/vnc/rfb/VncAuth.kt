package com.capturapantalla.vnc.rfb

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Autenticación VNC estándar (RFC 6143 Sección 7.2.2).
 * Cifra el desafío de 16 bytes enviado por el servidor utilizando DES/ECB/NoPadding
 * con los bits de cada byte de la clave invertidos.
 */
object VncAuth {

    fun generateChallengeResponse(password: String, challenge: ByteArray): ByteArray {
        require(challenge.size == 16) { "El desafío VNC debe contener exactamente 16 bytes." }

        val keyBytes = ByteArray(8)
        val pwdBytes = password.toByteArray(StandardCharsets.US_ASCII)
        val copyLength = minOf(pwdBytes.size, 8)
        System.arraycopy(pwdBytes, 0, keyBytes, 0, copyLength)

        val reversedKey = ByteArray(8) { i ->
            reverseBits(keyBytes[i])
        }

        val secretKey = SecretKeySpec(reversedKey, "DES")
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(challenge)
    }

    /**
     * Invierte los bits de un byte individual (MSB <-> LSB).
     */
    internal fun reverseBits(b: Byte): Byte {
        val reversed = Integer.reverse(b.toInt() and 0xFF) ushr 24
        return reversed.toByte()
    }
}
