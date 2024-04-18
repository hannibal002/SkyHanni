package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PowderPerHotmPerk {

    private val config get() = SkyHanniMod.feature.mining.hotmConfig

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return

        val itemName = event.itemStack.displayName
        val perk = HotmData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.getLevelUpCost() == null) return

        val currentPowderSpend = perk.calculateTotalCost(perk.activeLevel)
        val maxPowderNeeded = perk.totalCostMaxLevel
        val percentage = (currentPowderSpend.fractionOf(maxPowderNeeded) * 100).round(2)

        event.toolTip.add(" ")

        when (config.powderSpentDesign) {
            PowderSpentDesign.NUMBER -> {
                if (perk.activeLevel == perk.maxLevel) {
                    event.toolTip.add("§7Powder spent: §e${maxPowderNeeded.addSeparators()} §7(§aMax level§7)")
                } else {
                    event.toolTip.add("§7Powder spent: §e${currentPowderSpend.addSeparators()}§7 / §e${maxPowderNeeded.addSeparators()}")
                }
            }
            PowderSpentDesign.PERCENTAGE -> {
                if (perk.activeLevel == perk.maxLevel) {
                    event.toolTip.add("§7Powder spent: §e$percentage% §7(§aMax level§7)")
                } else {
                    event.toolTip.add("§7Powder spent: §e$percentage%§7 of max")
                }
            }
            PowderSpentDesign.NUMBER_AND_PERCENTAGE -> {
                if (perk.activeLevel == perk.maxLevel) {
                    event.toolTip.add("§7Powder spent: §e${maxPowderNeeded.addSeparators()} §7(§aMax level§7)")
                } else {
                    event.toolTip.add("§7Powder spent: §e${currentPowderSpend.addSeparators()}§7 / §e${maxPowderNeeded.addSeparators()}§7 (§e$percentage%§7)")
                }
            }
        }
    }

    enum class PowderSpentDesign(val str: String) {
        NUMBER("Number"),
        PERCENTAGE("Percentage"),
        NUMBER_AND_PERCENTAGE("Number and Percentage");

        override fun toString() = str
    }

    private fun isEnabled() = config.powderSpent && LorenzUtils.inSkyBlock && HotmData.inInventory
}
