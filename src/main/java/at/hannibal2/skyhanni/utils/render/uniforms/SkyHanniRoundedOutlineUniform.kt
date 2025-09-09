package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import java.nio.ByteBuffer
import net.minecraft.client.gl.DynamicUniformStorage

class SkyHanniRoundedOutlineUniform : AutoCloseable {
    private val UNIFORM_SIZE = Std140SizeCalculator().putFloat().putFloat().get()

    val storage = DynamicUniformStorage<UniformValue>("SkyHanni Rounded Outline Rect UBO", UNIFORM_SIZE, 2)

    fun writeWith(borderThickness: Float, borderBlur: Float): GpuBufferSlice {
        return storage.write(UniformValue(borderThickness, borderBlur))
    }

    fun clear() {
        storage.clear()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val borderThickness: Float,
        val borderBlur: Float,
    ) : DynamicUniformStorage.Uploadable {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(borderThickness)
                .putFloat(borderBlur)
        }
    }
}
