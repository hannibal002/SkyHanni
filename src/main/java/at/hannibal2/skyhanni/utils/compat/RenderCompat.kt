package at.hannibal2.skyhanni.utils.compat

import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.Framebuffer
import java.util.OptionalDouble
import java.util.OptionalInt
//#if MC < 1.21.6
import net.minecraft.client.render.RenderLayer
//#else
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline
//$$ import net.minecraft.client.gl.RenderPipelines
//#endif

object RenderCompat {

    //#if MC < 1.21.6
    fun getMinecraftGuiTextured() = RenderLayer::getGuiTextured
    //#else
    //$$ fun getMinecraftGuiTextured(): RenderPipeline = RenderPipelines.GUI_TEXTURED
    //#endif

    fun RenderPass.enableRenderPassScissorStateIfAble() {
        //#if MC < 1.21.6
        if (RenderSystem.SCISSOR_STATE.isEnabled) {
            this.enableScissor(RenderSystem.SCISSOR_STATE)
        }
        //#else
        //$$ val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        //$$ if (scissorState.method_72091()) {
        //$$     this.enableScissor(scissorState.method_72092(), scissorState.method_72093(), scissorState.method_72094(), scissorState.method_72095())
        //$$ }
        //#endif
    }

    fun RenderPass.drawIndexed(indices: Int) {
        //#if MC < 1.21.6
        drawIndexed(0, indices)
        //#else
        //$$ drawIndexed(0, 0, indices, 1)
        //#endif
    }

    private fun Framebuffer.findColorAttachment() =
        //#if MC < 1.21.6
        this.colorAttachment
    //#else
    //$$ this.colorAttachmentView
    //#endif

    private fun Framebuffer.findDepthAttachment() =
        //#if MC < 1.21.6
        if (this.useDepthAttachment) this.depthAttachment else null
    //#else
    //$$ if (this.useDepthAttachment) this.depthAttachmentView else null
    //#endif

    fun GpuDevice.createRenderPass(name: String, framebuffer: Framebuffer): RenderPass {
        return this.createCommandEncoder().createRenderPass(
            //#if MC > 1.21.6
            //$$ { name },
            //#endif
            framebuffer.findColorAttachment(),
            OptionalInt.empty(),
            framebuffer.findDepthAttachment(),
            OptionalDouble.empty(),
        )
    }

}
