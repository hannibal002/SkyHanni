import dev.kikugie.stonecutter.StonecutterExperimentalAPI

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

stonecutter active "26.2"

stonecutter handlers {
    configure("fsh", "vsh") {
        commenter = line("//")
    }
}

stonecutter parameters {
    replacements {
        string(current.parsed < "26.2") {
            replace(".gameRenderer.featureRenderDispatcher()", ".gameRenderer.getFeatureRenderDispatcher()")
            replace(".gameRenderer.gameRenderState()", ".gameRenderer.getGameRenderState()")
            replace(".gameRenderer.lighting()", ".gameRenderer.getLighting()")
            replace(".gui.hud.isHidden", ".options.hideGui")
            replace(".gui.setScreen(", ".setScreen(")
            replace(".mainCamera()", ".mainCamera")
            replace("Minecraft.getInstance().gui.screen()", "Minecraft.getInstance().screen")
            replace("gameRenderer.featureRenderDispatcher()", "gameRenderer.getFeatureRenderDispatcher()")
            replace("gameRenderer.gameRenderState()", "gameRenderer.getGameRenderState()")
            replace("gui.hud.chat", "gui.chat")
            replace("gui.hud.guiTicks", "gui.guiTicks")
            replace("gui.hud.tabList", "gui.tabList")
            replace("levelExtractor.allChanged()", "levelRenderer.allChanged()")
            replace(
                "net.minecraft.world.entity.monster.cubemob.MagmaCube",
                "net.minecraft.world.entity.monster.MagmaCube",
            )
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")
        }

        @OptIn(StonecutterExperimentalAPI::class)
        perl(current.parsed < "26.2") {
            val DYE_COLORS_LOWER = "black|blue|brown|cyan|gray|green|lime|magenta|orange|pink|purple|red|white|yellow"
            val DYE_COLORS_UPPER = "BLACK|BLUE|BROWN|CYAN|GRAY|GREEN|LIME|MAGENTA|ORANGE|PINK|PURPLE|RED|WHITE|YELLOW"
            val LIGHT_DYE_COLORS_LOWER = "light(?<baseColor>Blue|Gray)"
            val LIGHT_DYE_COLORS_UPPER = "LIGHT_(?<baseColor>BLUE|GRAY)"

            replace(
                "(?<itemType>DYE|WOOL|STAINED_GLASS(?:_PANE)?)\\.(?<color>$DYE_COLORS_LOWER)\\(\\)",
                "\\U\${color}_\${itemType}",

                "(?<color>$DYE_COLORS_UPPER)_(?<itemType>DYE|WOOL|STAINED_GLASS(?:_PANE)?)",
                "\${itemType}.\\L\${color}()",
            )
            replace(
                "(?<itemType>DYE|WOOL|STAINED_GLASS(?:_PANE)?)\\.(?:$LIGHT_DYE_COLORS_LOWER)\\(\\)",
                "LIGHT_\\U\${baseColor}_\${itemType}",

                "(?:$LIGHT_DYE_COLORS_UPPER)_(?<itemType>DYE|WOOL|STAINED_GLASS(?:_PANE)?)",
                "\${itemType}.light\\L\\u\${baseColor}()",
            )
            replace(
                "DYED_TERRACOTTA\\.(?<color>$DYE_COLORS_LOWER)\\(\\)",
                "\\U\${color}_TERRACOTTA",

                "(?<color>$DYE_COLORS_UPPER)_TERRACOTTA",
                "DYED_TERRACOTTA.\\L\${color}()",
            )
            replace(
                "(?<itemType>DYE|WOOL|STAINED_GLASS(?:_PANE)?)\\.(?:$LIGHT_DYE_COLORS_LOWER)\\(\\)",
                "LIGHT_\\U\${baseColor}_\${itemType}",

                "(?:$LIGHT_DYE_COLORS_UPPER)_TERRACOTTA",
                "DYED_TERRACOTTA.light\\L\\u\${baseColor}()",
            )
        }

        string(current.parsed < "26.1") {
            replace(";extractRenderState(", ";render(")
            replace(";text(", ";drawString(")
            replace("ContainerInput", "ClickType")
            replace("GuiGraphicsExtractor", "GuiGraphics")
            replace("InteractClickType", "InteractClickType") // prevent replacement
            replace("ProjectionMatrixBuffer", "CachedOrthoProjectionMatrixBuffer")
            replace("addBlitToCurrentLayer", "submitBlitToCurrentLayer")
            replace("classTweaker v1 official", "classTweaker v1 named")
            replace("drawContext.text", "drawContext.drawString")
            replace("extractContents", "renderContents")
            replace("extractSlot", "renderSlot")
            replace("lambda\$addMainPass\$0", "method_62214")
            replace("net.minecraft.client.multiplayer.chat.GuiMessage", "net.minecraft.client.GuiMessage")
            replace("net.minecraft.client.multiplayer.chat.GuiMessageTag", "net.minecraft.client.GuiMessageTag")
            replace("net.minecraft.client.renderer.state.gui", "net.minecraft.client.gui.render.state")
        }
    }

    filters.include("**/*.fsh", "**/*.vsh")
}
