package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.compat.IrisCompat
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.MATRICES_PROJECTION_SNIPPET
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.PosColorNormal
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.commonChromaUniforms
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.util.Optional

//? if >= 26.2 {
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BindGroupLayout
import net.minecraft.client.renderer.BindGroupLayouts
//?}

//? if < 26.2
//private typealias PrimitiveTopology = VertexFormat.Mode

enum class SkyHanniRenderPipeline(
    snippet: RenderPipeline.Snippet,
    vFormat: VertexFormat = DefaultVertexFormat.POSITION_COLOR,
    vDrawMode: PrimitiveTopology = PrimitiveTopology.QUADS,
    blend: BlendFunction? = null,
    withCull: Boolean? = false,
    vertexShaderPath: String? = null,
    fragmentShaderPath: String? = vertexShaderPath,
    sampler: String? = null,
    uniforms: Map<String, UniformType> = emptyMap(),
    depthWrite: Boolean = true,
    val irisProgram: IrisCompat.IrisProgram = IrisCompat.IrisProgram.BASIC,
) {
    LINES(
        snippet = RenderPipelines.LINES_SNIPPET,
        vFormat = PosColorNormal,
        vDrawMode = PrimitiveTopology.LINES,
        irisProgram = IrisCompat.IrisProgram.LINES,
    ),
    LINES_XRAY(
        snippet = RenderPipelines.LINES_SNIPPET,
        vFormat = PosColorNormal,
        vDrawMode = PrimitiveTopology.LINES,
        depthWrite = false,
        irisProgram = IrisCompat.IrisProgram.LINES,
    ),
    FILLED(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLE_STRIP,
    ),
    FILLED_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLE_STRIP,
        depthWrite = false,
    ),
    TRIANGLES(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLES,
    ),
    TRIANGLES_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLES,
        depthWrite = false,
    ),
    TRIANGLE_FAN(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLE_FAN,
    ),
    TRIANGLE_FAN_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = PrimitiveTopology.TRIANGLE_FAN,
        depthWrite = false,
    ),
    QUADS(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
    ),
    QUADS_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        depthWrite = false,
    ),
    CHROMA_STANDARD(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "standard_chroma",
        uniforms = commonChromaUniforms,
    ),
    CHROMA_TEXT(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_TEX_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "textured_chroma",
        sampler = "Sampler0",
        uniforms = commonChromaUniforms,
        irisProgram = IrisCompat.IrisProgram.TEXTURED,
    ),
    ROUNDED_RECT_DEFERRED(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = SkyHanniVertexFormats.POSITION_COLOR_ROUNDED,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect_deferred",
        depthWrite = false,
    ),
    ROUNDED_RECT_OUTLINE_DEFERRED(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = SkyHanniVertexFormats.POSITION_COLOR_ROUNDED,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect_outline_deferred",
        depthWrite = false,
    ),
    CIRCLE_DEFERRED(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = SkyHanniVertexFormats.POSITION_COLOR_ROUNDED,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "circle_deferred",
        depthWrite = false,
    ),
    ROUNDED_TEXTURED_RECT_DEFERRED(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = SkyHanniVertexFormats.POSITION_TEX_ROUNDED,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_texture_deferred",
        sampler = "Sampler0",
        depthWrite = false,
        irisProgram = IrisCompat.IrisProgram.TEXTURED,
    ),
    RADIAL_GRADIENT_CIRCLE_DEFERRED(
        snippet = MATRICES_PROJECTION_SNIPPET,
        vFormat = SkyHanniVertexFormats.POSITION_ROUNDED_GRADIENT,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "radial_gradient_circle_deferred",
        depthWrite = false,
    ),
    GUI_TEXTURED_TRANSLUCENT(
        snippet = RenderPipelines.GUI_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_TEX_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "gui_textured_translucent",
        sampler = "Sampler0",
        depthWrite = false,
        irisProgram = IrisCompat.IrisProgram.TEXTURED,
    ),
    ;

    private val internalPipeline: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(snippet)
            .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, this.name.lowercase()))
            //? if >= 26.2 {
            .withVertexBinding(0, vFormat)
            .withPrimitiveTopology(vDrawMode)
            //?} else
            //.withVertexFormat(vFormat, vDrawMode)
            .apply {
                // One or the other, never both
                blend?.let { withColorTargetState(ColorTargetState(it)) } ?: withCull?.let(this::withCull)
                vertexShaderPath?.let { withVertexShader(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, it)) }
                fragmentShaderPath?.let {
                    withFragmentShader(
                        Identifier.fromNamespaceAndPath(
                            SkyHanniMod.MODID, it
                        )
                    )
                }

                //? if >= 26.2 {
                if (sampler != null || uniforms.isNotEmpty()) {
                    withBindGroupLayout(
                        BindGroupLayout.builder().apply {
                            sampler?.let(this::withSampler)
                            uniforms.forEach(this::withUniform)
                        }.build(),
                    )
                }
                //?} else {
                /*sampler?.let(this::withSampler)
                uniforms.forEach(this::withUniform)
                *///?}

                if (!depthWrite) {
                    withDepthStencilState(Optional.empty())
                }
            }.build(),
    )

    operator fun invoke(): RenderPipeline = internalPipeline
}

private object SkyHanniRenderPipelineUtils {
    //? if >= 26.2 {
    val MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder().withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION).buildSnippet()
    //?} else
    //val MATRICES_PROJECTION_SNIPPET = RenderPipelines.MATRICES_PROJECTION_SNIPPET

    val commonChromaUniforms = mapOf("SkyHanniChromaUniforms" to UniformType.UNIFORM_BUFFER)
    val PosColorNormal: VertexFormat = DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH
}
