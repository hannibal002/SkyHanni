package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.network.chat.Component

@SkyHanniModule
object StarlynSisterCouponProfit {

    private val config get() = SkyHanniMod.feature.foraging.starlynContest

    private var display = emptyList<Renderable>()

    // TODO replace with inventory detector
    private var currentSisterType: StarlynSisterType? = null

    @HandleEvent
    private fun onInventoryClose() {
        currentSisterType = null
        display = emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.starlynCouponProfitEnabled) return
        StarlynSisterType.entries.forEach { sisterType ->
            if (event.inventoryName == sisterType.inventoryName) currentSisterType = sisterType
        }
        if (currentSisterType == null) return

        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            try {
                readItem(slot, item)?.let {
                    table.add(it)
                }
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error in StarlynSisterCouponProfit while reading item '${item.repoItemName}'",
                    "item" to item,
                    "name" to item.repoItemName,
                    "inventory name" to event.inventoryName,
                )
            }
        }

        display = buildList {
            addString("§eProfit per Coupon")
            add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
        }
    }

    private fun readItem(slot: Int, item: SafeItemStack): DisplayTableEntry? {
        if (!isValidSlotNumber(slot)) return null
        val (internalName, itemName) = workOutInternalNameOrNull(item) ?: return null
        val requiredItems = item.readLoreCosts()
        val price = internalName.getPrice()
        var totalCost = 0.0
        var couponAmount = 0L
        for ((name, amount) in requiredItems) {
            val itemPrice = name.getPriceOrNull() ?: continue
            totalCost += itemPrice * amount
            if (name == currentSisterType?.couponName) {
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
                add(requiredName.getPriceName(amount))
            }
            add("")
            add("§7Profit per sell: §6${profit.shortFormat()}")
            if (couponAmount > 0) {
                add("§7Profit per coupon: §6${profitPerCoupon.shortFormat()}")
            }
        }

        return DisplayTableEntry(
            itemName,
            "§6${profitPerCoupon.shortFormat()}".asComponent(),
            profitPerCoupon,
            internalName,
            hover.mapToComponents(),
            highlightsOnHoverSlots = listOf(slot),
        )
    }

    // TODO merge logic into core item utils logic, I think
    private fun workOutInternalNameOrNull(item: SafeItemStack): Pair<NeuInternalName, Component>? {
        val isEnchantedBook = item.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
        return if (isEnchantedBook) {
            val internalName = item.getInternalNameOrNull() ?: return null
            internalName to item.repoItemName.asComponent()
        } else {
            val internalName = NeuInternalName.fromItemNameOrNull(item.hoverName.formattedTextCompatLeadingWhiteLessResets()) ?: return null
            internalName to item.hoverName
        }
    }

    private fun isValidSlotNumber(slot: Int): Boolean {
        if (slot !in 9..44) return false
        val modNine = slot % 9
        return modNine != 0 && modNine != 8
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChestGuiRender() {
        if (currentSisterType == null) return
        config.starlynCouponProfitPos.renderRenderables(
            display,
            extraSpace = 5,
            posLabel = "Starlyn Sister Coupon Profit",
        )
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(139, "foraging.starlynContest.agathaCouponProfitEnabled", "foraging.starlynContest.starlynCouponProfitEnabled")
        event.move(139, "foraging.starlynContest.agathaCouponProfitPos", "foraging.starlynContest.starlynCouponProfitPos")
    }
}
