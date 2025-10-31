package at.hannibal2.hanni.features.garden.inventory

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.GardenCropMilestones
import at.hannibal2.hanni.data.GardenCropMilestones.getCounter
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.model.ComposterUpgrade
import at.hannibal2.hanni.events.RenderItemTipEvent
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object GardenInventoryNumbers {

    private val config get() = GardenApi.config.number

    /**
     * REGEX-TEST: §7Current Tier: §e6§7/§a9
     */
    private val upgradeTierPattern by RepoPattern.pattern(
        "garden.inventory.numbers.upgradetier",
        "§7Current Tier: §[ea](?<tier>.*)§7/§a.*",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderItemTip(event: RenderItemTipEvent) {

        if (InventoryUtils.openInventoryName() == "Crop Milestones") {
            if (!config.cropMilestone) return

            val crop = GardenCropMilestones.getCropTypeByLore(event.stack) ?: return
            val counter = crop.getCounter()
            val allowOverflow = GardenApi.config.cropMilestones.overflow.inventoryStackSize
            val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop, allowOverflow)
            event.stackTip = "" + currentTier
        }

        if (InventoryUtils.openInventoryName() == "Crop Upgrades") {
            if (!config.cropUpgrades) return

            upgradeTierPattern.firstMatcher(event.stack.getLore()) {
                event.stackTip = group("tier")
            }
        }

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (!config.composterUpgrades) return

            ComposterUpgrade.regex.matchMatcher(event.stack.displayName) {
                val level = group("level")?.romanToDecimalIfNecessary() ?: 0
                event.stackTip = "$level"
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.numberCropMilestone", "garden.number.cropMilestone")
        event.move(3, "garden.numberCropUpgrades", "garden.number.cropUpgrades")
        event.move(3, "garden.numberComposterUpgrades", "garden.number.composterUpgrades")
    }
}
