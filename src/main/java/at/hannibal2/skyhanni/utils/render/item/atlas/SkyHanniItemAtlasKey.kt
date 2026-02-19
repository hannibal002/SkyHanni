package at.hannibal2.skyhanni.utils.render.item.atlas

import net.minecraft.world.phys.Vec3
import java.util.Objects

open class SkyHanniAtlasKey(
    open val modelIdentity: Any,
    open val scale: Float,
    open val guiScale: Int,
    open val stableId: Int,
    open val rotationVector: Vec3,
) {
    open val rotationSnapDegrees: Float = 2f
    private val quantizedRotationVector by lazy {
        quantizeRotation(rotationVector)
    }

    private fun quantizeRotation(vec: Vec3): Vec3 {
        val snap = { angle: Double -> (angle / rotationSnapDegrees).toInt() * rotationSnapDegrees.toDouble() }
        return Vec3(snap(vec.x), snap(vec.y), snap(vec.z))
    }

    override fun equals(other: Any?): Boolean =
        if (other !is SkyHanniAtlasKey) false
        else if (this === other) true
        else modelIdentity == other.modelIdentity &&
            quantizedRotationVector == other.quantizedRotationVector &&
            scale == other.scale &&
            guiScale == other.guiScale &&
            stableId == other.stableId

    override fun hashCode(): Int = Objects.hash(modelIdentity, quantizedRotationVector, scale, guiScale)
}

data class SkyHanniAnimatedAtlasKey(
    override val modelIdentity: Any,
    override val scale: Float,
    override val guiScale: Int,
    override val stableId: Int,
    override val rotationVector: Vec3,
    val frameNumber: Int,
) : SkyHanniAtlasKey(modelIdentity, scale, guiScale, stableId, rotationVector) {
    override val rotationSnapDegrees: Float = 0.125f

    override fun equals(other: Any?): Boolean =
        if (this === other) true
        else if (other !is SkyHanniAnimatedAtlasKey) false
        else super.equals(other) && frameNumber == other.frameNumber

    override fun hashCode(): Int = Objects.hash(super.hashCode(), stableId)
}
