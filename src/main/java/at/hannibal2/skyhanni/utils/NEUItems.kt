package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object NEUItems {

    private val itemCache = mutableMapOf<String, ItemStack>()

    fun readItemFromRepo(internalName: String): ItemStack {
        if (itemCache.contains(internalName)) {
            return itemCache[internalName]!!
        }
        val itemStack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(
            NotEnoughUpdates.INSTANCE.manager.itemInformation[internalName]
        )
        if (itemStack != null) {
            itemCache[internalName] = itemStack
        }
        return itemStack
    }

    fun ItemStack.renderOnScreen(x: Float, y: Float) {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)

        val scale = if (item === Items.skull) 0.8f else 0.6f
        GlStateManager.scale(scale, scale, 1f)
        Utils.drawItemStack(this, 0, 0)
        GlStateManager.popMatrix()
    }
}