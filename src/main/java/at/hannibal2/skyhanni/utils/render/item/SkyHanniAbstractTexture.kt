package at.hannibal2.skyhanni.utils.render.item

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.renderer.texture.AbstractTexture

abstract class SkyHanniAbstractItemTexture : AbstractTexture(), AutoCloseable {

    protected var depthTexture: GpuTexture? = null
    protected var depthTextureView: GpuTextureView? = null

    @Suppress("UnsafeCallOnNullableType")
    protected fun allocateTextures(
        size: Int,
        colorLabel: String,
        depthLabel: String,
        colorUsage: Int,
    ) {
        val device = RenderSystem.getDevice()
        texture = device.createTexture(colorLabel, colorUsage, TextureFormat.RGBA8, size, size, 1, 1)
            //? if < 1.21.11 {
            .also { it.setTextureFilter(FilterMode.NEAREST, false) }
        //?}
        textureView = device.createTextureView(texture!!)
        depthTexture = device.createTexture(depthLabel, 8, TextureFormat.DEPTH32, size, size, 1, 1)
        depthTextureView = device.createTextureView(depthTexture!!)
        device.createCommandEncoder().clearColorAndDepthTextures(texture!!, 0, depthTexture!!, 1.0)
    }

    override fun close() {
        textureView?.close()
        textureView = null
        texture?.close()
        texture = null
        depthTextureView?.close()
        depthTextureView = null
        depthTexture?.close()
        depthTexture = null
    }
}
