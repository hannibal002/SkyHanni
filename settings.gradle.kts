import at.skyhanni.sharedvariables.MultiVersionStage

pluginManagement {
    includeBuild("sharedVariables")
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.fabricmc.net")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.nea.moe/releases")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("(com|io)\\.github\\..*")
            }
        }
        maven("https://maven.kikugie.dev/snapshots") // stone cutter
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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("at.skyhanni.shared-variables")
    id("dev.kikugie.stonecutter") version "0.8.3"
}

MultiVersionStage.initFrom(file(".gradle/private.properties"))

include("annotation-processors")
include("detekt")
rootProject.name = "SkyHanni"
rootProject.buildFileName = "root.gradle.kts"

stonecutter {
    create(rootProject) {
        fun setup(version: String) {
            if (stonecutter.eval(version, ">1.21.11")) {
                version(version).buildscript = "build.deobf.gradle.kts"
            } else {
                version(version).buildscript = "build.gradle.kts"
            }
        }
        setup("1.21.10")
        setup("1.21.11")
        setup("26.1")
    }
}
