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
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
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
        versions("1.21.10", "1.21.11")
    }
}
