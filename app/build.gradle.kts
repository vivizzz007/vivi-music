@file:Suppress("UnstableApiUsage")

val isFullBuild: Boolean by rootProject.extra

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

if (isFullBuild && System.getenv("PULL_REQUEST") == null) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}


android {
    namespace = "com.music.vivi"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.vivi.vivimusic"
        minSdk = 24
        targetSdk = 35
        versionCode = 40
        versionName = "2.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("full") {
            dimension = "version"
        }
        create("foss") {
            dimension = "version"
        }
    }

    //splits {
    // abi {
    //   isEnable = true
    //   reset()
    //   include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    //  isUniversalApk = false
    // }
    // }

    signingConfigs {
        getByName("debug") {
            if (System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD") != null) {
                storeFile = file(System.getenv("MUSIC_DEBUG_KEYSTORE_FILE"))
                storePassword = System.getenv("MUSIC_DEBUG_SIGNING_STORE_PASSWORD")
                keyAlias = "debug"
                keyPassword = System.getenv("MUSIC_DEBUG_SIGNING_KEY_PASSWORD")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
    lint {
        disable += "MissingTranslation"
        disable += "MissingQuantity"
        disable += "ImpliedQuantity"
    }
    // avoid DEPENDENCY_INFO_BLOCK for IzzyOnDroid
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    androidResources{
        generateLocaleConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.guava)
    implementation(libs.coroutines.guava)
    implementation(libs.concurrent.futures)

    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.datastore)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.reorderable1)
    implementation(libs.compose.reorderable2)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(projects.materialColorUtilities)
    implementation(libs.squigglyslider)
    implementation(libs.compose.icons.extended)

    implementation(libs.coil)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.material)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    kapt(libs.hilt.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.kizzy)

    implementation(libs.ktor.client.core)

    coreLibraryDesugaring(libs.desugaring)

    "fullImplementation"(platform(libs.firebase.bom))
    "fullImplementation"(libs.firebase.analytics)
    "fullImplementation"(libs.firebase.crashlytics)
    "fullImplementation"(libs.firebase.config)
    "fullImplementation"(libs.firebase.perf)
    "fullImplementation"(libs.mlkit.language.id)
    "fullImplementation"(libs.mlkit.translate)
    "fullImplementation"(libs.opencc4j)

    implementation(libs.timber)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.lottie.compose)
    // For JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation ("androidx.activity:activity-compose:1.8.0")
    implementation ("androidx.compose.material3:material3:1.1.2")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.4")
    debugImplementation ("androidx.compose.ui:ui-tooling:1.5.4")
//    implementation("androidx.compose.ui:ui:1.3.0")
// or a later version
    implementation("androidx.activity:activity-compose:1.5.0")
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("org.json:json:20231013")
    implementation ("androidx.compose:compose-bom:2023.08.00") // Use the latest stable BOM
    implementation ("androidx.compose.ui:ui")
    implementation ("androidx.compose.ui:ui-graphics")
    implementation ("androidx.compose.ui:ui-tooling-preview")
    // OLD: implementation 'androidx.compose.material:material'
    implementation ("androidx.compose.material3:material3:1.2.1") // <--- ADD THIS FOR MATERIAL 3 (check for latest stable version)
    implementation ("androidx.activity:activity-compose:1.8.0")
    debugImplementation ("androidx.compose.ui:ui-tooling")
    debugImplementation ("androidx.compose.ui:ui-test-manifest")
    implementation ("androidx.compose.runtime:runtime:1.0.0")
    implementation ("androidx.work:work-runtime-ktx:2.7.1")
    implementation ("androidx.compose.ui:ui:1.6.5")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.compose.foundation:foundation-layout:1.6.5")
    implementation ("androidx.activity:activity-compose:1.8.0")
    implementation ("androidx.compose.material3:material3:1.2.0")
    implementation ("com.airbnb.android:lottie-compose:6.0.0")
    implementation ("org.json:json:20231013")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation ("com.google.zxing:core:3.5.1")
    implementation ("androidx.core:core-ktx:1.10.1")

    implementation ("androidx.media3:media3-exoplayer:1.2.1")
    implementation ("androidx.navigation:navigation-compose:2.7.6")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("io.coil-kt:coil-compose:2.4.0")
    implementation ("com.airbnb.android:lottie-compose:6.1.0") // For Lottie animations
    implementation ("androidx.hilt:hilt-navigation-compose:1.2.0") // For Hilt with Compose
    implementation ("androidx.media3:media3-exoplayer:1.2.0") // For media playback
    implementation ("androidx.activity:activity-compose:1.9.0") // For Compose Activity
    implementation ("com.google.android.material:material:1.12.0")
    implementation("com.google.zxing:core:3.5.1")
    implementation ("com.google.android.material:material:1.9.0")
    // If you're using Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation ("com.google.zxing:core:3.5.2")
    implementation ("androidx.compose.ui:ui:1.6.0")
    implementation ("androidx.compose.foundation:foundation:1.6.0")
    implementation ("androidx.compose.material:material:1.6.0")
    implementation ("com.google.zxing:core:3.5.3")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation("io.coil-kt:coil:2.6.0")

    implementation (libs.androidx.runtime.v160) // or latest version
    implementation (libs.androidx.runtime.livedata)
    implementation (libs.ui.v160)
    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    // For Android Settings
    implementation (libs.core.ktx)// or lates
// Or the latest version compatible with your setup

}
