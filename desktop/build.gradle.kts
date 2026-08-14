import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// Single source of truth for the desktop release version (read by CI too).
// Desktop releases carry a `-DE` suffix (e.g. 6.0.5-DE) to distinguish them
// from Android releases.
val desktopVersion: String = rootProject.file("version.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.firstOrNull()
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "6.0.5-DE"

// jpackage requires a purely numeric MAJOR.MINOR.PATCH on Windows and macOS
// (see JDK-8283707), so strip the `-DE` suffix (and any SemVer pre-release /
// build metadata) to derive the numeric version used inside the packages.
// The full `-DE` version still appears on version.txt, the GitHub release
// tag/title and the artifact filenames (applied in the CI workflows).
val numericPackageVersion: String = desktopVersion
    .substringBefore('+')
    .substringBefore('-')

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
    implementation(project(":sync"))

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
            )
            packageName = "VIVIMusic"
            packageVersion = numericPackageVersion
            description = "VIVI Music — desktop client"
            vendor = "VIVI Music"

            windows {
                menuGroup = "VIVI Music"
                // Machine-wide install into Program Files (requires admin/UAC).
                perUserInstall = false
                installationPath = "C:/Program Files/VIVIMusic"
                iconFile.set(project.file("icons/logo_vmde.ico"))
            }

            linux {
                debMaintainer = "VIVI Music"
                iconFile.set(project.file("icons/logo_vmde.png"))
            }

            macOS {
                bundleID = "com.vivi.vivimusic.desktop"
                minimumSystemVersion = "10.15"
                iconFile.set(project.file("icons/logo_vmde.icns"))
            }
        }
    }
}
