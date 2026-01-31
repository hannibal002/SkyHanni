package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.render.layers.ChromaRenderLayer
import net.minecraft.util.Util
import net.minecraft.client.renderer.rendertype.RenderType
//? if < 1.21.11 {
import net.minecraft.client.renderer.RenderStateShard
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.renderer.rendertype.RenderType.CompositeState
//?} else {
/*import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.LayeringTransform
*///?}
import net.minecraft.resources.Identifier

object SkyHanniRenderLayers {

    //? if < 1.21.11 {
    private val linesCache = ConcurrentHashMap<Int, RenderType>()
    private val linesThroughWallsCache = ConcurrentHashMap<Int, RenderType>()
    //?}

    private val FILLED: RenderType = RenderType.create(
        "skyhanni_filled",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.FILLED(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.FILLED()).createRenderSetup(),
    )

    private val FILLED_XRAY: RenderType = RenderType.create(
        "skyhanni_filled_xray",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.FILLED_XRAY(),
        CompositeState.builder().createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.FILLED_XRAY()).createRenderSetup(),
    )

    private val TRIANGLES: RenderType = RenderType.create(
        "skyhanni_triangles",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLES(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLES()).createRenderSetup(),
    )

    private val TRIANGLES_XRAY: RenderType = RenderType.create(
        "skyhanni_triangles_xray",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLES_XRAY(),
        CompositeState.builder().createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLES_XRAY()).createRenderSetup(),
    )

    private val TRIANGLE_FAN: RenderType = RenderType.create(
        "skyhanni_triangle_fan",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLE_FAN(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLE_FAN()).createRenderSetup(),
    )

    private val TRIANGLE_FAN_XRAY: RenderType = RenderType.create(
        "skyhanni_triangle_fan_xray",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY(),
        CompositeState.builder().createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.TRIANGLE_FAN_XRAY()).createRenderSetup(),
    )

    private val QUADS: RenderType = RenderType.create(
        "skyhanni_quads",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.QUADS(),
        CompositeState.builder().setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.QUADS()).createRenderSetup(),
    )

    private val QUADS_XRAY: RenderType = RenderType.create(
        "skyhanni_quads_xray",
        //? if < 1.21.11 {
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SkyHanniRenderPipeline.QUADS_XRAY(),
        CompositeState.builder().createCompositeState(false),
        //?} else
        //RenderSetup.builder(SkyHanniRenderPipeline.QUADS_XRAY()).createRenderSetup(),
    )

    private val CHROMA_STANDARD: RenderType = ChromaRenderLayer(
        "skyhanni_standard_chroma",
        RenderType.SMALL_BUFFER_SIZE,
        false,
        false,
        SkyHanniRenderPipeline.CHROMA_STANDARD(),
        /*? if < 1.21.11 {*/ CompositeState.builder().createCompositeState(false), /*?}*/
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderType> = Util.memoize { texture ->
        ChromaRenderLayer(
            "skyhanni_text_chroma",
            RenderType.SMALL_BUFFER_SIZE,
            false,
            false,
            SkyHanniRenderPipeline.CHROMA_TEXT(),
            /*? if < 1.21.11 {*/
            CompositeState.builder()
                .setTextureState(RenderStateShard.TextureStateShard(texture, false))
                .createCompositeState(false),
            /*?} else {*/ /*texture *//*?}*/
        )
    }

    //? if < 1.21.11 {
    private fun createLineRenderLayer(lineWidth: Double, throughWalls: Boolean): RenderType {
        val pipeLine = if (throughWalls) SkyHanniRenderPipeline.LINES_XRAY() else SkyHanniRenderPipeline.LINES()
        return RenderType.create(
            "skyhanni_lines_${lineWidth}${if (throughWalls) "_xray" else ""}",
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            pipeLine,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(lineWidth)))
                .setLayeringState(if (throughWalls) RenderStateShard.NO_LAYERING else RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false),
        )
    }
    //?} else {
    /*private val LINES: RenderType = RenderType.create(
        "skyhanni_lines",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES())
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup(),
    )

    private val LINES_XRAY: RenderType = RenderType.create(
        "skyhanni_lines_xray",
        RenderSetup.builder(SkyHanniRenderPipeline.LINES_XRAY()).setLayeringTransform(LayeringTransform.NO_LAYERING)
            .createRenderSetup(),
    )
    *///?}

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

    fun getLines(lineWidth: Double, throughWalls: Boolean): RenderType {
        //? if < 1.21.11 {
        val cache = if (throughWalls) linesThroughWallsCache else linesCache
        return cache.computeIfAbsent(lineWidth.hashCode()) {
            createLineRenderLayer(lineWidth, throughWalls)
        }
        //?} else
        //return if (throughWalls) LINES_XRAY else LINES
    }

    fun getChromaTexturedWithIdentifier(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)

    fun getChromaStandard(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_STANDARD()
    fun getChromaTextured(): com.mojang.blaze3d.pipeline.RenderPipeline = SkyHanniRenderPipeline.CHROMA_TEXT()

}
