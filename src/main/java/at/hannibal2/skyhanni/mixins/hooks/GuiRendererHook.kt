package at.hannibal2.skyhanni.mixins.hooks
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.item.SkyHanniGuiItemRenderState
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderCoordinator
import at.hannibal2.skyhanni.utils.render.item.SkyHanniPipCoordinatorRenderer
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniChromaUniform
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph.GlyphInstance
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GlyphRenderState
import net.minecraft.client.gui.render.state.GuiElementRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

object GuiRendererHook {
    var chromaUniform = SkyHanniChromaUniform()
    var chromaBufferSlice: GpuBufferSlice? = null

    fun computeChromaBufferSlice() {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return

        val chromaSize: Float = ChromaManager.config.chromaSize * (GuiScreenUtils.displayWidth / 100f)
        var ticks = (ClientEvents.totalTicks) + Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(true)
        ticks = when (ChromaManager.config.chromaDirection) {
            Direction.FORWARD_RIGHT, Direction.BACKWARD_RIGHT -> ticks
            Direction.FORWARD_LEFT, Direction.BACKWARD_LEFT -> -ticks
        }
        val timeOffset: Float = ticks * (ChromaManager.config.chromaSpeed / 360f)
        val saturation: Float = ChromaManager.config.chromaSaturation
        val forwardDirection: Int = when (ChromaManager.config.chromaDirection) {
            Direction.FORWARD_RIGHT, Direction.FORWARD_LEFT -> 1
            Direction.BACKWARD_RIGHT, Direction.BACKWARD_LEFT -> 0
        }

        chromaBufferSlice = chromaUniform.writeWith(chromaSize, timeOffset, saturation, forwardDirection)
    }

    // This 'should' be fine being injected into GuiRenderer's render pass since if the bound pipeline's shader doesn't
    // have a uniform with the given name, then the buffer slice will never be bound
    fun insertChromaSetUniform(renderPass: RenderPass) {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return

        // A very explicit name is given since the uniform will show up in RenderPassImpl's simpleUniforms
        // map, and so it is made clear where this uniform is from
        chromaBufferSlice?.let { renderPass.setUniform("SkyHanniChromaUniforms", it) } ?: return
    }

    fun replacePipeline(state: GuiElementRenderState, original: Operation<RenderPipeline>): RenderPipeline {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return original.call(state)

        if (state is GlyphRenderState) {
            val drawnGlyph = state.renderable as? GlyphInstance ?: return original.call(state)
            val glyphColor = drawnGlyph.style.color
            if (glyphColor != null && glyphColor.name == "chroma") {
                return SkyHanniRenderPipeline.CHROMA_TEXT.invoke()
            }
        }

        return original.call(state)
    }

    fun preRenderAtlas(
        pictureInPictureRenderers: Map<Class<out PictureInPictureRenderState>, PictureInPictureRenderer<*>>,
        bufferSource: MultiBufferSource.BufferSource,
        featureRenderDispatcher: FeatureRenderDispatcher,
        frameNumber: Int,
    ) {
        val renderer = pictureInPictureRenderers[SkyHanniGuiItemRenderState::class.java]
        if (renderer !is SkyHanniPipCoordinatorRenderer) return

        // Peek, do not consume. States are still needed for per-item blit submission.
        val states = renderer.peekPendingStates()
        if (states.isEmpty()) return

        SkyHanniItemRenderCoordinator.preRenderAtlas(
            states,
            bufferSource,
            featureRenderDispatcher,
            frameNumber
        )
        renderer.clearPendingStates()
    }

    fun submitBlitForState(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        frameNumber: Int,
    ) {
        SkyHanniItemRenderCoordinator.submitBlit(state, guiRenderState, frameNumber)
    }

}
