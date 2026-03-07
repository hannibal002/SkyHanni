package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.mixins.transformers.renderer.MixinBufferBuilderAccessor
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import org.lwjgl.system.MemoryUtil

private typealias VFEType = VertexFormatElement.Type
private typealias VFEUsage = VertexFormatElement.Usage
internal typealias SHVFE = SkyHanniVertexFormats.SkyHanniVertexFormatElement

object SkyHanniVertexFormats {

    internal enum class SkyHanniVertexFormatElement(
        // The ID we use to register the format element with Minecraft.
        // see safeRegister() for details on how this is used and determined at runtime.
        private val registrationId: Int,
        private val index: Int = 0,
        private val type: VFEType = VFEType.FLOAT,
        private val usage: VFEUsage = VFEUsage.GENERIC,
        private val count: Int = 4,
    ) {
        // {radius, smoothness/borderThickness, adjustedHalfSizeX, adjustedHalfSizeY}
        ROUNDED_PARAMS_0(6),
        // {adjustedCenterPosX, adjustedCenterPosY, borderBlur/0, 0}
        ROUNDED_PARAMS_1(7),
        ;

        val element by lazy { safeRegister(registrationId, index, type, usage, count) }
    }

    /**
     * Registers a VertexFormatElement with the given parameters, automatically finding an available ID if the desired one is taken.
     * Logs an error if the desired ID was already taken, but still registers the element with a valid ID.
     * @param desiredId The preferred ID for the VertexFormatElement.
     * @param index The index of the element in the vertex format (default is 0).
     * @param type The data type of the element (default is FLOAT).
     * @param usage The intended usage of the element (default is GENERIC).
     * @param count The number of components in the element (default is 4).
     * @return The registered VertexFormatElement, guaranteed to have a unique ID.
     */
    private fun safeRegister(
        desiredId: Int,
        index: Int = 0,
        type: VFEType = VFEType.FLOAT,
        usage: VFEUsage = VFEUsage.GENERIC,
        count: Int = 4,
    ): VertexFormatElement {
        // Todo, it is exceptionally unlikely that a user will have enough mods to register 27 more vertex format elements,
        //  but, technically possible, and something we should account for eventually.
        val id = (desiredId until VertexFormatElement.MAX_COUNT).first { VertexFormatElement.byId(it) == null }
        if (id != desiredId) ErrorManager.logErrorStateWithData(
            "VertexFormatElement ID $desiredId was already taken, using $id instead",
            "SkyHanni vertex format element ID conflict. Desired ID $desiredId was already registered",
        )
        return VertexFormatElement.register(id, index, type, usage, count)
    }

    val POSITION_COLOR_ROUNDED: VertexFormat by lazy {
        VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("RoundedParams0", SkyHanniVertexFormatElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", SkyHanniVertexFormatElement.ROUNDED_PARAMS_1.element)
            .build()
    }

    internal fun BufferBuilder.writeParams(
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        format: SkyHanniVertexFormatElement,
    ) {
        val element = format.element
        val ptr = (this@writeParams as MixinBufferBuilderAccessor).invokeBeginElement(element).takeIf {
            it != -1L
        } ?: return
        MemoryUtil.memPutFloat(ptr, x)
        MemoryUtil.memPutFloat(ptr + 4L, y)
        MemoryUtil.memPutFloat(ptr + 8L, z)
        MemoryUtil.memPutFloat(ptr + 12L, w)
    }
}
