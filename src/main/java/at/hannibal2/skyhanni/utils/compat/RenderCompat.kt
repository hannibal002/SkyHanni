package at.hannibal2.skyhanni.utils.compat

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import java.util.OptionalDouble
import java.util.OptionalInt
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines

object RenderCompat {

    fun getMinecraftGuiTextured(): RenderPipeline = RenderPipelines.GUI_TEXTURED

    fun RenderPass.enableRenderPassScissorStateIfAble() {
        val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        if (scissorState.enabled()) {
            this.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height())
        }
    }

    fun RenderPass.drawIndexed(indices: Int) {
        drawIndexed(0, 0, indices, 1)
    }

    private fun RenderTarget.findColorAttachment() = this.colorTextureView

    private fun RenderTarget.findDepthAttachment() = if (this.useDepth) this.depthTextureView else null

    fun GpuDevice.createRenderPass(name: String, framebuffer: RenderTarget): RenderPass {
        return this.createCommandEncoder().createRenderPass(
            { name },
            framebuffer.findColorAttachment(),
            OptionalInt.empty(),
            framebuffer.findDepthAttachment(),
            OptionalDouble.empty(),
        )
    }

}
