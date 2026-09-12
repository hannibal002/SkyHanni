package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.CompactItemRecipeConfig
import at.hannibal2.skyhanni.data.jsonobjects.repo.CompactedItemFormRecipeEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.CompactedItemFormRecipesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.stackUnderCursor
import net.minecraft.network.chat.Component

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
            val itemName = stack.hoverName.formattedTextCompat()
            ChatUtils.chat("$itemName§c does not have a recipe for a compacted form.")
            return
        }

        when (recipe.type) {
            NeuRecipeType.CRAFTING -> HypixelCommands.viewRecipe(recipe.result)
            NeuRecipeType.FORGE -> sendForgeRecipeMessage(stack.hoverName, recipe.result.repoItemName)
            else -> ErrorManager.logErrorWithData(
                IllegalStateException("Unsupported NeuRecipeType"),
                "Unsupported recipe type for compacted form",
                "type" to recipe.type,
                "ingredient" to stack.getInternalName(),
                "result" to recipe.result,
            )
        }
    }

    private fun sendForgeRecipeMessage(ingredientName: Component, resultName: String) {
        val message = componentBuilder {
            append(ingredientName)
            append(" can be compacted into ")
            append(resultName)
            append(" at the Forge.")
        }.formattedTextCompat()

        when (config.forgeRecipeAction) {
            CompactItemRecipeConfig.ForgeRecipeAction.NONE -> ChatUtils.chat(message)
            CompactItemRecipeConfig.ForgeRecipeAction.WARP_FORGE -> {
                ChatUtils.clickableChat(
                    "$message Click §lHERE§r§e to warp to the Forge!",
                    { HypixelCommands.warp("forge") },
                    "§eClick to warp to the Forge!",
                )
            }
            CompactItemRecipeConfig.ForgeRecipeAction.CALL_FRED -> {
                ChatUtils.clickableChat(
                    "$message Click §lHERE§r§e to call Fred!",
                    { HypixelCommands.call("fred") },
                    "§eClick to call Fred!",
                )
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        recipes = event.getConstant<CompactedItemFormRecipesJson>("CompactedItemFormRecipes").recipes
    }
}
