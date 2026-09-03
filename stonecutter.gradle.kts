plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.kotlin.jvm) apply false
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
                includeGroupAndSubgroups("net.fabricmc")
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

        // libautoupdate
        exclusiveContent {
            forRepository {
                maven("https://repo.nea.moe/releases")
            }
            filter {
                includeGroup("moe.nea")
            }
        }

        // MoulConfig and a few Detekt rules
        exclusiveContent {
            forRepositories(
                repositories.mavenLocal(),
                repositories.maven("https://maven.notenoughupdates.org/releases"),
            )
            filter {
                includeGroupAndSubgroups("org.notenoughupdates")
            }
        }

        // Hypixel Mod API
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

        // REI for compat plugin
        exclusiveContent {
            forRepository {
                maven("https://maven.shedaniel.me")
            }
            filter {
                includeGroup("dev.architectury")
                includeGroupAndSubgroups("me.shedaniel")
            }
        }

        exclusiveContent {
            forRepositories(
                repositories.maven("https://maven.azureaaron.net/releases"),
            )
            filter {
                includeGroupAndSubgroups("net.azureaaron")
            }
        }
    }
}

stonecutter active "26.3"

stonecutter handlers {
    configure("fsh", "vsh") {
        commenter = line("//")
    }
}

stonecutter parameters {
    constants {
        this["iris_compat"] = eval(current.version, "< 26.3")
        this["rei_compat"] = eval(current.version, "< 26.3")
        this["render_chest"] = eval(current.version, "= 26.2")
    }

    replacements {
        string(current.parsed < "26.3") {
            replace("import net.minecraft.world.entity.monster.Enderman;", "import net.minecraft.world.entity.monster.EnderMan;")
            replace("import net.minecraft.world.entity.monster.Enderman", "import net.minecraft.world.entity.monster.EnderMan as Enderman")

            val renderpearlApis = setOf(
                "GpuFormat",
                "buffers.GpuBuffer",
                "buffers.GpuBufferSlice",
                "pipeline.BindGroupLayout",
                "pipeline.BlendFunction",
                "pipeline.ColorTargetState",
                "pipeline.DepthStencilState",
                "pipeline.RenderPipeline",
                "textures.FilterMode",
                "textures.GpuTexture",
                "textures.GpuTextureView",
                "vertex.VertexFormat",
                )
            renderpearlApis.forEach { name ->
                replace("com.mojang.renderpearl.api.$name", "com.mojang.blaze3d.$name")
                replace("com/mojang/renderpearl/api/${name.slashed}", "com/mojang/blaze3d/${name.slashed}")
            }

            val renderpearlRenamedApis = mapOf(
                "commands.RenderPass" to "systems.RenderPass",
                "device.GpuDevice" to "systems.GpuDevice",
                "pipeline.CompareOp" to "platform.CompareOp",
                "pipeline.IndexType" to "IndexType",
                "pipeline.PrimitiveTopology" to "PrimitiveTopology",
                "pipeline.UniformType" to "shaders.UniformType",
            )
            renderpearlRenamedApis.forEach { (old, new) ->
                replace("com.mojang.renderpearl.api.$old", "com.mojang.blaze3d.$new")
                replace("com/mojang/renderpearl/api/${old.slashed}", "com/mojang/blaze3d/${new.slashed}")
            }
        }

        string(current.parsed < "26.2") {
            replace("net.minecraft.world.entity.monster.cubemob.MagmaCube", "net.minecraft.world.entity.monster.MagmaCube")
            replace("net.minecraft.world.entity.monster.cubemob.Slime", "net.minecraft.world.entity.monster.Slime")

            val dyeColors = mapOf(
                "black" to "BLACK",
                "blue" to "BLUE",
                "brown" to "BROWN",
                "cyan" to "CYAN",
                "gray" to "GRAY",
                "green" to "GREEN",
                "lightBlue" to "LIGHT_BLUE",
                "lightGray" to "LIGHT_GRAY",
                "lime" to "LIME",
                "magenta" to "MAGENTA",
                "orange" to "ORANGE",
                "pink" to "PINK",
                "purple" to "PURPLE",
                "red" to "RED",
                "white" to "WHITE",
                "yellow" to "YELLOW",
            )
            dyeColors.forEach { (lower, upper) ->
                replace("DYE.$lower()", "${upper}_DYE")
                replace("WOOL.$lower()", "${upper}_WOOL")
                replace("STAINED_GLASS.$lower()", "${upper}_STAINED_GLASS")
                replace("STAINED_GLASS_PANE.$lower()", "${upper}_STAINED_GLASS_PANE")
                replace("DYED_TERRACOTTA.$lower()", "${upper}_TERRACOTTA")
            }
        }
    }

    filters.include("**/*.fsh", "**/*.vsh")
}

private val String.slashed get() = replace(".", "/")
