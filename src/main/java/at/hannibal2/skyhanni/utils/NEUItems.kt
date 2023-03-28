package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import io.github.moulberry.notenoughupdates.NEUManager
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import io.github.moulberry.notenoughupdates.util.ItemResolutionQuery
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object NEUItems {
    private val manager: NEUManager get() = NotEnoughUpdates.INSTANCE.manager
    private val itemCache = mutableMapOf<String, ItemStack>()
    private val itemNameCache = mutableMapOf<String, String>() // item name -> internal name
    private val multiplierCache = mutableMapOf<String, Pair<String, Int>>()
    private val recipesCache = mutableMapOf<String, Set<NeuRecipe>>()

    fun getInternalName(itemName: String): String {
        if (itemNameCache.containsKey(itemName)) {
            return itemNameCache[itemName]!!
        }
        var internalName = ItemResolutionQuery.findInternalNameByDisplayName(itemName, false)
        // This fixes a NEU bug with §9Hay Bale (cosmetic item)
        // TODO remove workaround when this is fixed in neu
        if (internalName == "HAY_BALE") {
            internalName = "HAY_BLOCK"
        }
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

    fun getItemStackOrNull(internalName: String): ItemStack? {
        if (itemCache.contains(internalName)) {
            return itemCache[internalName]!!.copy()
        }

        val itemStack = ItemResolutionQuery(manager)
            .withKnownInternalName(internalName)
            .resolveToItemStack() ?: return null
        itemCache[internalName] = itemStack
        return itemStack.copy()
    }

    fun getItemStack(internalName: String): ItemStack {
        val stack = getItemStackOrNull(internalName)
        if (stack == null) {
            val error = "ItemResolutionQuery returns null for internalName $internalName"
            LorenzUtils.error(error)
            throw RuntimeException(error)
        }
        return stack
    }

    fun isVanillaItem(item: ItemStack) = manager.auctionManager.isVanillaItem(item.getInternalName())

    fun ItemStack.renderOnScreen(x: Float, y: Float, scaleMultiplier: Double = 1.0) {
        val isSkull = item === Items.skull

        val baseScale = (if (isSkull) 0.8f else 0.6f)
        val finalScale = baseScale * scaleMultiplier
        val diff = ((finalScale - baseScale) * 10).toFloat()

        val translateX: Float
        val translateY: Float
        if (isSkull) {
            translateX = x - 2 - diff
            translateY = y - 2 - diff
        } else {
            translateX = x - diff
            translateY = y - diff
        }

        GlStateManager.pushMatrix();

        GlStateManager.translate(translateX, translateY, 1F)
        GlStateManager.scale(finalScale, finalScale, 1.0)

        RenderHelper.enableGUIStandardItemLighting()
        Minecraft.getMinecraft().renderItem.renderItemIntoGUI(this, 0, 0)
        RenderHelper.disableStandardItemLighting()

        GlStateManager.popMatrix()
    }

    fun getMultiplier(rawId: String, tryCount: Int = 0): Pair<String, Int> {
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
                var internalItemId = ingredient.internalItemId
                // ignore cactus green
                if (rawId == "ENCHANTED_CACTUS_GREEN") {
                    if (internalItemId == "INK_SACK-2") {
                        internalItemId = "CACTUS"
                    }
                }

                // ignore wheat in enchanted cookie
                if (rawId == "ENCHANTED_COOKIE") {
                    if (internalItemId == "WHEAT") {
                        continue
                    }
                }

                // ignore golden carrot in enchanted golden carrot
                if (rawId == "ENCHANTED_GOLDEN_CARROT") {
                    if (internalItemId == "GOLDEN_CARROT") {
                        continue
                    }
                }

//                println("")
//                println("rawId: $rawId")
//                println("internalItemId: $internalItemId")

                val old = map.getOrDefault(internalItemId, 0)
                map[internalItemId] = old + count
            }
            if (map.size != 1) continue
            val current = map.iterator().next().toPair()
            val id = current.first
            return if (current.second > 1) {
                val child = getMultiplier(id, tryCount + 1)
                val result = Pair(child.first, child.second * current.second)
                multiplierCache[rawId] = result
                result
            } else {
                Pair(rawId, 1)
            }
        }

        val result = Pair(rawId, 1)
        multiplierCache[rawId] = result
        return result
    }

    fun getRecipes(minionId: String): Set<NeuRecipe> {
        if (recipesCache.contains(minionId)) {
            return recipesCache[minionId]!!
        }
        val recipes = manager.getRecipesFor(minionId)
        recipesCache[minionId] = recipes
        return recipes
    }
}