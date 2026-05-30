package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.render.layers.ChromaRenderLayer
import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.resources.Identifier
import net.minecraft.util.Util

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
            texture,
        )
    }

    private val LINES: RenderType = RenderType.create(
        "skyhanni_lines",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES())
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup(),
    )

    private val LINES_XRAY: RenderType = RenderType.create(
        "skyhanni_lines_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES_XRAY())
            .setLayeringTransform(LayeringTransform.NO_LAYERING)
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

    fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_STANDARD()
    fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_TEXT()

}
