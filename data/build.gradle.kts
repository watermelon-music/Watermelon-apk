import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val supabaseProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun getSupabaseProperty(name: String, defaultValue: String = ""): String {
    return supabaseProperties.getProperty(name, defaultValue)
}

android {
    namespace = "com.watermelon.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "SUPABASE_URL", "\"${getSupabaseProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_KEY", "\"${getSupabaseProperty("SUPABASE_KEY")}\"")
            buildConfigField("String", "PODCAST_INDEX_API_KEY", "\"${getSupabaseProperty("PODCAST_INDEX_API_KEY")}\"")
            buildConfigField("String", "PODCAST_INDEX_SECRET", "\"${getSupabaseProperty("PODCAST_INDEX_SECRET")}\"")
            buildConfigField("String", "JAMENDO_CLIENT_ID", "\"${getSupabaseProperty("JAMENDO_CLIENT_ID")}\"")
            buildConfigField("String", "WATERMELON_API_URL", "\"${getSupabaseProperty("WATERMELON_API_URL", "https://watermelon-api-oxx2.onrender.com/")}\"")
        }
        release {
            buildConfigField("String", "SUPABASE_URL", "\"${getSupabaseProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_KEY", "\"${getSupabaseProperty("SUPABASE_KEY")}\"")
            buildConfigField("String", "PODCAST_INDEX_API_KEY", "\"${getSupabaseProperty("PODCAST_INDEX_API_KEY")}\"")
            buildConfigField("String", "PODCAST_INDEX_SECRET", "\"${getSupabaseProperty("PODCAST_INDEX_SECRET")}\"")
            buildConfigField("String", "JAMENDO_CLIENT_ID", "\"${getSupabaseProperty("JAMENDO_CLIENT_ID")}\"")
            buildConfigField("String", "WATERMELON_API_URL", "\"${getSupabaseProperty("WATERMELON_API_URL", "https://watermelon-api-oxx2.onrender.com/")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Supabase
    implementation(libs.supabase.core)
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)

    // Ktor client for Supabase
    implementation(libs.ktor.client.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)

    // YouTube
    implementation(libs.newpipe.extractor)

    // yt-dlp (URL extraction only — ffmpeg not needed)
    implementation(libs.youtubedl.android.library)

    // Logging
    implementation(libs.timber)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
