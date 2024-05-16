package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object CopyPlaytime {

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (InventoryUtils.openInventoryName() != "Detailed /playtime") return
        if (event.slot.slotNumber != 4) return

        event.toolTip.add("")
        event.toolTip.add("§7[§b§lClick to Copy§7]")
    }

    @SubscribeEvent
    fun onSlotClicked(event: GuiContainerEvent.SlotClickEvent) {
        if (InventoryUtils.openInventoryName() != "Detailed /playtime") return
        if (event.slotId != 4) return

        val text = event.item?.getLore()?.toMutableList() ?: return

        if (event.clickedButton == 0) {
            event.isCanceled = true
            text.add(0, "${LorenzUtils.getPlayerName()}'s Playtime Stats")
            ClipboardUtils.copyToClipboard(text.joinToString("\n") { it.removeColor() })
        }
    }
}
