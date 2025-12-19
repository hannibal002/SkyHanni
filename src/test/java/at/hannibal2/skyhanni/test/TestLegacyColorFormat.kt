package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhite
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import at.hannibal2.skyhanni.utils.compat.unformattedTextForChatCompat
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TestLegacyColorFormat {

    private val testText1 = Text.literal("")
        .append(Text.literal("[").withColor(Formatting.DARK_GRAY))
        .append(Text.literal("302").withColor(Formatting.BLUE))
        .append(Text.literal("] ").withColor(Formatting.DARK_GRAY))
        .append(Text.literal("♫ ").withColor(Formatting.GOLD))
        .append(Text.literal("[MVP").withColor(Formatting.AQUA))
        .append(Text.literal("+").withColor(Formatting.LIGHT_PURPLE))
        .append(Text.literal("] lrg89").withColor(Formatting.AQUA))
        .append(Text.literal(": test").withColor(Formatting.WHITE))

    private val testText2 = Text.literal("")
        .append(Text.literal("Test ").withColor(Formatting.WHITE))
        .append(Text.literal("Extra ").setStyle(Style.EMPTY.withBold(true)))
        .append(Text.literal("Resets ").setStyle(Style.EMPTY))
        .append(Text.literal("§r").setStyle(Style.EMPTY))
        .append(Text.literal("Done").setStyle(Style.EMPTY.withObfuscated(true)))

    @Test
    fun `test formatted text compat`() {
        Assertions.assertEquals("§8[§r§9302§r§8] §r§6♫ §r§b[MVP§r§d+§r§b] lrg89§r§f: test", testText1.formattedTextCompat())
        Assertions.assertEquals("Test §r§lExtra §rResets §r§r§r§kDone", testText2.formattedTextCompat())
    }

    @Test
    fun `test formatted text compat less resets`() {
        Assertions.assertEquals("§8[§9302§8] §6♫ §b[MVP§d+§b] lrg89§f: test", testText1.formattedTextCompatLessResets())
        Assertions.assertEquals("Test §lExtra Resets §r§kDone", testText2.formattedTextCompatLessResets())
    }

    @Test
    fun `test formatted text compat leading white`() {
        Assertions.assertEquals("§8[§r§9302§r§8] §r§6♫ §r§b[MVP§r§d+§r§b] lrg89§r§f: test", testText1.formattedTextCompatLeadingWhite())
        Assertions.assertEquals("§fTest §r§lExtra §rResets §r§r§r§kDone", testText2.formattedTextCompatLeadingWhite())
    }

    @Test
    fun `test formatted text compat leading white less resets`() {
        Assertions.assertEquals("§8[§9302§8] §6♫ §b[MVP§d+§b] lrg89§f: test", testText1.formattedTextCompatLeadingWhiteLessResets())
        Assertions.assertEquals("§fTest §lExtra Resets §r§kDone", testText2.formattedTextCompatLeadingWhiteLessResets())
    }

    @Test
    fun `test unformatted text`() {
        Assertions.assertEquals("[302] ♫ [MVP+] lrg89: test", testText1.unformattedTextCompat())
        Assertions.assertEquals("Test Extra Resets §rDone", testText2.unformattedTextCompat())
    }

}
