package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.composter.ComposterApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ComposterUpgradesData {
    val composterUpdateInventoryPattern by RepoPattern.pattern(
        "composter.upgrade.inventory",
        "Composter Upgrades",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!composterUpdateInventoryPattern.matches(event.inventoryName)) return
        for (item in event.inventoryItems.values) {
            ComposterUpgrade.composterUpgradePattern.matchMatcher(item.cleanName) {
                val name = group("name")
                val level = group("level")?.romanToDecimalIfNecessary() ?: 0
                val composterUpgrade = ComposterUpgrade.getByName(name) ?: continue
                ComposterApi.composterUpgrades?.put(composterUpgrade, level)
            }
        }
    }
}
