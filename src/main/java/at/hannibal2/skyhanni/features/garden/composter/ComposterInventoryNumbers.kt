package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ComposterInventoryNumbers {
    private val valuePattern = Pattern.compile("(?:.*) §e(.*)§6\\/(.*)")
    private val compostsPattern = Pattern.compile("§7§7Compost Available: §a(.*)")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.composterInventoryNumbers) return

        if (event.inventoryName != "Composter") return

        val stack = event.stack

        val slotNumber = event.slot.slotNumber

        // Composts Available
        if (slotNumber == 22) {
            for (line in stack.getLore()) {
                val matcher = compostsPattern.matcher(line)
                if (!matcher.matches()) continue

                val total = matcher.group(1).replace(",", "").toInt()
                if (total <= 64) continue

                event.offsetY = -2
                event.offsetX = -20
                event.stackTip = "§6$total"
                return
            }
        }

        // Organic Matter or Fuel
        if (slotNumber == 46 || slotNumber == 52) {
            for (line in stack.getLore()) {
                val matcher = valuePattern.matcher(line)
                if (!matcher.matches()) continue

                val having = matcher.group(1).removeColor().replace(",", "").toDouble().toInt()
                val havingFormat = NumberUtil.format(having)
                val total = matcher.group(2).removeColor()


                val color = if (slotNumber == 46) {
                    // Organic Matter
                    event.offsetY = -95
                    event.offsetX = 5
                    event.alignLeft = false
                    "§e"
                } else {
                    // Fuel
                    event.offsetY = -76
                    event.offsetX = -20
                    "§a"
                }

                event.stackTip = "$color$havingFormat/$total"
                return
            }
        }
    }
}