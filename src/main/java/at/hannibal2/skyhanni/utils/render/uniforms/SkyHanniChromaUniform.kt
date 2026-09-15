package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.renderpearl.api.buffers.GpuBuffer
import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import java.nio.ByteBuffer

//? if >= 26.3 {
import net.minecraft.client.renderer.DynamicGpuDataStorage
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped
//?} else {
/*import net.minecraft.client.renderer.DynamicUniformStorage
*///?}

class SkyHanniChromaUniform : AutoCloseable {
    private val uniformSize = Std140SizeCalculator().putFloat().putFloat().putFloat().putInt().get()

    //~ if < 26.3 'DynamicGpuDataStorageMapped' -> 'DynamicUniformStorage'
    val storage = DynamicGpuDataStorageMapped<UniformValue>(
        "SkyHanni Chroma UBO",
        uniformSize,
        //? if >= 26.3
        GpuBuffer.USAGE_UNIFORM,
        2,
    )

    fun writeWith(
        chromaSize: Float,
        timeOffset: Float,
        saturation: Float,
        forwardDirection: Int,
    ): GpuBufferSlice {
        //~ if < 26.3 'writeData' -> 'writeUniform'
        return storage.writeData(
            UniformValue(chromaSize, timeOffset, saturation, forwardDirection),
        )
    }

    // Imperative to clear DynamicUniformStorage every frame.
    // Handled in MixinRenderSystem.
    fun clear() {
        storage.endFrame()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val chromaSize: Float,
        val timeOffset: Float,
        val saturation: Float,
        val forwardDirection: Int,
        //~ if < 26.3 'DynamicGpuDataStorage.DynamicGpuData' -> 'DynamicUniformStorage.DynamicUniform'
    ) : DynamicGpuDataStorage.DynamicGpuData {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(chromaSize)
                .putFloat(timeOffset)
                .putFloat(saturation)
                .putInt(forwardDirection)
        }
    }
}
