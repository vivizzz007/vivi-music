package com.music.vivi.desktop

enum class DesktopOs {
    WINDOWS,
    MACOS,
    LINUX,
}

enum class DesktopArch {
    X64,
    ARM64,
}

/** Host platform detection used to pick the right update installer asset. */
object Platform {
    val os: DesktopOs = when {
        System.getProperty("os.name").lowercase().contains("win") -> DesktopOs.WINDOWS
        System.getProperty("os.name").lowercase().contains("mac") -> DesktopOs.MACOS
        else -> DesktopOs.LINUX
    }

    val arch: DesktopArch = when (System.getProperty("os.arch").lowercase()) {
        "aarch64", "arm64" -> DesktopArch.ARM64
        else -> DesktopArch.X64
    }
}
