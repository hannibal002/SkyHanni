package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.CompactedItemFormRecipeEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.CompactedItemFormRecipesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor

@SkyHanniModule
object CompactedItemFormRecipe {
    private val config get() = SkyHanniMod.feature.inventory.compactItemFormRecipe
    private var recipes = mapOf<NeuInternalName, CompactedItemFormRecipeEntry>()

    @HandleEvent
    fun onGuiKeyPress() {
        if (!config.keybind.isKeyHeld()) return
        val stack = stackUnderCursor() ?: return
        val item = stack.getInternalNameOrNull() ?: return
        val recipe = recipes[item]

        if (recipe == null) {
            ChatUtils.chat(
                componentBuilder {
                    append(stack.hoverName)
                    appendWithColor(" does not have a recipe for a compacted form!", LorenzColor.RED.toChatFormatting())
                }
            )
            return
        }
        
        when (recipe.type) {
            NeuRecipeType.CRAFTING -> HypixelCommands.viewRecipe(recipe.result)
            NeuRecipeType.FORGE -> ChatUtils.chat(
                componentBuilder {
                    append(stack.hoverName)
                    append(" can be compacted into ")
                    append(recipe.result.repoItemName)
                    append(" at the Forge.")
                }
            )
            else -> {}
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        recipes = event.getConstant<CompactedItemFormRecipesJson>("CompactedItemFormRecipes").recipes
    }
}
