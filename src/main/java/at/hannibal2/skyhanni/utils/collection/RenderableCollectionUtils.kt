package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import net.minecraft.item.ItemStack
import java.util.Collections

object RenderableCollectionUtils {

    fun MutableList<Renderable>.addString(
        text: String,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.string(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign))
    }

    fun MutableList<Searchable>.addSearchString(
        text: String,
        searchText: String? = null,
        horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    ) {
        add(Renderable.string(text, horizontalAlign = horizontalAlign, verticalAlign = verticalAlign).toSearchable(searchText))
    }

    fun MutableList<List<Renderable>>.addSingleString(text: String) {
        add(Collections.singletonList(Renderable.string(text)))
    }

    fun MutableList<Renderable>.addItemStack(
        itemStack: ItemStack,
        highlight: Boolean = false,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
    ) {
        if (highlight) {
            // Hack to add enchant glint, like Hypixel does it
            itemStack.addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
        }
        add(Renderable.itemStack(itemStack, scale = scale))
    }

    fun MutableList<Renderable>.addItemStack(internalName: NeuInternalName) {
        addItemStack(internalName.getItemStack())
    }

    fun Collection<Collection<Renderable>>.tableStretchXPadding(xSpace: Int): Int {
        if (this.isEmpty()) return xSpace
        val off = RenderableUtils.calculateTableXOffsets(this, 0)
        val xLength = off.size - 1
        val emptySpace = xSpace - off.last()
        return emptySpace / (xLength - 1)
    }

    fun Collection<Collection<Renderable>>.tableStretchYPadding(ySpace: Int): Int {
        if (this.isEmpty()) return ySpace
        val off = RenderableUtils.calculateTableYOffsets(this, 0)
        val yLength = off.size - 1
        val emptySpace = ySpace - off.last()
        return emptySpace / (yLength - 1)
    }

    fun MutableList<Renderable>.addHorizontalSpacer(width: Int = 3) {
        add(Renderable.placeholder(width, 0))
    }

    fun MutableList<Renderable>.addVerticalSpacer(height: Int = 10) {
        add(Renderable.placeholder(0, height))
    }
}
