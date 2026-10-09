package at.hannibal2.skyhanni.utils.render

import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import org.lwjgl.system.MemoryUtil

object SkyHanniVertexFormats {
    @Suppress("EmptyDefaultConstructor")
    internal enum class VertexElement {
        // {radius, smoothness/borderThickness, adjustedHalfSizeX, adjustedHalfSizeY}
        ROUNDED_PARAMS_0,
        // {adjustedCenterPosX, adjustedCenterPosY, borderBlur/angle1/0, angle2/0}
        ROUNDED_PARAMS_1,
        // {angle, progress, phaseOffset, reverse(float)}
        GRADIENT_PARAMS_0,
        // {startColor R, G, B, A}
        GRADIENT_PARAMS_1,
        // {endColor R, G, B, A}
        GRADIENT_PARAMS_2,
        ;

        val attributeName: String =
            name.lowercase().split("_").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    }

    val POSITION_COLOR_ROUNDED: VertexFormat by lazy {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
    }

    val POSITION_TEX_ROUNDED: VertexFormat by lazy {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
    }

    val POSITION_ROUNDED_GRADIENT: VertexFormat by lazy {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_2.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
    }

    internal fun BufferBuilder.writeParams(
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        format: VertexElement,
    ) {
        val vertexPointer = vertexPointer.takeIf { it != -1L } ?: return
        val element = this.format.getElement(format.attributeName) ?: return
        val ptr = vertexPointer + element.offset()
        MemoryUtil.memPutFloat(ptr, x)
        MemoryUtil.memPutFloat(ptr + 4L, y)
        MemoryUtil.memPutFloat(ptr + 8L, z)
        MemoryUtil.memPutFloat(ptr + 12L, w)
    }
}
