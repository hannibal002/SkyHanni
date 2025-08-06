package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemConvertible
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

@SkyHanniModule
object FusionProfitOverlay {

    val config get() = SkyHanniMod.feature.inventory.fusionProfitOverlay

    private var display = emptyList<Renderable>()

    /**
     * REGEX-TEST: §7Required to fuse: §b5
     */
    private val loreCostPattern by RepoPattern.pattern(
        "fusion.cost.amount",
        "§7Required to fuse: §b(?<cost>\\d+)",
    )

    /**
     * REGEX-TEST: §7Are you sure you want to combine §ax2 §a§7U11 §aMossybit §7and §ax5 §7E33 §5Ghost §5§7shards together?  §7You will obtain §ax2 §aRana§7.  §7This is a §dspecial §7fusion recipe! §ax2 §aShards§7!  §eClick to fuse!
     * REGEX-TEST: §7Are you sure you want to combine §ax2 §a§7U11 §aMossybit §7and §ax5 §7R10 §9Invisibug §9§7shards together?  §7You will obtain §ax2 §9Toad§7.  §7This is a §dspecial §7fusion recipe! §ax2 §aShards§7!  §eClick to fuse!
     */
    private val fusionLorePattern by RepoPattern.pattern(
        "fusion.lore",
        "§7Are you sure you want to combine §ax(?<amount1>\\d+) §a§7\\w\\d+ (?<shard1>§[^§]*) §7and §ax(?<amount2>\\d+) §7\\w\\d+ (?<shard2>§[^§]*) §.§7shards together\\?  §7You will obtain §ax(?<resultAmount>\\d+) (?<resultName>§[^§]*)§7\\."
    )

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.enabled) return
        if (event.inventoryName == "Shard Fusion") {
            handleShardFusionGui(event.inventoryItems)
        } else if (event.inventoryName == "Confirm Fusion") {
            handleConfirmFusionGui(event.inventoryItems)
        }
    }

    @HandleEvent
    fun onInventoryChange(event: InventoryUpdatedEvent) {
        if (!config.enabled) return
        if (event.inventoryName == "Shard Fusion") {
            if (!event.fullyOpenedOnce) return

            handleShardFusionGui(event.inventoryItems)
        } else if (event.inventoryName == "Confirm Fusion") {
            // Fusion slot: 33
            // Create ItemStacks from the lore of the confirm fusion item (its the easiest as everything is given in the lore)
            if (!event.fullyOpenedOnce) return

            handleConfirmFusionGui(event.inventoryItems)
        }
    }

    private fun buildDisplay(costs: MutableList<ItemStack>, results: MutableList<ItemStack>) {
        val costTips: MutableList<String> = mutableListOf()
        var totalCosts: Double = 0.0

        display = buildList {
            addString("§6§lFusion Profit Overlay")

            costs.forEach { item ->
                val amount = loreCostPattern.firstMatcher(item.getLore()) {
                    group("cost").toDoubleOrNull()
                }
                val internalName = item.getInternalNameOrNull() ?: return@forEach
                val price = internalName.getPrice() * (amount ?: 0.0)
                totalCosts += price
                costTips.add("§7${amount?.shortFormat()}x ${internalName.repoItemName}§7: §6${price.shortFormat()} coins")
            }
            addString("§7Cost: §c${totalCosts.shortFormat()}", tips = costTips)

            results.forEach { item ->
                // Im too stupid for regex so ill leave it like this for now :3
                val shardName = item.name.formattedTextCompatLeadingWhiteLessResets()
                    .replace(" §d§lNEW SHARD", "")
                    .removeColor()
                    .trim()

                if (shardName.isEmpty() || shardName == "Result Fusion") {
                    return@forEach
                }

                val internalName = NeuInternalName.fromItemNameOrNull("$shardName Shard")

                val pricePer = internalName?.getPrice() ?: 0.0
                val price = pricePer.times(item.count)

                val profit = price - totalCosts
                val profitString = if (profit > 0) "§6${profit.shortFormat()} coins" else "§c${profit.shortFormat()} coins"

                addString(
                    "§7Result: ${item.count}x §e${internalName?.repoItemName}§7: $profitString"
                )
            }
        }
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GALATEA)
    fun onRenderOverlay() {
        if (!config.enabled) return
        if (display.isEmpty()) return
        config.displayPosition.renderRenderables(display, posLabel = "Fusion Profit Overlay")
    }

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
    }

    private fun handleShardFusionGui(inventoryItems: Map<Int, ItemStack>) {
        val costItems: MutableList<ItemStack> = mutableListOf()
        val resultItems: MutableList<ItemStack> = mutableListOf()

        // Cost: 10, 12
        // Result: 14, 15, 16
        inventoryItems.forEach { (slot, itemStack) ->
            if (itemStack.isEmpty) return@forEach
            if (slot == 10 || slot == 12) {
                costItems.add(itemStack)
            } else if (slot == 14 || slot == 15 || slot == 16) {
                resultItems.add(itemStack)
            }
        }
        buildDisplay(costs = costItems, results = resultItems)
    }

    private fun handleConfirmFusionGui(inventoryItems: Map<Int, ItemStack>) {
        val fusionItem = inventoryItems[33] ?: return
        if (fusionItem.isEmpty) return

        val costItems: MutableList<ItemStack> = mutableListOf()
        val resultItems: MutableList<ItemStack> = mutableListOf()

        val lore = fusionItem.getLore().joinToString(" ")
        fusionLorePattern.matchMatcher(lore) {
            val amount1 = group("amount1").toIntOrNull() ?: return@matchMatcher
            val shard1 = group("shard1")
            val amount2 = group("amount2").toIntOrNull() ?: return@matchMatcher
            val shard2 = group("shard2")
            val resultAmount = group("resultAmount").toIntOrNull() ?: return@matchMatcher
            val resultName = group("resultName")

            val costStack1 = ItemUtils.createItemStack(
                Items.PLAYER_HEAD,
                shard1,
                lore = listOf("§7Required to fuse: §b$amount1"),
                amount = amount1,
            )
            costItems.add(costStack1)

            val costStack2 = ItemUtils.createItemStack(
                Items.PLAYER_HEAD,
                shard2,
                lore = listOf("§7Required to fuse: §b$amount2"),
                amount = amount2,
            )
            costItems.add(costStack2)

            val resultStack = ItemUtils.createItemStack(
                Items.PLAYER_HEAD,
                resultName,
                lore = emptyList(),
                amount = resultAmount,
            )
            resultItems.add(resultStack)
        }
        buildDisplay(costs = costItems, results = resultItems)
    }
}
