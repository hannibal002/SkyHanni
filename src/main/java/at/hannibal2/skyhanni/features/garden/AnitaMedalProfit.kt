package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AnitaMedalProfit {
    private val config get() = SkyHanniMod.feature.garden.anitaShop
    private var display = emptyList<List<Any>>()

    companion object {
        var inInventory = false
    }

    enum class MedalType(val displayName: String, val factorBronze: Int) {
        GOLD("§6Gold medal", 8),
        SILVER("§fSilver medal", 2),
        BRONZE("§cBronze medal", 1),
        ;
    }

    private fun getMedal(name: String) = MedalType.entries.firstOrNull { it.displayName == name }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.medalProfitEnabled) return
        if (event.inventoryName != "Anita") return
        if (VisitorAPI.inInventory) return

        inInventory = true

        val table = mutableMapOf<Pair<String, String>, Pair<Double, NEUInternalName>>()
        for ((_, item) in event.inventoryItems) {
            try {
                readItem(item, table)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error in AnitaMedalProfit while reading item '${item.nameWithEnchantment}'",
                    "item" to item,
                    "name" to item.nameWithEnchantment,
                    "inventory name" to InventoryUtils.openInventoryName(),
                )
            }
        }

        val newList = mutableListOf<List<Any>>()
        newList.addAsSingletonList("§eMedal Profit")
        LorenzUtils.fillTable(newList, table)
        display = newList
    }

    private fun readItem(item: ItemStack, table: MutableMap<Pair<String, String>, Pair<Double, NEUInternalName>>) {
        val itemName = item.nameWithEnchantment ?: return
        if (itemName == " ") return
        if (itemName == "§cClose") return
        if (itemName == "§eUnique Gold Medals") return
        if (itemName == "§aMedal Trades") return

        val fullCost = getFullCost(getRequiredItems(item))
        if (fullCost < 0) return

        val (name, amount) = ItemUtils.readItemAmount(itemName) ?: return

        var internalName = NEUItems.getInternalNameOrNull(name)
        if (internalName == null) {
            internalName = item.getInternalName()
        }

        val itemPrice = internalName.getPrice() * amount
        if (itemPrice < 0) return

        val profit = itemPrice - fullCost
        val format = NumberUtil.format(profit)
        val color = if (profit > 0) "§6" else "§c"
        table[Pair(itemName, "$color$format")] = Pair(profit, internalName)
    }

    private fun getFullCost(requiredItems: MutableList<String>): Double {
        val jacobTicketPrice = "JACOBS_TICKET".asInternalName().getPrice()
        var otherItemsPrice = 0.0
        for (rawItemName in requiredItems) {
            val pair = ItemUtils.readItemAmount(rawItemName)
            if (pair == null) {
                LorenzUtils.error("Could not read item '$rawItemName'")
                continue
            }

            val (name, amount) = pair
            val medal = getMedal(name)
            otherItemsPrice += if (medal != null) {
                val bronze = medal.factorBronze * amount
                bronze * jacobTicketPrice
            } else {
                NEUInternalName.fromItemName(name).getPrice() * amount
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
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (inInventory) {
            config.medalProfitPos.renderStringsAndItems(
                display,
                extraSpace = 5,
                itemScale = 1.7,
                posLabel = "Anita Medal Profit"
            )
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.anitaMedalProfitEnabled", "garden.anitaShop.medalProfitEnabled")
        event.move(3, "garden.anitaMedalProfitPos", "garden.anitaShop.medalProfitPos")
    }
}
