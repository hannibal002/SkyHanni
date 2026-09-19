package at.hannibal2.skyhanni.features.event.yearoftheseal

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LoreCostUtils.hasTradeLine
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object FishyTreatProfit {

    private val config get() = SkyHanniMod.feature.event.yearOfTheSeal
    private var display = emptyList<Renderable>()
    private val inventory = InventoryDetector { inventoryNamePattern }
    private val FISHY_TREAT = "FISHY_TREAT".toInternalName()

    // I don't know why this fetches price source based on tracker config,
    // but it already did before I changed how tracker config worked
    val priceSource get() = SkyHanniMod.feature.misc.tracker.priceSource

    private val patternGroup = RepoPattern.group("event.year-of-the-seal.fishy-treat")

    private val inventoryNamePattern by patternGroup.pattern(
        "inventory",
        "Lukas the Aquarist",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.fishyTreatProfit || !inventory.isInside()) return

        DelayedRun.runOrNextTick {
            val table = mutableListOf<DisplayTableEntry>()
            for ((slot, item) in event.inventoryItems) {
                // ignore the last line of menu items
                if (slot > 44) continue
                // background items, named with a single space
                if (item.hoverName.string == " ") continue
                try {
                    readItem(slot, item, table)
                } catch (e: Throwable) {
                    ErrorManager.logErrorWithData(
                        e, "Error in FishyTreatProfit while reading item '${item.repoItemName}'",
                        "item" to item,
                        "name" to item.repoItemName,
                        "inventory name" to InventoryUtils.openInventoryName(),
                    )
                }
            }

            val newList = mutableListOf<Renderable>()
            newList.addString("§eProfit per Fishy Treat")
            newList.add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
            display = newList
        }
    }

    private fun readItem(slot: Int, item: SafeItemStack, table: MutableList<DisplayTableEntry>) {
        val lore = item.getLoreComponent().map { it.formattedTextCompatLessResets() }
        if (!lore.hasTradeLine()) return

        val itemName = getItemName(item)
        val itemNameText = itemName.formattedTextCompatLeadingWhiteLessResets()
        val allMaterials = lore.readLoreCosts(itemNameText).associate { it.internalName to it.amount }

        // ignore shop entries that do not cost fishy treats
        val amountOfFishyTreat = allMaterials[FISHY_TREAT] ?: return

        val additionalMaterials = allMaterials.filter { it.key != FISHY_TREAT }
        val additionalCost = getAdditionalCost(additionalMaterials)

        val (name, amount) = ItemUtils.readItemAmount(itemNameText) ?: return

        var internalName = NeuInternalName.fromItemNameOrNull(name)
        if (internalName == null) {
            internalName = item.getInternalName()
        }

        val itemPrice = internalName.getPrice(priceSource) * amount
        if (itemPrice < 0) return

        val profitPerSell = itemPrice - additionalCost

        // profit per bronze
        val profitPerFishy = profitPerSell / amountOfFishyTreat

//         val profitPerSellFormat = profitPerSell.shortFormat()
        val profitPerFishyFormat = profitPerFishy.shortFormat()
        val color = if (profitPerFishy > 0) "§6" else "§c"

        val hover = buildList {
            add(itemName)
            add("")
            add("§7Sell price: §6${itemPrice.shortFormat()}")

            // TODO add more exact material cost breakdown
            add("§7Additional cost: §6${additionalCost.shortFormat()}")
            addAdditionalMaterials(additionalMaterials)

//             add("§7Profit per sell: §6$profitPerSellFormat")
            add("")
            add("§7Fishy Treat required: §c$amountOfFishyTreat")
            add("§7Profit per Fishy Treat: §6$profitPerFishyFormat")
        }
        table.add(
            DisplayTableEntry(
                itemName,
                "$color$profitPerFishyFormat".asComponent(),
                profitPerFishy,
                internalName,
                hover.mapToComponents(),
                highlightsOnHoverSlots = listOf(slot),
            ),
        )
    }

    private fun MutableList<Any>.addAdditionalMaterials(additionalMaterials: Map<NeuInternalName, Long>) {
        for ((internalName, amount) in additionalMaterials) {
            add(internalName.getPriceName(amount, internalName.getPrice(priceSource)))
        }
    }

    private fun getItemName(item: SafeItemStack): Component {
        val name = item.hoverName
        val isEnchantedBook = item.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
        return if (isEnchantedBook) {
            item.repoItemName.asComponent()
        } else name
    }

    private fun getAdditionalCost(requiredItems: Map<NeuInternalName, Long>): Double {
        var otherItemsPrice = 0.0
        for ((name, amount) in requiredItems) {
            otherItemsPrice += name.getPrice(priceSource) * amount
        }
        return otherItemsPrice
    }

    init {
        RenderDisplayHelper(
            condition = { config.fishyTreatProfit },
            inventory = inventory,
        ) {
            config.fishyTreatProfitPosition.renderRenderables(display, posLabel = "Fishy Treat Profit")
        }
    }
}
