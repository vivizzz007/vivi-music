import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Reused JVM-pure network/parsing modules (same code as the Android app)
    implementation(project(":innertube"))
    implementation(project(":spotify"))
    implementation(project(":lastfm"))
    implementation(project(":kizzy"))
    implementation(project(":shazamkit"))
    implementation(project(":jiosaavn"))
    implementation(project(":lyricsProvider"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
}

compose.desktop {
    application {
        mainClass = "com.music.vivi.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Deb,
                TargetFormat.AppImage,
            )
            packageName = "VIVIMusic"
            packageVersion = "6.0.5"
            description = "VIVI Music — desktop client"
            vendor = "VIVI Music"

            windows {
                menuGroup = "VIVI Music"
                // per-user install keeps Windows 10+ happy without admin rights
                perUserInstall = true
            }

            linux {
                debMaintainer = "VIVI Music"
            }

            macOS {
                bundleID = "com.vivi.vivimusic.desktop"
                minimumSystemVersion = "10.15"
            }
        }
    }
}
