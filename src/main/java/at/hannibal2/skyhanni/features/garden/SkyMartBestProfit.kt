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
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*
import java.util.regex.Pattern

class SkyMartBestProfit {

    private val pattern = Pattern.compile("§c(.*) Copper")
    private val display = mutableListOf<List<Any>>()
    private val config get() = SkyHanniMod.feature.garden

    @SubscribeEvent
    fun onChatPacket(event: InventoryOpenEvent) {
        if (!isEnabled()) return

        val inventory = event.inventory
        if (inventory.title != "SkyMart") return

        val priceMap = mutableMapOf<Pair<String, String>, Double>()
        val iconMap = mutableMapOf<String, ItemStack>()

        for (stack in inventory.items.values) {
            for (line in stack.getLore()) {
                val matcher = pattern.matcher(line)
                if (!matcher.matches()) continue

                val internalName = stack.getInternalName()
                val lowestBin = NEUItems.getPrice(internalName)
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

                iconMap[name] = NEUItems.getItemStack(internalName)

                val advancedStats = if (config.skyMartCopperPriceAdvancedStats) {
                    " §f(§6$priceFormat §f/ §c$amountFormat Copper§f)"
                } else ""
                val pair = Pair(name, "§6§l$perFormat$advancedStats")
                priceMap[pair] = factor
            }
        }

        display.clear()

        display.add(Collections.singletonList("Coins per §cCopper§f:"))
        display.add(Collections.singletonList(""))

        val keys = priceMap.sortedDesc().keys
        val renderer = Minecraft.getMinecraft().fontRendererObj
        val longest = keys.map { it.first }.maxOfOrNull { renderer.getStringWidth(it.removeColor()) } ?: 0

        for ((name, second) in keys) {
            val itemStack = iconMap[name]!!
            var displayName = "$name§f:"
            while (renderer.getStringWidth(displayName.removeColor()) < longest) {
                displayName += " "
            }
            display.add(listOf(itemStack, "$displayName   $second"))
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        display.clear()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (isEnabled()) {
            config.skyMartCopperPricePos.renderStringsAndItems(display)
        }
    }

    private fun isEnabled() =
        LorenzUtils.inSkyBlock && config.skyMartCopperPrice && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}