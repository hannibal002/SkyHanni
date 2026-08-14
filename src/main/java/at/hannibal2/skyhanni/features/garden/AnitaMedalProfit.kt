package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.LoreCostUtils.readLoreCosts
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.mapToComponents
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.network.chat.Component

@SkyHanniModule
object AnitaMedalProfit {

    private val config get() = GardenApi.config.anitaShop
    private var display = emptyList<Renderable>()

    var inInventory = false

    enum class MedalType(
        val simpleName: String,
        val factorBronze: Int,
        val currency: SkyblockCurrency,
    ) {
        GOLD("gold", 8, SkyblockCurrency.GOLD_MEDAL),
        SILVER("silver", 2, SkyblockCurrency.SILVER_MEDAL),
        BRONZE("bronze", 1, SkyblockCurrency.BRONZE_MEDAL),
        ;

        val color: LorenzColor get() = currency.color

        companion object {
            fun bySimpleNameOrNull(name: String): MedalType? = entries.firstOrNull { it.simpleName == name }

            fun getByInternalNameOrNull(internalName: NeuInternalName): MedalType? =
                entries.firstOrNull { it.currency.internalName == internalName }
        }
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
    }

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.medalProfitEnabled) return
        if (event.inventoryName != "Anita") return
        if (VisitorApi.inInventory) return

        inInventory = true

        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            try {
                readItem(slot, item, table)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error in AnitaMedalProfit while reading item '${item.repoItemName}'",
                    "item" to item,
                    "name" to item.repoItemName,
                    "inventory name" to InventoryUtils.openInventoryName(),
                )
            }
        }

        val newList = mutableListOf<Renderable>()
        newList.addString("§eProfit per Bronze Medal")
        newList.add(RenderableUtils.fillTable(table, padding = 5, itemScale = 0.7))
        display = newList
    }

    private fun readItem(slot: Int, item: SafeItemStack, table: MutableList<DisplayTableEntry>) {
        val itemName = getItemName(item)
        if (isInvalidItemName(itemName.string)) return

        val requiredItems = item.readLoreCosts()
        val additionalMaterials = getAdditionalMaterials(requiredItems)
        val additionalCost = getAdditionalCost(additionalMaterials)

        // Ignore items without medal cost, e.g. InfiniDirt Wand
        val bronzeCost = getBronzeCost(requiredItems) ?: return

        val (name, amount) = ItemUtils.readItemAmount(itemName.formattedTextCompatLeadingWhiteLessResets()) ?: return

        var internalName = NeuInternalName.fromItemNameOrNull(name)
        if (internalName == null) {
            internalName = item.getInternalName()
        }

        val itemPrice = internalName.getPrice() * amount
        if (itemPrice < 0) return

        val profitPerSell = itemPrice - additionalCost

        // profit per bronze
        val profitPerBronze = profitPerSell / bronzeCost

        val profitPerSellFormat = profitPerSell.shortFormat()
        val profitPerBronzeFormat = profitPerBronze.shortFormat()
        val color = if (profitPerBronze > 0) "§6" else "§c"

        val hover = buildList {
            add(itemName)
            add("")
            add("§7Sell price: §6${itemPrice.shortFormat()}")

            // TODO add more exact material cost breakdown
            add("§7Additional cost: §6${additionalCost.shortFormat()}")
            addAdditionalMaterials(additionalMaterials)

            add("§7Profit per sell: §6$profitPerSellFormat")
            add("")
            add("§7Bronze medals required: §c$bronzeCost")
            add("§7Profit per bronze medal: §6$profitPerBronzeFormat")
        }
        table.add(
            DisplayTableEntry(
                itemName,
                "$color$profitPerBronzeFormat".asComponent(),
                profitPerBronze,
                internalName,
                hover.mapToComponents(),
                highlightsOnHoverSlots = listOf(slot),
            ),
        )
    }

    private fun MutableList<Any>.addAdditionalMaterials(additionalMaterials: Map<NeuInternalName, Int>) {
        for ((internalName, amount) in additionalMaterials) {
            add(internalName.getPriceName(amount))
        }
    }

    private val invalidItemNames = listOf(
        " ",
        "Close",
        "Unique Gold Medals",
        "Medal Trades",
    )

    private fun isInvalidItemName(itemName: String): Boolean = itemName in invalidItemNames

    private fun getItemName(item: SafeItemStack): Component {
        val name = item.hoverName
        val isEnchantedBook = item.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
        return if (isEnchantedBook) {
            item.repoItemName.asComponent()
        } else name
    }

    private fun getAdditionalMaterials(requiredItems: List<LoreCostUtils.LoreCostEntry>): Map<NeuInternalName, Int> =
        requiredItems.filter { MedalType.getByInternalNameOrNull(it.internalName) == null }
            .associate { it.internalName to it.amount.toInt() }

    private fun getAdditionalCost(requiredItems: Map<NeuInternalName, Int>): Double {
        var otherItemsPrice = 0.0
        for ((name, amount) in requiredItems) {
            otherItemsPrice += name.getPrice() * amount
        }
        return otherItemsPrice
    }

    private fun getBronzeCost(requiredItems: List<LoreCostUtils.LoreCostEntry>): Int? {
        for (entry in requiredItems) {
            MedalType.getByInternalNameOrNull(entry.internalName)?.let {
                return it.factorBronze * entry.amount.toInt()
            }
        }
        return null
    }

    @HandleEvent
    private fun onChestGuiRender() {
        if (!inInventory || VisitorApi.inInventory) return
        config.medalProfitPos.renderRenderables(
            display,
            extraSpace = 5,
            posLabel = "Anita Medal Profit",
        )
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.anitaMedalProfitEnabled", "garden.anitaShop.medalProfitEnabled")
        event.move(3, "garden.anitaMedalProfitPos", "garden.anitaShop.medalProfitPos")
    }
}
