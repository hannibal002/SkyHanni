package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.render.layers.ChromaRenderLayer
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.resources.Identifier
import net.minecraft.util.Util

//? if >= 26.1 {
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.platform.CompareOp
//? } else {
/*import com.mojang.blaze3d.platform.DepthTestFunction*/
//? }

object SkyHanniRenderLayers {

    private val FILLED: RenderType = RenderType.create(
        "skyhanni_filled",
        RenderSetup.builder(SkyHanniRenderPipeline.FILLED()).createRenderSetup(),
    )

    private val FILLED_XRAY: RenderType = RenderType.create(
        "skyhanni_filled_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.FILLED_XRAY()).createRenderSetup(),
    )

    private val TRIANGLES: RenderType = RenderType.create(
        "skyhanni_triangles",
        RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLES()).createRenderSetup(),
    )

    private val TRIANGLES_XRAY: RenderType = RenderType.create(
        "skyhanni_triangles_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLES_XRAY()).createRenderSetup(),
    )

    private val TRIANGLE_FAN: RenderType = RenderType.create(
        "skyhanni_triangle_fan",
        RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLE_FAN()).createRenderSetup(),
    )

    private val TRIANGLE_FAN_XRAY: RenderType = RenderType.create(
        "skyhanni_triangle_fan_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY()).createRenderSetup(),
    )

    private val QUADS: RenderType = RenderType.create(
        "skyhanni_quads",
        RenderSetup.builder(SkyHanniRenderPipeline.QUADS()).createRenderSetup(),
    )

    private val QUADS_XRAY: RenderType = RenderType.create(
        "skyhanni_quads_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.QUADS_XRAY()).createRenderSetup(),
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderType> = Util.memoize { texture ->
        ChromaRenderLayer(
            "skyhanni_text_chroma",
            texture = texture,
        )
    }

    private val TEXT_NO_DEPTH_WRITE_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, "text_no_depth_write"))
            .withVertexShader("core/rendertype_text")
            .withFragmentShader("core/rendertype_text")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            //? if < 26.1 {
            /*.withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            *///? } else
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build(),
    )

    private val TEXT_INTENSITY_NO_DEPTH_WRITE_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, "text_intensity_no_depth_write"))
            .withVertexShader("core/rendertype_text_intensity")
            .withFragmentShader("core/rendertype_text_intensity")
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            //? if < 26.1 {
            /*.withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            *///? } else
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false, -1.0f, -10.0f))
            .build(),
    )

    private val TEXT_BACKGROUND_NO_DEPTH_WRITE_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, "text_background_no_depth_write"))
            .withVertexShader("core/rendertype_text_background")
            .withFragmentShader("core/rendertype_text_background")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP, VertexFormat.Mode.QUADS)
            //? if < 26.1 {
            /*.withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            *///? } else
            .withDepthStencilState(DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build(),
    )

    private val TEXT_NO_DEPTH_WRITE: java.util.function.Function<Identifier, RenderType> = Util.memoize { texture ->
        RenderType.create(
            "skyhanni_text_no_depth_write",
            RenderSetup.builder(TEXT_NO_DEPTH_WRITE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .bufferSize(786432)
                .createRenderSetup(),
        )
    }

    private val TEXT_INTENSITY_NO_DEPTH_WRITE: java.util.function.Function<Identifier, RenderType> = Util.memoize { texture ->
        RenderType.create(
            "skyhanni_text_intensity_no_depth_write",
            RenderSetup.builder(TEXT_INTENSITY_NO_DEPTH_WRITE_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .bufferSize(786432)
                .createRenderSetup(),
        )
    }

    private val TEXT_BACKGROUND_NO_DEPTH_WRITE: RenderType = RenderType.create(
        "skyhanni_text_background_no_depth_write",
        RenderSetup.builder(TEXT_BACKGROUND_NO_DEPTH_WRITE_PIPELINE).useLightmap().sortOnUpload().createRenderSetup(),
    )

    private val LINES: RenderType = RenderType.create(
        "skyhanni_lines",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES())
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup(),
    )

    private val LINES_XRAY: RenderType = RenderType.create(
        "skyhanni_lines_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES_XRAY()).setLayeringTransform(LayeringTransform.NO_LAYERING)
            .createRenderSetup(),
    )

    fun getFilled(throughWalls: Boolean): RenderType {
        return if (throughWalls) FILLED_XRAY else FILLED
    }

    fun getTriangles(throughWalls: Boolean): RenderType {
        return if (throughWalls) TRIANGLES_XRAY else TRIANGLES
    }

    fun getTriangleFan(throughWalls: Boolean): RenderType {
        return if (throughWalls) TRIANGLE_FAN_XRAY else TRIANGLE_FAN
    }

    fun getQuads(throughWalls: Boolean): RenderType {
        return if (throughWalls) QUADS_XRAY else QUADS
    }

    fun getLines(throughWalls: Boolean): RenderType {
        return if (throughWalls) LINES_XRAY else LINES
    }

    fun getChromaTexturedWithIdentifier(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)

    fun getTextNoDepthWrite(renderType: RenderType): RenderType = when (renderType.name) {
        "text" -> renderType.sampler0Texture()?.let(TEXT_NO_DEPTH_WRITE::apply) ?: renderType
        "text_intensity" -> renderType.sampler0Texture()?.let(TEXT_INTENSITY_NO_DEPTH_WRITE::apply) ?: renderType
        "text_background" -> TEXT_BACKGROUND_NO_DEPTH_WRITE
        else -> renderType
    }

    private fun RenderType.sampler0Texture(): Identifier? = this.state.textures["Sampler0"]?.location

    fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_STANDARD()
    fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_TEXT()

}
