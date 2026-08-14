package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.utils.StringUtils.createCommaSeparatedList
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.createCommaSeparatedList
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

    @Test
    fun `create Oxford Comma separated components`() {
        val componentList: MutableList<Component> = mutableListOf()
        val stringList: MutableList<String> = mutableListOf()
        Assertions.assertTrue(componentList.createCommaSeparatedList() == Component.empty())

        val plotNamedWheat = "Wheat".asComponent().withColor(ChatFormatting.AQUA)
        val wheatPlotLegacy = plotNamedWheat.formattedTextCompatLessResets()

        stringList.add(wheatPlotLegacy)
        componentList.add(plotNamedWheat)
        oxfordCommaComparer(componentList, stringList)

        componentList.add(plotNamedWheat)
        stringList.add(wheatPlotLegacy)
        oxfordCommaComparer(componentList, stringList)

        componentList.add(plotNamedWheat)
        stringList.add(wheatPlotLegacy)
        oxfordCommaComparer(componentList, stringList)
    }

    private fun oxfordCommaComparer(
        componentList: List<Component>,
        stringList: List<String>,
    ) {
        Assertions.assertEquals(
            componentList.createCommaSeparatedList(ChatFormatting.GRAY)
                .formattedTextCompatLessResets(),
            stringList.createCommaSeparatedList("§7"),
        )
    }
}
