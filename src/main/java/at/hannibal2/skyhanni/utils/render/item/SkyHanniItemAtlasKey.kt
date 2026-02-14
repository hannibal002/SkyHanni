package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.world.phys.Vec3

internal interface AtlasKey {
    val modelIdentity: Any
    val scale: Float
    val guiScale: Int
}

internal data class SkyHanniAtlasKey(
    override val modelIdentity: Any,
    val rotVec: Vec3,
    override val scale: Float,
    override val guiScale: Int,
) : AtlasKey {
    // Snap to nearest degree (or 2 degrees for even fewer entries)
    @Suppress("SameParameterValue")
    private fun quantizeRotation(vec: Vec3, snapDegrees: Float = 1f): Vec3 {
        val snap = { angle: Double -> (angle / snapDegrees).toInt() * snapDegrees.toDouble() }
        return Vec3(snap(vec.x), snap(vec.y), snap(vec.z))
    }

    private val quantizedRotVec = quantizeRotation(rotVec, 2f) // 2-degree snapping

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkyHanniAtlasKey) return false
        // Use stable comparison for model identity (hashCode) instead of referential ===
        return modelIdentity.hashCode() == other.modelIdentity.hashCode() &&
            quantizedRotVec == other.quantizedRotVec &&  // Use quantized version
            scale == other.scale &&
            guiScale == other.guiScale
    }

    override fun hashCode(): Int {
        return 31 * modelIdentity.hashCode() + quantizedRotVec.hashCode() + scale.hashCode() + guiScale.hashCode()
    }
}

internal class SkyHanniAnimatedKey(
    override val modelIdentity: Any,
    override val scale: Float,
    override val guiScale: Int,
    val stableId: Int,
) : AtlasKey {
    override fun equals(other: Any?): Boolean =
        if (this === other) true
        else if (other !is SkyHanniAnimatedKey) false
        else modelIdentity.hashCode() == other.modelIdentity.hashCode() &&
            stableId == other.stableId &&
            scale == other.scale &&
            guiScale == other.guiScale

    override fun hashCode(): Int {
        var result = modelIdentity.hashCode()
        result = 31 * result + stableId
        result = 31 * result + scale.hashCode()
        result = 31 * result + guiScale.hashCode()
        return result
    }
}

internal data class SkyHanniAtlasPosition(
    val x: Int,
    val y: Int,
    val u: Float,
    val v: Float,
    val lastRenderedFrame: Int,
)
