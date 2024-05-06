package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ChatHoverEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.isRoman
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ReplaceRomanNumerals {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return

        event.toolTip.replaceAll { it.transformLine() }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onChatHover(event: ChatHoverEvent) {
        if (event.getHoverEvent().action != HoverEvent.Action.SHOW_TEXT) return
        if (!isEnabled()) return

        val lore = event.getHoverEvent().value.formattedText.split("\n").toMutableList()
        lore.replaceAll { it.transformLine() }

        val chatComponentText = ChatComponentText(lore.joinToString("\n"))
        val hoverEvent = HoverEvent(event.component.chatStyle.chatHoverEvent.action, chatComponentText)

        GuiChatHook.replaceOnlyHoverEvent(hoverEvent)
    }

    private fun String.transformLine() = split(" ").joinToString(" ") {
        it.takeIf { it.isValidRomanNumeral() }?.coloredRomanToDecimal() ?: it
    }

    private fun String.removeFormatting() = removeColor().replace(",", "")

    private fun String.isValidRomanNumeral() = removeFormatting()
        .let { it.isRoman() && it.isNotEmpty() }

    private fun String.coloredRomanToDecimal() = removeFormatting()
        .let { replace(it, it.romanToDecimal().toString()) }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.replaceRomanNumerals
}
