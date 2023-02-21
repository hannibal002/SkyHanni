package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class GardenInventoryNumbers {

    private var patternTierProgress = Pattern.compile("§7Progress to Tier (.*): §e(?:.*)")
    private var patternUpgradeTier = Pattern.compile("§7Current Tier: §e(.*)§7/§a.*")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return

        if (InventoryUtils.openInventoryName() == "Crop Milestones") {
            if (!SkyHanniMod.feature.garden.numberCropMilestone) return

            event.stack.getLore()
                .map { patternTierProgress.matcher(it) }
                .filter { it.matches() }
                .map { it.group(1).romanToDecimal() - 1 }
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
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}