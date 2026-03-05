package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.mixins.transformers.renderer.MixinBufferBuilderAccessor
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import org.lwjgl.system.MemoryUtil

private typealias VFEType = VertexFormatElement.Type
private typealias VFEUsage = VertexFormatElement.Usage
internal typealias SkyHanniVFE = SkyHanniVertexFormats

object SkyHanniVertexFormats {

    private fun safeRegister(
        desiredId: Int,
        index: Int = 0,
        type: VFEType = VFEType.FLOAT,
        usage: VFEUsage = VFEUsage.GENERIC,
        count: Int = 4,
    ): VertexFormatElement {
        val id = (desiredId until VertexFormatElement.MAX_COUNT).first { VertexFormatElement.byId(it) == null }
        if (id != desiredId) ErrorManager.logErrorWithData(
            IllegalStateException("VertexFormatElement ID $desiredId was already taken, using $id instead"),
            "SkyHanni vertex format element ID conflict — desired ID $desiredId was already registered",
        )
        return VertexFormatElement.register(id, index, type, usage, count)
    }

    internal fun BufferBuilder.beginElementAccess(element: VertexFormatElement): Long =
        (this as MixinBufferBuilderAccessor).invokeBeginElement(element)

    // {radius, smoothness/borderThickness, adjustedHalfSizeX, adjustedHalfSizeY}
    val ROUNDED_PARAMS_0: VertexFormatElement = safeRegister(6)

    // {adjustedCenterPosX, adjustedCenterPosY, borderBlur/0, 0}
    val ROUNDED_PARAMS_1: VertexFormatElement = safeRegister(7)

    val POSITION_COLOR_ROUNDED: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("RoundedParams0", ROUNDED_PARAMS_0)
        .add("RoundedParams1", ROUNDED_PARAMS_1)
        .build()

    internal inline fun BufferBuilder.writeParams(
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        elementSelector: () -> VertexFormatElement
    ) {
        val element = elementSelector()
        val ptr = beginElementAccess(element)
        if (ptr == -1L) return
        MemoryUtil.memPutFloat(ptr, x)
        MemoryUtil.memPutFloat(ptr + 4L, y)
        MemoryUtil.memPutFloat(ptr + 8L, z)
        MemoryUtil.memPutFloat(ptr + 12L, w)
    }
}
