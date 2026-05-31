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
    }
}

stonecutter active "26.1"

stonecutter handlers {
    configure("fsh", "vsh") {
        commenter = line("//")
    }
}

stonecutter parameters {
    replacements {
        string(current.parsed < "26.1") {
            replace("AFTER_CLIENT_LEVEL_CHANGE", "AFTER_CLIENT_WORLD_CHANGE")
            replace("AfterClientLevelChange", "AfterClientWorldChange")
            replace("ClientLevelEvents", "ClientWorldEvents")
            replace("CompareOp.ALWAYS_PASS", "DepthTestFunction.NO_DEPTH_TEST")
            replace("CompareOp.LESS_THAN_OR_EQUAL", "DepthTestFunction.LEQUAL_DEPTH_TEST")
            replace("END_LEVEL_TICK", "END_WORLD_TICK")
            replace("EndLevelTick", "EndWorldTick")
            replace("START_LEVEL_TICK", "START_WORLD_TICK")
            replace("StartLevelTick", "StartWorldTick")
            replace("classTweaker v1 official", "classTweaker v1 named")
            replace("net.minecraft.client.multiplayer.chat.GuiMessage", "net.minecraft.client.GuiMessage")
            replace("net.minecraft.client.multiplayer.chat.GuiMessageTag", "net.minecraft.client.GuiMessageTag")
            replace("net.minecraft.client.renderer.state.gui", "net.minecraft.client.gui.render.state")
        }

        regex(current.parsed < "26.1") {
            replace("\\bContainerInput\\b" to "ClickType", "\\bClickType\\b" to "ContainerInput")
            replace("\\bGuiGraphicsExtractor\\b" to "GuiGraphics", "\\bGuiGraphics\\b" to "GuiGraphicsExtractor")
        }
    }

    filters.include("**/*.fsh", "**/*.vsh")
}
