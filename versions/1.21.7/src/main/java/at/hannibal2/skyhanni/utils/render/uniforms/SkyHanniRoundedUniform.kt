package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import java.nio.ByteBuffer
import net.minecraft.client.gl.DynamicUniformStorage
import org.joml.Matrix4fc

class SkyHanniRoundedUniform : AutoCloseable {
    private val UNIFORM_SIZE = Std140SizeCalculator().putFloat().putFloat().putFloat().putVec2().putVec2().putMat4f().get()

    val storage = DynamicUniformStorage<UniformValue>("SkyHanni Rounded Rect UBO", UNIFORM_SIZE, 2)

    fun writeWith(
        scaleFactor: Float,
        radius: Float,
        smoothness: Float,
        halfSize: FloatArray,
        centerPos: FloatArray,
        modelViewMatrix: Matrix4fc
    ): GpuBufferSlice {
        return storage.write(
            UniformValue(scaleFactor, radius, smoothness, halfSize, centerPos, modelViewMatrix)
        )
    }

    fun clear() {
        storage.clear()
    }

    override fun close() {
        storage.close()
    }

    data class UniformValue(
        val scaleFactor: Float,
        val radius: Float,
        val smoothness: Float,
        val halfSize: FloatArray,
        val centerPos: FloatArray,
        val modelViewMatrix: Matrix4fc
    ): DynamicUniformStorage.Uploadable {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(scaleFactor)
                .putFloat(radius)
                .putFloat(smoothness)
                .putVec2(halfSize[0], halfSize[1])
                .putVec2(centerPos[0], centerPos[1])
                .putMat4f(modelViewMatrix)
        }
    }
}
