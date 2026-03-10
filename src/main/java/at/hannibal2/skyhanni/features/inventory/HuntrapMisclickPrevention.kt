package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HuntrapMisclickPrevention {

    private val config get() = SkyHanniMod.feature.hunting
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
        ChatUtils.notifyOrDisable(
            "Prevented clicking an empty trap in Hunting Toolkit!",
            config::huntrapMisclick,
        )
    }
}
