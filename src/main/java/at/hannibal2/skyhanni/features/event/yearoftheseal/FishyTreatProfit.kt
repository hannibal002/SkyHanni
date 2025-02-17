package at.hannibal2.skyhanni.features.event.yearoftheseal

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.add
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object FishyTreatProfit {

    private val config get() = SkyHanniMod.feature.event.yearOfTheSeal
    private var display = emptyList<Renderable>()
    private val inventory = InventoryDetector { name -> name == "Lukas the Aquarist" }
    private val FISHY_TREAT = "FISHY_TREAT".toInternalName()

    private val patternGroup = RepoPattern.group("event.year-of-the-seal.fishy-treat")

    /**
     * REGEX-TEST: §62,000,000 Coins
     */
    private val coinsPattern by patternGroup.pattern(
        "coins",
        "§6(?<coins>.*) Coins",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.fishyTreatProfit || !inventory.isInside()) return
        val table = mutableListOf<DisplayTableEntry>()
        for ((slot, item) in event.inventoryItems) {
            // ignore the last line of menu items
            if (slot > 44) continue
            // background items
            if (item.name == " ") continue
            try {
                readItem(slot, item, table)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Error in FishyTreatProfit while reading item '${item.itemName}'",
                    "item" to item,
                    "name" to item.itemName,
                    "inventory name" to InventoryUtils.openInventoryName(),
                )
            }
        }

        val newList = mutableListOf<Renderable>()
        newList.add(Renderable.string("§eProfit per Fishy Treat"))
        newList.add(LorenzUtils.fillTable(table, padding = 5, itemScale = 0.7))
        display = newList
        return
    }

    private fun readItem(slot: Int, item: ItemStack, table: MutableList<DisplayTableEntry>) {
        val itemName = getItemName(item)
        val allMaterials = getAdditionalMaterials(getRequiredItems(item))
        val additionalMaterials = allMaterials.filter { it.key != FISHY_TREAT }
        val amountOfFishyTreat = allMaterials[FISHY_TREAT] ?: run {
            ErrorManager.logErrorStateWithData(
                "failed reading fishy treat amount",
                "fishy treat amount not found in additionalMaterials",
                "itemName" to itemName,
                "additionalMaterials" to allMaterials,
                "inventory" to "",
            )
            return
        }

        val additionalCost = getAdditionalCost(additionalMaterials)

        val (name, amount) = ItemUtils.readItemAmount(itemName) ?: return

        var internalName = NeuInternalName.fromItemNameOrNull(name)
        if (internalName == null) {
            internalName = item.getInternalName()
        }

        val itemPrice = internalName.getPrice() * amount
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
                "$color$profitPerFishyFormat",
                profitPerFishy,
                internalName,
                hover,
                highlightsOnHoverSlots = listOf(slot),
            ),
        )
    }

    private fun MutableList<String>.addAdditionalMaterials(additionalMaterials: Map<NeuInternalName, Int>) {
        for ((internalName, amount) in additionalMaterials) {
            add(internalName.getPriceName(amount))
        }
    }

    private fun getItemName(item: ItemStack): String {
        val name = item.name
        val isEnchantedBook = item.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
        return if (isEnchantedBook) {
            item.itemName
        } else name
    }

    private fun getAdditionalMaterials(requiredItems: Map<String, Int>): Map<NeuInternalName, Int> {
        val additionalMaterials = mutableMapOf<NeuInternalName, Int>()
        for ((name, amount) in requiredItems) {
            coinsPattern.matchMatcher(name) {
                additionalMaterials[NeuInternalName.SKYBLOCK_COIN] = group("coins").formatInt()
            } ?: run {
                additionalMaterials[NeuInternalName.fromItemName(name)] = amount
            }
        }
        return additionalMaterials
    }

    private fun getAdditionalCost(requiredItems: Map<NeuInternalName, Int>): Double {
        var otherItemsPrice = 0.0
        for ((name, amount) in requiredItems) {
            otherItemsPrice += name.getPrice() * amount
        }
        return otherItemsPrice
    }

    private fun getRequiredItems(item: ItemStack): MutableMap<String, Int> {
        val items = mutableMapOf<String, Int>()
        var next = false
        val lore = item.getLore()
        for (line in lore) {
            if (line == "§7Cost") {
                next = true
                continue
            }
            if (next) {
                if (line == "") {
                    next = false
                    continue
                }

                val rawItemName = line.replace("§8 ", " §8")

                val pair = ItemUtils.readItemAmount(rawItemName)
                if (pair == null) {
                    ErrorManager.logErrorStateWithData(
                        "Error in FishyTreat Profit", "Could not read item amount",
                        "rawItemName" to rawItemName,
                        "name" to item.name,
                        "lore" to lore,
                    )
                    continue
                }
                items.add(pair)
            }
        }
        return items
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
