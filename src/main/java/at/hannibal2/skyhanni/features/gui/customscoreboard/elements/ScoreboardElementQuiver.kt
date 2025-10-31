package at.hannibal2.hanni.features.gui.customscoreboard.elements

import at.hannibal2.hanni.config.features.gui.customscoreboard.ArrowConfig.ArrowAmountDisplay
import at.hannibal2.hanni.data.QuiverApi
import at.hannibal2.hanni.data.QuiverApi.NONE_ARROW_TYPE
import at.hannibal2.hanni.data.QuiverApi.asArrowPercentage
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.percentageColor

// internal and item in hand
// quiver update event and item in hand event
object ScoreboardElementQuiver : ScoreboardElement() {
    private val config get() = CustomScoreboard.displayConfig.arrow

    override fun getDisplay(): String {
        val currentArrow = QuiverApi.currentArrow ?: return "§cChange your Arrow once"
        if (currentArrow == NONE_ARROW_TYPE) return "No Arrows selected"

        val colorPrefix = when (config.colorArrowAmount) {
            true -> percentageColor(QuiverApi.currentAmount.toLong(), QuiverApi.MAX_ARROW_AMOUNT.toLong()).getChatColor()
            false -> ""
        }

        val amountDisplay = when {
            QuiverApi.wearingSkeletonMasterChestplate -> "∞"
            config.arrowAmountDisplay == ArrowAmountDisplay.PERCENTAGE -> "${QuiverApi.currentAmount.asArrowPercentage()}%"
            else -> QuiverApi.currentAmount.addSeparators()
        }

        val amountString = colorPrefix + amountDisplay

        return CustomScoreboardUtils.formatNumberDisplay(
            currentArrow.arrow,
            amountString,
            "§f",
        )
    }

    override fun showWhen() = !(CustomScoreboard.informationFilteringConfig.hideIrrelevantLines && !QuiverApi.hasBowInInventory())

    override val configLine = "Flint Arrow: §f1,234"

    override fun showIsland() = !RiftApi.inRift()
}

// click: open /quiver
