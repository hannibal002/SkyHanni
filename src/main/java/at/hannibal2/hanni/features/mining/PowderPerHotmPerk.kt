package at.hannibal2.hanni.features.mining

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hotx.HotmData
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.fractionOf
import at.hannibal2.hanni.utils.NumberUtil.roundTo
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import org.lwjgl.input.Keyboard

@HanniModule
object PowderPerHotmPerk {

    private val config get() = HanniMod.feature.mining.hotm

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val itemName = event.itemStack.displayName
        val perk = HotmData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.getLevelUpCost() == null) return

        if (config.powderSpent) event.toolTip.add(2, handlePowderSpend(perk))
        if (config.powderFor10Levels) handlePowderFor10Levels(event, perk)
    }

    private fun handlePowderFor10Levels(event: ToolTipEvent, perk: HotmData) {
        if (!Keyboard.KEY_LSHIFT.isKeyHeld()) return

        val indexOfCost = event.toolTip.indexOfFirst { HotmData.perkCostPattern.matches(it) }

        if (indexOfCost == -1) return

        val powderFor10Levels =
            perk.calculateTotalCost((perk.rawLevel + 10).coerceAtMost(perk.maxLevel)) - perk.calculateTotalCost(perk.rawLevel)

        val numberOfLevels = (perk.maxLevel - perk.rawLevel).coerceAtMost(10)
        val levelsFormat = StringUtils.pluralize(numberOfLevels, "level")

        event.toolTip.add(indexOfCost + 2, "§7Powder for $numberOfLevels $levelsFormat §e${powderFor10Levels.addSeparators()}")
    }

    private fun handlePowderSpend(perk: HotmData): String {
        val currentPowderSpend = perk.calculateTotalCost(perk.rawLevel)
        val maxPowderNeeded = perk.totalCostMaxLevel
        val percentage = (currentPowderSpend.fractionOf(maxPowderNeeded) * 100).roundTo(2)

        return when (config.powderSpentDesign) {
            PowderSpentDesign.NUMBER -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Powder spent: §e${maxPowderNeeded.addSeparators()} §7(§aMax level§7)"
                } else {
                    "§7Powder spent: §e${currentPowderSpend.addSeparators()}§7 / §e${maxPowderNeeded.addSeparators()}"
                }
            }

            PowderSpentDesign.PERCENTAGE -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Powder spent: §e$percentage% §7(§aMax level§7)"
                } else {
                    "§7Powder spent: §e$percentage%§7 of max"
                }
            }

            PowderSpentDesign.NUMBER_AND_PERCENTAGE -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Powder spent: §e${maxPowderNeeded.addSeparators()} §7(§aMax level§7)"
                } else {
                    "§7Powder spent: §e${currentPowderSpend.addSeparators()}§7/§e${maxPowderNeeded.addSeparators()}§7 (§e$percentage%§7)"
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

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HotmData.inInventory && (config.powderSpent || config.powderFor10Levels)
}
