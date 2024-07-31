import at.skyhanni.sharedvariables.ProjectTarget

pluginManagement {
    includeBuild("sharedVariables")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.nea.moe/releases")
        maven("https://repo.sk1er.club/repository/maven-releases/")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.replaymod.preprocess" -> useModule("com.github.replaymod:preprocessor:${requested.version}")
                "gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.6.0")
    id("at.skyhanni.shared-variables")
}

include("annotation-processors")
rootProject.name = "SkyHanni"
rootProject.buildFileName = "root.gradle.kts"

ProjectTarget.values().forEach { target ->
    include(target.projectPath)
    val p = project(target.projectPath)
    p.projectDir = file("versions/${target.projectName}")
    p.buildFileName = "../../build.gradle.kts"
}

