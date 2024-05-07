package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ChatHoverEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.isRoman
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ReplaceRomanNumerals {
    // Using toRegex here since toPattern doesn't seem to provide the necessary functionality
    private val splitRegex = "((§\\w)|(\\s+)|(\\W))+|(\\w*)".toRegex()

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

    @SubscribeEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (!isEnabled()) return
        event.applyIfPossible { it.transformLine() }
    }

    private fun SystemMessageEvent.applyIfPossible(transform: (String) -> String) {
        val original = chatComponent.formattedText
        val new = transform(original)
        if (new == original) return

        val clickEvents = mutableListOf<ClickEvent>()
        val hoverEvents = mutableListOf<HoverEvent>()
        chatComponent.findAllEvents(clickEvents, hoverEvents)

        if (clickEvents.size > 1 || hoverEvents.size > 1) return

        chatComponent = ChatComponentText(new)
        if (clickEvents.size == 1) chatComponent.chatStyle.chatClickEvent = clickEvents.first()
        if (hoverEvents.size == 1) chatComponent.chatStyle.chatHoverEvent = hoverEvents.first()
    }

    private fun IChatComponent.findAllEvents(
        clickEvents: MutableList<ClickEvent>,
        hoverEvents: MutableList<HoverEvent>
    ) {
        siblings.forEach { it.findAllEvents(clickEvents, hoverEvents) }

        val clickEvent = chatStyle.chatClickEvent
        val hoverEvent = chatStyle.chatHoverEvent

        if (clickEvent?.action != null && clickEvents.none { it.value == clickEvent.value }) {
            clickEvents.add(clickEvent)
        }
        if (hoverEvent?.action != null && hoverEvents.none { it.value == hoverEvent.value }) {
            hoverEvents.add(hoverEvent)
        }
    }

    private fun String.transformLine() = splitRegex.findAll(this).map { it.value }.joinToString("") {
        it.takeIf { it.isValidRomanNumeral() }?.coloredRomanToDecimal() ?: it
    }

    private fun String.removeFormatting() = removeColor().replace(",", "")

    private fun String.isValidRomanNumeral() = removeFormatting()
        .let { it.isRoman() && it.isNotEmpty() }

    private fun String.coloredRomanToDecimal() = removeFormatting()
        .let { replace(it, it.romanToDecimal().toString()) }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.misc.replaceRomanNumerals
}
