plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.powerAssert) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
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
            forRepositories(
                repositories.mavenLocal(),
                repositories.maven("https://maven.notenoughupdates.org/releases"),
            )
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
        maven("https://maven.gegy.dev/releases/") // mojbackward
    }
}

stonecutter active "26.1"

stonecutter handlers {
    inherit("accesswidener", "classtweaker")

    configure("fsh", "vsh") {
        commenter = line("//")
    }
}

stonecutter parameters {
    replacements {
        string(current.parsed >= "26.1") {
            replace("ClientWorldEvents", "ClientLevelEvents")
            replace("START_WORLD_TICK", "START_LEVEL_TICK")
            replace("StartWorldTick", "StartLevelTick")
            replace("END_WORLD_TICK", "END_LEVEL_TICK")
            replace("EndWorldTick", "EndLevelTick")
            replace("AFTER_CLIENT_WORLD_CHANGE", "AFTER_CLIENT_LEVEL_CHANGE")
            replace("AfterClientWorldChange", "AfterClientLevelChange")
            replace("GuiGraphics", "GuiGraphicsExtractor")
            replace("import net.minecraft.client.GuiMessage", "import net.minecraft.client.multiplayer.chat.GuiMessage")
            replace("import net.minecraft.client.GuiMessageTag", "import net.minecraft.client.multiplayer.chat.GuiMessageTag")

            listOf(
                "GlyphRenderState",
                "GuiElementRenderState",
                "GuiRenderState",
                "GuiItemRenderState",
                "BlitRenderState",
                "pip.PictureInPictureRenderState"
            ).forEach {
                replace(
                    "import net.minecraft.client.gui.render.state.$it",
                    "import net.minecraft.client.renderer.state.gui.$it"
                )
            }

            replace("DepthTestFunction.LEQUAL_DEPTH_TEST", "CompareOp.LESS_THAN_OR_EQUAL")
            replace("DepthTestFunction.NO_DEPTH_TEST", "CompareOp.ALWAYS_PASS")
        }

        regex(current.parsed >= "26.1") {
            replace("\bClickType\b" to "ContainerInput", "\bContainerInput\b" to "ClickType")
        }
    }

    filters.include("**/*.fsh", "**/*.vsh")
}
