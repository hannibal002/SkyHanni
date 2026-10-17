package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.garden.greenhouse.GrowthCycle.patternGroup
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object GreenhouseUtils {

    /**
     * REGEX-TEST: Crop Diagnostics
     */
    val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Crop Diagnostics",
    )

    val cropDiagnosticInventory = InventoryDetector { inventoryPattern }

    fun isInGreenhouse(): Boolean = scoreboardShowsGreenhouse() || GardenPlotApi.inGreenhouse()

    fun scoreboardShowsGreenhouse(): Boolean =
        SkyBlockUtils.scoreboardArea == "Greenhouse" ||
            ScoreboardData.sidebarLinesFormatted.any {
                it.removeColor().contains("Greenhouse", ignoreCase = true)
            }
}
