package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.StringUtils.removeColor
import kotlin.time.Duration.Companion.seconds

@HanniModule
object HuntrapMisclickPrevention {

    private val config get() = HanniMod.feature.hunting
    private var lastNotified = SimpleTimeMark.farPast()

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.huntrapMisclick) return

        val inventoryName = InventoryUtils.openInventoryName()
        if (!inventoryName.startsWith("Hunting Toolkit")) return

        val clickedItem = event.item ?: return
        val lore = clickedItem.getLore()
        val hasEmptyStatus = lore.any { loreLine ->
            loreLine.removeColor().contains("Status: EMPTY")
        }

        if (!hasEmptyStatus) return
        event.cancel()

        if (lastNotified.passedSince() < 10.seconds) return
        lastNotified = SimpleTimeMark.now()
        ChatUtils.clickableChat(
            "Prevented clicking an empty trap in Hunting Toolkit! Click here to disable this feature.",
            { config::huntrapMisclick.jumpToEditor() },
            replaceSameMessage = true,
        )
    }
}
