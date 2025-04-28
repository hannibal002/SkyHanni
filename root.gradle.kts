import at.skyhanni.sharedvariables.ProjectTarget
import com.replaymod.gradle.preprocess.Node

plugins {
    id("com.github.SkyHanniStudios.SkyHanni-Preprocessor") version "20415a5ee3"
    id("gg.essential.loom") version "1.9.29" apply false
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.power-assert") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.24" apply false
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
}

allprojects {
    group = "at.hannibal2.skyhanni"

    /**
     * The version of the project.
     * Stable version
     * Beta version
     * Bugfix version
     */
    version = "3.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.minecraftforge.net") {
            metadataSources {
                artifact() // We love missing POMs
            }
        }
        maven("https://repo.spongepowered.org/maven/") // mixin
        maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") // DevAuth
        maven("https://jitpack.io") {
            // NotEnoughUpdates (compiled against)
            content {
                includeGroupByRegex("(com|io)\\.github\\..*")
            }
        }
        maven("https://repo.nea.moe/releases") // libautoupdate
        maven("https://maven.notenoughupdates.org/releases") // NotEnoughUpdates (dev env)
        maven("https://repo.hypixel.net/repository/Hypixel/") // mod-api
        maven("https://maven.teamresourceful.com/repository/thatgravyboat/") // DiscordIPC
        maven("https://api.modrinth.com/maven") // Modrinth
    }
}

preprocess {
    val nodes = mutableMapOf<ProjectTarget, Node>()
    ProjectTarget.activeVersions().forEach { target ->
        nodes[target] = createNode(target.projectName, target.minecraftVersion.versionNumber, target.mappingStyle.identifier)
        val p = project(target.projectPath)
        if (target.isForge) {
            p.extra.set("loom.platform", "forge")
        }
    }

    fun File.ifExists(modifier: String = ""): File? = if (exists()) {
        println("Loading ${modifier}mappings from $this")
        this
    } else {
        println("Skipped loading ${modifier}mappings from $this")
        null
    }

    ProjectTarget.activeVersions().forEach { child ->
        val parent = child.linkTo ?: return@forEach
        val pNode = nodes[parent]
        if (pNode == null) {
            println("Parent target to ${child.projectName} not available in this multi version stage. Not setting parent.")
            return@forEach
        }
        val mappingFile = file("versions/mapping-${parent.projectName}-${child.projectName}.txt").ifExists()
        val patternMappingsFile = file("versions/pattern-mappings-${parent.projectName}-${child.projectName}.txt").ifExists("pattern ")

        pNode.link(nodes[child]!!, mappingFile, patternMappingsFile)
    }
}
