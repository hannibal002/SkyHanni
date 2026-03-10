package at.hannibal2.skyhanni.utils.render.item.atlas

import net.minecraft.world.phys.Vec3
import java.util.Objects

open class SkyHanniAtlasKey(
    open val item: String,
    open val modelIdentity: Any,
    open val rotationVector: Vec3,
) {
    open val rotationSnapDegrees: Float = 2f
    private val quantizedRotationVector: Vec3 by lazy {
        quantizeRotation(rotationVector)
    }

    private fun quantizeRotation(vec: Vec3): Vec3 {
        val snap = { angle: Double -> (angle / rotationSnapDegrees).toInt() * rotationSnapDegrees.toDouble() }
        return Vec3(snap(vec.x), snap(vec.y), snap(vec.z))
    }

    /**
     * We intentionally do not include stable ID in the equals.
     * If two separate renderables generate the same atlas key except for stable ID, we want them to share an atlas space.
     */
    override fun equals(other: Any?): Boolean =
        if (other !is SkyHanniAtlasKey) false
        else if (this === other) true
        else item == other.item &&
            modelIdentity == other.modelIdentity &&
            quantizedRotationVector == other.quantizedRotationVector

    /**
     * We intentionally do not include stable ID in the hashcode.
     * If two separate renderables generate the same atlas key except for stable ID, we want them to share an atlas space.
     */
    override fun hashCode(): Int = Objects.hash(item, modelIdentity, quantizedRotationVector)
}

data class SkyHanniAnimatedAtlasKey(
    override val item: String,
    override val modelIdentity: Any,
    override val rotationVector: Vec3,
    val frameNumber: Int,
) : SkyHanniAtlasKey(item, modelIdentity, rotationVector) {
    override val rotationSnapDegrees: Float = 0.125f

    constructor(baseKey: SkyHanniAtlasKey, frameNumber: Int) : this(
        item = baseKey.item,
        modelIdentity = baseKey.modelIdentity,
        rotationVector = baseKey.rotationVector,
        frameNumber = frameNumber,
    )

    override fun equals(other: Any?): Boolean =
        if (this === other) true
        else if (other !is SkyHanniAnimatedAtlasKey) false
        else super.equals(other) && frameNumber == other.frameNumber

    override fun hashCode(): Int = Objects.hash(super.hashCode(), frameNumber)
}
