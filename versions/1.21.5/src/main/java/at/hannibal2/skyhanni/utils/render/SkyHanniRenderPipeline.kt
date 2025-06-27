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

enum class SkyHanniRenderPipeline(
    snippet: RenderPipeline.Snippet,
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
    FILLED(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_STRIP,
    ),
    FILLED_XRAY(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_STRIP,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLES(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLES,
    ),
    TRIANGLES_XRAY(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLES,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    TRIANGLE_FAN(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_FAN,
    ),
    TRIANGLE_FAN_XRAY(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        vDrawMode = VertexFormat.DrawMode.TRIANGLE_FAN,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    QUADS(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
    ),
    QUADS_XRAY(
        snippet = RenderPipelines.POSITION_COLOR_SNIPPET,
        depthWrite = false,
        depthTestFunction = DepthTestFunction.NO_DEPTH_TEST,
    ),
    ROUNDED_RECT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
    ),
    ROUNDED_TEXTURED_RECT(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_TEXTURE,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_texture",
        sampler = "textureSampler",
        uniforms = getCommonRoundedUniforms(),
        depthWrite = false,
    ),
    ROUNDED_RECT_OUTLINE(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "rounded_rect_outline",
        uniforms = getCommonRoundedUniforms(withSmoothness = false) + mapOf(
            "borderThickness" to UniformType.FLOAT,
            "borderBlur" to UniformType.FLOAT,
        ),
        depthWrite = false,
    ),
    CIRCLE(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR,
        blend = BlendFunction.TRANSLUCENT,
        vertexShaderPath = "circle",
        uniforms = getCommonRoundedUniforms(withHalfSize = false) + mapOf(
            "angle1" to UniformType.FLOAT,
            "angle2" to UniformType.FLOAT,
        ),
    ),
    CHROMA_STANDARD(
        snippet = RenderPipelines.MATRICES_SNIPPET,
        vFormat = VertexFormats.POSITION_COLOR,
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
    ),
    ;

    private val _pipe: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(snippet)
            .withLocation(Identifier.of(SkyHanniMod.MODID, this.name.lowercase()))
            .withVertexFormat(vFormat, vDrawMode)
            .apply {
                // One or the other, never both
                blend?.let(this::withBlend) ?: withCull?.let(this::withCull)
                vertexShaderPath?.let { withVertexShader(Identifier.of(SkyHanniMod.MODID, it)) }
                fragmentShaderPath?.let { withFragmentShader(Identifier.of(SkyHanniMod.MODID, it)) }
                sampler?.let(this::withSampler)
                uniforms.forEach(this::withUniform)
                withDepthWrite(depthWrite)
                withDepthTestFunction(depthTestFunction)
            }.build(),
    )

    operator fun invoke(): RenderPipeline = _pipe
}

private object SkyHanniRenderPipelineUtils {
    fun getCommonRoundedUniforms(
        withSmoothness: Boolean = true,
        withHalfSize: Boolean = true,
    ): Map<String, UniformType> = mapOf(
        "scaleFactor" to UniformType.FLOAT,
        "radius" to UniformType.FLOAT,
        "smoothness" to UniformType.FLOAT,
        "halfSize" to UniformType.VEC2,
        "centerPos" to UniformType.VEC2,
        "modelViewMatrix" to UniformType.MATRIX4X4,
    ).filter {
        (withSmoothness || it.key != "smoothness") &&
            (withHalfSize || it.key != "halfSize")
    }

    val commonChromaUniforms = mapOf(
        "chromaSize" to UniformType.FLOAT,
        "timeOffset" to UniformType.FLOAT,
        "saturation" to UniformType.FLOAT,
        "forwardDirection" to UniformType.INT,
    )
}
