package at.hannibal2.skyhanni.utils.render.item.atlas

internal open class SkyHanniItemAtlasEntry(
    open val x: Int,
    open val y: Int,
    open val u: Float,
    open val v: Float,
)

internal data class SkyHanniAnimatedItemAtlasEntry(
    override val x: Int,
    override val y: Int,
    override val u: Float,
    override val v: Float,
    val lastRenderedFrame: Int,
) : SkyHanniItemAtlasEntry(x, y, u, v)

