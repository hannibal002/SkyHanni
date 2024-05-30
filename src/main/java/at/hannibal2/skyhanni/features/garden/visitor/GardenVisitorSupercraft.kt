package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.allIngredients
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.entity.player.InventoryPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenVisitorSupercraft {

    private val isSupercraftEnabled get() = VisitorAPI.config.shoppingList.showSuperCraft

    private var hasIngredients = false
    private var lastClick = SimpleTimeMark.farPast()
    private var lastSuperCraftMaterial = ""

    private val superCraftItem by lazy {
        val neuItem = "GOLD_PICKAXE".asInternalName().getItemStack()
        Utils.createItemStack(
            neuItem.item,
            "§bSuper Craft",
            "§7You have the items to craft",
            "§7Click me to open the super crafter!"
        )
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (hasIngredients) {
            hasIngredients = false
        }
    }

    // needs to run later than onVisitorOpen at GardenVisitorFeatures
    @SubscribeEvent(priority = EventPriority.LOW)
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

    private fun getSupercraftForSacks(internalName: NEUInternalName, amount: Int) {
        val ingredients = NEUItems.getRecipes(internalName)
            // TODO describe what this line does
            .firstOrNull() { !it.allIngredients().first().internalItemId.contains("PEST") }
            ?.allIngredients() ?: return
        val ingredientReqs = mutableMapOf<String, Int>()
        for (ingredient in ingredients) {
            val key = ingredient.internalItemId
            ingredientReqs[key] = ingredientReqs.getOrDefault(key, 0) + ingredient.count.toInt()
        }
        hasIngredients = true
        for ((key, value) in ingredientReqs) {
            val sackItem = key.asInternalName().getAmountInSacks()
            lastSuperCraftMaterial = internalName.itemName.removeColor()
            if (sackItem < value * amount) {
                hasIngredients = false
                break
            }
        }
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!hasIngredients) return
        if (event.inventory is InventoryPlayer) return

        if (event.slotNumber == 31) {
            event.replaceWith(superCraftItem)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!hasIngredients) return

        if (event.slotId != 31) return
        event.isCanceled = true
        if (lastClick.passedSince() > 0.3.seconds) {
            HypixelCommands.recipe(lastSuperCraftMaterial)
            lastClick = SimpleTimeMark.now()
        }
    }
}
