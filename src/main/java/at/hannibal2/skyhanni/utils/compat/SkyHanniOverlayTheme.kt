package at.hannibal2.skyhanni.utils.compat

import java.awt.Color

/**
 * Defines the visual style for a [SkyHanniContainerOverlayScreen].
 * Subclass and override fields for custom screen styling.
 *
 * @param backgroundColor The overlay background color.
 * @param topBorderColor The top gradient color of the border outline.
 * @param bottomBorderColor The bottom gradient color of the border outline.
 * @param outlineThickness Pixel thickness of slot outlines.
 * @param outlineBlur Blur factor applied to slot outlines.
 * @param backgroundPadding Padding in pixels between content and the rounded background rect (unscaled).
 * @param borderRadius Corner radius of the background rounded rect.
 */
open class SkyHanniOverlayTheme(
    val backgroundColor: Color = Color(0, 0, 0, 150),
    val topBorderColor: Color = Color(170, 0, 170),
    val bottomBorderColor: Color = Color(85, 0, 85),
    val outlineThickness: Int = 2,
    val outlineBlur: Float = 0.5f,
    val backgroundPadding: Int = 10,
    val borderRadius: Int = 8,
) {
    companion object {
        val DEFAULT = SkyHanniOverlayTheme()
    }
}
