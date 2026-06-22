# ProGuard rules for Watermelon

# ── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.jvm.JvmStatic *;
}

# ── Hilt / Dagger ───────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# ── Jetpack Compose ─────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── Kotlinx Serialization ───────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Supabase / Ktor ─────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── OkHttp / Retrofit ───────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# ── Coil ────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ── Firebase ────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Razorpay ────────────────────────────────────────────────────────────────
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# ── YoutubeDL-Android ───────────────────────────────────────────────────────
-keep class com.yausername.** { *; }
-dontwarn com.yausername.**

# ── Media3 / ExoPlayer ──────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── NewPipe Extractor ───────────────────────────────────────────────────────
-keep class org.schabi.newpipe.** { *; }
-dontwarn org.schabi.newpipe.**

# ── App Models (prevent stripping data classes used in serialization) ────────
-keep class com.watermelon.domain.model.** { *; }
-keep class com.watermelon.data.remote.** { *; }
-keep class com.watermelon.data.local.** { *; }

# ── Timber (logging — no-op in release) ─────────────────────────────────────
-assumenosideeffects class timber.log.Timber {
    public static void d(...);
    public static void v(...);
    public static void i(...);
}

# ── General ─────────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keepattributes EnclosingMethod
-keepattributes InnerClasses
