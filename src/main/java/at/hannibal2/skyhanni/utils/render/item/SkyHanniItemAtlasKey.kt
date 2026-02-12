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
) : AtlasKey

internal data class SkyHanniAnimatedKey(
    override val modelIdentity: Any,
    override val scale: Float,
    override val guiScale: Int,
) : AtlasKey

internal data class SkyHanniAtlasPosition(
    val x: Int,
    val y: Int,
    val u: Float,
    val v: Float,
    val lastRenderedFrame: Int,
)
