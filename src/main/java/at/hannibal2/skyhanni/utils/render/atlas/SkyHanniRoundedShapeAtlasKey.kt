package at.hannibal2.skyhanni.utils.render.atlas

/**
 * Identifies a static (non-animated) rounded shape for atlas caching.
 * Two draw calls with the same key will share a single atlas slot.
 */
sealed class SkyHanniRoundedShapeAtlasKey {
    abstract val pixelWidth: Int
    abstract val pixelHeight: Int
    abstract val smoothness: Float

    /**
     * A solid rounded rectangle with a single color or vertical color gradient.
     * Only atlasable when [topColor] == [bottomColor] (single color, truly static).
     */
    data class RoundedRect(
        override val pixelWidth: Int,
        override val pixelHeight: Int,
        val color: Int,
        val radius: Int,
        override val smoothness: Float,
    ) : SkyHanniRoundedShapeAtlasKey()

    /**
     * A solid circle (or arc) with a fixed fill color.
     * Only atlasable when the arc angles are the default full-circle values.
     */
    data class Circle(
        val radiusPixels: Int,
        val color: Int,
        override val smoothness: Float,
        val angle1: Float,
        val angle2: Float,
    ) : SkyHanniRoundedShapeAtlasKey() {
        override val pixelWidth get() = radiusPixels * 2
        override val pixelHeight get() = radiusPixels * 2
    }
}
