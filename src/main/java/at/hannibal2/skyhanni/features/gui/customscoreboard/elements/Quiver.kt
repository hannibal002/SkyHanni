package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.ArrowConfig.ArrowAmountDisplay
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.NONE_ARROW_TYPE
import at.hannibal2.skyhanni.data.QuiverAPI.asArrowPercentage
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.arrowConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.percentageColor

object Quiver : ScoreboardElement() {
    override fun getDisplay(): List<Any> {
        if (QuiverAPI.currentArrow == null)
            return listOf("§cChange your Arrow once")
        if (QuiverAPI.currentArrow == NONE_ARROW_TYPE)
            return listOf("No Arrows selected")

        val amountString = (
            if (arrowConfig.colorArrowAmount) {
                percentageColor(
                    QuiverAPI.currentAmount.toLong(),
                    QuiverAPI.MAX_ARROW_AMOUNT.toLong(),
                ).getChatColor()
            } else "") +
            if (QuiverAPI.wearingSkeletonMasterChestplate) "∞"
            else {
                when (arrowConfig.arrowAmountDisplay) {
                    ArrowAmountDisplay.NUMBER -> QuiverAPI.currentAmount.addSeparators()
                    ArrowAmountDisplay.PERCENTAGE -> "${QuiverAPI.currentAmount.asArrowPercentage()}%"
                    else -> QuiverAPI.currentAmount.addSeparators()
                }
            }

        return listOf(
            if (displayConfig.displayNumbersFirst) {
                "$amountString ${QuiverAPI.currentArrow?.arrow}s"
            } else {
                "Arrows: $amountString ${QuiverAPI.currentArrow?.arrow?.replace(" Arrow", "")}"
            },
        )
    }

    override fun showWhen() = !(informationFilteringConfig.hideIrrelevantLines && !QuiverAPI.hasBowInInventory())

    override val configLine = "Flint Arrow: §f1,234"

    override fun showIsland() = !RiftAPI.inRift()
}
