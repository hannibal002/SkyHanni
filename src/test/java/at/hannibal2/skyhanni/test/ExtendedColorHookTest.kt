package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.mixins.hooks.ExtendedColorHook
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Style
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExtendedColorHookTest {
    @Test
    fun `skips legacy colors inside rgb sequence`() {
        val text = "§#§6§a§e§e§4§8§/text"

        assertTrue(ExtendedColorHook.shouldSkipLegacyFormatting(text, 0, '#'))
        assertTrue(ExtendedColorHook.shouldSkipLegacyFormatting(text, 2, '6'))
        assertTrue(ExtendedColorHook.shouldSkipLegacyFormatting(text, 4, 'a'))
        assertTrue(ExtendedColorHook.shouldSkipLegacyFormatting(text, 14, '/'))
    }

    @Test
    fun `applies rgb color at closing marker`() {
        val text = "§#§6§a§e§e§4§8§/text"
        val style = ExtendedColorHook.applyExtendedColorStyle(Style.EMPTY, text, 14, '/')

        assertEquals(0x6aee48, style.color?.value)
    }

    @Test
    fun `extended color acts like a legacy color reset`() {
        val text = "§#§6§a§e§e§4§8§/text"
        val startingStyle = Style.EMPTY.withBold(true).withUnderlined(true).withColor(ChatFormatting.RED)
        val style = ExtendedColorHook.applyExtendedColorStyle(startingStyle, text, 14, '/')

        assertEquals(0x6aee48, style.color?.value)
        assertFalse(style.isBold)
        assertFalse(style.isUnderlined)
    }

    @Test
    fun `ignores incomplete rgb sequence close marker`() {
        val text = "§#§6§a§/"

        assertFalse(ExtendedColorHook.shouldSkipLegacyFormatting(text, 6, '/'))
        assertEquals(null, ExtendedColorHook.applyExtendedColorStyle(Style.EMPTY, text, 6, '/').color)
    }

    @Test
    fun `applies argb alpha at color conversion`() {
        val text = "§#§8§0§6§a§e§e§4§8§/text"
        val style = ExtendedColorHook.applyExtendedColorStyle(Style.EMPTY, text, 18, '/')

        assertEquals(0x6aee48, style.color?.value)
        assertEquals(0x806aee48.toInt(), ExtendedColorHook.applyExtendedColorAlpha(0xff6aee48.toInt(), style.color))
    }

    @Test
    fun `rgb sequence keeps original draw alpha`() {
        val text = "§#§6§a§e§e§4§8§/text"
        val style = ExtendedColorHook.applyExtendedColorStyle(Style.EMPTY, text, 14, '/')

        assertEquals(0x336aee48, ExtendedColorHook.applyExtendedColorAlpha(0x336aee48, style.color))
    }
}
