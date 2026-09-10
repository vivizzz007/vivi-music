plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.music.vivi.db"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
        getByName("test").assets.srcDirs("$projectDir/schemas")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Room Database
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    // Shared Project Modules
    api(project(":innertube"))
    implementation(project(":spotify"))

    // Kotlin Coroutines
    implementation(libs.coroutines.guava)

    // Compose Runtime (for @Immutable annotations on models)
    implementation(libs.compose.runtime)

    // Search Indexing, Collation & CJK Tokenizers
    implementation(libs.kuromoji.ipadic)
    implementation(libs.tinypinyin)
    implementation(libs.apache.lang3)

    // AndroidX Core KTX & Ktor
    implementation(libs.androidx.core.ktx)
    implementation(libs.ktor.client.core)

    // Logging & Desugaring
    implementation(libs.timber)
    coreLibraryDesugaring(libs.desugaring)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
}
