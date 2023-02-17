package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class SkyMartBestProfit {

    private val display = mutableListOf<String>()

    @SubscribeEvent
    fun onChatPacket(event: InventoryOpenEvent) {
        if (!isEnabled()) return

        val inventory = event.inventory
        if (inventory.title != "SkyMart") return

        val pattern = Pattern.compile("§c(.*) Copper")
        val priceMap = mutableMapOf<Pair<String, String>, Double>()

        val auctionManager = NotEnoughUpdates.INSTANCE.manager.auctionManager
        for (stack in inventory.items.values) {
            for (line in stack.getLore()) {
                val matcher = pattern.matcher(line)
                if (!matcher.matches()) continue

                val internalName = stack.getInternalName()
                val lowestBin = auctionManager.getBazaarOrBin(internalName, false)
                if (lowestBin == -1.0) continue

                val amount = matcher.group(1).replace(",", "").toInt()
                val factor = lowestBin / amount
                val perFormat = NumberUtil.format(factor)
                val priceFormat = NumberUtil.format(lowestBin)
                val amountFormat = NumberUtil.format(amount)

                var name = stack.name!!
                if (name == "§fEnchanted Book") {
                    name = "§9Sunder I"
                }

                val pair = Pair("$name§f:", "§6§l$perFormat §f(§6$priceFormat §f/ §c$amountFormat copper§f)")
                priceMap[pair] = factor
            }
        }

        display.clear()

        display.add("Coins per §ccopper§f:")
        display.add(" ")

        val keys = priceMap.sortedDesc().keys
        val renderer = Minecraft.getMinecraft().fontRendererObj
        val longest = keys.map { it.first }.maxOfOrNull { renderer.getStringWidth(it.removeColor()) } ?: 0

        for ((first, second) in keys) {
            var name = first
            while (renderer.getStringWidth(name.removeColor()) < longest) {
                name += " "
            }
            display.add("$name   $second")
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display.clear()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (isEnabled()) {
            SkyHanniMod.feature.garden.skyMartCopperPricePos.renderStrings(display)
        }
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock &&
                SkyHanniMod.feature.garden.skyMartCopperPrice &&
                LorenzUtils.skyBlockIsland == IslandType.GARDEN
}