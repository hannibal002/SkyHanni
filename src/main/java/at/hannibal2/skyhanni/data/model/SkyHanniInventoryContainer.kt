package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.RenderableInventory.fakeInventory
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack

class SkyHanniInventoryContainer(
    @Expose val internalName: String,
    @Expose val rowSize: Int,
    @Expose var items: List<ItemStack?>,
    @Expose var displayName: String = internalName,
    @Expose val primaryCords: LorenzVec? = null,
    @Expose val secondaryCords: LorenzVec? = null,
) {
    fun toRenderable(scale: Double = 1.0): Renderable = with(Renderable) {
        vertical(
            text(displayName, scale),
            fakeInventory(
                items,
                rowSize,
                scale,
            ),
        )
    }

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
