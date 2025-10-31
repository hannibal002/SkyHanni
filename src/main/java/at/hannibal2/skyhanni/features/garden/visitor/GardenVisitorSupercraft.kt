package at.hannibal2.hanni.features.garden.visitor

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SackApi.getAmountInSacks
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.hanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.PrimitiveIngredient.Companion.toPrimitiveItemStacks
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.collection.CollectionUtils.addOrPut
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Items
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GardenVisitorSupercraft {

    private val isSupercraftEnabled get() = VisitorApi.config.shoppingList.showSuperCraft

    private var hasIngredients = false
    private var lastClick = SimpleTimeMark.farPast()
    private var lastSuperCraftMaterial = NeuInternalName.NONE

    private val superCraftItem by lazy {
        ItemUtils.createItemStack(
            Items.golden_pickaxe,
            "§bSupercraft",
            "§8(From Hanni)",
            "",
            "§7You have the items to craft.",
            "§7Click me to open the supercrafter!",
        )
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (hasIngredients) {
            hasIngredients = false
        }
    }

    // needs to run later than onVisitorOpen at GardenVisitorFeatures
    @HandleEvent(priority = HandleEvent.LOW)
    fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        visitor.offer?.offerItem ?: return
        for ((internalName, amount) in visitor.shoppingList) {
            if (isSupercraftEnabled) {
                try {
                    getSupercraftForSacks(internalName, amount)
                } catch (e: NoSuchElementException) {
                    ErrorManager.logErrorWithData(
                        e,
                        "Failed to calculate supercraft recipes for visitor",
                        "internalName" to internalName,
                        "amount" to amount,
                        "visitor" to visitor.visitorName,
                        "visitor.offer?.offerItem" to visitor.offer?.offerItem,
                    )
                }
            }
        }
    }

    private fun getSupercraftForSacks(internalName: NeuInternalName, amount: Int) {
        val amountInSacks = internalName.getAmountInSacks()
        if (amountInSacks >= amount) return

        val ingredients = NeuItems.getRecipes(internalName)
            // TODO describe what this line does
            .firstOrNull { !it.ingredients.first().internalName.contains("PEST") }
            ?.ingredients ?: return
        val requiredIngredients = mutableMapOf<NeuInternalName, Int>()
        for ((key, count) in ingredients.toPrimitiveItemStacks()) {
            requiredIngredients.addOrPut(key, count)
        }
        hasIngredients = true
        for ((key, value) in requiredIngredients) {
            val sackItem = key.getAmountInSacks()
            lastSuperCraftMaterial = internalName
            if (sackItem < value * (amount - amountInSacks)) {
                hasIngredients = false
                break
            }
        }
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!hasIngredients) return
        if (event.inventory is InventoryPlayer) return

        if (event.slot == 31) {
            event.replace(superCraftItem)
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!hasIngredients) return

        if (event.slotId != 31) return
        event.cancel()
        if (lastClick.passedSince() > 0.3.seconds) {
            HypixelCommands.viewRecipe(lastSuperCraftMaterial)
            lastClick = SimpleTimeMark.now()
        }
    }
}
