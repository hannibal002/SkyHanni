package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getCurrentMilestoneTier
import at.hannibal2.skyhanni.data.garden.cropmilestones.CropMilestonesApi.getMaxTier
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
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

            val crop = CropMilestonesApi.getCropTypeByLore(event.stack) ?: return
            val allowOverflow = GardenApi.config.cropMilestones.overflow.inventoryStackSize
            val currentTier = crop.getCurrentMilestoneTier() ?: return
            val displayTier = if (!allowOverflow) minOf(getMaxTier(), currentTier) else currentTier
            event.stackTip = "" + displayTier
        }

        if (InventoryUtils.openInventoryName() == "Crop Upgrades") {
            if (!config.cropUpgrades) return

            upgradeTierPattern.firstMatcher(event.stack.getLore()) {
                event.stackTip = group("tier")
            }
        }

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (!config.composterUpgrades) return

            ComposterUpgrade.regex.matchMatcher(event.stack.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
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
