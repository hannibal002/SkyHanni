plugins {
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT" apply false
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.power-assert") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.2.21-2.0.4" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.7" apply false
    id("dev.kikugie.stonecutter")
}

allprojects {
    group = "at.hannibal2.skyhanni"

    val buildToolsPath = when (name) {
        "SkyHanni" -> layout.projectDirectory.dir("buildTools")
        "annotation-processors", "detekt" -> layout.projectDirectory.dir("../buildTools")
        else -> layout.projectDirectory.dir("../../buildTools")
    }

    /**
     * The version of the project.
     * Stable version
     * Beta version
     * Bugfix version
     */
    version = providers.fileContents(buildToolsPath.file("PROJECT_VERSION")).asText.map { it.trim() }.get()

    repositories {
        mavenCentral()
        mavenLocal()

        // Fabric
        exclusiveContent {
            forRepository {
                maven("https://maven.fabricmc.net")
            }
            filter {
                includeGroup("net.fabricmc")
                includeGroup("net.fabricmc.fabric-api")
            }
        }

        // Mixin
        exclusiveContent {
            forRepository {
                maven("https://repo.spongepowered.org/repository/maven-public")
            }
            filter {
                includeGroup("org.spongepowered")
            }
        }

        // DevAuth
        exclusiveContent {
            forRepository {
                maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
            }
            filter {
                includeGroup("me.djtheredstoner")
            }
        }

        // libautoupdate and shots
        exclusiveContent {
            forRepository {
                maven("https://repo.nea.moe/releases")
            }
            filter {
                includeGroup("moe.nea")
            }
        }

        // moulconfig and a few detekt rules
        exclusiveContent {
            forRepository {
                maven("https://maven.notenoughupdates.org/releases")
            }
            filter {
                includeGroup("org.notenoughupdates")
                includeGroup("org.notenoughupdates.moulconfig")
            }
        }

        // Hypixel mod api
        exclusiveContent {
            forRepository {
                maven("https://repo.hypixel.net/repository/Hypixel")
            }
            filter {
                includeGroup("net.hypixel")
            }
        }

        // Modrinth
        exclusiveContent {
            forRepository {
                maven("https://api.modrinth.com/maven")
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }

        // Rei for compat plugin
        exclusiveContent {
            forRepository {
                maven("https://maven.shedaniel.me")
            }
            filter {
                includeGroup("me.shedaniel")
                includeGroup("dev.architectury")
                includeGroup("me.shedaniel.cloth")
            }
        }

        maven("https://jitpack.io") {
            // NotEnoughUpdates (compiled against), Changelog builder, Preprocessor, Discord IPC
            content {
                includeGroupByRegex("(com|io)\\.github\\..*")
            }
        }
    }
}

stonecutter active "1.21.5"

stonecutter parameters {
    replacements {
        string(current.parsed >= "1.21.6") {
            replace("RenderPipelines.MATRICES_SNIPPET", "RenderPipelines.MATRICES_PROJECTION_SNIPPET")
            replace("net.minecraft.SharedConstants.getCurrentVersion().name", "net.minecraft.SharedConstants.getCurrentVersion().name()")
            replace("Font.StringRenderOutput", "Font.PreparedTextBuilder")
        }
        string(current.parsed >= "1.21.10") {
            replace("Minecraft.getInstance().window.window", "Minecraft.getInstance().window.handle()")
            replace(".skin.texture()", ".skin.body()")
            replace("net.minecraft.client.resources.PlayerSkin", "net.minecraft.world.entity.player.PlayerSkin")
            replace("BeaconRenderer.renderBeaconBeam", "BeaconRenderer.submitBeaconBeam")
            replace("net.minecraft.client.gui.font.glyphs.BakedGlyph", "net.minecraft.client.gui.font.glyphs.BakedSheetGlyph")
        }
    }
}