package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.ItemBuyApi.buy
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.isBazaarItem
import at.hannibal2.skyhanni.features.inventory.shoppinglist.ShoppingList.ItemsOverallEntry
import at.hannibal2.skyhanni.features.inventory.shoppinglist.ShoppingList.currentlyOpenRecipe
import at.hannibal2.skyhanni.features.inventory.shoppinglist.ShoppingList.resetDisplayItem
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.HypixelCommands.craft
import at.hannibal2.skyhanni.utils.HypixelCommands.viewRecipe
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventoryAndSacks
import at.hannibal2.skyhanni.utils.ItemPriceUtils.isAuctionHouseItem
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.MIDDLE_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.isVanillaItem
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.SignUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils.noTradeMode
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.ClickTypeWithModifiers
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Keyboard.KEY_DOWN
import org.lwjgl.input.Keyboard.KEY_UP

@Suppress("TooManyFunctions")
class ShoppingListItem(
    val internalName: NeuInternalName,
    var amount: Double = 1.0,
    val topLevelCategory: ShoppingListCategory,
    val topLevelItem: ShoppingListItem? = null,
    var recipe: PrimitiveRecipe? = null,
    var hidden: Boolean = false,
) {

    // TODO soon (probably): add a way to offset the amount of an item counted in the inventory etc.

    val totalAmount: Double
        get() = amount * (topLevelItem?.remainingAmount ?: 1.0)

    val remainingAmount: Double
        get() = if (getCurrentAmount() > totalAmount) 0.0 else totalAmount - getCurrentAmount()

    var possibleRecipes: List<PrimitiveRecipe> = emptyList()
    var displayItem: ItemStack? = null
    val downBreakable: Boolean
        get() {
            return subItems.isEmpty() && possibleRecipes.isNotEmpty()
        }

    val subItems = mutableListOf<ShoppingListItem>()

    init {
        loadPossibleRecipes()
    }

    /*
    TODO later: make this all configurable
    what do we want to be able to do from the display widget:
        left click is for doing stuff with it
        - (left click) get from ah/bz  (switch ah/bz and break down as a setting) also only (click) if no recipe
        - open recipe to craft it
        - (shift + left click) break down into its subitems
        right click is for doing stuff with the item itself
        - (right click) remove
        - (shift + right click) hide/unhide
        - (ctrl + shift + right click) hide/unhide all whole tree
        - (ctrl + right click) move to top

        - (middle click) copy to clipboard

        maybe?
        (probably not as it isn't really necessary and a lot of work)
        - move to another category
        - copy to another category
        - changing amount with arrow keys

     what may we want to see of the item:
        - the name with rarity as color
        - the required amount
        - the possesed amount
        - the missing amount
        - the price for 1 (on hover)
        - the price for the required amount

        - icon (if plausible)

     what may we want to see of subitems additionally:
        - the amount per craft (on hover)
        - the price for the amount per craft (on hover)
     */

    override fun toString(): String {
        return "${internalName.repoItemName} x$amount" + if (subItems.isNotEmpty()) {
            " (${subItems.joinToString(", ")})"
        } else {
            ""
        }
    }

    fun breakDownIntoSubitems() {

        if (recipe == null) {
            val success = decideRecipe()
            if (!success) {
                return
            }
        }

        subItems.clear()

        addRecipe()

        ShoppingList.update()
    }

    fun PrimitiveRecipe.isRecursing(): Boolean {
        return ingredients.any { it.internalName == topLevelItem?.internalName }
    }

    fun PrimitiveRecipe.isRecursingCompacting(): Boolean {
        val firstIngredient = ingredients.firstOrNull() ?: return false
        if (ingredients.any { it.internalName != firstIngredient.internalName }) {
            return false
        }

        val recipes = NeuItems.getRecipes(firstIngredient.internalName).filter { it.isCraftingRecipe() }
        return recipes.any { recipe -> recipe.ingredients.any { it.internalName == internalName } }
    }

    fun loadPossibleRecipes() {
        if (recipe != null) return

        possibleRecipes = NeuItems.getRecipes(internalName).filter { it.isCraftingRecipe() }.filter { recipe ->
            !recipe.isRecursing() && !recipe.isRecursingCompacting()
        }
    }

    fun decideRecipe(): Boolean {
        if (possibleRecipes.isEmpty()) {
            ChatUtils.chat("No recipes found for ${internalName.itemNameWithoutColor}")
            return false
        }

        if (possibleRecipes.size > 1) {
            ChatUtils.chat("Multiple recipes found for ${internalName.itemNameWithoutColor}\n§7Select one")

            val lore = buildList {
                add("§8(From SkyHanni)")

                // TODO (maybe): add stuff
            }

            displayItem = ItemStack(Blocks.diamond_block).setLore(lore).setStackDisplayName("§bSelect Recipe")
            ShoppingList.displayItem = displayItem

            viewRecipe(internalName)
        } else {
            recipe = possibleRecipes[0]
        }
        return true
    }

    fun onItemClick(clickedItem: ItemStack): Boolean {
        if (displayItem != null && clickedItem == displayItem) {
            recipe = currentlyOpenRecipe
            displayItem = null
            resetDisplayItem()
            breakDownIntoSubitems()
            return true
        }
        subItems.forEach {
            if (it.onItemClick(clickedItem)) {
                return true
            }
        }
        return false
    }

    fun addRecipe() {
        val usedRecipe: PrimitiveRecipe = recipe?.copy() ?: return

        for (ingredient: PrimitiveIngredient in usedRecipe.ingredients) {
            val item = subItems.firstOrNull { it.internalName == ingredient.internalName } as ShoppingListItem?

            val ingredientAmount = ingredient.count / (usedRecipe.output?.count ?: 1.0)

            if (item == null) {
                subItems.add(ShoppingListItem(ingredient.internalName, ingredientAmount, topLevelCategory, this))
            } else {
                item.changeAmountBy(ingredientAmount)
            }
        }
    }

    fun changeAmountBy(amount: Double) {
        this.amount += amount
    }

    fun changeAmountTo(amount: Double) {
        this.amount = amount
    }

    fun getCurrentAmount(): Int {
        // TODO later: also get the amount in the storage (as an option), as it's relevant for supercraft
        return internalName.getAmountInInventoryAndSacks()
    }

    fun getMissingAmountInInventory(): Double {
        return totalAmount - internalName.getAmountInInventory()
    }

    fun hasItems(): Boolean {
        return totalAmount <= getCurrentAmount()
    }

    fun hasAllSubItems(): Boolean {
        return if (subItems.isEmpty()) {
            hasItems()
        } else {
            subItems.all { it.hasAllSubItems() }
        }
    }

    fun getItemsOverall(): Map<NeuInternalName, ItemsOverallEntry> {
        return buildMap {
            this[internalName] = ItemsOverallEntry(totalAmount, 1)

            subItems.forEach { item ->
                item.getItemsOverall().forEach { (name, itemEntry: ItemsOverallEntry) ->
                    if (this.containsKey(name)) {
                        this[name]?.let { this[name] = it.plus(itemEntry) }
                    } else {
                        this[name] = itemEntry
                    }
                }
            }
        }
    }

    fun checkIfInSignAndInsertAmount(): Boolean {
        if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
            ChatUtils.chat("Detected sign gui, pasting number into sign instead")
            SignUtils.setTextIntoSign("${remainingAmount.toInt()}")
            return true
        } else {
            return false
        }
    }

    fun buyItem() {
        if (checkIfInSignAndInsertAmount()) return
        if (remainingAmount <= 0) return

        internalName.buy(remainingAmount.toInt())
    }

    fun openCraftingRecipe() {
        if (checkIfInSignAndInsertAmount()) return

        if (internalName.isVanillaItem()) {
            ChatUtils.chat("Vanilla item, can't open recipe, opening the crafting table and getting all required items instead")
            subItems.forEach {
                it.fetchItemFromAvailableStorage()
                craft()
            }

        } else {
            viewRecipe(internalName)
        }
    }

    fun fetchItemFromAvailableStorage() {
        if (checkIfInSignAndInsertAmount()) return
        if (getMissingAmountInInventory() <= 0) return

        GetFromSackApi.getFromSack(internalName, getMissingAmountInInventory().toInt())
    }

    fun removeItem() {
        if (topLevelItem != null) return
        topLevelCategory.remove(internalName)
        ShoppingList.update()
    }

    fun moveItemToTop(item: ShoppingListItem) {
        subItems.remove(item)
        subItems.add(0, item)
        ShoppingList.update()
    }

    fun moveThisToTop() {
        topLevelItem?.moveItemToTop(this) ?: topLevelCategory.moveItemToTop(this)
    }

    fun unhideCategory() {
        topLevelCategory.hidden = false
    }

    fun toggleHide(hideTree: Boolean = false, forceSetTo: Boolean? = null) {
        hidden = forceSetTo ?: !hidden
        if (hideTree) {
            subItems.forEach {
                it.toggleHide(true, forceSetTo ?: hidden)
            }
        }
        if (!hidden) {
            unhideCategory()
        }
        if (forceSetTo == null) {
            ShoppingList.update()
        }
    }

    fun getCopyContent(): Pair<String, String> =
        if (topLevelItem == null) { // try to copy into clipboard something that can be pasted into /shsladd
            if (topLevelCategory.name == "Items") {
                Pair(
                    "${internalName.asString()} ${amount.displayAmount()}",
                    "copied command pastable to clipboard",
                )
            } else {
                Pair(
                    "${internalName.asString()} ${amount.displayAmount()} ${topLevelCategory.name}",
                    "copied command pastable to clipboard",
                )
            }
        } else {
            if (topLevelCategory.name == "Items") {
                Pair(
                    "${amount.displayAmount()}x ${internalName.asString()} ${totalAmount.displayAmount()} " +
                        "(${topLevelItem.internalName.asString()} x${topLevelItem.amount.displayAmount()})",
                    "copied to clipboard",
                )
            } else {
                Pair(
                    "${amount.displayAmount()}x ${internalName.asString()} ${totalAmount.displayAmount()} " +
                        "(${topLevelItem.internalName.asString()} x${topLevelItem.amount.displayAmount()}) ${topLevelCategory.name}",
                    "copied to clipboard",
                )
            }
        }

    fun copyToClipboard() {
        val (copyContent, chatMessage) = getCopyContent()

        ClipboardUtils.copyToClipboard(copyContent)
        ChatUtils.chat(chatMessage)
    }

    private fun Double.displayAmount(): String {
        return if (this % 1 == 0.0) {
            this.toInt().toString()
        } else {
            this.toString()
        }
    }

    fun getDisplayRepresentation(indent: String): String {
        var text = "§8$indent"

        if (topLevelItem != null) {
            text += "§7${amount.displayAmount()}x "
        }

        text += "${internalName.repoItemName} §f${getCurrentAmount()}/${totalAmount.displayAmount()}"

        if (ShoppingList.config.showOverall) {
            ShoppingList.ItemsOverall.get(internalName)?.let {
                if (it.frequency > 1) {
                    text += " (${it.amount.displayAmount()} total over ${it.frequency} items)"
                }
            }
        }

        if (hasItems()) {
            text += " §a✓"
        } else if (hasAllSubItems()) {
            text += " §e✓"
        }

        return text
    }

    @Suppress("LongMethod")
    fun getClickLayout(): Pair<Map<ClickTypeWithModifiers, () -> Unit>, List<String>> {

        val clickLayout: MutableMap<ClickTypeWithModifiers, () -> Unit> = mutableMapOf()
        // TODO (maybe): make the tooltips be generated from the clickLayout
        val tooltip = mutableListOf<String>()

        // left click
        val buyTooltip: String? = if (internalName.isBazaarItem()) {
            " to search in Bazaar!"
        } else if (internalName.isAuctionHouseItem()) {
            " to search in Auction House!"
        } else {
            null
        }

        if (hasItems()) {
            clickLayout[ClickTypeWithModifiers(LEFT_MOUSE)] = { fetchItemFromAvailableStorage() }
            tooltip.add("§7left click to fetch from storage")
            clickLayout[ClickTypeWithModifiers(LEFT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { breakDownIntoSubitems() }
            tooltip.add("§7shift + left click to break down recipe")
        } else if (hasAllSubItems()) {
            clickLayout[ClickTypeWithModifiers(LEFT_MOUSE)] = { openCraftingRecipe() }
            tooltip.add("§7left click to open crafting recipe")
            clickLayout[ClickTypeWithModifiers(LEFT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { breakDownIntoSubitems() }
            tooltip.add("§7shift + left click to break down recipe")
        } else {
            if (downBreakable || buyTooltip == null || noTradeMode) {
                clickLayout[ClickTypeWithModifiers(LEFT_MOUSE)] = { breakDownIntoSubitems() }
                if (downBreakable) {
                    tooltip.add("§7left click to break down recipe")
                }
                clickLayout[ClickTypeWithModifiers(LEFT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { buyItem() }
                if (buyTooltip != null && !noTradeMode) {
                    tooltip.add("§7shift + left click$buyTooltip")
                }
            } else {
                clickLayout[ClickTypeWithModifiers(LEFT_MOUSE)] = { buyItem() }
                tooltip.add("§7left click$buyTooltip")
                clickLayout[ClickTypeWithModifiers(LEFT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { breakDownIntoSubitems() }
                tooltip.add("§7shift + left click to break down recipe")
            }
        }

        // right click
        if (topLevelItem == null) {
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE)] = { removeItem() }
            tooltip.add("§7right click to remove")
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { toggleHide() }
            tooltip.add("§7shift + right click to ${if (hidden) "un" else ""}hide")
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LCONTROL, Keyboard.KEY_LSHIFT))] = { toggleHide(true) }
            tooltip.add("§7ctrl + shift + right click to ${if (hidden) "un" else ""}hide tree")
        } else {
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE)] = { toggleHide() }
            tooltip.add("§7right click to ${if (hidden) "un" else ""}hide")
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LSHIFT))] = { toggleHide(true) }
            tooltip.add("§7shift + right click to ${if (hidden) "un" else ""}hide tree")
            clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LCONTROL, Keyboard.KEY_LSHIFT))] = { toggleHide(true) }
        }

        clickLayout[ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LCONTROL))] = { moveThisToTop() }
        tooltip.add("§7ctrl + right click to move to top")
        clickLayout[ClickTypeWithModifiers(MIDDLE_MOUSE)] = { copyToClipboard() }
        tooltip.add("§7middle click to copy to clipboard")

        // arrow keys
        if (topLevelItem == null) {
            clickLayout[ClickTypeWithModifiers(KEY_UP)] = {
                changeAmountBy(1.0)
                ShoppingList.update()
            }
            tooltip.add("§7up arrow to increase amount by 1")
            clickLayout[ClickTypeWithModifiers(KEY_DOWN)] = {
                changeAmountBy(-1.0)
                ShoppingList.update()
            }
            tooltip.add("§7down arrow to decrease amount by 1")

            var previousGoalAmount = 1.0
            var goalAmount: Double
            var flag = false
            for (i in 2..6) {
                goalAmount = (1 shl i).toDouble()
                if (amount < goalAmount) {
                    clickLayout[ClickTypeWithModifiers(KEY_UP, setOf(Keyboard.KEY_LSHIFT))] = {
                        changeAmountTo(goalAmount)
                        ShoppingList.update()
                    }
                    tooltip.add("§7shift + up arrow to set amount to ${goalAmount.displayAmount()}")

                    if (amount == previousGoalAmount) {
                        previousGoalAmount = previousGoalAmount / 2
                    }

                    if (previousGoalAmount > 1.0) {
                        clickLayout[ClickTypeWithModifiers(KEY_DOWN, setOf(Keyboard.KEY_LSHIFT))] = {
                            changeAmountTo(previousGoalAmount)
                            ShoppingList.update()
                        }
                        tooltip.add("§7shift + down arrow to set amount to ${previousGoalAmount.displayAmount()}")
                    }
                    flag = true
                    break
                }
                previousGoalAmount = goalAmount
            }

            if (!flag) {
                clickLayout[ClickTypeWithModifiers(KEY_UP, setOf(Keyboard.KEY_LSHIFT))] = {
                    changeAmountBy(64.0)
                    ShoppingList.update()
                }
                tooltip.add("§7shift + up arrow to increase amount by 64")

                if (amount == 64.0) {
                    clickLayout[ClickTypeWithModifiers(KEY_DOWN, setOf(Keyboard.KEY_LSHIFT))] = {
                        changeAmountTo(32.0)
                        ShoppingList.update()
                    }
                    tooltip.add("§7shift + down arrow to set amount to 32")
                } else {
                    clickLayout[ClickTypeWithModifiers(KEY_DOWN, setOf(Keyboard.KEY_LSHIFT))] = {
                        changeAmountBy(-64.0)
                        ShoppingList.update()
                    }
                    tooltip.add("§7shift + down arrow to decrease amount by 64")
                }
            }
        }

        return Pair(clickLayout, tooltip)
    }

    fun getRenderables(indent: String, continuedIndent: String? = null): List<Renderable> {
        val renderables = mutableListOf<Renderable>()
        if (!hidden || ShoppingList.isInventoryOpen()) {

            var text = getDisplayRepresentation(indent)

            if (hidden) {
                text = "§8${text.removeColor()}"
            }

            val (clickLayout, tooltip) = getClickLayout()

            renderables.add(
                Renderable.clickableWithModifiers(
                    text = text,
                    tips = tooltip,
                    onAnyClick = clickLayout,
                ),
            )
        }

        for (i in 0 until subItems.size) {
            val isLastItem = i == subItems.size - 1
            var newContinuedIndent = continuedIndent ?: indent

            var newIndent = continuedIndent ?: indent
            if (!isLastItem) {
                newIndent += "|·"
                newContinuedIndent += "| "
            } else {
                newIndent += "`·"
                newContinuedIndent += "  "
            }

            subItems[i].getRenderables(newIndent, newContinuedIndent).forEach {
                renderables.add(it)
            }
        }
        return renderables
    }
}
