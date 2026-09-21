# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ViewComponentBuilderEntryPoint { *; }

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Keep data classes for Gson serialization
-keep class br.com.ticket.companion.data.remote.dto.** { *; }
-keep class br.com.ticket.companion.data.local.entities.** { *; }
-keep class br.com.ticket.companion.domain.connection.Connection { *; }
-keep class br.com.ticket.companion.domain.connection.RetiredConnection { *; }

# Tink references optional Error Prone compile-time annotations.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Ponte JS do atendimento web: o R8 não pode renomear/remover os métodos chamados pela página.
-keepclassmembers class br.com.ticket.companion.ui.web.WebActivity$NativeNotificationBridge {
    @android.webkit.JavascriptInterface <methods>;
}
