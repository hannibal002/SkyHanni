package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.util.stripControlCodes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object FannCost {

    private val config get() = SkyHanniMod.feature.inventory.fannCost
    private val showCoins get() = config.coinsPerXP
    private val showBits get() = config.xpPerBit
    private var isDayTrainingMode = true
    private val patternGroup = RepoPattern.group("fann.inventory")
    private val trainingSlotInventoryPattern by patternGroup.pattern(
        "training",
        "Training Slot [1-3]",
    )
    private val anvilPattern by patternGroup.pattern(
        "anvil",
        "§aBegin Training",
    )
    private val expEarnedPattern by patternGroup.pattern(
        "training.exp.earned",
        """Will earn a total of (\d{1,3}(,\d{3})*(\.\d+)?|\d+(\.\d+)?) EXP\.?""",
    )

    @SubscribeEvent
    fun onFannAnvilTooltip(event: LorenzToolTipEvent) {
        if (!trainingSlotInventoryPattern.matches(InventoryUtils.openInventoryName())) return
        if (!anvilPattern.matches(event.itemStack.displayName)) return
        val tooltip = event.toolTip
        for (line in tooltip) {
            val linePlain = line.stripControlCodes()

        }
    }

    private fun readExpEarned(lore: List<String>): Double {
        for (line in lore) {
            val linePlain = line.stripControlCodes()
            if (expEarnedPattern.matches(linePlain)) {
                return expEarnedPattern.matcher(linePlain).group(1).replace(",", "").toDouble()
            }
        }
        return 0.0
    }

    private fun readTrainingMode(lore: List<String>) {
        var i = 0
        val size = lore.size
        while (i < size) {
            val line = lore[i]
            if (i + 1 < size) {
                val nextLine = lore[i + 1]
                isDayTrainingMode = line.plainContains("Duration:") && nextLine.plainContains("Reaches:")
                return
            }
            i++
        }
    }

    private fun String.plainContains(other: String): Boolean {
        return stripControlCodes().contains(other)
    }
}
