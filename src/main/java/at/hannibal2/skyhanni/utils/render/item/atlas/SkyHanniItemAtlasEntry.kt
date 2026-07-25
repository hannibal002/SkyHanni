package at.hannibal2.skyhanni.utils.render.item.atlas

import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniAbstractAtlasEntry

internal open class SkyHanniItemAtlasEntry(
    x: Int, y: Int, u: Float, v: Float, pixelSize: Int,
) : SkyHanniAbstractAtlasEntry(x, y, u, v, pixelSize)

internal data class SkyHanniAnimatedItemAtlasEntry(
    override val x: Int,
    override val y: Int,
    override val u: Float,
    override val v: Float,
    override val pixelSize: Int,
    val lastRenderedFrame: Int,
) : SkyHanniItemAtlasEntry(x, y, u, v, pixelSize)

