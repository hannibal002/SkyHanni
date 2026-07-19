package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.data.model.ComposterUpgrade.entries
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.composter.ComposterApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils.enumJoinToPattern
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ComposterUpgradesData {

    /**
     * REGEX-TEST: Composter Speed II
     * REGEX-TEST: Multi Drop III
     * REGEX-TEST: Fuel Cap I
     * REGEX-TEST: Organic Matter Cap IV
     * REGEX-TEST: Cost Reduction V
     */
    val composterUpgradePattern by RepoPattern.pattern(
        "composter.upgrade.name",
        "(?<name>${enumJoinToPattern<ComposterUpgrade> { it.displayName }})(?: (?<level>.*))?",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Composter Upgrades") return
        for (item in event.inventoryItems.values) {
            composterUpgradePattern.matchMatcher(item.cleanName) {
                val name = group("name")
                val level = group("level")?.romanToDecimalIfNecessary() ?: 0
                val composterUpgrade = ComposterUpgrade.getByName(name) ?: return@matchMatcher
                ComposterApi.composterUpgrades?.put(composterUpgrade, level)
            }
        }
    }
}
