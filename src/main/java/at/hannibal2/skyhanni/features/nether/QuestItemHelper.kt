package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class QuestItemHelper {

    private val itemCollectionPattern = ". (?<name>[\\w ]+) x(?<amount>\\d+)".toPattern()
    private var hasGottenItemsFromSack = false
    private var isInQuestGUI = false
    private var questItem = ""
    private var questAmount = 0

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.crimsonIsle.crimsonQuestItems) return
        if (event.inventoryName != "Fetch") {
            isInQuestGUI = false // double check just in case onInventoryClose fails to fire
            return
        }
        isInQuestGUI = true
        loop@ for ((_, item) in event.inventoryItems) {
            itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                if (!matches()) continue@loop
                questItem = group("name")
                questAmount = group("amount").toInt()
                break@loop
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        isInQuestGUI = false
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isInQuestGUI) return
        if (event.toolTip[0].contains("Close")) return
        getNewFetchTooltip(event.toolTip, questItem, questAmount)
    }

    @SubscribeEvent
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!SkyHanniMod.feature.crimsonIsle.crimsonQuestItems) return
        if (!isInQuestGUI) return

        if (!hasGottenItemsFromSack) {
//            event.isCanceled = true
            SackAPI.getFromSacks(questItem, questAmount)
            hasGottenItemsFromSack = true
        }
    }

    private fun getNewFetchTooltip(originalTooltip: MutableList<String>, item: String, amount: Int) {
        originalTooltip.add("§eGet x$amount $item from sacks!")
    }
}