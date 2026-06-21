package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.mixins.transformers.renderer.MixinBufferBuilderAccessor
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import org.lwjgl.system.MemoryUtil

private typealias VFEType = VertexFormatElement.Type
//? if < 26.1
//private typealias VFEUsage = VertexFormatElement.Usage

object SkyHanniVertexFormats {

    // Different versions of MC use differing counts, so load the last registered ID dynamically.
    val lastRegisteredId by lazy {
        (0 until VertexFormatElement.MAX_COUNT).filter { VertexFormatElement.byId(it) != null }.max()
    }

    @Suppress("UnusedPrivateProperty")
    internal enum class VertexElement(
        private val index: Int = 0,
        private val type: VFEType = VFEType.FLOAT,
        //~ if < 26.1 'normalized: Boolean = false' -> 'usage: VFEUsage = VFEUsage.GENERIC'
        private val normalized: Boolean = false,
        private val count: Int = 4,
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

        // The ID we use to register the format element with Minecraft.
        // see safeRegister() for details on how this is used and determined at runtime.
        private val registrationId: Int by lazy { lastRegisteredId + (ordinal + 1) }
        val element by lazy {
            //~ if < 26.1 'false' -> 'usage'
            safeRegister(registrationId, index, type, false, count)
        }
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
    @Suppress("UnusedParameter")
    private fun safeRegister(
        desiredId: Int,
        index: Int = 0,
        type: VFEType = VFEType.FLOAT,
        //~ if < 26.1 'normalized: Boolean = false' -> 'usage: VFEUsage = VFEUsage.GENERIC'
        normalized: Boolean = false,
        count: Int = 4,
    ): VertexFormatElement {
        // Todo, it is exceptionally unlikely that a user will have enough mods to register 27 more vertex format elements,
        //  but, technically possible, and something we should account for eventually.
        val id = (desiredId until VertexFormatElement.MAX_COUNT).first { VertexFormatElement.byId(it) == null }
        if (id != desiredId && PlatformUtils.isDevEnvironment) ErrorManager.logErrorStateWithData(
            "VertexFormatElement ID $desiredId was already taken, using $id instead",
            "SkyHanni vertex format element ID conflict. Desired ID $desiredId was already registered",
        )
        //~ if < 26.1 'normalized' -> 'usage'
        return VertexFormatElement.register(id, index, type, normalized, count)
    }

    val POSITION_COLOR_ROUNDED: VertexFormat by lazy {
        VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .build()
    }

    val POSITION_TEX_ROUNDED: VertexFormat by lazy {
        VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .build()
    }

    val POSITION_ROUNDED_GRADIENT: VertexFormat by lazy {
        VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("RoundedParams0", VertexElement.ROUNDED_PARAMS_0.element)
            .add("RoundedParams1", VertexElement.ROUNDED_PARAMS_1.element)
            .add("GradientParams0", VertexElement.GRADIENT_PARAMS_0.element)
            .add("GradientParams1", VertexElement.GRADIENT_PARAMS_1.element)
            .add("GradientParams2", VertexElement.GRADIENT_PARAMS_2.element)
            .build()
    }

    internal fun BufferBuilder.writeParams(
        x: Float,
        y: Float,
        z: Float,
        w: Float,
        format: VertexElement,
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
