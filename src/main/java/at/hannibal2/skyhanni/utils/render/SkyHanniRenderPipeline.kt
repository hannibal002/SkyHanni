package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.commonChromaUniforms
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.getCommonRoundedUniforms
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

enum class SkyHanniRenderPipeline(
    snippet: RenderPipeline.Snippet,
    vFormat: VertexFormat = DefaultVertexFormat.POSITION_COLOR,
    vDrawMode: VertexFormat.Mode = VertexFormat.Mode.QUADS,
    blend: BlendFunction? = null,
    withCull: Boolean? = false,
    vertexShaderPath: String? = null,
    fragmentShaderPath: String? = vertexShaderPath,
    sampler: String? = null,
    uniforms: Map<String, UniformType> = emptyMap(),
    depthWrite: Boolean = true,
    depthTestFunction: DepthTestFunction = DepthTestFunction.LEQUAL_DEPTH_TEST,
) {
    LINES(
        snippet = RenderPipelines.LINES_SNIPPET,
        vFormat = DefaultVertexFormat./*? if < 1.21.11 {*/ POSITION_COLOR_NORMAL /*?} else {*/ /*POSITION_COLOR_NORMAL_LINE_WIDTH *//*?}*/,
        vDrawMode = VertexFormat.Mode.LINES,
    ),
    LINES_XRAY(
        snippet = RenderPipelines.LINES_SNIPPET,
        vFormat = DefaultVertexFormat./*? if < 1.21.11 {*/ POSITION_COLOR_NORMAL /*?} else {*/ /*POSITION_COLOR_NORMAL_LINE_WIDTH *//*?}*/,
        vDrawMode = VertexFormat.Mode.LINES,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    FILLED(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLE_STRIP,
    ),
    FILLED_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLE_STRIP,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLES(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLES,
    ),
    TRIANGLES_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLES,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLE_FAN(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLE_FAN,
    ),
    TRIANGLE_FAN_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        vDrawMode = VertexFormat.Mode.TRIANGLE_FAN,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    QUADS(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
    ),
    QUADS_XRAY(
        snippet = RenderPipelines.DEBUG_FILLED_SNIPPET,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    ROUNDED_RECT(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
    ),
    ROUNDED_TEXTURED_RECT(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_TEX,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_texture",
        sampler = "textureSampler",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
    ),
    ROUNDED_RECT_OUTLINE(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect_outline",
        uniforms = getCommonRoundedUniforms() + mapOf(
            "SkyHanniRoundedOutlineUniforms" to UniformType.UNIFORM_BUFFER
        ),
        depthWrite = false,
    ),
    CIRCLE(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "circle",
        uniforms = getCommonRoundedUniforms() + mapOf(
            "SkyHanniCircleUniforms" to UniformType.UNIFORM_BUFFER
        ),
    ),
    RADIAL_GRADIENT_CIRCLE(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "radial_gradient_circle",
        uniforms = getCommonRoundedUniforms() + mapOf(
            "SkyHanniRadialGradientCircleUniforms" to UniformType.UNIFORM_BUFFER
        ),
    ),
    CHROMA_STANDARD(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "standard_chroma",
        uniforms = commonChromaUniforms,
    ),
    CHROMA_TEXT(
        snippet = RenderPipelines.MATRICES_PROJECTION_SNIPPET,
        vFormat = DefaultVertexFormat.POSITION_TEX_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "textured_chroma",
        sampler = "Sampler0",
        uniforms = commonChromaUniforms,
    ), ;

    private val _pipe: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(snippet)
            .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, this.name.lowercase()))
            .withVertexFormat(vFormat, vDrawMode).apply {
                // One or the other, never both
                blend?.let(this::withBlend) ?: withCull?.let(this::withCull)
                vertexShaderPath?.let { withVertexShader(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, it)) }
                fragmentShaderPath?.let {
                    withFragmentShader(
                        Identifier.fromNamespaceAndPath(
                            SkyHanniMod.MODID, it
                        )
                    )
                }
                sampler?.let(this::withSampler)
                uniforms.forEach(this::withUniform)
                withDepthWrite(depthWrite)
                withDepthTestFunction(depthTestFunction)
            }.build(),
    )

    operator fun invoke(): RenderPipeline = _pipe
}

private object SkyHanniRenderPipelineUtils {
    fun getCommonRoundedUniforms(): Map<String, UniformType> {
        return mapOf("SkyHanniRoundedUniforms" to UniformType.UNIFORM_BUFFER)
    }

    val commonChromaUniforms = mapOf(
        "SkyHanniChromaUniforms" to UniformType.UNIFORM_BUFFER,
    )
}
