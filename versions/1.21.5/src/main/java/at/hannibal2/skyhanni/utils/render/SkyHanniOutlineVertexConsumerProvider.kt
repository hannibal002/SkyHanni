package at.hannibal2.skyhanni.utils.render

import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.OutlineVertexConsumerProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
//#if MC > 1.21.6
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//#endif

class SkyHanniOutlineVertexConsumerProvider(parent: VertexConsumerProvider.Immediate) : OutlineVertexConsumerProvider(parent) {

    override fun draw() {
        beginRendering()
        super.draw()
        finishRendering()
    }

    override fun getBuffer(renderLayer: RenderLayer): VertexConsumer {
        beginRendering()
        val returnVal = super.getBuffer(renderLayer)
        finishRendering()
        return returnVal
    }

    companion object {

        @JvmStatic
        val vertexConsumers by lazy { SkyHanniOutlineVertexConsumerProvider(MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers) }

        //#if MC < 1.21.6
        private var vanillaDepthAttachment: GpuTexture? = null

        @JvmStatic
        fun getOverrideDepthAttachment(): GpuTexture? {
            if (!currentlyActive) return null
            return vanillaDepthAttachment
        }
        //#else
        //$$ private var vanillaDepthAttachmentView: GpuTextureView? = null
        //#endif

        @JvmStatic
        var currentlyActive = false

        private fun beginRendering() {
            currentlyActive = true
            //#if MC > 1.21.6
            //$$ RenderSystem.outputDepthTextureOverride = vanillaDepthAttachmentView
            //#endif
        }

        private fun finishRendering() {
            currentlyActive = false
            //#if MC > 1.21.6
            //$$ RenderSystem.outputDepthTextureOverride = null
            //#endif
        }

        @JvmStatic
        fun storeVanillaDepthAttachment() {
            //#if MC < 1.21.6
            vanillaDepthAttachment = MinecraftClient.getInstance().framebuffer.depthAttachment
            //#else
            //$$ vanillaDepthAttachmentView = MinecraftClient.getInstance().framebuffer.depthAttachmentView
            //#endif
        }
    }
}
