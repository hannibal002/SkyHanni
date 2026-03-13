package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.features.garden.greenhouse.GrowthCycle.patternGroup
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector

@SkyHanniModule
object GreenhouseUtils {

    /**
     * @regexTest Crop Diagnostics
     */
    val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Crop Diagnostics",
    )

    val cropDiagnosticInventory = InventoryDetector(inventoryPattern)
}
