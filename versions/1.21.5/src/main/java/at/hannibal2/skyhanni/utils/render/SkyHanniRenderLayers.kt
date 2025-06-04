package at.hannibal2.skyhanni.utils.render

import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.util.BufferAllocator
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap

object SkyHanniRenderLayers {

    private val linesCache = ConcurrentHashMap<Int, MultiPhase>()
    private val linesThroughWallsCache = ConcurrentHashMap<Int, MultiPhase>()

    private val FILLED: MultiPhase = RenderLayer.of(
        "skyhanni_filled",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipelines.FILLED,
        MultiPhaseParameters.builder().layering(RenderPhase.VIEW_OFFSET_Z_LAYERING).build(false),
    )

    private val FILLED_XRAY: MultiPhase = RenderLayer.of(
        "skyhanni_filled_xray",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipelines.FILLED_XRAY,
        MultiPhaseParameters.builder().build(false),
    )

    private fun createLineRenderLayer(lineWidth: Double, throughWalls: Boolean): MultiPhase {
        val pipeLine = if (throughWalls) SkyHanniRenderPipelines.LINES_XRAY else SkyHanniRenderPipelines.LINES
        return RenderLayer.of(
            "skyhanni_lines_${lineWidth}${if (throughWalls) "_xray" else ""}",
            RenderLayer.DEFAULT_BUFFER_SIZE,
            false,
            true,
            pipeLine,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(lineWidth)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false),
        )
    }

    fun getBufferFromLayer(layer: MultiPhase): BufferBuilder {
        return BufferBuilder(
            BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE),
            layer.pipeline.vertexFormatMode,
            layer.pipeline.vertexFormat,
        )
    }

    fun getFilled(throughWalls: Boolean): MultiPhase {
        return if (throughWalls) FILLED_XRAY else FILLED
    }

    fun getLines(lineWidth: Double, throughWalls: Boolean): MultiPhase {
        val cache = if (throughWalls) linesThroughWallsCache else linesCache
        return cache.computeIfAbsent(lineWidth.hashCode()) {
            createLineRenderLayer(lineWidth, throughWalls)
        }
    }
}
