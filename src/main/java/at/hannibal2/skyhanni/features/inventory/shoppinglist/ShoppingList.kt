package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
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
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.InventoryUtils.closeInventory
import at.hannibal2.skyhanni.utils.InventoryUtils.inAnyInventory
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RecipeType
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack

@SkyHanniModule
object ShoppingList {
    val config get() = SkyHanniMod.feature.inventory.shoppingList

    private val categories: MutableList<ShoppingListCategory> = mutableListOf()
    private val items: ShoppingListCategory = ShoppingListCategory("Items")

    // TODO soon: somehow also make it searchable?
    private var display: List<Renderable> = listOf()

    private var inventoryOpen = false

    var currentlyOpenRecipe: PrimitiveRecipe? = null
    var displayItem: ItemStack? = null

    // all the functions for interacting with the shopping list come here
    fun removeCategory(category: ShoppingListCategory) {
        if (!isEnabled()) return

        categories.remove(category)
        update()
    }

    fun clear() {
        categories.clear()
        items.clear()

        update()
    }

    // logic and related functions
    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun String.isCategory(): Boolean = categories.any { it.name == this }

    fun String.getCategory(): ShoppingListCategory? = categories.firstOrNull { it.name == this }

    fun resetDisplayItem() {
        displayItem = null
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

    fun moveCategoryToTop(category: ShoppingListCategory) {
        if (!isEnabled()) return

        categories.remove(category)
        categories.add(0, category)

        update()
    }

    // all display related functions
    fun createDisplay() {
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

        createDisplay()
    }

    val testItem1 = "ASPECT_OF_THE_END".toInternalName()
    val testItem2 = "ENCHANTED_CARROT".toInternalName()
    val testItem3 = "DIAMOND".toInternalName()

    fun test() {
        println("test triggered")

        println("categories: $categories")
        println("items: $items")

        clear()

        categories.add(ShoppingListCategory("Weapons"))
        val weapons = "Weapons".getCategory()
        weapons?.items?.add(ShoppingListItem(testItem1, 2.0, weapons))

        categories.add(ShoppingListCategory("Visitors"))
        val visitors = "Visitors".getCategory()
        visitors?.items?.add(ShoppingListItem(testItem2, 49.0, visitors))
        items.items.add(ShoppingListItem(testItem3, 136.0, items))

        update()

        println("test done")
    }

    fun InventoryFullyOpenedEvent.isRecipe() = inventoryName.contains("Recipe") && inventorySize == 54

    // all events come here
    @HandleEvent(onlyOnSkyblock = true)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemAddInInventoryEvent(event: ItemAddInInventoryEvent) {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSackUpdate(event: SackDataUpdateEvent) {
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        recheckInInventory()
        currentlyOpenRecipe = null
        update()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        recheckInInventory()
        update()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
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

        val ingredients = listOf(10, 11, 12, 13, 19, 20, 21, 28, 29, 30).mapNotNull {
            event.inventoryItems[it]?.toPrimitiveStackOrNull()?.toPrimitiveIngredient()
        }.toSet<PrimitiveIngredient>()

        val result = event.inventoryItems[25]?.toPrimitiveStackOrNull()?.toPrimitiveIngredient()

        currentlyOpenRecipe = PrimitiveRecipe(ingredients, setOf(result ?: return), RecipeType.CRAFTING)

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
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderRenderables(display, posLabel = "Shopping List")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
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

        }
    }

    // this event should be last
    @HandleEvent()
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shshoppinglisttest") {
            description = "Test the shopping list feature"
            category = CommandCategory.DEVELOPER_TEST
            aliases = listOf("shsltest")
            callback { test() }
        }

    }

}
