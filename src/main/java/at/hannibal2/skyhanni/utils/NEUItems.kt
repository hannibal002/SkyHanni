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
    private val itemNameCache = mutableMapOf<String, String>() // item name -> internal name

    fun getInternalName(itemName: String): String {
        if (itemNameCache.containsKey(itemName)) {
            return itemNameCache[itemName]!!
        }
        // We love hypixel naming moments
        // TODO remove workaround
        val name = if (itemName.contains("Jack o' Lantern")) {
            itemName.replace("Jack o' Lantern", "Jack o'Lantern")
        } else itemName
        val internalName = ItemResolutionQuery.findInternalNameByDisplayName(name, false)
        itemNameCache[itemName] = internalName
        return internalName
    }

    fun getInternalName(itemStack: ItemStack): String {
        return ItemResolutionQuery(manager)
            .withCurrentGuiContext()
            .withItemStack(itemStack)
            .resolveInternalName() ?: ""
    }

    fun getPrice(internalName: String, useSellingPrice: Boolean = false): Double {
        val result = manager.auctionManager.getBazaarOrBin(internalName, useSellingPrice)
        // TODO remove workaround
        if (internalName == "JACK_O_LANTERN" && result == -1.0) {
            return getPrice("PUMPKIN") + 1
        }
        return result
    }

    fun getItemStack(internalName: String): ItemStack {
        if (itemCache.contains(internalName)) {
            return itemCache[internalName]!!.copy()
        }

        val itemStack = ItemResolutionQuery(manager)
            .withKnownInternalName(internalName)
            .resolveToItemStack()
        if (itemStack == null) {
            val error = "ItemResolutionQuery returns null for internalName $internalName"
            LorenzUtils.error(error)
            throw RuntimeException(error)
        }
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