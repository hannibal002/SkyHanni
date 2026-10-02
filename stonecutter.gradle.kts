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
                *listOfNotNull(
                    providers.gradleProperty("moulconfigPortRepository").orNull?.let { repositories.maven(it) },
                    repositories.mavenLocal(),
                    repositories.maven("https://maven.notenoughupdates.org/releases"),
                ).toTypedArray()
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
                *listOfNotNull(
                    providers.gradleProperty("renderChestPortRepository").orNull?.let { repositories.maven(it) },
                    repositories.maven("https://maven.azureaaron.net/releases"),
                ).toTypedArray()
            )
            filter {
                includeGroupAndSubgroups("net.azureaaron")
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

    replacements {
        string(current.parsed >= "26.3") {
            replace("#version 150", "#version 330\n#extension GL_ARB_separate_shader_objects : require")
            replace("#moj_import", "#include")
            replace("org.lwjgl.glfw.GLFW", "at.hannibal2.skyhanni.utils.compat.SdlInputCompat")
            replace("GLFW.", "SdlInputCompat.")
            replace("com.mojang.blaze3d.pipeline.RenderPipeline", "com.mojang.renderpearl.api.pipeline.RenderPipeline")
            replace("EnderMan", "Enderman")
            replace("MixinEnderman", "MixinEnderMan")
            replace("Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer\$CrumblingOverlay;", "Lnet/minecraft/client/renderer/texture/UvMapping;I")
            replace("IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer\$CrumblingOverlay;)V", "III)V")
            replace("entityTranslucentCullItemTarget", "entityTranslucentCull")
            replace("RenderTypes.glintTranslucent", "at.hannibal2.skyhanni.utils.render.NativeRenderTypes263.glintTranslucent")
            replace("com.mojang.blaze3d.PrimitiveTopology", "com.mojang.renderpearl.api.pipeline.PrimitiveTopology")
            replace("com/mojang/blaze3d/PrimitiveTopology", "com/mojang/renderpearl/api/pipeline/PrimitiveTopology")
            replace("com.mojang.blaze3d.IndexType", "com.mojang.renderpearl.api.pipeline.IndexType")
            replace("com/mojang/blaze3d/IndexType", "com/mojang/renderpearl/api/pipeline/IndexType")
            replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
            replace("com/mojang/blaze3d/GpuFormat", "com/mojang/renderpearl/api/GpuFormat")
            replace("com.mojang.blaze3d.pipeline.BindGroupLayout", "com.mojang.renderpearl.api.pipeline.BindGroupLayout")
            replace("com/mojang/blaze3d/pipeline/BindGroupLayout", "com/mojang/renderpearl/api/pipeline/BindGroupLayout")
            replace("com.mojang.blaze3d.pipeline.BlendFunction", "com.mojang.renderpearl.api.pipeline.BlendFunction")
            replace("com/mojang/blaze3d/pipeline/BlendFunction", "com/mojang/renderpearl/api/pipeline/BlendFunction")
            replace("com.mojang.blaze3d.pipeline.ColorTargetState", "com.mojang.renderpearl.api.pipeline.ColorTargetState")
            replace("com/mojang/blaze3d/pipeline/ColorTargetState", "com/mojang/renderpearl/api/pipeline/ColorTargetState")
            replace("com.mojang.blaze3d.pipeline.DepthStencilState", "com.mojang.renderpearl.api.pipeline.DepthStencilState")
            replace("com/mojang/blaze3d/pipeline/DepthStencilState", "com/mojang/renderpearl/api/pipeline/DepthStencilState")
            replace("com.mojang.blaze3d.shaders.UniformType", "com.mojang.renderpearl.api.pipeline.UniformType")
            replace("com/mojang/blaze3d/shaders/UniformType", "com/mojang/renderpearl/api/pipeline/UniformType")
            replace("com.mojang.blaze3d.systems.RenderPass", "com.mojang.renderpearl.api.commands.RenderPass")
            replace("com/mojang/blaze3d/systems/RenderPass", "com/mojang/renderpearl/api/commands/RenderPass")
            replace("com.mojang.blaze3d.systems.GpuDevice", "com.mojang.renderpearl.api.device.GpuDevice")
            replace("com/mojang/blaze3d/systems/GpuDevice", "com/mojang/renderpearl/api/device/GpuDevice")
            replace("com.mojang.blaze3d.buffers.GpuBuffer", "com.mojang.renderpearl.api.buffers.GpuBuffer")
            replace("com/mojang/blaze3d/buffers/GpuBuffer", "com/mojang/renderpearl/api/buffers/GpuBuffer")
            replace("com.mojang.blaze3d.textures.GpuTexture", "com.mojang.renderpearl.api.textures.GpuTexture")
            replace("com/mojang/blaze3d/textures/GpuTexture", "com/mojang/renderpearl/api/textures/GpuTexture")
            replace("com.mojang.blaze3d.textures.FilterMode", "com.mojang.renderpearl.api.textures.FilterMode")
            replace("com/mojang/blaze3d/textures/FilterMode", "com/mojang/renderpearl/api/textures/FilterMode")
            replace("com.mojang.blaze3d.textures.GpuSampler", "com.mojang.renderpearl.api.textures.GpuSampler")
            replace("com/mojang/blaze3d/textures/GpuSampler", "com/mojang/renderpearl/api/textures/GpuSampler")
            replace("com.mojang.blaze3d.vertex.VertexFormat", "com.mojang.renderpearl.api.vertex.VertexFormat")
            replace("com/mojang/blaze3d/vertex/VertexFormat", "com/mojang/renderpearl/api/vertex/VertexFormat")
            replace("InputConstants.Type.KEYSYM", "InputConstants.Type.KEYBOARD")
            replace("com/mojang/blaze3d/pipeline/RenderPipeline", "com/mojang/renderpearl/api/pipeline/RenderPipeline")
        }
    }

    filters.include("**/*.fsh", "**/*.vsh")
}
