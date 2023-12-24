package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ComposterInventoryNumbers {
    private val valuePattern = ".* §e(?<having>.*)§6/(?<total>.*)".toPattern()
    private val compostsPattern = "§7§7Compost Available: §a(?<amount>.*)".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.config.composters.inventoryNumbers) return

        if (event.inventoryName != "Composter") return

        val stack = event.stack

        val slotNumber = event.slot.slotNumber

        // Composts Available
        if (slotNumber == 13) {
            for (line in stack.getLore()) {
                compostsPattern.matchMatcher(line) {
                    val total = group("amount").replace(",", "").toInt()
                    event.offsetY = -2
                    event.offsetX = -20
                    event.stackTip = "§6${total.addSeparators()}"
                    return
                }
            }
        }

        // Organic Matter or Fuel
        if (slotNumber == 46 || slotNumber == 52) {
            for (line in stack.getLore()) {
                valuePattern.matchMatcher(line) {
                    val having = group("having").removeColor().replace(",", "").toDouble().toInt()
                    val havingFormat = NumberUtil.format(having)
                    val total = group("total").removeColor()


                    val color = if (slotNumber == 46) {
                        // Organic Matter
                        event.offsetY = -95
                        event.offsetX = 5
                        event.alignLeft = false
                        "§e"
                    } else {
                        // Fuel
                        event.offsetY = -41
                        event.offsetX = -20
                        "§a"
                    }

                    event.stackTip = "$color$havingFormat/$total"
                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.composterInventoryNumbers", "garden.composters.inventoryNumbers")
    }
}
