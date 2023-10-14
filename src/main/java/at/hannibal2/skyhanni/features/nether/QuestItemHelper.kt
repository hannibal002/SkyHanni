package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class QuestItemHelper {
    private val config get() = SkyHanniMod.feature.crimsonIsle

    private val itemCollectionPattern = ". (?<name>[\\w ]+) x(?<amount>\\d+)".toPattern()
    private var hasGottenItemsFromSack = false
    private var inInventory = false
    private var questItem = ""
    private var questAmount = 0

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        // double check just in case onInventoryClose fails to fire
        inInventory = event.inventoryName == "Fetch"
        if (!inInventory) return

        items@ for (item in event.inventoryItems.values) {
            itemCollectionPattern.matchMatcher(item.displayName.removeColor()) {
                questItem = group("name")
                questAmount = group("amount").toInt()
                break@items
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (!inInventory) return
        if (event.toolTip[0].contains("Close")) return

        event.toolTip.add("§eGet x$questAmount $questItem from sacks!")
    }

    @SubscribeEvent
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        if (!hasGottenItemsFromSack) {
            SackAPI.commandGetFromSacks(questItem, questAmount)
            hasGottenItemsFromSack = true
        }
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.questdailyFetchItemsFromSacks

}