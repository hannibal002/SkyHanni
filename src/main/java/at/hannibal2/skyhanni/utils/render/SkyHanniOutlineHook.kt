package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.Minecraft

//~ if < 26.2 'GpuFormat' -> 'textures.TextureFormat'
import com.mojang.blaze3d.GpuFormat

// The idea and implementation for this class was inspired by Skyblocker. This implementation has
// been modified from the original Skyblocker code to work across multiple versions.
object SkyHanniOutlineHook {

    private var customDepthAttachment: GpuTexture? = null

    private var customDepthAttachmentView: GpuTextureView? = null

    @JvmStatic
    var currentlyActive = false

    @JvmStatic
    fun beginRendering() {
        currentlyActive = true
        RenderSystem.outputDepthTextureOverride = customDepthAttachmentView
    }

    @JvmStatic
    fun finishRendering() {
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
            //~ if < 26.2 'gameRenderer.mainRenderTarget()' -> 'mainRenderTarget'
            val gpuTexture = Minecraft.getInstance().gameRenderer.mainRenderTarget().depthTexture ?: return
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
            customDepthAttachment?.let {
                it.close()
                customDepthAttachmentView?.close()
            }
            val device = RenderSystem.getDevice()
            val depthAttachment = device.createTexture(
                "SkyHanni Custom Depth",
                GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                //~ if < 26.2 'GpuFormat.D32_FLOAT_S8_UINT' -> 'TextureFormat.DEPTH32'
                GpuFormat.D32_FLOAT_S8_UINT,
                lastWidth, lastHeight, 1, 1,
            )
            customDepthAttachment = depthAttachment
            customDepthAttachmentView = device.createTextureView(depthAttachment)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
        }
    }
}
