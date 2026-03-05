package at.hannibal2.skyhanni.utils.render.item.atlas

internal open class SkyHanniItemAtlasEntry(
    open val x: Int,
    open val y: Int,
    open val u: Float,
    open val v: Float,
    open val pixelSize: Int,
)

internal data class SkyHanniAnimatedItemAtlasEntry(
    override val x: Int,
    override val y: Int,
    override val u: Float,
    override val v: Float,
    override val pixelSize: Int,
    val lastRenderedFrame: Int,
) : SkyHanniItemAtlasEntry(x, y, u, v, pixelSize)

