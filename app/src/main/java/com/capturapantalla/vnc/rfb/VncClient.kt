package com.capturapantalla.vnc.rfb

import com.capturapantalla.vnc.core.CapturedImage
import com.capturapantalla.vnc.core.Result
import com.capturapantalla.vnc.core.VncConfig
import com.capturapantalla.vnc.core.VncError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

/**
 * Cliente VNC / RFB 3.8 nativo especializado en modo estricto 'View Only':
 * - Garantiza que jamás envía eventos de teclado, ratón o portapapeles.
 * - Solicita formato de píxeles ARGB 32-bit estándar en Little Endian.
 * - Cierra la conexión de inmediato tras obtener el fotograma completo.
 */
class VncClient {

    suspend fun capture(
        config: VncConfig,
        onProgress: (String) -> Unit = {}
    ): Result<CapturedImage, VncError> = withContext(Dispatchers.IO) {
        if (config.host.isBlank()) {
            return@withContext Result.Failure(
                VncError.InvalidConfiguration("El host o dirección IP no puede estar vacío.")
            )
        }

        var socket: Socket? = null
        val timeoutMillis = (config.timeoutSeconds * 1000).toInt()

        try {
            onProgress("Conectando a ${config.host}:${config.port}...")
            val newSocket = Socket()
            socket = newSocket
            newSocket.connect(InetSocketAddress(config.host, config.port), timeoutMillis)
            newSocket.soTimeout = timeoutMillis

            val inStream = DataInputStream(BufferedInputStream(newSocket.getInputStream(), 65536))
            val outStream = DataOutputStream(BufferedOutputStream(newSocket.getOutputStream(), 65536))

            onProgress("Iniciando handshake RFB...")
            val serverVersionBytes = ByteArray(12)
            inStream.readFully(serverVersionBytes)
            val serverVersionStr = String(serverVersionBytes, StandardCharsets.US_ASCII)

            val clientVersion = "RFB 003.008\n".toByteArray(StandardCharsets.US_ASCII)
            outStream.write(clientVersion)
            outStream.flush()

            onProgress("Negociando tipo de seguridad...")
            val secCount = inStream.readUnsignedByte()
            if (secCount == 0) {
                val reasonLen = inStream.readInt()
                val reasonBytes = ByteArray(reasonLen)
                inStream.readFully(reasonBytes)
                val reason = String(reasonBytes, StandardCharsets.UTF_8)
                return@withContext Result.Failure(VncError.AuthenticationFailed("Servidor rechazó conexión: $reason"))
            }

            val securityTypes = ByteArray(secCount)
            inStream.readFully(securityTypes)

            val hasVncAuth = securityTypes.any { it.toInt() == 2 }
            val hasNone = securityTypes.any { it.toInt() == 1 }

            val chosenType = when {
                hasVncAuth && config.password.isNotEmpty() -> 2
                hasNone -> 1
                hasVncAuth -> 2
                else -> securityTypes.first().toInt()
            }

            outStream.writeByte(chosenType)
            outStream.flush()

            if (chosenType == 2) {
                onProgress("Autenticando con contraseña VNC...")
                val challenge = ByteArray(16)
                inStream.readFully(challenge)

                val response = VncAuth.generateChallengeResponse(config.password, challenge)
                outStream.write(response)
                outStream.flush()

                val securityResult = inStream.readInt()
                if (securityResult != 0) {
                    return@withContext Result.Failure(VncError.AuthenticationFailed())
                }
            } else if (chosenType == 1) {
                val securityResult = inStream.readInt()
                if (securityResult != 0) {
                    return@withContext Result.Failure(VncError.AuthenticationFailed("Acceso denegado con seguridad None"))
                }
            }

            onProgress("Inicializando sesión compartida...")
            outStream.writeByte(if (config.shared) 1 else 0)
            outStream.flush()

            val fbWidth = inStream.readUnsignedShort()
            val fbHeight = inStream.readUnsignedShort()

            val serverPixelFormat = ByteArray(16)
            inStream.readFully(serverPixelFormat)

            val nameLength = inStream.readInt()
            val nameBytes = ByteArray(nameLength)
            inStream.readFully(nameBytes)

            sendSetPixelFormat(outStream)

            sendSetEncodings(outStream)

            delay(300)

            onProgress("Descargando captura de pantalla ($fbWidth x $fbHeight)...")
            val decoder = FramebufferDecoder(fbWidth, fbHeight)

            var retries = 0
            val maxRetries = 3

            while (retries < maxRetries) {
                sendFramebufferUpdateRequest(outStream, fbWidth, fbHeight, incremental = false)
                readFramebufferUpdate(inStream, decoder)

                if (!decoder.isCompletelyBlack()) {
                    break
                }
                retries++
                if (retries < maxRetries) {
                    onProgress("Fotograma en negro detectado, refrescando ($retries/$maxRetries)...")
                    delay(600)
                }
            }

            if (decoder.isCompletelyBlack()) {
                return@withContext Result.Failure(VncError.FramebufferEmpty(retries))
            }

            onProgress("Procesando imagen capturada...")
            val bitmap = decoder.toBitmap()
            val image = CapturedImage(
                bitmap = bitmap,
                width = fbWidth,
                height = fbHeight
            )

            onProgress("Captura finalizada.")
            Result.Success(image)

        } catch (e: SocketTimeoutException) {
            Result.Failure(VncError.Timeout(config.timeoutSeconds))
        } catch (e: Exception) {
            Result.Failure(
                VncError.ConnectionFailed(
                    host = config.host,
                    port = config.port,
                    message = e.localizedMessage ?: e.javaClass.simpleName
                )
            )
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun sendSetPixelFormat(out: DataOutputStream) {
        out.writeByte(0)
        out.writeByte(0)
        out.writeByte(0)
        out.writeByte(0)

        out.writeByte(32)
        out.writeByte(24)
        out.writeByte(0)
        out.writeByte(1)
        out.writeShort(255)
        out.writeShort(255)
        out.writeShort(255)
        out.writeByte(16)
        out.writeByte(8)
        out.writeByte(0)
        out.writeByte(0)
        out.writeByte(0)
        out.writeByte(0)
        out.flush()
    }

    private fun sendSetEncodings(out: DataOutputStream) {
        out.writeByte(2)
        out.writeByte(0)
        out.writeShort(1)
        out.writeInt(0)
        out.flush()
    }

    private fun sendFramebufferUpdateRequest(
        out: DataOutputStream,
        width: Int,
        height: Int,
        incremental: Boolean
    ) {
        out.writeByte(3)
        out.writeByte(if (incremental) 1 else 0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(width)
        out.writeShort(height)
        out.flush()
    }

    private fun readFramebufferUpdate(inStream: DataInputStream, decoder: FramebufferDecoder) {
        val msgType = inStream.readUnsignedByte()
        require(msgType == 0) { "Esperaba FramebufferUpdate (tipo 0), recibido tipo $msgType" }

        inStream.readByte()
        val numRectangles = inStream.readUnsignedShort()

        val buffer = ByteArray(65536)

        for (i in 0 until numRectangles) {
            val rx = inStream.readUnsignedShort()
            val ry = inStream.readUnsignedShort()
            val rw = inStream.readUnsignedShort()
            val rh = inStream.readUnsignedShort()
            val encodingType = inStream.readInt()

            if (encodingType == 0) {
                val totalBytes = rw * rh * 4
                val rectData = if (totalBytes <= buffer.size) buffer else ByteArray(totalBytes)
                inStream.readFully(rectData, 0, totalBytes)
                decoder.decodeRawRect(rx, ry, rw, rh, rectData)
            } else {
                throw IllegalStateException("Codificación no soportada recibida: $encodingType")
            }
        }
    }
}
