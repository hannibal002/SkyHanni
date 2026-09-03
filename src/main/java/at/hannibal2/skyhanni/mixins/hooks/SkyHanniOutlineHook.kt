@file:Suppress("NoEmptyFile")

package at.hannibal2.skyhanni.mixins.hooks

//? if < 26.2 {
/*import at.hannibal2.skyhanni.test.command.ErrorManager
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.rendertype.RenderType

// The idea and implementation for this class was inspired by Skyblocker.
// This implementation has been modified from the original Skyblocker code to work across multiple versions.
object SkyHanniOutlineHook {
    class SkyHanniOutlineVertexConsumerProvider : OutlineBufferSource() {
        override fun endOutlineBatch() {
            beginRendering()
            super.endOutlineBatch()
            finishRendering()
        }

        override fun getBuffer(renderLayer: RenderType): VertexConsumer {
            beginRendering()
            val returnVal = super.getBuffer(renderLayer)
            finishRendering()
            return returnVal
        }
    }

    @JvmStatic
    val vertexConsumers by lazy {
        SkyHanniOutlineVertexConsumerProvider()
    }

    private var customDepthAttachment: GpuTexture? = null

    private var customDepthAttachmentView: GpuTextureView? = null

    private var customDepthAttachmentFormat: TextureFormat? = null

    @JvmStatic
    var isCurrentlyActive = false
        private set

    private fun beginRendering() {
        val depthAttachmentView = customDepthAttachmentView ?: return
        isCurrentlyActive = true
        RenderSystem.outputDepthTextureOverride = depthAttachmentView
    }

    private fun finishRendering() {
        isCurrentlyActive = false
        RenderSystem.outputDepthTextureOverride = null
    }

    private var lastWidth = 0
    private var lastHeight = 0

    @JvmStatic
    fun checkIfDepthAttachmentNeedsUpdating() {
        val gpuTexture = Minecraft.getInstance().mainRenderTarget.depthTexture ?: return
        val width = gpuTexture.getWidth(0)
        val height = gpuTexture.getHeight(0)
        val format = gpuTexture.format
        try {
            if (
                customDepthAttachment == null ||
                width != lastWidth ||
                height != lastHeight ||
                format != customDepthAttachmentFormat
            ) {
                lastWidth = width
                lastHeight = height
                customDepthAttachmentFormat = format
                updateDepthAttachment(format)
            }
            val depthAttachment = customDepthAttachment ?: return
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                gpuTexture,
                depthAttachment,
                0, 0, 0, 0, 0, lastWidth, lastHeight,
            )
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to copy depth attachment")
        }
    }

    private fun updateDepthAttachment(format: TextureFormat) {
        try {
            customDepthAttachment?.let {
                it.close()
                customDepthAttachmentView?.close()
            }
            val device = RenderSystem.getDevice()
            val depthAttachment = device.createTexture(
                "SkyHanni Custom Depth",
                GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                format,
                lastWidth, lastHeight, 1, 1,
            )
            customDepthAttachment = depthAttachment
            customDepthAttachmentView = device.createTextureView(depthAttachment)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
        }
    }
}
*///?}
