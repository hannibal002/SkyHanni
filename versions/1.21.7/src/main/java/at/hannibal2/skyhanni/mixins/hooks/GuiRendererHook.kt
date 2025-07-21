package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.features.chroma.ChromaConfig.Direction
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.mixins.transformers.AccessorMinecraft
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.render.SkyHanniRenderPipeline
import at.hannibal2.skyhanni.utils.render.uniforms.SkyHanniChromaUniform
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.render.state.GlyphGuiElementRenderState
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState

object GuiRendererHook {
    var chromaUniform = SkyHanniChromaUniform()
    var chromaBufferSlice: GpuBufferSlice? = null

    fun computeChromaBufferSlice() {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return

        val chromaSize: Float = ChromaManager.config.chromaSize * (GuiScreenUtils.displayWidth / 100f)
        var ticks = (ClientEvents.totalTicks) + (MinecraftClient.getInstance() as AccessorMinecraft).timer.getTickProgress(true)
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

    fun replacePipeline(state: SimpleGuiElementRenderState, original: Operation<RenderPipeline>): RenderPipeline {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return original.call(state)

        if (state is GlyphGuiElementRenderState) {
            val glyphColor = state.instance().style().color
            if (glyphColor != null && glyphColor.name == "chroma") {
                return SkyHanniRenderPipeline.CHROMA_TEXT.invoke()
            }
        }

        return original.call(state)
    }

}
