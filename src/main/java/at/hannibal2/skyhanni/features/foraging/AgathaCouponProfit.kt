package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sublistAfter
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.item.ItemStack

@SkyHanniModule
object AgathaCouponProfit {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest

    private var display = emptyList<Renderable>()

    // TODO replace with inventory detector
    private var inInventory = false
    private val AGATHA_COUPON = "AGATHA_COUPON".toInternalName()

    @HandleEvent
    fun onInventoryClose() {
        inInventory = false
        display = emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.agathaCouponProfitEnabled) return
        if (event.inventoryName != "Agatha's Shop") return
        inInventory = true

        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            try {
                readItem(slot, item)?.let {
                    table.add(it)
                }
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error in AgathaCouponProfit while reading item '${item.repoItemName}'",
                    "item" to item,
                    "name" to item.repoItemName,
                    "inventory name" to event.inventoryName,
                )
            }
        }

        display = buildList {
            addString("§eProfit per Agatha Coupon")
            add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
        }
    }

    private fun readItem(slot: Int, item: ItemStack): DisplayTableEntry? {
        if (!isValidSlotNumber(slot)) return null
        val (internalName, itemName) = workOutInternalNameOrNull(item) ?: return null
        val requiredItems = getRequiredItems(item)
        val price = internalName.getPrice()
        var totalCost = 0.0
        var couponAmount = 0
        for ((name, amount) in requiredItems) {
            val itemPrice = name.getPriceOrNull() ?: continue
            totalCost += itemPrice * amount
            if (name == AGATHA_COUPON) {
                couponAmount = amount
            }
        }
        val profit = price - totalCost
        val profitPerCoupon = if (couponAmount > 0) profit / couponAmount else 0.0

        val hover = buildList {
            add(itemName)
            add("")
            add("§7Sell price: §6${price.shortFormat()}")
            add("§7Total cost: §6${totalCost.shortFormat()}")
            for ((requiredName, amount) in requiredItems) {
                val itemPrice = requiredName.getPriceOrNull()?.times(amount) ?: continue
                add(" §8x$amount ${requiredName.repoItemName}: §7(§6${itemPrice.shortFormat()}§7)")
            }
            add("")
            add("§7Profit per sell: §6${profit.shortFormat()}")
            if (couponAmount > 0) {
                add("§7Profit per coupon: §6${profitPerCoupon.shortFormat()}")
            }
        }

        return DisplayTableEntry(
            itemName,
            "§6${profitPerCoupon.shortFormat()}",
            profitPerCoupon,
            internalName,
            hover,
            highlightsOnHoverSlots = listOf(slot),
        )
    }

    // TODO merge logic into core item utils logic, i think
    private fun workOutInternalNameOrNull(item: ItemStack): Pair<NeuInternalName, String>? {
        val isEnchantedBook = item.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
        return if (isEnchantedBook) {
            val internalName = item.getInternalNameOrNull() ?: return null
            internalName to item.repoItemName
        } else {
            val internalName = NeuInternalName.fromItemNameOrNull(item.displayName) ?: return null
            internalName to item.displayName
        }
    }

    private fun getRequiredItems(item: ItemStack): MutableMap<NeuInternalName, Int> {
        val items = mutableMapOf<NeuInternalName, Int>()

        val lore = item.getLore()
        val costLines = lore
            .sublistAfter({ it == "§7Cost" }, skip = 1, amount = lore.size)
            .takeWhile { it.isNotEmpty() }

        for (line in costLines) {
            val rawItemName = line.replace("§8 ", " §8")

            val originalPair = ItemUtils.readItemAmount(rawItemName)
            if (originalPair == null) {
                ErrorManager.logErrorStateWithData(
                    "Error in AnitaCoupon Profit", "Could not read item amount",
                    "rawItemName" to rawItemName,
                    "name" to item.displayName,
                    "lore" to lore,
                )
                continue
            }
            val newPair = NeuInternalName.fromItemName(originalPair.first) to originalPair.second
            items.add(newPair)
        }
        return items
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot < 9 || slot > 44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!inInventory) return
        config.agathaCouponProfitPos.renderRenderables(
            display,
            extraSpace = 5,
            posLabel = "Anita Medal Profit",
        )
    }
}
