package at.hannibal2.skyhanni.features.inventory.chocolatefactory.hitman

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.hitmanInventoryPattern
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.toSingleLineLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical

@SkyHanniModule
object HitmanSlots {

    /**
     * REGEX-TEST: Hitman can store more eggs you miss! Cost 20,000,000 Coins Click to purchase!
     */
    private val slotCostPattern by CFApi.patternGroup.pattern(
        "hitman.slotcost.colorless",
        ".*Cost (?<cost>[\\d,]+) Coins.*",
    )

    private val config get() = CFApi.config
    private var slotPricesPaid: List<Long> = emptyList()
    private var slotPricesLeft: List<Long> = emptyList()
    private var inInventory = false

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = hitmanInventoryPattern.matches(event.inventoryName)
        if (!inInventory) return
        handleSlotStorageUpdate(event)
    }

    @HandleEvent
    private fun onChestGuiRender() {
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
            val lore = item.getCleanLore()
            item.hoverName.formattedTextCompatLeadingWhiteLessResets().isNotEmpty() && lore.isNotEmpty() &&
                slotCostPattern.matches(lore.toSingleLineLore())
        }
        val ownedSlots = CFApi.hitmanCosts.size - leftToPurchase

        slotPricesPaid = CFApi.hitmanCosts.take(ownedSlots)
        slotPricesLeft = CFApi.hitmanCosts.drop(ownedSlots)
    }

    private fun Map<Int, SafeItemStack>.filterNotBorderSlots() = filterKeys {
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
