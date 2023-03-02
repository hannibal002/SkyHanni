package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import io.github.moulberry.notenoughupdates.NEUManager
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import io.github.moulberry.notenoughupdates.util.ItemResolutionQuery
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object NEUItems {
    val manager: NEUManager get() = NotEnoughUpdates.INSTANCE.manager
    private val itemCache = mutableMapOf<String, ItemStack>()
    private val itemNameCache = mutableMapOf<String, String>() // item name -> internal name
    private val multiplierCache = mutableMapOf<String, Pair<String, Int>>()
    private val recipesCache = mutableMapOf<String, List<NeuRecipe>>()

    fun getInternalName(itemName: String): String {
        if (itemNameCache.containsKey(itemName)) {
            return itemNameCache[itemName]!!
        }
        val internalName = ItemResolutionQuery.findInternalNameByDisplayName(itemName, false)
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
        if (result == -1.0) {
            if (internalName == "JACK_O_LANTERN") {
                return getPrice("PUMPKIN") + 1
            }
            if (internalName == "GOLDEN_CARROT") {
                // 6.8 for some players
                return 7.0 // NPC price
            }
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
        GlStateManager.scale(scale, scale, 0f)
        drawItemStack(this)
        GlStateManager.popMatrix()
    }

    private fun drawItemStack(stack: ItemStack) {
        val itemRender = Minecraft.getMinecraft().renderItem

        Utils.disableCustomDungColours = true
        RenderHelper.enableGUIStandardItemLighting()
        Utils.hasEffectOverride = true
        itemRender.renderItemAndEffectIntoGUI(stack, 0, 0)
        itemRender.renderItemOverlayIntoGUI(Minecraft.getMinecraft().fontRendererObj, stack, 0, 0, null)
        Utils.hasEffectOverride = false
        RenderHelper.disableStandardItemLighting()
        Utils.disableCustomDungColours = false
    }

    fun getMultiplier(rawId: String, tryCount: Int = 0, parent: String? = null): Pair<String, Int> {
        if (multiplierCache.contains(rawId)) {
            return multiplierCache[rawId]!!
        }
        if (tryCount == 10) {
            val message = "Error reading getMultiplier for item '$rawId'"
            Error(message).printStackTrace()
            LorenzUtils.error(message)
            return Pair(rawId, 1)
        }
        for (recipe in getRecipes(rawId)) {
            if (recipe !is CraftingRecipe) continue

            val map = mutableMapOf<String, Int>()
            for (ingredient in recipe.ingredients) {
                val count = ingredient.count.toInt()
                val internalItemId = ingredient.internalItemId
                val old = map.getOrDefault(internalItemId, 0)
                map[internalItemId] = old + count
            }
            if (map.size != 1) continue
            val current = map.iterator().next().toPair()
            val id = current.first
            return if (id != parent) {
                val child = getMultiplier(id, tryCount + 1, rawId)
                val result = Pair(child.first, child.second * current.second)
                multiplierCache[rawId] = result
                result
            } else {
                Pair(parent, 1)
            }
        }

        val result = Pair(rawId, 1)
        multiplierCache[rawId] = result
        return result
    }

    fun getRecipes(minionId: String): List<NeuRecipe> {
        if (recipesCache.contains(minionId)) {
            return recipesCache[minionId]!!
        }
        val recipes = manager.getAvailableRecipesFor(minionId)
        recipesCache[minionId] = recipes
        return recipes
    }
}