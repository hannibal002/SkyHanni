package at.hannibal2.skyhanni.test.repoitem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.RepoItemEditorConfig
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveIngredient.Companion.toPrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RecipeType
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import net.minecraft.item.ItemStack

@SkyHanniModule
object NpcShopExporter {
    val config get(): RepoItemEditorConfig = SkyHanniMod.feature.dev.devTool.repoItemEditor

    private val patternGroup = RepoPattern.group("dev.repoitemeditor")

    private val clickToTradeLine by patternGroup.pattern(
        "clicktotrade",
        "§eClick to trade!",
    )

    private var inNpcShop = false
    private var display = emptyList<Renderable>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!RepoItemEditor.config.editModeEnabled) return
        val shopSlot = event.inventorySize - 5
        val shopStack = event.inventoryItems[shopSlot]?.orNull() ?: return
        if (!UtilsPatterns.lastLoreLineOfSellPattern.matches(shopStack.getLore().lastOrNull())) return
        inNpcShop = true

        processInventoryAsNpcShop(event.inventoryItems.values.toList())
    }

    fun processCurrentlyOpenInventory() {
        if (display.isNotEmpty()) return
        val inventoryItems = InventoryUtils.getItemsInOpenChest().mapNotNull { it.stack.orNull() }
        processInventoryAsNpcShop(inventoryItems)
        if (display.isEmpty()) {
            ChatUtils.chat("§cNo Npc Trades found in the current inventory!")
        } else {
            inNpcShop = true
        }
    }

    private fun processInventoryAsNpcShop(inventoryItems: List<ItemStack>) {
        val recipes: MutableList<PrimitiveRecipe> = mutableListOf()

        for (item in inventoryItems) {
            val recipe = parseRecipeFromItem(item) ?: continue
            recipes.add(recipe)
        }

        if (recipes.isEmpty()) return
        val clickable = Renderable.clickable(
            "§aClick to export recipes",
            onLeftClick = { exportRecipes(recipes) },
        )
        display = buildList {
            add(RenderableString("§aNpc Shop Exporter"))
            add(RenderableString("§7Found ${recipes.size} Npc Shop Recipes"))
            add(clickable)
        }
    }

    private fun exportRecipes(recipes: List<PrimitiveRecipe>) {
        val nearbyMobs = MobData.currentMobs.sortedBy { it.distanceToPlayer() }
        val closestMob = nearbyMobs.firstOrNull { it.mobType == Mob.Type.DISPLAY_NPC }
        if (closestMob == null) {
            ChatUtils.chat("§cNo NPC found nearby to export recipes!")
            return
        }
        val npcData = NpcLocationExporter.processMobAsNpc(closestMob)
        val json = npcData.repoItemJson

        val recipesArray = JsonArray()
        for (recipe in recipes) {
            recipesArray.add(recipe.asRepoJson())
        }
        json.add("recipes", recipesArray)
        RepoItemEditor.saveItemToRepo(npcData.npcInternalName, json)
        display = buildList {
            add(RenderableString("§aNpc Shop Exporter"))
            add(RenderableString("§7Exported ${recipes.size} Npc Shop Recipes to repo folder!"))
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inNpcShop = false
        display = emptyList()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!inNpcShop) return

        config.displayPosition.renderRenderables(display, posLabel = "Npc Shop Exporter")
    }

    private fun parseRecipeFromItem(item: ItemStack): PrimitiveRecipe? {
        val resultItem = item.toPrimitiveIngredient()
        if (resultItem.internalName == NeuInternalName.NONE) return null
        var inCost = false
        val cost = mutableSetOf<PrimitiveIngredient>()

        loop@ for (line in item.getLore()) {
            if (UtilsPatterns.costLinePattern.matches(line)) {
                inCost = true
                continue
            }
            if (inCost && clickToTradeLine.matches(line)) {
                break
            }
            if (!inCost) continue
            if (line.isBlank()) break

            UtilsPatterns.coinsPattern.matchMatcher(line) {
                cost.add(PrimitiveIngredient.coinIngredient(group("coins").formatInt().toDouble()))
                continue@loop
            }

            val (itemName, amount) = ItemUtils.readItemAmount(line) ?: continue

            val internalName = NeuInternalName.fromItemName(itemName)
            cost.add(PrimitiveIngredient(internalName, amount.toDouble()))
        }
        return PrimitiveRecipe(cost, setOf(resultItem), RecipeType.NPC_SHOP)
    }

}
