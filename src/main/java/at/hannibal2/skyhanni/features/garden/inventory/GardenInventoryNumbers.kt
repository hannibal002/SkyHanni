package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestones.getCounter
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenInventoryNumbers {

    private val config get() = GardenAPI.config.number

    private val upgradeTierPattern by RepoPattern.pattern(
        "garden.inventory.numbers.upgradetier",
        "§7Current Tier: §[ea](?<tier>.*)§7/§a.*"
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!GardenAPI.inGarden()) return

        if (InventoryUtils.openInventoryName() == "Crop Milestones") {
            if (!config.cropMilestone) return

            val crop = GardenCropMilestones.getCropTypeByLore(event.stack) ?: return
            val counter = crop.getCounter()
            val currentTier = GardenCropMilestones.getTierForCropCount(counter, crop)
            event.stackTip = "" + currentTier
        }

        if (InventoryUtils.openInventoryName() == "Crop Upgrades") {
            if (!config.cropUpgrades) return

            event.stack.getLore()
                .map { upgradeTierPattern.matcher(it) }
                .filter { it.matches() }
                .map { it.group("tier") }
                .forEach { event.stackTip = "" + it }
        }

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (!config.composterUpgrades) return

            ComposterUpgrade.regex.matchMatcher(event.stack.name) {
                val level = group("level")?.romanToDecimalIfNecessary() ?: 0
                event.stackTip = "$level"
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.numberCropMilestone", "garden.number.cropMilestone")
        event.move(3, "garden.numberCropUpgrades", "garden.number.cropUpgrades")
        event.move(3, "garden.numberComposterUpgrades", "garden.number.composterUpgrades")
    }
}
