package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableInventory
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack

class SkyHanniInventoryContainer(
    @Expose val internalName: String,
    @Expose val rowSize: Int,
    @Expose var items: List<ItemStack?>,
    @Expose var displayName: String = internalName,
) {
    fun toRenderable(scale: Double = 1.0): Renderable =
        VerticalContainerRenderable(
            listOf(
                StringRenderable(displayName, scale),
                RenderableInventory.fakeInventory(
                    items,
                    rowSize,
                    scale,
                ),
            ),
        )

    override fun toString() = internalName

    override fun equals(other: Any?): Boolean {
        if (other !is SkyHanniInventoryContainer) return false
        return internalName == other.internalName
    }

    override fun hashCode() = internalName.hashCode()

    fun getDebug() = buildList {
        add("internalName: $internalName")
        add("displayName: $displayName")
        add("items: ")
        buildString {
            items.forEachIndexed { index, itemStack ->
                append(itemStack?.repoItemName?.removeColor() ?: "empty")
                if (index % rowSize == rowSize - 1) {
                    add(" $this")
                    clear()
                } else {
                    append(", ")
                }
            }
        }
    }
}
