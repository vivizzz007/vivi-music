plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.music.vivi.artistvideo"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.animation)
    implementation(libs.material3)
    
    // ExoPlayer for playing the artist video
    implementation(libs.media3)
    implementation(libs.media3.ui)
    implementation(libs.media3.okhttp)
    
    coreLibraryDesugaring(libs.desugaring)
}
