package at.hannibal2.skyhanni.features.inventory.shoppinglist

import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.ClickTypeWithModifiers
import net.minecraft.item.ItemStack
import org.lwjgl.input.Keyboard

class ShoppingListCategory(
    val name: String,
    val color: LorenzColor = LorenzColor.GOLD,
    val displayCondition: () -> Boolean = { true }, // TODO: later (maybe): implement only in area somehow
    // TODO later maybe?: implement icons
    var hidden: Boolean = false,
) {
    val items = mutableListOf<ShoppingListItem>()

    val clickLayout: MutableMap<ClickTypeWithModifiers, () -> Unit> = mutableMapOf(
        ClickTypeWithModifiers(LEFT_MOUSE) to { },
        ClickTypeWithModifiers(RIGHT_MOUSE) to { ShoppingList.removeCategory(this) },
        ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LSHIFT)) to { toggleHide() },
        ClickTypeWithModifiers(RIGHT_MOUSE, setOf(Keyboard.KEY_LCONTROL)) to { moveThisToTop() },
    )

    /*
    TODO later: make this all configurable
    what do we want to be able to do from the display widget:
        - (right click) remove it
        - (shift + right click) hide/unhide it along with tree
        - (ctrl + right click) move to top

    what may we want to see:
        - name
        - optional icon?
        - TODO: total cost
     */

    override fun toString(): String {
        return name + " (" + items.size + " items)"
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    fun remove(item: ShoppingListItem, amount: Double? = null) {
        if (amount == null) {
            items.remove(item)
        } else {
            item.changeAmountBy(-amount)
            if (item.amount <= 0.0) {
                items.remove(item)
            }
        }
    }

    fun clear() {
        items.clear()
    }

    fun contains(itemName: NeuInternalName): Boolean {
        return items.any { it.internalName == itemName }
    }

    fun onItemClicked(clickedItem: ItemStack): Boolean {
        items.forEach {
            if (it.onItemClick(clickedItem)) {
                return true
            }
        }
        return false
    }

    fun toggleHide() {
        hidden = !hidden
        items.forEach {
            it.toggleHide(true, hidden)
        }
        ShoppingList.update()
    }

    fun moveItemToTop(item: ShoppingListItem) {
        items.remove(item)
        items.add(0, item)
        ShoppingList.update()
    }

    fun moveThisToTop() {
        ShoppingList.moveCategoryToTop(this)
    }

    fun getRenderables(indent: Int, showThis: Boolean = true): List<Renderable> {
        val renderables = mutableListOf<Renderable>()

        if ((!hidden || ShoppingList.isInventoryOpen()) && displayCondition()) {
            if (showThis) {
                var text = ""
                val tooltip = mutableListOf<String>()

                text += if (!hidden) color.getChatColor() else "§8"
                text += "§n$name"

                tooltip.add("§7Right click to remove")
                tooltip.add("§7Shift + right click to ${if (hidden) "un" else ""}hide")
                tooltip.add("§7Ctrl + right click to move to top")

                renderables.add(
                    Renderable.clickableWithModifiers(
                        text = text,
                        tips = tooltip,
                        onAnyClick = clickLayout.toMap(),
                    ),
                )
            }

            items.forEach { item ->
                renderables.addAll(item.getRenderables("  ".repeat(indent)))
            }
        }
        return renderables
    }
}
