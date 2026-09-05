package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import java.util.regex.Pattern

/**
 * Adds a "Total: X" line (coloured by highest rarity obtained) to a trophy item's tooltip inside its
 * NPC menu, which Hypixel omits. Shared by Trophy Fish (Odger) and Trophy Frogs (Researcher Ribery).
 */
abstract class TrophyTotalCaught {

    protected abstract val discoveredPattern: Pattern
    protected abstract val bronzePattern: Pattern

    protected abstract fun isInInventory(): Boolean
    protected abstract fun isEnabled(): Boolean
    protected abstract fun countsFor(cleanName: String): Map<TrophyRarity, Int>?

    protected fun handleToolTip(event: ToolTipTextEvent) {
        if (!isInInventory()) return
        if (!isEnabled()) return

        if (event.toolTip.none { discoveredPattern.matcher(it.string).find() }) return

        val counts = countsFor(event.itemStack.cleanName) ?: return
        val bestObtained = counts.filter { it.value > 0 }.keys.maxOrNull() ?: TrophyRarity.BRONZE
        val bronzeLineIndex = event.toolTip.indexOfFirst { bronzePattern.matcher(it.string).find() }

        if (bronzeLineIndex > 0) {
            event.toolTip.add(bronzeLineIndex + 1, "")
            event.toolTip.add(
                bronzeLineIndex + 2,
                "§7Total: ${bestObtained.formatCode}${counts.values.sum().addSeparators()}",
            )
        }
    }
}
