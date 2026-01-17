package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import org.lwjgl.glfw.GLFW

abstract class CurrencyPerHotxPerk<HotxType : HotxHandler<*, *, *>>(private val hotx: HotxType, private val displayText: String) {

    fun handleHotxCurrency(
        event: ToolTipTextEvent,
        showCurrencySpent: Boolean,
        showCurrencyFor10Levels: Boolean,
        showCurrentCurrency: Boolean,
        currencySpentDesign: CurrencySpentDesign,
    ) {
        val itemName = event.itemStack.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val perk = hotx.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.getLevelUpCost() == null) return
        if (showCurrencySpent) event.toolTip.add(2, handleCurrencySpent(currencySpentDesign, perk))
        if (showCurrencyFor10Levels) handleCurrencyFor10Levels(event, perk)
        if (showCurrentCurrency) handleCurrentCurrency(event, perk)
    }

    private fun handleCurrentCurrency(event: ToolTipTextEvent, perk: HotxData<*>) {
        if (!perk.isUnlocked || perk.isMaxLevel) return
        val indexOfCost = event.toolTip.indexOfFirst { HotmData.perkCostPattern.matches(it) }
        if (indexOfCost == -1) return
        val currentCurrencyLine = currentCurrencyLineString(perk) ?: return
        event.toolTip.add(indexOfCost + 2, " ")
        event.toolTip.add(indexOfCost + 3, "§7You have")
        event.toolTip.add(indexOfCost + 4, currentCurrencyLine)
    }

    abstract fun currentCurrencyLineString(perk: HotxData<*>): String?

    private fun handleCurrencyFor10Levels(event: ToolTipTextEvent, perk: HotxData<*>) {
        if (!GLFW.GLFW_KEY_LEFT_SHIFT.isKeyHeld()) return
        val indexOfCost = event.toolTip.indexOfFirst { HotmData.perkCostPattern.matches(it) }
        if (indexOfCost == -1) return

        val currencyFor10Levels =
            perk.calculateTotalCost((perk.rawLevel + 10).coerceAtMost(perk.maxLevel)) - perk.calculateTotalCost(perk.rawLevel)
        val numberOfLevels = (perk.maxLevel - perk.rawLevel).coerceAtMost(10)
        val levelsFormat = StringUtils.pluralize(numberOfLevels, "level")
        event.toolTip.add(indexOfCost + 2, "§7$displayText for $numberOfLevels $levelsFormat §e${currencyFor10Levels.addSeparators()}")
    }

    private fun handleCurrencySpent(currencySpentDesign: CurrencySpentDesign, perk: HotxData<*>): String {
        val currentAmountSpent = perk.calculateTotalCost(perk.rawLevel)
        val maxCurrencyNeeded = perk.totalCostMaxLevel
        val percentage = (currentAmountSpent * 100.0 / maxCurrencyNeeded).roundTo(2)
        val isMaxLevel = perk.rawLevel == perk.maxLevel
        val label = "§7$displayText spent:"

        return if (isMaxLevel) {
            when (currencySpentDesign) {
                CurrencySpentDesign.NUMBER -> "$label §e${maxCurrencyNeeded.addSeparators()} §7(§aMax level§7)"
                CurrencySpentDesign.PERCENTAGE -> "$label §e$percentage% §7(§aMax level§7)"
                CurrencySpentDesign.NUMBER_AND_PERCENTAGE -> "$label §e${maxCurrencyNeeded.addSeparators()} §7(§aMax level§7)"
            }
        } else {
            when (currencySpentDesign) {
                CurrencySpentDesign.NUMBER -> "$label §e${currentAmountSpent.addSeparators()}§7 / §e${maxCurrencyNeeded.addSeparators()}"
                CurrencySpentDesign.PERCENTAGE -> "$label §e$percentage%§7 of max"
                CurrencySpentDesign.NUMBER_AND_PERCENTAGE ->
                    "$label §e${currentAmountSpent.addSeparators()}§7/§e${maxCurrencyNeeded.addSeparators()}§7 (§e$percentage%§7)"
            }
        }
    }

    enum class CurrencySpentDesign(val display: String) {
        NUMBER("Number"),
        PERCENTAGE("Percentage"),
        NUMBER_AND_PERCENTAGE("Number and Percentage");

        override fun toString() = display
    }

    open fun isEnabled(): Boolean = SkyBlockUtils.inSkyBlock && hotx.inInventory
}
