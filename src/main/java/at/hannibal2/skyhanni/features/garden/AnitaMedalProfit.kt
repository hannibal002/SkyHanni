package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AnitaMedalProfit {
    private val config get() = SkyHanniMod.feature.garden
    private var display = listOf<List<Any>>()

    companion object {
        var inInventory = false
    }

    enum class MedalType(val displayName: String, val factorBronze: Int) {
        GOLD("§6Gold medal", 8),
        SILVER("§fSilver medal", 2),
        BRONZE("§cBronze medal", 1),
        ;
    }

    private fun getMedal(name: String) = MedalType.values().firstOrNull { it.displayName == name }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.anitaMedalProfitEnabled) return
        if (event.inventoryName != "Anita") return

        inInventory = true

        val table = mutableMapOf<Pair<String, String>, Pair<Double, String>>()
        for ((_, item) in event.inventoryItems) {
            readItem(item, table)
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§eMedal Profit")
        LorenzUtils.fillTable(newList, table)
        display = newList
    }

    private fun readItem(item: ItemStack, table: MutableMap<Pair<String, String>, Pair<Double, String>>) {
        var itemName = item.name ?: return
        if (itemName == " ") return
        if (itemName.endsWith("Enchanted Book")) {
            itemName = item.getLore()[0]
        }

        val fullCost = getFullCost(getRequiredItems(item))
        if (fullCost < 0) return

        val (name, amount) = ItemUtils.readItemAmount(itemName)
        if (name == null) return

        val internalName = try {
            NEUItems.getInternalName(name)
        } catch (e: Exception) {
            // TODO make a better alternative
            item.getInternalName()
        }

        val itemPrice = NEUItems.getPrice(internalName) * amount
        if (itemPrice < 0) return

        val profit = itemPrice - fullCost
        val format = NumberUtil.format(profit)
        val color = if (profit > 0) "§6" else "§c"
        table[Pair(itemName, "$color$format")] = Pair(profit, internalName)
    }

    private fun getFullCost(requiredItems: MutableList<String>): Double {
        val jacobTicketPrice = NEUItems.getPrice("JACOBS_TICKET")
        var otherItemsPrice = 0.0
        for (rawItemName in requiredItems) {
            val (name, amount) = ItemUtils.readItemAmount(rawItemName)
            if (name == null) {
                LorenzUtils.error("§c[SkyHanni] Could not read item '$rawItemName'")
                continue
            }

            val medal = getMedal(name)
            otherItemsPrice += if (medal != null) {
                val bronze = medal.factorBronze * amount
                bronze * jacobTicketPrice
            } else {
                val internalName = NEUItems.getInternalName(name)
                NEUItems.getPrice(internalName) * amount
            }
        }
        return otherItemsPrice
    }

    private fun getRequiredItems(item: ItemStack): MutableList<String> {
        val items = mutableListOf<String>()
        var next = false
        for (line in item.getLore()) {
            if (line == "§7Cost") {
                next = true
                continue
            }
            if (next) {
                if (line == "") {
                    next = false
                    continue
                }

                items.add(line.replace("§8 ", " §8"))
            }
        }
        return items
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.anitaMedalProfitPos.renderStringsAndItems(
                display,
                extraSpace = 5,
                itemScale = 1.7,
                posLabel = "Anita Medal Profit"
            )
        }
    }
}