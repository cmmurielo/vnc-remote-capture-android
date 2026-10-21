# AndroidX Security Crypto & Tink
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Tink optional dependencies
-dontwarn com.google.api.client.http.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.joda.time.**

# Core Models & Data classes
-keep class com.capturapantalla.vnc.core.** { *; }
-keepclassmembers class com.capturapantalla.vnc.core.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
