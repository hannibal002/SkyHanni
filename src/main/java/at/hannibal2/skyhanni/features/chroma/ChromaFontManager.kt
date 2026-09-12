package at.hannibal2.skyhanni.features.chroma

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph.GlyphInstance
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor

object ChromaFontManager {
    private const val CHROMA_COLOR_NAME = "skyhanni_chroma"

    // Unicode private use area character used to preview SkyHanni's chroma,
    // avoids the 'z' color code to stop other mods styling our preview text.
    const val CHROMA_PREVIEW_COLOR_CODE = '\uE002'

    /**
     * Chroma [TextColor] object meant for general use.
     */
    val chromaTextColor by lazy { TextColor(0xFFFFFF, CHROMA_COLOR_NAME) }

    /**
     * This is only to be used internally for the "Everything Chroma" feature.
     */
    private val allChromaTextColor by lazy { TextColor(0xFFFFFE, CHROMA_COLOR_NAME) }

    @JvmStatic
    var renderingChat = false

    @JvmStatic
    var glyphIsChroma = false
        private set

    fun TextColor.isChroma() = name == CHROMA_COLOR_NAME

    @JvmStatic
    fun checkIfGlyphIsChroma(drawnGlyph: GlyphInstance) {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return
        val colorName = drawnGlyph.style.color?.name

        glyphIsChroma = colorName == CHROMA_COLOR_NAME
    }

    @JvmStatic
    fun setChromaColorStyle(style: Style, colorCode: Char): Style {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return style
        if (colorCode.lowercaseChar() == 'z' || colorCode == CHROMA_PREVIEW_COLOR_CODE) {
            return Style.EMPTY.withColor(chromaTextColor)
        }
        return style
    }

    @JvmStatic
    fun forceWhiteTextColorForChroma(color: TextColor?): TextColor? {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return color

        val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
        val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

        if (allChroma && !chatFlag) {
            return chromaTextColor
        }
        return color
    }

    @JvmStatic
    fun forceChromaStyleIfNecessary(style: Style): Style {
        if (!SkyHanniMod.feature.gui.chroma.enabled.get()) return style

        val allChroma = SkyHanniMod.feature.gui.chroma.allChroma
        val chatFlag = SkyHanniMod.feature.gui.chroma.ignoreChat && renderingChat

        if (allChroma && !chatFlag) {
            return style.withColor(allChromaTextColor)
        }
        return style
    }

    @JvmStatic
    fun isNotActuallyEqualBecauseOfChroma(
        textColor: TextColor,
        testObject: Any,
    ): Boolean = testObject is TextColor &&
        (textColor.isChroma() || testObject.isChroma()) &&
        (textColor.name != testObject.name || textColor.value != testObject.value)
}
