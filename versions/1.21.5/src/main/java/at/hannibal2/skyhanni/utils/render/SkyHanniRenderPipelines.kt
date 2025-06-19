package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.commonChromaUniforms
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipelineUtils.getCommonRoundedUniforms
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

object SkyHanniRenderPipelines {
    val LINES: RenderPipeline get() = SkyHanniRenderPipeline.LINES.pipelineInstance
    val LINES_XRAY: RenderPipeline get() = SkyHanniRenderPipeline.LINES_XRAY.pipelineInstance
    val FILLED: RenderPipeline get() = SkyHanniRenderPipeline.FILLED.pipelineInstance
    val FILLED_XRAY: RenderPipeline get() = SkyHanniRenderPipeline.FILLED_XRAY.pipelineInstance
    val TRIANGLES: RenderPipeline get() = SkyHanniRenderPipeline.TRIANGLES.pipelineInstance
    val TRIANGLES_XRAY: RenderPipeline get() = SkyHanniRenderPipeline.TRIANGLES_XRAY.pipelineInstance
    val TRIANGLE_FAN: RenderPipeline get() = SkyHanniRenderPipeline.TRIANGLE_FAN.pipelineInstance
    val TRIANGLE_FAN_XRAY: RenderPipeline get() = SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY.pipelineInstance
    val QUADS: RenderPipeline get() = SkyHanniRenderPipeline.QUADS.pipelineInstance
    val QUADS_XRAY: RenderPipeline get() = SkyHanniRenderPipeline.QUADS_XRAY.pipelineInstance
    val ROUNDED_RECT: RenderPipeline get() = SkyHanniRenderPipeline.ROUNDED_RECT.pipelineInstance
    val ROUNDED_TEXTURED_RECT: RenderPipeline get() = SkyHanniRenderPipeline.ROUNDED_TEXTURED_RECT.pipelineInstance
    val ROUNDED_RECT_OUTLINE: RenderPipeline get() = SkyHanniRenderPipeline.ROUNDED_RECT_OUTLINE.pipelineInstance
    val CHROMA_STANDARD: RenderPipeline get() = SkyHanniRenderPipeline.CHROMA_STANDARD.pipelineInstance
    val CHROMA_TEXT: RenderPipeline get() = SkyHanniRenderPipeline.CHROMA_TEXT.pipelineInstance
}

private enum class SkyHanniRenderPipeline(
    snippet: RenderPipeline.Snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
    vFormat: VertexFormat = VertexFormats.POSITION_COLOR,
    vDrawMode: VertexFormat.DrawMode = VertexFormat.DrawMode.QUADS,
    blend: BlendFunction? = null,
    withCull: Boolean? = false,
    vertexShaderPath: String? = null,
    fragmentShaderPath: String? = vertexShaderPath,
    sampler: String? = null,
    uniforms: Map<String, UniformType> = emptyMap(),
    depthWrite: Boolean = true,
    depthTestFunction: DepthTestFunction = DepthTestFunction.LEQUAL_DEPTH_TEST,
    additionalBuild: RenderPipeline.Builder.() -> Unit = { },
) {
    LINES(
        snippet = RenderPipelines.RENDERTYPE_LINES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR_NORMAL,
        vDrawMode = VertexFormat.DrawMode.LINES,
    ),
    LINES_XRAY(
        snippet = RenderPipelines.RENDERTYPE_LINES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR_NORMAL,
        vDrawMode = VertexFormat.DrawMode.LINES,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    FILLED(vDrawMode = VertexFormat.DrawMode.TRIANGLE_STRIP),
    FILLED_XRAY(
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_STRIP,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLES(vDrawMode = VertexFormat.DrawMode.TRIANGLES),
    TRIANGLES_XRAY(
        vDrawMode = VertexFormat.DrawMode.TRIANGLES,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLE_FAN(vDrawMode = VertexFormat.DrawMode.TRIANGLE_FAN),
    TRIANGLE_FAN_XRAY(
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_FAN,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    QUADS(vDrawMode = VertexFormat.DrawMode.QUADS),
    QUADS_XRAY(
        vDrawMode = VertexFormat.DrawMode.QUADS,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    ROUNDED_RECT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR,
        vDrawMode = VertexFormat.DrawMode.QUADS,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    ROUNDED_TEXTURED_RECT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_TEXTURE,
        vDrawMode = VertexFormat.DrawMode.QUADS,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_texture",
        sampler = "textureSampler",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    ROUNDED_RECT_OUTLINE(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect_outline",
        uniforms = getCommonRoundedUniforms(withSmoothness = false) + mapOf(
            "borderThickness" to UniformType.FLOAT,
            "borderBlur" to UniformType.FLOAT,
        ),
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    CHROMA_STANDARD(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "standard_chroma",
        uniforms = commonChromaUniforms,
    ),
    CHROMA_TEXT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_TEXTURE_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "textured_chroma",
        sampler = "Sampler0",
        uniforms = commonChromaUniforms,
    )
    ;

    val pipelineInstance: RenderPipeline by lazy {
        RenderPipelines.register(
            RenderPipeline.builder(snippet)
                .withLocation(Identifier.of(SkyHanniMod.MODID, this.name.lowercase()))
                .withVertexFormat(vFormat, vDrawMode)
                .apply {
                    if (blend != null) withBlend(blend)
                    if (withCull != null) withCull(withCull)
                    if (vertexShaderPath != null) withVertexShader(Identifier.of(SkyHanniMod.MODID, vertexShaderPath))
                    if (fragmentShaderPath != null) withFragmentShader(Identifier.of(SkyHanniMod.MODID, fragmentShaderPath))
                    if (sampler != null) withSampler(sampler)
                    uniforms.forEach(this::withUniform)
                }
                .withDepthWrite(depthWrite)
                .withDepthTestFunction(depthTestFunction)
                .apply(additionalBuild)
                .build()
        )
    }
}

private object SkyHanniRenderPipelineUtils {
    fun getCommonRoundedUniforms(
        withSmoothness: Boolean = true,
    ): Map<String, UniformType> = mapOf(
        "scaleFactor" to UniformType.FLOAT,
        "radius" to UniformType.FLOAT,
        "smoothness" to UniformType.FLOAT,
        "halfSize" to UniformType.VEC2,
        "centerPos" to UniformType.VEC2,
        "modelViewMatrix" to UniformType.MATRIX4X4,
    ).filter {
        (withSmoothness || it.key != "smoothness")
    }

    val commonChromaUniforms = mapOf(
        "chromaSize" to UniformType.FLOAT,
        "timeOffset" to UniformType.FLOAT,
        "saturation" to UniformType.FLOAT,
        "forwardDirection" to UniformType.INT,
    )
}
