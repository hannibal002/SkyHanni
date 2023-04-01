package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenInventoryNumbers {

    private var patternTierProgress = Pattern.compile("§7Progress to Tier (.*): §e(?:.*)")
    private var patternUpgradeTier = Pattern.compile("§7Current Tier: §e(.*)§7/§a.*")
    private val patternComposterUpgrades =
        Pattern.compile("§a(?:Composter Speed|Multi Drop|Fuel Cap|Organic Matter Cap|Cost Reduction) ?(.*)?")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!GardenAPI.inGarden()) return

        if (InventoryUtils.openInventoryName() == "Crop Milestones") {
            if (!SkyHanniMod.feature.garden.numberCropMilestone) return

            event.stack.getLore()
                .map { patternTierProgress.matcher(it) }
                .filter { it.matches() }
                .map { it.group(1).romanToDecimalIfNeeded() - 1 }
                .forEach { event.stackTip = "" + it }
        }

        if (InventoryUtils.openInventoryName() == "Crop Upgrades") {
            if (!SkyHanniMod.feature.garden.numberCropUpgrades) return

            event.stack.getLore()
                .map { patternUpgradeTier.matcher(it) }
                .filter { it.matches() }
                .map { it.group(1) }
                .forEach { event.stackTip = "" + it }
        }

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (!SkyHanniMod.feature.garden.numberComposterUpgrades) return

            event.stack.name?.let {
                val matcher = patternComposterUpgrades.matcher(it)
                if (matcher.matches()) {
                    event.stackTip = if (matcher.groupCount() != 0) {
                        val group = matcher.group(1)
                        "" + group.romanToDecimalIfNeeded()
                    } else "0"
                }
            }
        }
    }
}