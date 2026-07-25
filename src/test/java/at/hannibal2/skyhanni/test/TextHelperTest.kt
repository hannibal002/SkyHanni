package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TextHelperTest {

    @Test
    fun `split removes delimiters and keeps split text`() {
        val component = Component.literal("")
            .append(Component.literal("Cookie Buff\n").withColor(ChatFormatting.LIGHT_PURPLE))
            .append(Component.literal("10 months, 19 days").withColor(ChatFormatting.GREEN))

        val split = TextHelper.split(component, "\n") ?: emptyList()

        Assertions.assertEquals(listOf("Cookie Buff", "10 months, 19 days"), split.map { it.string })
        Assertions.assertFalse(split.any { "\n" in it.string })
        Assertions.assertEquals("§dCookie Buff", split[0].formattedTextCompatLessResets())
        Assertions.assertEquals("§a10 months, 19 days", split[1].formattedTextCompatLessResets())
    }
}
