import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// version.txt is the single source of truth for release metadata:
//   line 1 = mobile (Android) version, e.g. 6.0.5
//   line 2 = desktop ("DE") version, e.g. 1.0.0  (the program's own SemVer)
//   line 3 = release channel: stable / rc / beta / alpha / nightly
//   line 4 = desktop version code (monotonic counter, e.g. 57)
// The human-readable desktop version is "<mobile>_DE-<de>", e.g. 6.0.5_DE-1.0.0.
val versionLines: List<String> = rootProject.file("version.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.map { it.trim() }
    ?: emptyList()

val mobileVersion: String = versionLines.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val deVersion: String = versionLines.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: "0.0.0"
val releaseChannel: String = versionLines.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: "stable"
val fullVersion: String = "${mobileVersion}_DE-${deVersion}"

// Installers/package managers require a purely numeric MAJOR.MINOR.PATCH on
// Windows and macOS (jpackage JDK-8283707, Inno Setup AppVersion). That numeric
// version is the DE version — the part after "DE-".
val numericPackageVersion: String = deVersion.substringBefore('+').substringBefore('-')

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Ship version.txt + CHANGELOG.md as classpath resources so the About screen
// can read build metadata and the changelog at runtime (config-cache friendly).
tasks.processResources {
    from(rootProject.file("version.txt"))
    from(rootProject.file("CHANGELOG.md"))
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Material 3 tonal color palette (same seed-based scheme as the Android app)
    implementation(libs.materialKolor)

    // Reused JVM-pure network/parsing modules (same code as the Android app)
    implementation(project(":innertube"))
    implementation(project(":spotify"))
    implementation(project(":lastfm"))
    implementation(project(":kizzy"))
    implementation(project(":shazamkit"))
    implementation(project(":jiosaavn"))
    implementation(project(":lyricsProvider"))
    implementation(project(":sync"))
    implementation(project(":canvas"))
    implementation(project(":applecanvas"))
    implementation(project(":vivimusiccanvas"))

    implementation(libs.kotlinx.coroutines.core)

    // Thumbnail / artwork loading (Coil 3, desktop JVM support)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Pure-Java MP4 (incl. fragmented/DASH fMP4) demuxer + bundled JAAD AAC decoder
    // for self-contained desktop audio playback.
    implementation("org.jcodec:jcodec:0.2.5")

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Local LAN relay (embedded WebSocket server) for offline device pairing
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    // QR code (ZXing) + mDNS service registration (JmDNS) for LAN discovery
    implementation(libs.zxing.core)
    implementation(libs.jmdns)

    // Drag-to-reorder for the Queue screen (same lib as the Android app)
    implementation(libs.compose.reorderable)
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
            // jlink bundles only a minimal set of modules by default. The dev
            // tools (CPU/RAM/thread stats) use `java.lang.management` and the
            // richer `com.sun.management` bean; without these modules the
            // packaged launcher crashes on startup with "Failed to launch JVM".
            modules("java.management", "jdk.management")
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
