package at.hannibal2.hanni.features.inventory.chocolatefactory.hitman

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.features.event.hoppity.HoppityApi.hitmanInventoryPattern
import at.hannibal2.hanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import net.minecraft.item.ItemStack

@HanniModule
object HitmanSlots {

    /**
     * REGEX-TEST: §7Hitman can store more eggs you miss! §7Cost §620,000,000 Coins §eClick to purchase!
     */
    private val slotCostPattern by CFApi.patternGroup.pattern(
        "hitman.slotcost",
        ".*§7Cost §6(?<cost>[\\d,]+) Coins.*",
    )

    private val config get() = CFApi.config
    private var slotPricesPaid: List<Long> = emptyList()
    private var slotPricesLeft: List<Long> = emptyList()
    private var inInventory = false

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = hitmanInventoryPattern.matches(event.inventoryName)
        if (!inInventory) return
        handleSlotStorageUpdate(event)
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.hitmanCosts || slotPricesLeft.isEmpty()) return
        if (!inInventory) return
        config.hitmanCostsPosition.renderRenderable(
            getSlotPriceRenderable(),
            posLabel = "Hitman Slot Costs",
        )
    }

    private fun handleSlotStorageUpdate(event: InventoryOpenEvent) {
        if (!config.hitmanCosts) return
        val leftToPurchase = event.inventoryItems.filterNotBorderSlots().count { (_, item) ->
            item.displayName.isNotEmpty() && item.getLore().isNotEmpty() &&
                slotCostPattern.matches(item.getSingleLineLore())
        }
        val ownedSlots = CFApi.hitmanCosts.size - leftToPurchase

        slotPricesPaid = CFApi.hitmanCosts.take(ownedSlots)
        slotPricesLeft = CFApi.hitmanCosts.drop(ownedSlots)
    }

    private fun Map<Int, ItemStack>.filterNotBorderSlots() = filterKeys {
        it !in 0..8 && it !in 45..53 && // Horizontal borders
            it % 9 != 0 && (it + 1) % 9 != 0 // Vertical borders
    }

    private fun getSlotPriceRenderable(): Renderable = Renderable.vertical {
        addString("§eHitman Slot Progress")

        if (slotPricesPaid.isNotEmpty()) {
            add(
                Renderable.hoverTips(
                    "§aPurchased Slots§7: §a${slotPricesPaid.size}",
                    listOf("§7Total Paid: §6${slotPricesPaid.sum().addSeparators()} Coins"),
                ),
            )
        }

        val remainingSlotsText = buildList {
            add("§7Total Remaining: §6${slotPricesLeft.sum().addSeparators()} Coins")
            slotPricesLeft.take(5).forEachIndexed { index, price ->
                add("§7Slot ${slotPricesPaid.size + index + 1}: §6${price.addSeparators()} Coins")
            }
            if (slotPricesLeft.size > 5) {
                add("§8... and ${slotPricesLeft.size - 5} more")
            }
        }

        add(
            Renderable.hoverTips(
                "§cRemaining Slots§7: §c${slotPricesLeft.size}",
                remainingSlotsText,
            ),
        )
    }

}
