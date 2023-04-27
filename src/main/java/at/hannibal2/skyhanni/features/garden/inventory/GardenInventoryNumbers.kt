package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenInventoryNumbers {
    private val config get() = SkyHanniMod.feature.garden

    private var patternTierProgress = "§7Progress to Tier (.*): §e(?:.*)".toPattern()
    private var patternUpgradeTier = "§7Current Tier: §[ea](.*)§7/§a.*".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!GardenAPI.inGarden()) return

        if (InventoryUtils.openInventoryName() == "Crop Milestones") {
            if (!config.numberCropMilestone) return

            event.stack.getLore()
                .map { patternTierProgress.matcher(it) }
                .filter { it.matches() }
                .map { it.group(1).romanToDecimalIfNeeded() - 1 }
                .forEach { event.stackTip = "" + it }
        }

        if (InventoryUtils.openInventoryName() == "Crop Upgrades") {
            if (!config.numberCropUpgrades) return

            event.stack.getLore()
                .map { patternUpgradeTier.matcher(it) }
                .filter { it.matches() }
                .map { it.group(1) }
                .forEach { event.stackTip = "" + it }
        }

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (!config.numberComposterUpgrades) return

            event.stack.name?.let {
                val matcher = ComposterUpgrade.regex.matcher(it)
                if (matcher.matches()) {
                    val level = matcher.group("level")?.romanToDecimalIfNeeded() ?: 0
                    event.stackTip = "$level"
                }
            }
        }
    }
}