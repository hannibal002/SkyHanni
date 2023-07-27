package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addFormatting
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class AuctionHouseCompareValue {
    private var map = mapOf<Int, Long>()
    private var best = 0L
    private var worst = 0L
    private var inInventory = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inInventory = false
        if (!event.inventoryName.startsWith("Auctions")) return
        inInventory = true

        best = 0L
        worst = 0L

        val map = mutableMapOf<Int, Long>()

        for ((slot, stack) in event.inventoryItems) {
            val buyLine = stack.getLore().find { it.contains("Buy it now:") } ?: continue
//            val esitmatedPrice = EstimatedItemValue.getEstimatedItemPrice(stack) ?: continue
            val pair = EstimatedItemValue.getData(stack, mutableListOf())
            val (totalPrice, basePrice) = pair
            if (totalPrice == basePrice) continue
            val esitmatedPrice = totalPrice.toLong()


            val name = stack.name
            val binPrice = "§7Buy it now: §6(?<price>.*) coins".toPattern().matchMatcher(buyLine) {
                group("price").formatNumber()
            } ?: continue
            println(" ")
            println("name: $name")
            println("esitmatedPrice: ${esitmatedPrice.addSeparators()}")
            println("binPrice: ${binPrice.addSeparators()}")
            val diff = esitmatedPrice - binPrice
            map[slot] = diff
            if (diff >= 0) {
                if (diff > best) {
                    best = diff
                }
            } else {
                if (diff < worst) {
                    worst = diff
                }
            }
        }
        this.map = map

        println("map: ${map.values.map { it.addSeparators() }}}")
        println("best: ${best.addSeparators()}")
        println("worst: ${worst.addSeparators()}")
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inInventory) return

//        val good = LorenzColor.GREEN.toColor()
//        val good = LorenzColor.DARK_GREEN.toColor()
        val veryGood = LorenzColor.DARK_GREEN.toColor()
        val aBitGood = LorenzColor.GREEN.toColor()

        val aBitBad = LorenzColor.YELLOW.toColor()
//        val bad = LorenzColor.RED.toColor()
        val veryBad = LorenzColor.RED.toColor()


        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val diff = map[slot.slotIndex] ?: continue
            if (diff == 0L) {
                slot highlight aBitGood
                continue
            }
            val isGood = diff >= 0
            val percentage = if (isGood) {
                diff.toDouble() / best
            } else {
                -diff.toDouble() / -worst
            }
            val color = if (isGood) {
                getColorInBetween(aBitGood, veryGood, percentage)
//                neutral + (good - neutral * percentage)
            } else {
                getColorInBetween(aBitBad, veryBad, percentage)
//                neutral + (bad - neutral * percentage)
            }
            slot highlight color


//            if (diff >= 0) {
////                slot highlight LorenzColor.GREEN
//            } else {
//                slot highlight LorenzColor.RED
//            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inInventory) return

        val diff = map[event.slot.slotIndex] ?: return
        if (diff >= 0) {
            event.toolTip.add("§aThis item is §6${diff.addFormatting()} §aCHEAPER")
            event.toolTip.add("§athan the estimated item value!")
        } else {
            event.toolTip.add("§cThis item is §6${(-diff).addFormatting()} §cMORE EXPENSIVE")
            event.toolTip.add("§cthan the estimated item value!")
        }

    }

    fun getColorInBetween(color1: Color, color2: Color, percentage: Double): Color {
        val r1 = color1.red
        val g1 = color1.green
        val b1 = color1.blue

        val r2 = color2.red
        val g2 = color2.green
        val b2 = color2.blue

        val rDiff = r2 - r1
        val gDiff = g2 - g1
        val bDiff = b2 - b1

        val newRed = (r1 + rDiff * percentage).toInt()
        val newGreen = (g1 + gDiff * percentage).toInt()
        val newBlue = (b1 + bDiff * percentage).toInt()

        return Color(newRed, newGreen, newBlue)
    }
}
