package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import java.nio.ByteBuffer
import net.minecraft.client.gl.DynamicUniformStorage

class SkyHanniCircleUniform : AutoCloseable {
    private val UNIFORM_SIZE = Std140SizeCalculator().putFloat().putFloat().get()

    val storage = DynamicUniformStorage<UniformValue>("SkyHanni Circle UBO", UNIFORM_SIZE, 2)

    fun writeWith(angle1: Float, angle2: Float): GpuBufferSlice {
        return storage.write(UniformValue(angle1, angle2))
    }

    fun clear() {
        storage.clear()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val angle1: Float,
        val angle2: Float
    ): DynamicUniformStorage.Uploadable {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(angle1)
                .putFloat(angle2)
        }
    }
}
