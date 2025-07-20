import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.music.vivi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vivi.vivimusic"
        minSdk = 26
        targetSdk = 36
        versionCode = 45
        versionName = "3.0.4"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "abi"
    productFlavors {
        create("universal") {
            dimension = "abi"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
            buildConfigField("String", "ARCHITECTURE", "\"universal\"")
        }
        create("arm64") {
            dimension = "abi"
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("String", "ARCHITECTURE", "\"arm64\"")
        }
        create("armeabi") {
            dimension = "abi"
            ndk { abiFilters += "armeabi-v7a" }
            buildConfigField("String", "ARCHITECTURE", "\"armeabi\"")
        }
        create("x86") {
            dimension = "abi"
            ndk { abiFilters += "x86" }
            buildConfigField("String", "ARCHITECTURE", "\"x86\"")
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters += "x86_64" }
            buildConfigField("String", "ARCHITECTURE", "\"x86_64\"")
        }
    }

    signingConfigs {
        // ✅ Only release config remains
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            // ✅ Uses Android Studio's default debug keystore
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/CONTRIBUTORS.md"
            excludes += "META-INF/LICENSE.md"
        }
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
    implementation(libs.compose.animation)
    implementation(libs.compose.reorderable)

    implementation(libs.viewmodel)
    implementation(libs.viewmodel.compose)

    implementation(libs.material3)
    implementation(libs.palette)
    implementation(libs.materialKolor)

    implementation(libs.coil)

    implementation(libs.shimmer)

    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)
    implementation(libs.squigglyslider)

    implementation(libs.room.runtime)
    implementation(libs.kuromoji.ipadic)
    implementation(libs.animation)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    implementation(libs.apache.lang3)

    implementation(libs.hilt)
    implementation(libs.jsoup)
    kapt(libs.hilt.compiler)

    implementation(project(":innertube"))
    implementation(project(":kugou"))
    implementation(project(":lrclib"))
    implementation(project(":kizzy"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.serialization.json)

    coreLibraryDesugaring(libs.desugaring)

    implementation(libs.multidex)

    implementation(libs.timber)


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

    implementation("com.google.accompanist:accompanist-flowlayout:0.30.1")
    // For Android Settings

    implementation ("androidx.compose.ui:ui:1.5.4")
    implementation ("androidx.compose.ui:ui-unit:1.5.4")
// Or the latest version compatible with your setup


    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.compose.material:material-icons-extended")


}




