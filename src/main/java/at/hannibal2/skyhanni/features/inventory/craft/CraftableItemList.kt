package at.hannibal2.skyhanni.features.inventory.craft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.isVanillaItem
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.toPrimitiveStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe
import io.github.moulberry.notenoughupdates.recipes.NeuRecipe
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor

class CraftableItemList {
    private val config get() = SkyHanniMod.feature.inventory.craftableItemList

    private var display = listOf<Renderable>()
    private var inInventory = false
    private val craftItemPattern by RepoPattern.pattern(
        "craftableitemlist.craftitem",
        "Craft Item"
    )

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (!craftItemPattern.matches(event.inventoryName)) return
        inInventory = true

        val pricePer = mutableMapOf<NEUInternalName, Double>()
        val lines = mutableMapOf<NEUInternalName, Renderable>()
        val avaliableMaterial = readItems()
        for (internalName in NEUItems.allInternalNames) {
            if (config.excludeVanillaItems && internalName.isVanillaItem()) continue

            val recipes = NEUItems.getRecipes(internalName)

            for (recipe in recipes) {
                if (recipe !is CraftingRecipe) continue
                val neededItems = neededItems(recipe, internalName.toString())
                // Just a fail save, should not happen normally
                if (neededItems.isEmpty()) continue
                val amount = canCraftAmount(neededItems, avaliableMaterial)
                if (amount > 0) {
                    pricePer[internalName] = pricePer(neededItems) * amount
                    lines[internalName] = Renderable.clickAndHover("§8x${amount.addSeparators()} ${internalName.itemName}",
                        tips = listOf("Click to craft ${internalName.itemName}!"),
                        onClick = {
                            ChatUtils.sendCommandToServer("recipe ${internalName.asString()}")
                        })
                }
            }
        }

        display = if (lines.isEmpty()) {
            listOf(Renderable.string("§7No Items to craft"))
        } else {
            val list = pricePer.sortedDesc().keys.map { lines[it] ?: error("impossible") }
            listOf(
                Renderable.string("§7Recipes: ${list.size} total"),
                Renderable.scrollList(list, height = 120),
            )
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    private fun pricePer(neededItems: MutableMap<NEUInternalName, Int>): Double =
        neededItems.map { it.key.getPrice() * it.value }.sum()

    private fun canCraftAmount(
        need: MutableMap<NEUInternalName, Int>,
        avaliable: MutableMap<NEUInternalName, Long>,
    ): Int {
        val canCraftTotal = mutableListOf<Int>()
        for ((name, neededAmount) in need) {
            val inventory = avaliable[name] ?: 0
            val sacks = if (config.includeSacks) name.getAmountInSacks() else 0
            val having = inventory + sacks
            val canCraft = floor(having.toDouble() / neededAmount).toInt()
            canCraftTotal.add(canCraft)
        }
        return canCraftTotal.min()
    }

    private fun neededItems(recipe: NeuRecipe, from: String): MutableMap<NEUInternalName, Int> {
        val neededItems = mutableMapOf<NEUInternalName, Int>()
        for (ingredient in recipe.ingredients) {
            val material = ingredient.internalItemId.asInternalName()
            val amount = ingredient.count.toInt()
            neededItems.addOrPut(material, amount)
        }
        return neededItems
    }

    private fun readItems(): MutableMap<NEUInternalName, Long> {
        val materials = mutableMapOf<NEUInternalName, Long>()
        for (stack in InventoryUtils.getItemsInOwnInventory()) {
            val item = stack.toPrimitiveStackOrNull() ?: continue
            materials.addOrPut(item.internalName, item.amount.toLong())
        }
        return materials
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        config.position.renderRenderables(display, posLabel = "Craft Materials From Bazaar")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
