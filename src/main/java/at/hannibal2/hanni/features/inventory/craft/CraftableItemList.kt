package at.hannibal2.hanni.features.inventory.craft

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SackApi
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryOpenEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.hanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NeuItems.isVanillaItem
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.hanni.utils.PrimitiveRecipe
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.hanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.hanni.utils.collection.CollectionUtils.toSingletonListOrEmpty
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.SearchTextInput
import at.hannibal2.hanni.utils.renderables.Searchable
import at.hannibal2.hanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.hanni.utils.renderables.toSearchable
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.math.floor

@HanniModule
object CraftableItemList {
    private val config get() = HanniMod.feature.inventory.craftableItemList

    private var display = listOf<Renderable>()
    private var inInventory = false
    private val textInput = SearchTextInput()
    private val craftItemPattern by RepoPattern.pattern(
        "craftableitemlist.craftitem",
        "Craft Item",
    )

    @HandleEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!craftItemPattern.matches(event.inventoryName)) return
        inInventory = true

        val pricePer = mutableMapOf<NeuInternalName, Double>()
        val lines = mutableMapOf<NeuInternalName, Searchable>()
        loadItems(pricePer, lines)

        display = if (lines.isEmpty()) {
            Renderable.hoverTips(
                "§7No Items to craft",
                tips = listOf(
                    "§7No items found in your Inventory",
                    "or sacks that can be used as",
                    "material in crafting recipes.",
                ),
            ).toSingletonListOrEmpty()
        } else {
            buildList<Renderable> {
                val items = pricePer.sortedDesc().keys.map { lines[it] ?: error("impossible") }
                addString("§e§lCraftable Items §7(${items.size})")
                add(items.buildSearchableScrollable(height = 250, textInput, velocity = 20.0))
            }
        }
    }

    private fun loadItems(
        pricePer: MutableMap<NeuInternalName, Double>,
        lines: MutableMap<NeuInternalName, Searchable>,
    ) {
        val availableMaterial = readItems()
        for (internalName in NeuItems.allInternalNames.values) {
            if (config.excludeVanillaItems && internalName.isVanillaItem()) continue

            val recipes = NeuItems.getRecipes(internalName)
            for (recipe in recipes) {
                if (!recipe.isCraftingRecipe()) continue
                val renderable = createItemRenderable(recipe, availableMaterial, pricePer, internalName) ?: continue
                lines[internalName] = renderable
            }
        }
    }

    private fun createItemRenderable(
        recipe: PrimitiveRecipe,
        availableMaterial: Map<NeuInternalName, Long>,
        pricePer: MutableMap<NeuInternalName, Double>,
        internalName: NeuInternalName,
    ): Searchable? {
        val neededItems = ItemUtils.neededItems(recipe)
        // Just a fail save, should not happen normally
        if (neededItems.isEmpty()) return null

        val canCraftAmount = canCraftAmount(neededItems, availableMaterial)
        if (canCraftAmount <= 0) return null

        val amountFormat = canCraftAmount.addSeparators()
        val totalPrice = pricePer(neededItems)
        pricePer[internalName] = totalPrice
        val itemName = internalName.repoItemName
        val tooltip = buildList {
            add(itemName)
            add("")
            add("§7Craft cost: §6${totalPrice.shortFormat()}")
            for ((item, amount) in neededItems) {
                add(" ${item.getPriceName(amount)}")
            }
            add("")
            add("§7You have enough materials")
            val timeName = StringUtils.pluralize(canCraftAmount, "time", "times")
            add("§7to craft this item §e$amountFormat §7$timeName!")
            add("")
            add("§eClick to craft!")
        }
        return Renderable.clickable(
            "§8x$amountFormat $itemName",
            tips = tooltip,
            onLeftClick = {
                HypixelCommands.viewRecipe(internalName)
            },
        ).toSearchable(itemName)
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    private fun pricePer(neededItems: Map<NeuInternalName, Int>): Double = neededItems.map {
        it.key.getPrice() * it.value
    }.sum()

    private fun canCraftAmount(
        need: Map<NeuInternalName, Int>,
        available: Map<NeuInternalName, Long>,
    ): Int {
        val canCraftTotal = mutableListOf<Int>()
        for ((name, neededAmount) in need) {
            val having = available[name] ?: 0
            val canCraft = floor(having.toDouble() / neededAmount).toInt()
            canCraftTotal.add(canCraft)
        }
        return canCraftTotal.min()
    }

    private fun readItems(): Map<NeuInternalName, Long> {
        val materials = mutableMapOf<NeuInternalName, Long>()
        for (stack in InventoryUtils.getItemsInOwnInventory()) {
            val item = stack.toPrimitiveStackOrNull() ?: continue
            materials.addOrPut(item.internalName, item.amount.toLong())
        }
        if (config.includeSacks) {
            for ((internalName, item) in SackApi.sackData) {
                materials.addOrPut(internalName, item.amount.toLong())
            }
        }
        return materials
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        config.position.renderRenderables(display, posLabel = "Craftable Item List")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
