package at.hannibal2.skyhanni.utils.compat

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import java.util.OptionalDouble
import java.util.OptionalInt
//? < 1.21.6 {
import net.minecraft.client.renderer.RenderType
//?} else {
/*import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines
*///?}

object RenderCompat {

    //? < 1.21.6 {
    fun getMinecraftGuiTextured() = RenderType::guiTextured
    //?} else {
    /*fun getMinecraftGuiTextured(): RenderPipeline = RenderPipelines.GUI_TEXTURED
    *///?}

    fun RenderPass.enableRenderPassScissorStateIfAble() {
        //? < 1.21.6 {
        if (RenderSystem.SCISSOR_STATE.isEnabled) {
            this.enableScissor(RenderSystem.SCISSOR_STATE)
        }
        //?} else {
        /*val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        if (scissorState.enabled()) {
            this.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height())
        }
        *///?}
    }

    fun RenderPass.drawIndexed(indices: Int) {
        //? < 1.21.6 {
        drawIndexed(0, indices)
        //?} else {
        /*drawIndexed(0, 0, indices, 1)
        *///?}
    }

    private fun RenderTarget.findColorAttachment() =
        //? < 1.21.6 {
        this.colorTexture
    //?} else {
    /*this.colorTextureView
    *///?}

    private fun RenderTarget.findDepthAttachment() =
        //? < 1.21.6 {
        if (this.useDepth) this.depthTexture else null
    //?} else {
    /*if (this.useDepth) this.depthTextureView else null
    *///?}

    fun GpuDevice.createRenderPass(name: String, framebuffer: RenderTarget): RenderPass {
        return this.createCommandEncoder().createRenderPass(
            //? > 1.21.6 {
            /*{ name },
            *///?}
            framebuffer.findColorAttachment(),
            OptionalInt.empty(),
            framebuffer.findDepthAttachment(),
            OptionalDouble.empty(),
        )
    }

}
