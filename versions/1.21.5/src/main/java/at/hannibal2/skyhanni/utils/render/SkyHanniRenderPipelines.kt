package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

object SkyHanniRenderPipelines {

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "line"))
            .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, VertexFormat.DrawMode.LINES)
            .withCull(false)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build(),
    )

    val LINES_XRAY: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "line_xray"))
            .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, VertexFormat.DrawMode.LINES)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build(),
    )

    val FILLED: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "filled"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build(),
    )

    val FILLED_XRAY: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "filled_xray"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build(),
    )
    val TRIANGLE_FAN: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "filled"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build(),
    )

    val TRIANGLE_FAN_XRAY: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "filled_xray"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build(),
    )

    val QUADS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "quads"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build(),
    )

    val QUADS_XRAY: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "quads_xray"))
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build(),
    )

    val ROUNDED_RECT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "rounded_rect"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(Identifier.of(SkyHanniMod.MODID, "rounded_rect"))
            .withFragmentShader(Identifier.of(SkyHanniMod.MODID,"rounded_rect"))
            .withUniform("scaleFactor", UniformType.FLOAT)
            .withUniform("radius", UniformType.FLOAT)
            .withUniform("smoothness", UniformType.FLOAT)
            .withUniform("halfSize", UniformType.VEC2)
            .withUniform("centerPos", UniformType.VEC2)
            .withUniform("modelViewMatrix", UniformType.MATRIX4X4)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    )

    val ROUNDED_TEXTURED_RECT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "rounded_texture_rect"))
            .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(Identifier.of(SkyHanniMod.MODID, "rounded_texture"))
            .withFragmentShader(Identifier.of(SkyHanniMod.MODID, "rounded_texture"))
            .withSampler("textureSampler")
            .withUniform("scaleFactor", UniformType.FLOAT)
            .withUniform("radius", UniformType.FLOAT)
            .withUniform("smoothness", UniformType.FLOAT)
            .withUniform("halfSize", UniformType.VEC2)
            .withUniform("centerPos", UniformType.VEC2)
            .withUniform("modelViewMatrix", UniformType.MATRIX4X4)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    )

    val ROUNDED_RECT_OUTLINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
            .withLocation(Identifier.of(SkyHanniMod.MODID, "rounded_rect_outline"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(Identifier.of(SkyHanniMod.MODID, "rounded_rect_outline"))
            .withFragmentShader(Identifier.of(SkyHanniMod.MODID, "rounded_rect_outline"))
            .withUniform("scaleFactor", UniformType.FLOAT)
            .withUniform("radius", UniformType.FLOAT)
            .withUniform("halfSize", UniformType.VEC2)
            .withUniform("centerPos", UniformType.VEC2)
            .withUniform("modelViewMatrix", UniformType.MATRIX4X4)
            .withUniform("borderThickness", UniformType.FLOAT)
            .withUniform("borderBlur", UniformType.FLOAT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    )
}
