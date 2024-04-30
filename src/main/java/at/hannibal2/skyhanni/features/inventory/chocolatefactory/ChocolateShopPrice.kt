package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.million
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChocolateShopPrice {
    private val config get() = ChocolateFactoryAPI.config.chocolateShopPrice

    private var display = emptyList<Renderable>()

    private val menuNamePattern by RepoPattern.pattern(
        "chocolatefactory.inventory.title",
        "^Chocolate Shop$"
    )

    companion object {
        var inInventory = false
    }

    private fun ItemStack.loreCosts(): MutableList<NEUInternalName> {
        var found = false
        val list = mutableListOf<NEUInternalName>()
        for (lines in getLore()) {
            if (lines == "§7Cost") {
                found = true
                continue
            }

            if (!found) continue
            if (lines.isEmpty()) return list

            NEUInternalName.fromItemNameOrNull(lines)?.let {
                list.add(it)
            }
        }
        return list
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!menuNamePattern.matches(event.inventoryName)) return

        val multiplier = 1.million

        inInventory = true
        // TODO merge core with SkyMartCopperPrice into a utils
        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            val lore = item.getLore()
            val chocolate = ChocolateFactoryAPI.getChocolateBuyCost(lore) ?: continue
            val internalName = item.getInternalName()
            val itemPrice = internalName.getPriceOrNull() ?: continue

            val profit = itemPrice

            val factor = (profit / chocolate) * multiplier
            val perFormat = NumberUtil.format(factor)

            val itemName = item.itemName
            val hover = buildList {
                add(itemName)

                add("")
                add("§7Item price: §6${NumberUtil.format(itemPrice)} ")
                add("§7Chocolate amount: §c${NumberUtil.format(chocolate)} ")

                add("")
                add("§7Profit per million chocolate: §6${perFormat} ")
            }
            table.add(
                DisplayTableEntry(
                    "$itemName§f:",
                    "§6§l$perFormat",
                    factor,
                    internalName,
                    hover,
                    highlightsOnHoverSlots = listOf(slot)
                )
            )
        }

        val newList = mutableListOf<Renderable>()
        newList.add(Renderable.string("§eCoins per million chocolate§f:"))
        newList.add(Renderable.string("§eCurrent Chocolate: §6${ChocolateAmount.CURRENT.formatted}"))
        newList.add(LorenzUtils.fillTable(table, padding = 5, itemScale = config.itemScale))
        display = newList
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (inInventory) {
            config.position.renderRenderables(
                display,
                extraSpace = 5,
                posLabel = "Chocolate Shop Price"
            )
        }
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.enabled
}
