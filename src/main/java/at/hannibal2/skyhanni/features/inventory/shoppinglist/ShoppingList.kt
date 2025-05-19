package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SackDataUpdateEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.inventory.shoppinglist.CategoryTemplate.Companion.toCategoryTemplate
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.closeInventory
import at.hannibal2.skyhanni.utils.InventoryUtils.inAnyInventory
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RecipeType
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

@SkyHanniModule
object ShoppingList {
    val config get() = SkyHanniMod.feature.inventory.shoppingList

    private val storage: ProfileSpecificStorage.ShoppingListStorage? get() = ProfileStorageData.profileSpecific?.shoppingList

    private var isConfigLoaded = false
    private val categories: MutableList<ShoppingListCategory> = mutableListOf()
    private var items: ShoppingListCategory = ShoppingListCategory("Items")

    data class ItemsOverallEntry(val amount: Double, val frequency: Int) {
        fun plus(other: ItemsOverallEntry): ItemsOverallEntry {
            return ItemsOverallEntry(
                amount = this.amount + other.amount,
                frequency = this.frequency + other.frequency,
            )
        }
    }

    object ItemsOverall {
        private val allItems: MutableMap<NeuInternalName, ItemsOverallEntry> = mutableMapOf()

        fun update() {
            if (!isConfigLoaded) return

            allItems.clear()
            for (category in categories + items) {

                category.getItemsOverall().forEach { (name, itemEntry: ItemsOverallEntry) ->
                    if (allItems.containsKey(name)) {
                        allItems[name]?.let { allItems[name] = it.plus(itemEntry) }
                    } else {
                        allItems[name] = itemEntry
                    }
                }
            }
        }

        fun getItems(): MutableMap<NeuInternalName, ItemsOverallEntry> {
            return allItems
        }

        override fun toString(): String {
            var result = "ItemsOverall("

            for ((item, pair) in allItems) {
                result += ("\nItem: $item, Amount: ${pair.amount}, in items: ${pair.frequency}")
            }

            result += "\n)"
            return result
        }

        fun get(item: NeuInternalName) = allItems[item]
    }

    // TODO soon: somehow also make it searchable?
    private var display: List<Renderable> = listOf()

    private var inventoryOpen = false

    var currentlyOpenRecipe: PrimitiveRecipe? = null
    var displayItem: ItemStack? = null

    private class CommandArguments(val itemName: String, val amount: Double?, val categoryName: String?)

    // all the functions for interacting with the shopping list come here
    // parseCommandArguments only works if only the item name is also okay
    private fun parseCommandArguments(args: Array<String>): CommandArguments? {
        if (args.isEmpty()) {
            ChatUtils.userError("No arguments entered")
            return null
        }

        val itemName: String?
        var amount: Double? = null
        var categoryName: String? = null

        val numberEntries = mutableListOf<Int>()

        for (i in args.indices) {
            if (args[i].toDoubleOrNull() != null) {
                numberEntries.add(i)
            }
        }

        if (args.size == 1) {
            itemName = args[0]
        } else if (numberEntries.isEmpty()) {
            if (args.last().isCategory() || !args.joinToString(" ").toInternalName().isKnownItem()) {
                itemName = args.take(args.size - 1).joinToString(" ")
                categoryName = args.last()
                add(itemName.toInternalName(), categoryName = categoryName)
            } else {
                itemName = args.joinToString(" ")
            }
        } else {
            itemName = args.take(numberEntries.last()).joinToString(" ")
            amount = args[numberEntries.last()].toDoubleOrNull()
            categoryName = args.getOrNull(numberEntries.last() + 1)
        }

        return CommandArguments(itemName, amount, categoryName)
    }

    private fun add(arguments: CommandArguments) {
        add(arguments.itemName.toInternalName(), arguments.amount ?: 1.0, arguments.categoryName)
    }

    fun add(itemName: NeuInternalName, amount: Double = 1.0, categoryName: String? = null) {
        if (!isConfigLoaded) return

        // TODO: shouldn't happen @Thunderblade73
        if (!isEnabled()) return

        val category: ShoppingListCategory
        if (categoryName != null) {
            if (!categoryName.isCategory()) {
                category = ShoppingListCategory(categoryName)
                categories.add(category)
            } else {
                category = categories.firstOrNull { it.name == categoryName } ?: return
            }
        } else {
            category = items
        }
        category.add(itemName, amount)

        createDisplay()
    }

    fun onAddCommand(args: Array<String>) {
        val arguments = parseCommandArguments(args) ?: return

        add(arguments)
    }

    fun addCategory(
        categoryName: String,
        color: LorenzColor? = null,
        saveInStorage: Boolean = true,
        displayCondition: () -> Boolean = { true },
    ) {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        if (categories.any { it.name == categoryName }) return

        if (color == null) {
            categories.add(ShoppingListCategory(categoryName, saveInStorage = saveInStorage, displayCondition = displayCondition))
        } else {
            categories.add(
                ShoppingListCategory(
                    categoryName,
                    color = color,
                    saveInStorage = saveInStorage,
                    displayCondition = displayCondition,
                ),
            )
        }

        update()
    }

    fun set(itemName: NeuInternalName, amount: Double, categoryName: String? = null) {
        if (!isConfigLoaded) return

        // TODO: shouldn't happen @Thunderblade73
        if (!isEnabled()) return

        val category: ShoppingListCategory
        if (categoryName != null) {
            if (!categoryName.isCategory()) {
                category = ShoppingListCategory(categoryName)
                categories.add(category)
            } else {
                category = categories.firstOrNull { it.name == categoryName } ?: return
            }
        } else {
            category = items
        }
        category.set(itemName, amount)

        createDisplay()
    }

    fun onSetCommand(args: Array<String>) {
        if (args.size < 2) {
            ChatUtils.userError("Please specify an item and an amount to set")
            return
        }

        val itemName: String?
        val amount: Double?
        val categoryName: String?

        val numberEntries = mutableListOf<Int>()

        for (i in args.indices) {
            if (args[i].toDoubleOrNull() != null) {
                numberEntries.add(i)
            }
        }

        if (numberEntries.isEmpty()) {
            ChatUtils.userError("Please specify an item and an amount to set")
            return
        }

        itemName = args.take(numberEntries.last()).joinToString(" ")
        amount = args[numberEntries.last()].toDouble()
        categoryName = args.getOrNull(numberEntries.last() + 1)
        set(itemName.toInternalName(), amount, categoryName)
    }

    fun removeCategory(categoryName: String) {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        val category = categories.firstOrNull { it.name == categoryName } ?: return
        categories.remove(category)
        update()
    }

    fun removeCategory(category: ShoppingListCategory) {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        categories.remove(category)
        update()
    }

    private fun remove(arguments: CommandArguments) {
        remove(arguments.itemName, arguments.amount, arguments.categoryName)
    }

    fun remove(name: String, amount: Double? = null, categoryName: String? = null) {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        val itemName: NeuInternalName? = name.toInternalName()

        if (itemName == null || !itemName.isKnownItem()) {
            handleItemNotFound(name)
        } else if (categoryName != null) {
            removeFromCategory(itemName, categoryName, amount)
        } else {
            removeItemFromItemsOrCategories(itemName, amount)
        }

        update()
    }

    fun onRemoveCommand(args: Array<String>) {
        val arguments = parseCommandArguments(args) ?: return

        remove(arguments)
    }

    private fun handleItemNotFound(name: String) {
        if (name.isCategory()) {
            removeCategory(name)
        } else {
            ChatUtils.userError("Item $name not found")
        }
    }

    private fun removeFromCategory(itemName: NeuInternalName, categoryName: String, amount: Double?) {
        if (!categoryName.isCategory()) {
            ChatUtils.userError("Category $categoryName not found")
            return
        }

        val category = categories.firstOrNull { it.name == categoryName }
        category?.remove(itemName, amount) ?: ChatUtils.userError("Category $categoryName not found")
    }

    private fun removeItemFromItemsOrCategories(itemName: NeuInternalName, amount: Double?) {
        if (items.contains(itemName)) {
            items.remove(itemName, amount)
        } else {
            removeFromMultipleCategories(itemName, amount)
        }
    }

    private fun removeFromMultipleCategories(itemName: NeuInternalName, amount: Double?) {
        var category: ShoppingListCategory? = null
        for (cat in categories) {
            if (cat.contains(itemName)) {
                if (category != null) {
                    ChatUtils.userError(
                        "Item ${itemName.repoItemName}§c found in multiple categories, please specify the category to remove from",
                    )
                    return
                }
                category = cat
            }
        }

        category?.remove(itemName, amount) ?: ChatUtils.userError("Item ${itemName.repoItemName}§c not found")
    }

    fun clear() {
        if (!isConfigLoaded) return

        categories.clear()
        items.clear()

        update()
    }

    // logic and related functions
    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    fun String.isCategory(): Boolean = categories.any { it.name == this }

    fun String.getCategory(): ShoppingListCategory? = categories.firstOrNull { it.name == this }

    fun resetDisplayItem() {
        displayItem = null
    }

    fun createDisplayItem() {
        if (currentlyOpenRecipe == null) return
        if (displayItem != null && displayItem?.displayName == "§bSelect Recipe") return

        val lore = mutableListOf<String>()
        lore.add("§7Left click to add the recipe to the shopping list")
        lore.add("§7Right click to only add the result to the shopping list")

        displayItem = ItemStack(Blocks.diamond_block).setLore(lore).setStackDisplayName("§bAdd Recipe to shopping list")
    }

    fun isInventoryOpen() = inventoryOpen

    fun recheckInInventory() {
        if (!isEnabled()) return
        val currentlyOpen = inAnyInventory()
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }
    }

    fun loadShoppingList(forceOverwriteCurrent: Boolean = false) {
        if (isConfigLoaded && !forceOverwriteCurrent) return
        if (storage == null) return // technically not needed I guess

        val storedCategories = storage?.categories ?: return
        val storedItems = storage?.items ?: return

        categories.clear()
        for (category in storedCategories) {
            categories.add(category.toShoppingListCategory())
        }

        items = storedItems.toShoppingListCategory()

        isConfigLoaded = true
    }

    fun saveShoppingList() {
        if (!isConfigLoaded) return

        val tempCategories = mutableListOf<CategoryTemplate>()
        for (category in categories) {
            if (category.saveInStorage) {
                tempCategories.add(category.toCategoryTemplate())
            }
        }

        ProfileStorageData.profileSpecific?.shoppingList?.categories = tempCategories
        ProfileStorageData.profileSpecific?.shoppingList?.items = items.toCategoryTemplate()
    }

    fun moveCategoryToTop(category: ShoppingListCategory) {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        categories.remove(category)
        categories.add(0, category)

        update()
    }

    // all display related functions
    fun createDisplay() {
        if (!isConfigLoaded) return

        if (!isEnabled() || (categories.isEmpty() && items.isEmpty())) {
            display = listOf()
            return
        }

        display = buildList {
            addString("§lShopping List")
            categories.forEach {
                addAll(it.getRenderables(1))
            }
            addAll(items.getRenderables(0, showThis = false))
        }
    }

    // other functions etc.
    fun update() {
        if (!isEnabled()) return
        if (!isConfigLoaded) return

        ItemsOverall.update()

        createDisplay()

        saveShoppingList()
    }

    val testItem1 = "ASPECT_OF_THE_END".toInternalName()
    val testItem2 = "ENCHANTED_CARROT".toInternalName()
    val testItem3 = "DIAMOND".toInternalName()

    fun test() {
        println("test triggered")

        println("is config loaded: $isConfigLoaded")
        println("categories: $categories")
        println("items: $items")

        clear()

        add(testItem1, 2.0, categoryName = "Weapons")
        addCategory("Visitors", saveInStorage = false)
        add(testItem2, 49.0, categoryName = "Visitors")
        add(testItem3, 136.0)

        update()

        println("test done")
    }

    fun InventoryFullyOpenedEvent.isRecipe() = inventoryName.contains("Recipe") && inventorySize == 54

    // all events come here
    @HandleEvent(onlyOnSkyblock = true, eventType = OwnInventoryItemUpdateEvent::class)
    fun onOwnInventoryItemUpdate() {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true, eventType = ItemAddInInventoryEvent::class)
    fun onItemAddInInventoryEvent() {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true, eventType = SackDataUpdateEvent::class)
    fun onSackUpdate() {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true, eventType = InventoryCloseEvent::class)
    fun onInventoryClose() {
        recheckInInventory()
        currentlyOpenRecipe = null
        update()
    }

    @HandleEvent(eventType = WorldChangeEvent::class)
    fun onWorldChange() {
        recheckInInventory()
        update()
    }

    @HandleEvent(eventType = IslandChangeEvent::class)
    fun onIslandChange() {
        recheckInInventory()
        update()
    }

    // this triggers only when opening another inventory, not the own inventory
    @HandleEvent(onlyOnSkyblock = true)
    fun onInventorOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!event.isRecipe()) {
            currentlyOpenRecipe = null
            return
        }

        val ingredients: Set<PrimitiveIngredient> = listOf(10, 11, 12, 13, 19, 20, 21, 28, 29, 30).mapNotNull {
            event.inventoryItems[it]?.toPrimitiveStackOrNull()?.toPrimitiveIngredient()
        }.toSet()

        val result = event.inventoryItems[25]?.toPrimitiveStackOrNull()?.toPrimitiveIngredient()

        currentlyOpenRecipe = PrimitiveRecipe(ingredients, setOf(result ?: return), RecipeType.CRAFTING)

        createDisplayItem()

        recheckInInventory()
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled() || currentlyOpenRecipe == null) return
        if (event.inventory !is InventoryPlayer && event.slot == 51) {
            displayItem?.let { event.replace(it) }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (event.slotId != 51) return
        if (event.item == null) return

        if (!isConfigLoaded) return

        val currentlyOpenRecipe = currentlyOpenRecipe

        if (currentlyOpenRecipe == null) {
            return
        }

        if (event.item.displayName == "§bSelect Recipe") {
            event.cancel()
            for (category in categories + items) {
                if (category.onItemClicked(event.item)) {
                    closeInventory()
                    return
                }
            }
        } else if (event.item.displayName == "§bAdd Recipe to shopping list") {
            event.cancel()
            val output = currentlyOpenRecipe.output

            if (output == null) {
                ChatUtils.chat("Invalid Recipe with no output")
                return
            }

            if (event.clickedButton == 2) {
                items.add(output.internalName, 1.0)
            } else {
                items.add(output.internalName, 1.0, recipe = currentlyOpenRecipe)
            }

        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        loadShoppingList()
        update()
    }

    @HandleEvent(onlyOnSkyblock = true, eventType = GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRender() {
        if (!isEnabled()) return
        config.position.renderRenderables(display, posLabel = "Shopping List")
    }

    @HandleEvent(onlyOnSkyblock = true, eventType = GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onInsideChestRender() {
        if (!isEnabled()) return
        if (!inventoryOpen) {
            inventoryOpen = true
            update()
        }
        config.position.renderRenderables(display, posLabel = "Shopping List")
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Shopping List")
        if (!isEnabled()) {
            event.addIrrelevant("Shopping List is disabled")
            return
        }

        if (!isConfigLoaded) {
            event.addIrrelevant("Shopping List is not loaded")
            return
        }

        if (categories.isEmpty() && items.isEmpty()) {
            event.addIrrelevant("Shopping List is empty")
            return
        }

        event.addData {
            categories.forEach {
                add("§${it.color.chatColorCode}${it.name}")
                it.items.forEach { item ->
                    add("  $item")
                }
            }

            add("")

            items.items.forEach { item ->
                add(item.toString())
            }

            add("")

            add("ItemsOverall:")
            for ((item, pair) in ItemsOverall.getItems()) {
                add("  Item: ${item.itemNameWithoutColor}, Amount: ${pair.amount}, in items: ${pair.frequency}")
            }
        }
    }

    // this event should be last
    @HandleEvent()
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shshoppinglistclear") {
            description = "Clear the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shslclear")
            callback { clear() }
        }
        event.register("shshoppinglistadd") {
            description = "Add an item to the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shsladd", "shsla")
            callback { onAddCommand(it) }
        }
        event.register("shshoppinglistset") {
            description = "Set the amount of an item in the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shslset")
            callback { onSetCommand(it) }
        }
        event.register("shshoppinglistremove") {
            description = "Remove an item from the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shslremove")
            callback { onRemoveCommand(it) }
        }
        event.register("shshoppinglistremovecategory") {
            description = "Remove a category from the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shslremovecategory")
            callback { removeCategory(it[0]) }
        }
        event.register("shshoppinglistupdate") {
            description = "Update the shopping list"
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("shslupdate")
            callback { update() }
        }
        event.register("shshoppinglisttest") {
            description = "Test the shopping list feature"
            category = CommandCategory.DEVELOPER_TEST
            aliases = listOf("shsltest")
            callback { test() }
        }

    }

}
