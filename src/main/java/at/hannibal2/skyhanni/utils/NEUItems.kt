package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import io.github.moulberry.notenoughupdates.NEUManager
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.util.ItemResolutionQuery
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object NEUItems {
    val manager: NEUManager get() = NotEnoughUpdates.INSTANCE.manager
    private val itemCache = mutableMapOf<String, ItemStack>()

    fun getInternalName(itemName: String): String {
        return ItemResolutionQuery.findInternalNameByDisplayName(itemName, false)
    }

    fun getInternalName(itemStack: ItemStack): String {
        return ItemResolutionQuery(manager)
            .withCurrentGuiContext()
            .withItemStack(itemStack)
            .resolveInternalName() ?: ""
    }

    fun getPrice(internalName: String, useSellingPrice: Boolean = false): Double {
        return manager.auctionManager.getBazaarOrBin(internalName, useSellingPrice)
    }

    fun getItemStack(internalName: String): ItemStack {
        if (itemCache.contains(internalName)) {
            return itemCache[internalName]!!.copy()
        }

        val itemStack = ItemResolutionQuery(manager)
            .withKnownInternalName(internalName)
            .resolveToItemStack()!!
        itemCache[internalName] = itemStack
        return itemStack.copy()
    }

    fun isVanillaItem(item: ItemStack) = manager.auctionManager.isVanillaItem(item.getInternalName())

    fun ItemStack.renderOnScreen(x: Float, y: Float) {
        GlStateManager.pushMatrix()
        val isSkull = item === Items.skull
        if (isSkull) {
            GlStateManager.translate(x - 2, y - 2, 0f)
        } else {
            GlStateManager.translate(x, y, 0f)
        }

        val scale = if (isSkull) 0.8f else 0.6f
        GlStateManager.scale(scale, scale, 1f)
        Utils.drawItemStack(this, 0, 0)
        GlStateManager.popMatrix()
    }
}