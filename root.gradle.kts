import at.skyhanni.sharedvariables.ProjectTarget
import com.replaymod.gradle.preprocess.Node

plugins {
    id("com.replaymod.preprocess") version "b09f534"
    id("gg.essential.loom") version "1.6.+" apply false
    kotlin("jvm") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

allprojects {
    group = "at.hannibal2.skyhanni"
    version = "0.26.Beta.20"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://repo.spongepowered.org/maven/") // mixin
        maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") // DevAuth
        maven("https://jitpack.io") { // NotEnoughUpdates (compiled against)
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
        maven("https://repo.nea.moe/releases") // libautoupdate
        maven("https://maven.notenoughupdates.org/releases") // NotEnoughUpdates (dev env)
        maven("https://repo.hypixel.net/repository/Hypixel/") // mod-api
        maven("https://maven.teamresourceful.com/repository/thatgravyboat/") // DiscordIPC
    }
}

preprocess {
    val nodes = mutableMapOf<ProjectTarget, Node>()
    ProjectTarget.values().forEach { target ->
        nodes[target] = createNode(target.projectName, target.minecraftVersion.versionNumber, target.mappingStyle.identifier)
        val p = project(target.projectPath)
        if (target.isForge)
            p.extra.set("loom.platform", "forge")
    }
    ProjectTarget.values().forEach { child ->
        val parent = child.linkTo ?: return@forEach
        val mappingFile = file("versions/mapping-${parent.projectName}-${child.projectName}.txt")
        if (mappingFile.exists()) {
            nodes[parent]!!.link(nodes[child]!!, mappingFile)
        } else {
            nodes[parent]!!.link(nodes[child]!!)
        }
    }
}
