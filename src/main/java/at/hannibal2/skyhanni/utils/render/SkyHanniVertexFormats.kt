package at.hannibal2.skyhanni.utils.render

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import org.lwjgl.system.MemoryUtil

//? if >= 26.2 {
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.vertex.DefaultVertexFormat
//?} else {
/*import com.mojang.blaze3d.vertex.VertexFormatElement
*///?}

object SkyHanniVertexFormats {
    //? if < 26.2 {
    /*// Vanilla registers IDs 0-6.
    private var startingId = 7
    *///?}

    @Suppress("EmptyDefaultConstructor")
    internal enum class VertexElement(
        //? if < 26.2 {
        /*private val index: Int = 0,
        private val type: VertexFormatElement.Type = FLOAT,
        private val normalized: Boolean = false,
        private val count: Int = 4,
        *///?}
    ) {
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

        //? if < 26.2 {
        /*val element by lazy {
            safeRegister(index, type, normalized, count)
        }
        *///?}
    }

    //? if < 26.2 {
    /*/**
     * Registers a VertexFormatElement with the given parameters,
     * automatically finding an available ID.
     *
     * @param index The index of the element in the vertex format (default is 0).
     * @param type The data type of the element (default is FLOAT).
     * @param normalized Whether integer data types should be mapped to the range [-1.0, 1.0]
     *     (for signed values) or [0.0, 1.0] (for unsigned values) before converting to float.
     * @param count The number of components in the element (default is 4).
     * @return The registered VertexFormatElement, guaranteed to have a unique ID.
     */
    private fun safeRegister(
        index: Int = 0,
        type: VertexFormatElement.Type = FLOAT,
        normalized: Boolean = false,
        count: Int = 4,
    ): VertexFormatElement {
        // It is technically possible, but exceptionally unlikely, that a user will have enough
        // mods to register 27 more vertex format elements. We deliberately let the game crash
        // here rather than attempting to recover, because a failed registration would leave a
        // lot of mod features broken, and forcibly extending the array would be potentially
        // unsafe and require mixins into mods such as Sodium that hardcode the size as 32.
        val id = (startingId++ until VertexFormatElement.MAX_COUNT).firstOrNull { VertexFormatElement.byId(it) == null }
            ?: error("Too many mods trying to register VertexFormatElements")
        return VertexFormatElement.register(id, index, type, normalized, count)
    }
    *///?}

    val POSITION_COLOR_ROUNDED: VertexFormat by lazy {
        //? if >= 26.2 {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.COLOR_SEMANTIC_NAME, GpuFormat.RGBA8_UNORM)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
        //?} else {
        /*VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .build()
        *///?}
    }

    val POSITION_TEX_ROUNDED: VertexFormat by lazy {
        //? if >= 26.2 {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(DefaultVertexFormat.UV0_SEMANTIC_NAME, GpuFormat.RG32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
        //?} else {
        /*VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .build()
        *///?}
    }

    val POSITION_ROUNDED_GRADIENT: VertexFormat by lazy {
        //? if >= 26.2 {
        VertexFormat.builder(0)
            .addAttribute(DefaultVertexFormat.POSITION_SEMANTIC_NAME, GpuFormat.RGB32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.ROUNDED_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_0.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_1.attributeName, GpuFormat.RGBA32_FLOAT)
            .addAttribute(VertexElement.GRADIENT_PARAMS_2.attributeName, GpuFormat.RGBA32_FLOAT)
            .build()
        //?} else {
        /*VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .add("GradientParams0", VertexElement.GRADIENT_PARAMS_0.element)
            .add("GradientParams1", VertexElement.GRADIENT_PARAMS_1.element)
            .add("GradientParams2", VertexElement.GRADIENT_PARAMS_2.element)
            .build()
        *///?}
    }

    internal fun BufferBuilder.writeParams(
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        format: VertexElement,
    ) {
        //? if >= 26.2 {
        val vertexPointer = vertexPointer.takeIf { it != -1L } ?: return
        val element = this.format.getElement(format.attributeName) ?: return
        val ptr = vertexPointer + element.offset()
        //?} else {
        /*val element = format.element
        val ptr = beginElement(element).takeIf {
            it != -1L
        } ?: return
        *///?}
        MemoryUtil.memPutFloat(ptr, x)
        MemoryUtil.memPutFloat(ptr + 4L, y)
        MemoryUtil.memPutFloat(ptr + 8L, z)
        MemoryUtil.memPutFloat(ptr + 12L, w)
    }
}
