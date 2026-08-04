package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.compat.RenderCompat
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.texture.AbstractTexture

//? if >= 26.2 {
import com.mojang.blaze3d.GpuFormat
//?} else {
/*import com.mojang.blaze3d.textures.TextureFormat
*///?}

abstract class SkyHanniAbstractItemTexture : AbstractTexture(), AutoCloseable {

    companion object {
        /**
         * The depth of an empty render target. 26.2 renders with a reversed depth range,
         * so "nothing drawn" is 0 rather than 1.
         */
        //~ if < 26.2 '0.0' -> '1.0'
        const val CLEAR_DEPTH = 0.0
    }

    protected var depthTexture: GpuTexture? = null
    protected var depthTextureView: GpuTextureView? = null
    private val usageInt = GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST

    @Suppress("UnsafeCallOnNullableType")
    protected fun allocateTextures(
        size: Int,
        colorLabel: String,
        depthLabel: String,
        colorUsage: Int,
    ) {
        val device = RenderSystem.getDevice()
        //~ if < 26.2 'GpuFormat.RGBA8_UNORM' -> 'TextureFormat.RGBA8'
        texture = device.createTexture(colorLabel, colorUsage, GpuFormat.RGBA8_UNORM, size, size, 1, 1)
        textureView = device.createTextureView(texture!!)
        //~ if < 26.2 'GpuFormat.D32_FLOAT' -> 'TextureFormat.DEPTH32'
        depthTexture = device.createTexture(depthLabel, usageInt, GpuFormat.D32_FLOAT, size, size, 1, 1)
        depthTextureView = device.createTextureView(depthTexture!!)
        device.createCommandEncoder().clearColorAndDepthTextures(
            texture!!,
            GuiRenderer.CLEAR_COLOR,
            depthTexture!!,
            RenderCompat.CLEAR_DEPTH,
        )
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
