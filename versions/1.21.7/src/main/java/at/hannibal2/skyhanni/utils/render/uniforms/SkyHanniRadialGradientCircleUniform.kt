package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import net.minecraft.client.gl.DynamicUniformStorage
import org.joml.Vector4f
import java.nio.ByteBuffer

class SkyHanniRadialGradientCircleUniform : AutoCloseable {
    private val UNIFORM_SIZE = Std140SizeCalculator().putFloat().putVec4().putVec4().putFloat().putFloat().putInt().get()

    val storage = DynamicUniformStorage<UniformValue>(
        "Skyhanni Gradient Circle UBO",
        UNIFORM_SIZE,
        6
    )

    fun writeWith(
        angle: Float,
        startColor: Vector4f,
        endColor: Vector4f,
        progress: Float,
        phaseOffset: Float,
        reverse: Int
    ): GpuBufferSlice {
        return storage.write(
            UniformValue(angle, startColor, endColor, progress, phaseOffset, reverse)
        )
    }

    fun clear() {
        storage.clear()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val angle: Float,
        val startColor: Vector4f,
        val endColor: Vector4f,
        val progress: Float,
        val phaseOffset: Float,
        val reverse: Int,
    ): DynamicUniformStorage.Uploadable {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(angle)
                .putVec4(startColor)
                .putVec4(endColor)
                .putFloat(progress)
                .putFloat(phaseOffset)
                .putInt(reverse)
        }
    }
}
