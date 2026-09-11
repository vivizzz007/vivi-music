plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.music.vivi.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vivi.vivimusic"
        minSdk = 30
        targetSdk = 34
        versionCode = 10001
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".wear"
            isDebuggable = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=androidx.wear.compose.material3.ExperimentalWearMaterial3Api"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    // Shared Core Architecture
    implementation(project(":core-database"))
    implementation(project(":innertube"))
    implementation(project(":spotify"))

    // Wear Compose Material 3
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.compose.ui.tooling)

    // Wear Tiles & ProtoLayout
    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.material)
    implementation(libs.wear.tiles.tooling)
    implementation(libs.wear.tiles.tooling.preview)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material3)
    implementation(libs.wear.protolayout.expression)

    // Wear Complications & Watchface
    implementation(libs.wear.watchface.complications.data)
    implementation(libs.wear.watchface.complications.data.source)
    implementation(libs.wear.watchface.complications.data.source.ktx)

    // Wear Core & Play Services
    implementation(libs.wear)
    implementation(libs.play.services.wearable)

    // Compose Foundation & Runtime
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation(libs.activity)
    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Media3 Audio Playback, Session & Offline Cache
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.hls)
    implementation(libs.media3.datasource)
    implementation(libs.media3.database)

    // Networking & Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Image Rendering
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // DataStore
    implementation(libs.datastore)

    // QR Code Generation
    implementation(libs.zxing.core)

    // Desugaring & Logging
    implementation(libs.timber)
    coreLibraryDesugaring(libs.desugaring)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
}
