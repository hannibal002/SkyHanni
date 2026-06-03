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
            replace(".mainCamera()", ".mainCamera")
            replace("DYE.black()", "BLACK_DYE")
            replace("DYE.blue()", "BLUE_DYE")
            replace("DYE.brown()", "BROWN_DYE")
            replace("DYE.cyan()", "CYAN_DYE")
            replace("DYE.gray()", "GRAY_DYE")
            replace("DYE.green()", "GREEN_DYE")
            replace("DYE.lightBlue()", "LIGHT_BLUE_DYE")
            replace("DYE.lightGray()", "LIGHT_GRAY_DYE")
            replace("DYE.lime()", "LIME_DYE")
            replace("DYE.magenta()", "MAGENTA_DYE")
            replace("DYE.orange()", "ORANGE_DYE")
            replace("DYE.pink()", "PINK_DYE")
            replace("DYE.purple()", "PURPLE_DYE")
            replace("DYE.red()", "RED_DYE")
            replace("DYE.white()", "WHITE_DYE")
            replace("DYE.yellow()", "YELLOW_DYE")
            replace("DYED_TERRACOTTA.black()", "BLACK_TERRACOTTA")
            replace("DYED_TERRACOTTA.blue()", "BLUE_TERRACOTTA")
            replace("DYED_TERRACOTTA.brown()", "BROWN_TERRACOTTA")
            replace("DYED_TERRACOTTA.cyan()", "CYAN_TERRACOTTA")
            replace("DYED_TERRACOTTA.gray()", "GRAY_TERRACOTTA")
            replace("DYED_TERRACOTTA.green()", "GREEN_TERRACOTTA")
            replace("DYED_TERRACOTTA.lightBlue()", "LIGHT_BLUE_TERRACOTTA")
            replace("DYED_TERRACOTTA.lightGray()", "LIGHT_GRAY_TERRACOTTA")
            replace("DYED_TERRACOTTA.lime()", "LIME_TERRACOTTA")
            replace("DYED_TERRACOTTA.magenta()", "MAGENTA_TERRACOTTA")
            replace("DYED_TERRACOTTA.orange()", "ORANGE_TERRACOTTA")
            replace("DYED_TERRACOTTA.pink()", "PINK_TERRACOTTA")
            replace("DYED_TERRACOTTA.purple()", "PURPLE_TERRACOTTA")
            replace("DYED_TERRACOTTA.red()", "RED_TERRACOTTA")
            replace("DYED_TERRACOTTA.white()", "WHITE_TERRACOTTA")
            replace("DYED_TERRACOTTA.yellow()", "YELLOW_TERRACOTTA")
            replace("STAINED_GLASS.black()", "BLACK_STAINED_GLASS")
            replace("STAINED_GLASS.blue()", "BLUE_STAINED_GLASS")
            replace("STAINED_GLASS.brown()", "BROWN_STAINED_GLASS")
            replace("STAINED_GLASS.cyan()", "CYAN_STAINED_GLASS")
            replace("STAINED_GLASS.gray()", "GRAY_STAINED_GLASS")
            replace("STAINED_GLASS.green()", "GREEN_STAINED_GLASS")
            replace("STAINED_GLASS.lightBlue()", "LIGHT_BLUE_STAINED_GLASS")
            replace("STAINED_GLASS.lightGray()", "LIGHT_GRAY_STAINED_GLASS")
            replace("STAINED_GLASS.lime()", "LIME_STAINED_GLASS")
            replace("STAINED_GLASS.magenta()", "MAGENTA_STAINED_GLASS")
            replace("STAINED_GLASS.orange()", "ORANGE_STAINED_GLASS")
            replace("STAINED_GLASS.pink()", "PINK_STAINED_GLASS")
            replace("STAINED_GLASS.purple()", "PURPLE_STAINED_GLASS")
            replace("STAINED_GLASS.red()", "RED_STAINED_GLASS")
            replace("STAINED_GLASS.white()", "WHITE_STAINED_GLASS")
            replace("STAINED_GLASS.yellow()", "YELLOW_STAINED_GLASS")
            replace("STAINED_GLASS_PANE.black()", "BLACK_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.blue()", "BLUE_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.brown()", "BROWN_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.cyan()", "CYAN_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.gray()", "GRAY_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.green()", "GREEN_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.lightBlue()", "LIGHT_BLUE_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.lightGray()", "LIGHT_GRAY_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.lime()", "LIME_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.magenta()", "MAGENTA_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.orange()", "ORANGE_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.pink()", "PINK_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.purple()", "PURPLE_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.red()", "RED_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.white()", "WHITE_STAINED_GLASS_PANE")
            replace("STAINED_GLASS_PANE.yellow()", "YELLOW_STAINED_GLASS_PANE")
            replace("WOOL.black()", "BLACK_WOOL")
            replace("WOOL.blue()", "BLUE_WOOL")
            replace("WOOL.brown()", "BROWN_WOOL")
            replace("WOOL.cyan()", "CYAN_WOOL")
            replace("WOOL.gray()", "GRAY_WOOL")
            replace("WOOL.green()", "GREEN_WOOL")
            replace("WOOL.lightBlue()", "LIGHT_BLUE_WOOL")
            replace("WOOL.lightGray()", "LIGHT_GRAY_WOOL")
            replace("WOOL.lime()", "LIME_WOOL")
            replace("WOOL.magenta()", "MAGENTA_WOOL")
            replace("WOOL.orange()", "ORANGE_WOOL")
            replace("WOOL.pink()", "PINK_WOOL")
            replace("WOOL.purple()", "PURPLE_WOOL")
            replace("WOOL.red()", "RED_WOOL")
            replace("WOOL.white()", "WHITE_WOOL")
            replace("WOOL.yellow()", "YELLOW_WOOL")
            replace("gui.hud.chat", "gui.chat")
            replace("gui.hud.guiTicks", "gui.guiTicks")
            replace("gui.hud.tabList", "gui.tabList")
            replace(".gui.hud.isHidden", ".options.hideGui")
            replace(".gui.screen()", ".screen")
            replace(".gui.setScreen(", ".setScreen(")
            replace(".gameRenderer.featureRenderDispatcher()", ".gameRenderer.getFeatureRenderDispatcher()")
            replace(".gameRenderer.gameRenderState()", ".gameRenderer.getGameRenderState()")
            replace(".gameRenderer.lighting()", ".gameRenderer.getLighting()")
            replace("gameRenderer.featureRenderDispatcher()", "gameRenderer.getFeatureRenderDispatcher()")
            replace("gameRenderer.gameRenderState()", "gameRenderer.getGameRenderState()")
            replace("levelExtractor.allChanged()", "levelRenderer.allChanged()")
            replace("net.minecraft.world.entity.monster.cubemob.MagmaCube", "net.minecraft.world.entity.monster.MagmaCube")
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")
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
