package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ClipboardUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object CopyPlaytime {

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTooltip(event: ToolTipEvent) {
        if (InventoryUtils.openInventoryName() != "Detailed /playtime") return
        if (event.slot.slotNumber != 4) return

        event.toolTip.add("")
        event.toolTip.add("§eClick to Copy!")
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (InventoryUtils.openInventoryName() != "Detailed /playtime") return
        if (event.slotId != 4) return
        if (event.clickedButton != 0) return

        event.cancel()
        val text = event.item?.getLore()?.toMutableList() ?: return

        val profile = HypixelData.profileName.firstLetterUppercase()
        text.add(0, "${PlayerUtils.getName()}'s - $profile Playtime Stats")

        ClipboardUtils.copyToClipboard(text.joinToString("\n") { it.removeColor() })
        ChatUtils.chat("Copied playtime stats into clipboard.")
    }
}
