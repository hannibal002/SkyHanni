pluginManagement {
    includeBuild("sharedVariables")
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") {
            content {
                includeGroupByRegex("net.fabricmc.*")
            }
        }
        maven("https://repo.spongepowered.org/maven/") {
            content {
                includeGroup("org.spongepowered")
            }
        }
        maven("https://repo.nea.moe/releases") {
            content {
                includeGroup("moe.nea")
            }
        }
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("(com|io)\\.github\\..*")
            }
        }
        // Stonecutter
        maven("https://maven.kikugie.dev/snapshots") {
            content {
                includeGroupByRegex("dev.kikugie.*")
            }
        }
    }
    resolutionStrategy.eachPlugin {
        requested.apply {
            if ("$id".startsWith("com.github.")) {
                val (_, _, user, name) = "$id".split(".", limit = 4)
                useModule("com.github.$user:$name:$version")
            }
        }
    }
}

plugins {
    // We can't use libs refs in settings, so these are not stored in `libs.versions.toml`
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("at.skyhanni.shared-variables")
    id("dev.kikugie.stonecutter") version "0.9"
}

include("annotation-processors")
include("detekt")
rootProject.name = "SkyHanni"
rootProject.buildFileName = "root.gradle.kts"

stonecutter {
    create(rootProject) {
        versions("1.21.11", "26.1")
        vcsVersion = "26.1"
    }
}
