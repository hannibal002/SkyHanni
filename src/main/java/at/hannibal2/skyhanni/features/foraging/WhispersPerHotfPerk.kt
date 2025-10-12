package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import org.lwjgl.input.Keyboard

@SkyHanniModule
object WhispersPerHotfPerk {

    private val config get() = SkyHanniMod.feature.foraging.hotf

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val itemName = event.itemStack.displayName
        val perk = HotfData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.getLevelUpCost() == null) return

        if (config.whispersSpent) event.toolTip.add(2, handleWhispersSpent(perk))
        if (config.whispersFor10Levels) handleWhispersFor10Levels(event, perk)
        if (config.currentWhispers) handleCurrentWhispers(event, perk)
    }

    private fun handleCurrentWhispers(event: ToolTipEvent, perk: HotfData) {
        if (!perk.isUnlocked || perk.isMaxLevel) return

        val indexOfCost = event.toolTip.indexOfFirst { HotmData.perkCostPattern.matches(it) }

        event.toolTip.add(indexOfCost + 2, " ")
        event.toolTip.add(indexOfCost + 3, "You have")
        event.toolTip.add(indexOfCost + 4, "§3${HotfData.whispersCurrent.addSeparators()} Forest Whispers")
    }

    private fun handleWhispersFor10Levels(event: ToolTipEvent, perk: HotfData) {
        if (!Keyboard.KEY_LSHIFT.isKeyHeld()) return

        val indexOfCost = event.toolTip.indexOfFirst { HotmData.perkCostPattern.matches(it) }

        if (indexOfCost == -1) return

        val whispersFor10Levels =
            perk.calculateTotalCost((perk.rawLevel + 10).coerceAtMost(perk.maxLevel)) - perk.calculateTotalCost(perk.rawLevel)

        val numberOfLevels = (perk.maxLevel - perk.rawLevel).coerceAtMost(10)
        val levelsFormat = StringUtils.pluralize(numberOfLevels, "level")

        event.toolTip.add(indexOfCost + 2, "§7Whispers for $numberOfLevels $levelsFormat §e${whispersFor10Levels.addSeparators()}")
    }

    private fun handleWhispersSpent(perk: HotfData): String {
        val currentWhispersSpent = perk.calculateTotalCost(perk.rawLevel)
        val maxWhispersNeeded = perk.totalCostMaxLevel
        val percentage = (currentWhispersSpent.fractionOf(maxWhispersNeeded) * 100).roundTo(2)

        return when (config.whispersSpentDesign) {
            WhispersSpentDesign.NUMBER -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Whispers spent: §e${maxWhispersNeeded.addSeparators()} §7(§aMax level§7)"
                } else {
                    "§7Whispers spent: §e${currentWhispersSpent.addSeparators()}§7 / §e${maxWhispersNeeded.addSeparators()}"
                }
            }

            WhispersSpentDesign.PERCENTAGE -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Whispers spent: §e$percentage% §7(§aMax level§7)"
                } else {
                    "§7Whispers spent: §e$percentage%§7 of max"
                }
            }

            WhispersSpentDesign.NUMBER_AND_PERCENTAGE -> {
                if (perk.rawLevel == perk.maxLevel) {
                    "§7Whispers spent: §e${maxWhispersNeeded.addSeparators()} §7(§aMax level§7)"
                } else {
                    "§7Whispers spent: " +
                        "§e${currentWhispersSpent.addSeparators()}§7/§e${maxWhispersNeeded.addSeparators()}§7 (§e$percentage%§7)"
                }
            }
        }
    }

    enum class WhispersSpentDesign(val str: String) {
        NUMBER("Number"),
        PERCENTAGE("Percentage"),
        NUMBER_AND_PERCENTAGE("Number and Percentage");

        override fun toString() = str
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HotfData.inInventory &&
        (config.whispersSpent || config.whispersFor10Levels || config.currentWhispers)
}
