package at.hannibal2.hanni.utils.render

import at.hannibal2.hanni.utils.render.layers.ChromaRenderLayer
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import net.minecraft.util.Util

object HanniRenderLayers {

    private val linesCache = ConcurrentHashMap<Int, MultiPhase>()
    private val linesThroughWallsCache = ConcurrentHashMap<Int, MultiPhase>()

    private val FILLED: MultiPhase = RenderLayer.of(
        "hanni_filled",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.FILLED(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val FILLED_XRAY: MultiPhase = RenderLayer.of(
        "hanni_filled_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.FILLED_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val TRIANGLES: MultiPhase = RenderLayer.of(
        "hanni_triangles",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.TRIANGLES(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val TRIANGLES_XRAY: MultiPhase = RenderLayer.of(
        "hanni_triangles_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.TRIANGLES_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val TRIANGLE_FAN: MultiPhase = RenderLayer.of(
        "hanni_triangle_fan",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.TRIANGLE_FAN(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val TRIANGLE_FAN_XRAY: MultiPhase = RenderLayer.of(
        "hanni_triangle_fan_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.TRIANGLE_FAN_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val QUADS: MultiPhase = RenderLayer.of(
        "hanni_quads",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.QUADS(),
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val QUADS_XRAY: MultiPhase = RenderLayer.of(
        "hanni_quads_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        HanniRenderPipeline.QUADS_XRAY(),
        MultiPhaseParameters.builder().build(false),
    )

    private val CHROMA_STANDARD: MultiPhase = ChromaRenderLayer(
        "hanni_standard_chroma",
        RenderLayer.CUTOUT_BUFFER_SIZE,
        false,
        false,
        HanniRenderPipeline.CHROMA_STANDARD(),
        MultiPhaseParameters.builder().build(false),
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderLayer> = Util.memoize { texture ->
        ChromaRenderLayer(
            "hanni_text_chroma",
            RenderLayer.CUTOUT_BUFFER_SIZE,
            false,
            false,
            HanniRenderPipeline.CHROMA_TEXT(),
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
        val pipeLine = if (throughWalls) HanniRenderPipeline.LINES_XRAY() else HanniRenderPipeline.LINES()
        return RenderLayer.of(
            "hanni_lines_${lineWidth}${if (throughWalls) "_xray" else ""}",
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
    fun getChromaTextured() = HanniRenderLayers::getChromaTexturedWithIdentifier
    //#else
    //$$ fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = HanniRenderPipeline.CHROMA_STANDARD()
    //$$ fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = HanniRenderPipeline.CHROMA_TEXT()
    //#endif

}
