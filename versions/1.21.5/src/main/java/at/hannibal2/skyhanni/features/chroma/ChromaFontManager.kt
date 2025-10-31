package at.hannibal2.hanni.features.chroma

import at.hannibal2.hanni.HanniMod
import net.minecraft.client.font.BakedGlyph.DrawnGlyph
import net.minecraft.text.Style
import net.minecraft.text.TextColor

var renderingChat: Boolean = false
private val textColor = TextColor(0xFFFFFF, "chroma")
private val textColorOffWhite = TextColor(0xFFFFFE, "chroma")
var glyphIsChroma = false

fun checkIfGlyphIsChroma(drawnGlyph: DrawnGlyph) {
    if (!HanniMod.feature.gui.chroma.enabled.get()) return
    val colorName = drawnGlyph.style.color?.name

    glyphIsChroma = colorName == "chroma"
}

fun setChromaColorStyle(style: Style, text: String, colorCode: Char): Style {
    if (!HanniMod.feature.gui.chroma.enabled.get()) return style
    if (colorCode.lowercaseChar() == 'z') {
        return Style.EMPTY.withColor(textColor)
    }
    return style
}

fun forceWhiteTextColorForChroma(color: TextColor?): TextColor? {
    if (!HanniMod.feature.gui.chroma.enabled.get()) return color

    val allChroma = HanniMod.feature.gui.chroma.allChroma
    val chatFlag = HanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return textColor
    }
    return color
}

fun forceChromaStyleIfNecessary(style: Style): Style {
    if (!HanniMod.feature.gui.chroma.enabled.get()) return style

    val allChroma = HanniMod.feature.gui.chroma.allChroma
    val chatFlag = HanniMod.feature.gui.chroma.ignoreChat && renderingChat

    if (allChroma && !chatFlag) {
        return style.withColor(textColorOffWhite)
    }
    return style
}

fun isNotActuallyEqualBecauseOfChroma(
    textColor: TextColor,
    testObject: Any,
): Boolean = testObject is TextColor &&
    (textColor.name == "chroma" || testObject.name == "chroma") &&
    textColor.getTextColorName() != testObject.getTextColorName()

// the get name inside of text colour does a string format and is very bad for performance
private fun TextColor.getTextColorName(): String? {
    return if (name != null) name else rgb.toString()
}
