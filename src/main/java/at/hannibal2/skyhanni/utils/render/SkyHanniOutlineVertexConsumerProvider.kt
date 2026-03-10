package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper.isModelSubmitCustomOutline
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.AddressMode
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OutlineBufferSource
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.rendertype.RenderType
import org.spongepowered.asm.mixin.Unique

// The idea and implementation for this class was inspired by SkyBlocker.
// This implementation has been modified from the original SkyBlocker code to work across multiple versions.
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

    companion object {

        @JvmStatic
        val vertexConsumers by lazy {
            SkyHanniOutlineVertexConsumerProvider()
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
                customDepthAttachment?.let {
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
                //? if < 1.21.11 {
                depthAttachment.setTextureFilter(FilterMode.NEAREST, false)
                depthAttachment.setAddressMode(AddressMode.CLAMP_TO_EDGE)
                //?}
                customDepthAttachment = depthAttachment
                customDepthAttachmentView = device.createTextureView(depthAttachment)
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
            }
        }

        /**
         * Returns true if this model submit should be routed through the SkyHanni depth-aware
         * (NO_XRAY) outline buffer instead of the vanilla x-ray outline buffer.
         *
         * Two cases:
         * 1. Normal entity body parts — the entity render state carries skyhanni$isUsingCustomOutline
         * (original behaviour, must be preserved for normal zombies etc.).
         * 2. Skull head geometry (armor-stand rat mobs) — submitted via SkullBlockRenderer which
         * creates ModelSubmit nodes with a block-entity state, not an EntityRenderState.
         * MixinModelCommand mixes GlowingStateStore into ModelSubmit and
         * MixinSubmitNodeCollection tags the node at submission time via the synchronous
         * isSubmittingCustomOutlineSkull flag set by MixinHeadFeatureRenderer.
         */
        @Unique
        private fun shouldUseCustomOutline(model: SubmitNodeStorage.ModelSubmit<*>): Boolean {
            // Case 1 – normal entity body (original check, must not be removed).
            val state = model.state() ?: return false
            val currentState = state as? EntityRenderStateStore ?: return false
            if (currentState.`skyhanni$isUsingCustomOutline`()) {
                return true
            }

            // Case 2 – skull/block-entity model: state object was registered in a WeakHashMap at
            // submission time by MixinSubmitNodeCollection.onSubmitModelHead when the synchronous
            // isSubmittingCustomOutlineSkull flag (set by MixinHeadFeatureRenderer) was active.
            return isModelSubmitCustomOutline(state)
        }

        fun outlineBufferHook(
            outlineConsumer: OutlineBufferSource?,
            layer: RenderType?,
            original: Operation<VertexConsumer?>,
            model: SubmitNodeStorage.ModelSubmit<*>,
        ): VertexConsumer? {
            return if (shouldUseCustomOutline(model)) {
                original.call(vertexConsumers, layer)
            } else {
                original.call(outlineConsumer, layer)
            }
        }

        fun outlineColorHook(
            outlineConsumer: OutlineBufferSource?,
            color: Int,
            original: Operation<Int?>,
            model: SubmitNodeStorage.ModelSubmit<*>,
        ) {
            if (shouldUseCustomOutline(model)) {
                original.call(vertexConsumers, color)
            } else {
                original.call(outlineConsumer, color)
            }
        }
    }
}
