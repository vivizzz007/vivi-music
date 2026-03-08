@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { 
            setUrl("https://jitpack.io") 
            content {
                excludeGroup("com.github.promeG")
                excludeGroup("com.github.promeg")
                excludeGroup("com.github.promeG.tinypinyin")
            }
        }
        maven {
            setUrl("https://maven.aliyun.com/repository/public")
            content {
                includeGroup("com.github.promeg")
                includeGroup("com.github.promeG")
                includeGroup("com.github.promeG.tinypinyin")
            }
        }
    }
}

// F-Droid doesn't support foojay-resolver plugin
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
}

rootProject.name = "vivimusic"
include(":app")
include(":canvas")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":kizzy")
include(":lastfm")
include(":betterlyrics")
include(":simpmusic")
include(":youlyplus")
include(":shazamkit")
include(":artistvideo")

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that vivimusic and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in innertube/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.newpipe.extractor)
// To:
//      implementation("com.github.teamnewpipe:NewPipeExtractor")
// includeBuild("../NewPipeExtractor") {
//     dependencySubstitution {
//         substitute(module("com.github.TeamNewPipe:NewPipeExtractor")).using(project(":extractor"))
//     }
// }

// includeBuild("../TinyPinyin") {
//     dependencySubstitution {
//         substitute(module("com.github.promeG:tinypinyin")).using(project(":lib"))
//     }
// }
