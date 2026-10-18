# VNC Remote Capture - Aplicación Nativa para Android


Aplicación nativa de Android desarrollada en **Kotlin** y **Jetpack Compose** con diseño moderno **Material 3**. Permite conectarse a cualquier servidor VNC (especialmente en redes privadas como **Tailscale** o redes locales), capturar la pantalla remota en su resolución original, visualizarla con gestos táctiles de zoom y paneo, guardarla en la Galería de fotos (`MediaStore`) y compartirla directamente.

---

## Características Principales

* **Protocolo RFB 3.8 Puro y Ligero:**
  * Implementado directamente sobre `Socket` asíncrono con Coroutines de Kotlin.
  * Cero dependencias nativas complejas en C/C++; no requiere NDK.
  * Soporta autenticación VNC estándar (RFC 6143 con cifrado DES y bits invertidos) y conexiones sin contraseña.
* **Garantía Estricta 'View Only':**
  * Solo solicita y lee fotogramas. Jamás envía eventos de pulsación de teclado, movimientos/clics de ratón ni sincronización de portapapeles.
* **Visualizador Táctil Interactivo:**
  * Soporte de gestos táctiles: *pinch-to-zoom* (hasta 6x), paneo libre y reinicio rápido de zoom con doble toque.
* **Integración Nativa con Android:**
  * **Guardado en Galería:** Utiliza la API moderna de `MediaStore` para almacenar las capturas en `Pictures/CapturasRemotas/` sin solicitar permisos intrusivos de almacenamiento en Android 10+ (API 29+).
  * **Compartir Directo:** `FileProvider` configurado para enviar la captura a WhatsApp, Telegram, correo o Drive con un solo toque.
* **Persistencia de Configuración:**
  * Recuerda automáticamente el último Host, Puerto, Contraseña y Calidad configurados.
* **Arquitectura Limpia y Patrones Modernos:**
  * Patrón **Railway Oriented Programming** con `Result<T, E>` y tipos sellados (`sealed interface VncError`).
  * Inyección de dependencias por constructor y arquitectura MVVM con `StateFlow`.

---

## Requisitos

* **Android:** Android 8.0 (Oreo / API 26) o superior.
* **Conectividad:** Si el host VNC es una IP de Tailscale (ej. `100.105.103.8`), la app oficial de **Tailscale para Android** debe estar conectada en el teléfono.
* **Entorno de compilación:**
  * Android Studio Ladybug / Koala o superior.
  * JDK 17 LTS (configurado automáticamente en `gradle.properties`).
  * Android SDK con plataforma `android-35`.

---

## Estructura del Proyecto

```
android/
├── gradle/
│   ├── libs.versions.toml             # Version Catalog centralizado
│   └── wrapper/                       # Gradle Wrapper 8.11.1
├── app/
│   ├── build.gradle.kts               # Configuración del módulo Android
│   └── src/main/
│       ├── AndroidManifest.xml        # Permisos y FileProvider
│       ├── res/                       # Iconos vectoriales, colores y temas
│       └── java/com/capturapantalla/vnc/
│           ├── MainActivity.kt        # Actividad principal
│           ├── core/
│           │   ├── Result.kt          # Sealed interface Result<T, E>
│           │   ├── VncConfig.kt       # Modelos inmutables de configuración
│           │   └── VncError.kt        # Dominio algebraico de errores
│           ├── rfb/
│           │   ├── VncClient.kt       # Cliente socket RFB 3.8
│           │   ├── VncAuth.kt         # Cifrado DES conforme a RFC 6143
│           │   └── FramebufferDecoder.kt # Decodificación RAW 32-bit a Bitmap
│           ├── data/
│           │   ├── PreferencesRepository.kt # Guardado de configuración
│           │   └── ScreenshotStorage.kt     # MediaStore y FileProvider
│           └── ui/
│               ├── MainViewModel.kt   # Gestor de estado StateFlow
│               ├── theme/             # Tema oscuro Material 3
│               ├── components/        # Tarjetas de config, estado y zoom
│               └── screens/           # Pantalla principal Compose
```

---

## Compilación y Generación de la APK

### 1. Compilar APK de Depuración (Debug)
Desde una terminal en la carpeta `android/`:
```bash
./gradlew assembleDebug
```
La APK se generará en:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

### 2. Instalar directamente en el teléfono por cable USB (ADB)
Con la depuración USB activa en el teléfono:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Abrir en Android Studio
1. Abre **Android Studio**.
2. Selecciona **Open** y navega hasta la carpeta `c:/Dev/CapturaPantallaRemota/android`.
3. Deja que Gradle sincronice las dependencias y presiona el botón verde de **Run (Shift + F10)**.
