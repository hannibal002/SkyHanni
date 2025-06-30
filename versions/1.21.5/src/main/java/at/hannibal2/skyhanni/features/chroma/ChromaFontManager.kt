package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.client.font.BakedGlyph.DrawnGlyph
import net.minecraft.text.Style
import net.minecraft.text.TextColor

var renderingChat: Boolean = false
private val textColor = TextColor(0xFFFFFF, "chroma")
private val textColorOffWhite = TextColor(0xFFFFFE, "chroma")
var glyphIsChroma = false

fun checkIfGlyphIsChroma(drawnGlyph: DrawnGlyph) {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return
    val colorName = drawnGlyph.style.color?.name

    glyphIsChroma = colorName == "chroma"
}

fun setChromaColorStyle(style: Style, text: String, colorCode: Char): Style {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return style
    if (colorCode.lowercaseChar() == 'z') {
        return Style.EMPTY.withColor(textColor)
    }
    return style
}

fun forceWhiteTextColorForChroma(color: TextColor?): TextColor? {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return color

    val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
    val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return textColor
    }
    return color
}

fun forceChromaStyleIfNecessary(style: Style): Style {
    if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return style

    val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
    val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return style.withColor(textColorOffWhite)
    }
    return style
}

fun isNotActuallyEqualBecauseOfChroma(
    textColor: TextColor,
    testObject: Any
): Boolean = testObject is TextColor &&
    (textColor.name == "chroma" || testObject.name == "chroma") &&
    textColor.name != testObject.name
