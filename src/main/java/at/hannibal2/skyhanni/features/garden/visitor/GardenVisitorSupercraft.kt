package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
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

    @SubscribeEvent(priority = EventPriority.NORMAL)
    fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        val offerItem = visitor.offer?.offerItem ?: return

        val lore = offerItem.getLore()
        for (line in lore) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val pair = ItemUtils.readItemAmount(line)
            if (pair == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read Shopping List in Visitor Inventory", "ItemUtils.readItemAmount returns null",
                    "line" to line,
                    "offerItem" to offerItem,
                    "lore" to lore,
                    "visitor" to visitor
                )
                continue
            }
            val (itemName, amount) = pair
            val internalName = NEUInternalName.fromItemName(itemName)
            if (isSupercraftEnabled) getSupercraftForSacks(internalName, amount)
        }
    }

    fun getSupercraftForSacks(internalName: NEUInternalName, amount: Int) {
        val ingredients = NEUItems.getRecipes(internalName).first { !it.ingredients.first().internalItemId.contains("PEST") }.ingredients
        val ingredientReqs = mutableMapOf<String, Int>()
        for (ingredient in ingredients) {
            ingredientReqs[ingredient.internalItemId] = ingredientReqs.getOrDefault(ingredient.internalItemId, 0) + ingredient.count.toInt()
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
    fun onStackClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!hasIngredients) return

        if (event.slotId == 31) {
            event.isCanceled = true
            if (lastClick.passedSince() > 0.3.seconds) {
                ChatUtils.sendCommandToServer("recipe $lastSuperCraftMaterial")
                lastClick = SimpleTimeMark.now()
            }
        }
    }
}
