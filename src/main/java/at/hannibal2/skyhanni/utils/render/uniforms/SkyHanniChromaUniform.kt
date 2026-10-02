package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
//? if < 26.3 {
import net.minecraft.client.renderer.DynamicUniformStorage
//?}
import java.nio.ByteBuffer

class SkyHanniChromaUniform : AutoCloseable {
    private val uniformSize = Std140SizeCalculator().putFloat().putFloat().putFloat().putInt().get()

    //? if < 26.3 {
    val storage = DynamicUniformStorage<UniformValue>("SkyHanni Chroma UBO", uniformSize, 2)
    //?}

    fun writeWith(
        chromaSize: Float,
        timeOffset: Float,
        saturation: Float,
        forwardDirection: Int,
    ): GpuBufferSlice {
        //? if >= 26.3 {
        /*val memory = org.lwjgl.system.MemoryStack.stackPush()
        return memory.use {
            val buffer = it.malloc(uniformSize)
            UniformValue(chromaSize, timeOffset, saturation, forwardDirection).write(buffer)
            buffer.position(0).limit(uniformSize)
            val device = com.mojang.blaze3d.systems.RenderSystem.getDevice()
            device.createCommandEncoder().transientMemory().uploadGpu(
                buffer, device.getDeviceInfo().limits().minUniformOffsetAlignment().toLong(), com.mojang.renderpearl.api.buffers.GpuBuffer.USAGE_UNIFORM,
            )
        }
        *///?} else {
        return storage.writeUniform(
            UniformValue(chromaSize, timeOffset, saturation, forwardDirection),
        )
        //?}
    }

    // Imperative to clear DynamicUniformStorage every frame.
    // Handled in MixinRenderSystem.
    fun clear() {
        //? if < 26.3 {
        storage.endFrame()
        //?}
    }

    override fun close() {
        //? if < 26.3 {
        storage.close()
        //?}
    }

    data class UniformValue(
        val chromaSize: Float,
        val timeOffset: Float,
        val saturation: Float,
        val forwardDirection: Int,
    )
    //? if < 26.3 {
    : DynamicUniformStorage.DynamicUniform
    //?}
    {
        //? if >= 26.3 {
        /*fun write(buffer: ByteBuffer) {
        *///?} else {
        override fun write(buffer: ByteBuffer) {
        //?}
            Std140Builder.intoBuffer(buffer)
                .putFloat(chromaSize)
                .putFloat(timeOffset)
                .putFloat(saturation)
                .putInt(forwardDirection)
        }
    }
}
