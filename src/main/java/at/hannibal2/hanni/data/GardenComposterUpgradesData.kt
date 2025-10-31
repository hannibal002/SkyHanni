package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.model.ComposterUpgrade
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.features.garden.composter.ComposterApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher

@HanniModule
object GardenComposterUpgradesData {

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Composter Upgrades") return
        for (item in event.inventoryItems.values) {
            ComposterUpgrade.regex.matchMatcher(item.displayName) {
                val name = group("name")
                val level = group("level")?.romanToDecimalIfNecessary() ?: 0
                val composterUpgrade = ComposterUpgrade.getByName(name)!!
                ComposterApi.composterUpgrades?.put(composterUpgrade, level)
            }
        }
    }
}
