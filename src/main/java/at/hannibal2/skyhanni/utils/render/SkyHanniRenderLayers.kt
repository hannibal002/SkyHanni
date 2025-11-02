package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.render.layers.ChromaRenderLayer
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import net.minecraft.util.Util

object SkyHanniRenderLayers {

    private val linesCache = ConcurrentHashMap<Int, MultiPhase>()
    private val linesThroughWallsCache = ConcurrentHashMap<Int, MultiPhase>()

    private val FILLED: MultiPhase = RenderLayer.of(
        "skyhanni_filled",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.FILLED(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val FILLED_XRAY: MultiPhase = RenderLayer.of(
        "skyhanni_filled_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.FILLED_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val TRIANGLES: MultiPhase = RenderLayer.of(
        "skyhanni_triangles",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLES(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val TRIANGLES_XRAY: MultiPhase = RenderLayer.of(
        "skyhanni_triangles_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLES_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val TRIANGLE_FAN: MultiPhase = RenderLayer.of(
        "skyhanni_triangle_fan",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLE_FAN(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val TRIANGLE_FAN_XRAY: MultiPhase = RenderLayer.of(
        "skyhanni_triangle_fan_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val QUADS: MultiPhase = RenderLayer.of(
        "skyhanni_quads",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.QUADS(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val QUADS_XRAY: MultiPhase = RenderLayer.of(
        "skyhanni_quads_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.QUADS_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val CHROMA_STANDARD: MultiPhase = ChromaRenderLayer(
        "skyhanni_standard_chroma",
        RenderLayer.CUTOUT_BUFFER_SIZE,
        false,
        false,
        SkyHanniRenderPipeline.CHROMA_STANDARD(),
        MultiPhaseParameters.builder().build(false),
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderLayer> = Util.memoize { texture ->
        ChromaRenderLayer(
            "skyhanni_text_chroma",
            RenderLayer.CUTOUT_BUFFER_SIZE,
            false,
            false,
            SkyHanniRenderPipeline.CHROMA_TEXT(),
            MultiPhaseParameters.builder()
                //#if MC < 1.21.6
                .texture(RenderPhase.Texture(texture, TriState.FALSE, false))
                //#else
                //$$ .texture(RenderPhase.Texture(texture, false))
                //#endif
                .build(false),
        )
    }

    private fun createLineRenderLayer(lineWidth: Double, throughWalls: Boolean): MultiPhase {
        val pipeLine = if (throughWalls) SkyHanniRenderPipeline.LINES_XRAY() else SkyHanniRenderPipeline.LINES()
        return RenderLayer.of(
            "skyhanni_lines_${lineWidth}${if (throughWalls) "_xray" else ""}",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            false,
            true,
            pipeLine,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(lineWidth)))
                .layering(if (throughWalls) RenderPhase.NO_LAYERING else RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false),
        )
    }

    fun getFilled(throughWalls: Boolean): MultiPhase {
        return if (throughWalls) FILLED_XRAY else FILLED
    }

    fun getTriangles(throughWalls: Boolean): MultiPhase {
        return if (throughWalls) TRIANGLES_XRAY else TRIANGLES
    }

    fun getTriangleFan(throughWalls: Boolean): MultiPhase {
        return if (throughWalls) TRIANGLE_FAN_XRAY else TRIANGLE_FAN
    }

    fun getQuads(throughWalls: Boolean): MultiPhase {
        return if (throughWalls) QUADS_XRAY else QUADS
    }

    fun getLines(lineWidth: Double, throughWalls: Boolean): MultiPhase {
        val cache = if (throughWalls) linesThroughWallsCache else linesCache
        return cache.computeIfAbsent(lineWidth.hashCode()) {
            createLineRenderLayer(lineWidth, throughWalls)
        }
    }

    fun getChromaTexturedWithIdentifier(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)

    //#if MC < 1.21.6
    fun getChromaStandard() = CHROMA_STANDARD
    fun getChromaTextured() = SkyHanniRenderLayers::getChromaTexturedWithIdentifier
    //#else
    //$$ fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_STANDARD()
    //$$ fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_TEXT()
    //#endif

}
