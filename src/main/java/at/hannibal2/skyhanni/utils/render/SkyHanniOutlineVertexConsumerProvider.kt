package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.textures.GpuTextureView

// The idea and implementation for this class was inspired by SkyBlocker.
// This implementation has been modified from the original SkyBlocker code to work across multiple versions.


class SkyHanniOutlineVertexConsumerProvider(parent: MultiBufferSource.BufferSource) : OutlineBufferSource() {

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

    companion object {

        @JvmStatic
        val vertexConsumers by lazy {
            SkyHanniOutlineVertexConsumerProvider(
                Minecraft.getInstance().renderBuffers().bufferSource()
            )
        }

        private var customDepthAttachment: GpuTexture? = null

        private var customDepthAttachmentView: GpuTextureView? = null

        @JvmStatic
        var currentlyActive = false

        private fun beginRendering() {
            currentlyActive = true
            RenderSystem.outputDepthTextureOverride = customDepthAttachmentView
        }

        private fun finishRendering() {
            currentlyActive = false
            RenderSystem.outputDepthTextureOverride = null
        }

        private var lastWidth = 0
        private var lastHeight = 0

        @JvmStatic
        fun checkIfDepthAttachmentNeedsUpdating() {
            val window = Minecraft.getInstance().window
            if (customDepthAttachment == null || window.width != lastWidth || window.height != lastHeight) {
                lastWidth = window.width
                lastHeight = window.height
                updateDepthAttachment()
            }
            try {
                val gpuTexture = Minecraft.getInstance().mainRenderTarget.depthTexture ?: return
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

        private fun updateDepthAttachment() {
            try {
                @Suppress("SimpleRedundantLet") customDepthAttachment?.let {
                    it.close()
                    customDepthAttachmentView?.close()
                }
                val device = RenderSystem.getDevice()
                val depthAttachment = device.createTexture(
                    "SkyHanni Custom Depth",
                    GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.DEPTH32,
                    lastWidth, lastHeight, 1, 1,
                )
                depthAttachment.setTextureFilter(FilterMode.NEAREST, false)
                depthAttachment.setAddressMode(AddressMode.CLAMP_TO_EDGE)
                customDepthAttachment = depthAttachment
                customDepthAttachmentView = device.createTextureView(depthAttachment)
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
            }
        }
    }
}
