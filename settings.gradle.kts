import at.skyhanni.sharedvariables.MultiVersionStage
import at.skyhanni.sharedvariables.ProjectTarget

pluginManagement {
    includeBuild("sharedVariables")
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://repo.nea.moe/releases")
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("(com|io)\\.github\\..*")
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
        when (requested.id.id) {
            "gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
    id("at.skyhanni.shared-variables")
}

MultiVersionStage.initFrom(file(".gradle/private.properties"))

include("annotation-processors")
include("detekt")
rootProject.name = "SkyHanni"
rootProject.buildFileName = "root.gradle.kts"

ProjectTarget.activeVersions().forEach { target ->
    include(target.projectPath)
    val p = project(target.projectPath)
    p.projectDir = file("versions/${target.projectName}")
    p.buildFileName = "../../build.gradle.kts"
}

