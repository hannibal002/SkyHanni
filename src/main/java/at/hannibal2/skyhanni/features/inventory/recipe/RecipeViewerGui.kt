package at.hannibal2.skyhanni.features.inventory.recipe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_BACK
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_BG
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_CLOSE
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_HEADER
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_NAV_ACTIVE
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_NAV_INACTIVE
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_OUTLINE_BOT
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_OUTLINE_TOP
import at.hannibal2.skyhanni.features.inventory.recipe.layout.COLOR_SUBHEADER
import at.hannibal2.skyhanni.features.inventory.recipe.layout.CraftingRecipeLayout
import at.hannibal2.skyhanni.features.inventory.recipe.layout.ForgeRecipeLayout
import at.hannibal2.skyhanni.features.inventory.recipe.layout.GenericRecipeLayout
import at.hannibal2.skyhanni.features.inventory.recipe.layout.HA
import at.hannibal2.skyhanni.features.inventory.recipe.layout.KatUpgradeRecipeLayout
import at.hannibal2.skyhanni.features.inventory.recipe.layout.VA
import at.hannibal2.skyhanni.features.inventory.recipe.layout.scaledItem
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color

@SkyHanniModule
object RecipeViewerGui {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shviewrecipes") {
            description = "View NEU repo recipes for a given item"
            category = CommandCategory.DEVELOPER_TEST
            argCallback(
                "internalName",
                BrigadierArguments.greedyString(),
                BrigadierUtils.dynamicSuggestionProvider { EnoughUpdatesManager.getInternalNames().map { it.asString() } },
            ) { raw ->
                val internalName = raw.uppercase().toInternalName()
                if (EnoughUpdatesManager.getItemById(internalName) == null) {
                    ChatUtils.userError("Item §e$raw§c not found in the NEU repo.")
                    return@argCallback
                }
                if (EnoughUpdatesManager.getRecipesFor(internalName).isEmpty()) {
                    ChatUtils.chat("§e$raw §7has no recipes in the NEU repo.")
                }
                SkyHanniMod.shouldCloseScreen = false
                SkyHanniMod.screenToOpen = RecipeViewerScreen(internalName)
            }
        }
    }

    /**
     * Constructs the full display [Renderable] for the current state of [screen].
     * Stateless — all mutable state lives in [RecipeViewerScreen].
     */
    fun buildDisplay(screen: RecipeViewerScreen): Renderable {
        val recipes = when (screen.viewMode) {
            RecipeViewerScreen.RecipeViewMode.RECIPES_FOR -> EnoughUpdatesManager.getRecipesFor(screen.internalName)
            RecipeViewerScreen.RecipeViewMode.RECIPES_USING -> EnoughUpdatesManager.getRecipesUsing(screen.internalName)
        }.toList()
        val index = screen.recipeIndex.coerceIn(0, (recipes.size - 1).coerceAtLeast(0))

        val body = if (recipes.isEmpty()) buildNoRecipesPlaceholder(screen.internalName)
        else Renderable.vertical(spacing = 6, horizontalAlign = HA.CENTER) {
            add(buildNavRow(screen, recipes, index))
            add(buildRecipeRenderable(recipes[index], screen))
        }

        return Renderable.drawInsideFloatingRectWithBorder(
            Renderable.vertical(spacing = 8, horizontalAlign = HA.CENTER) {
                add(screen.internalName.buildHeaderRow())
                if (screen.viewMode == RecipeViewerScreen.RecipeViewMode.RECIPES_USING) Renderable.text(
                    "§8Viewing recipes using item",
                    scale = 0.8,
                    color = COLOR_SUBHEADER.toColor(),
                    horizontalAlign = HA.CENTER,
                ).let { add(it) }
                add(body)
                add(buildActionsRow(screen))
            },
            backgroundColor = COLOR_BG,
            lightColor = COLOR_OUTLINE_TOP,
            darkColor = COLOR_OUTLINE_BOT,
            padding = 10,
            radius = 12,
            smoothness = 2,
            borderThickness = 2,
        )
    }

    private fun NeuInternalName.buildHeaderRow() = Renderable.vertical(spacing = 4, horizontalAlign = HA.CENTER) {
        Renderable.horizontal(horizontalAlign = HA.CENTER, verticalAlign = VA.CENTER) {
            add(scaledItem())
        }.let { add(it) }
        Renderable.vertical(spacing = 2, horizontalAlign = HA.CENTER) {
            add(Renderable.text(repoItemName, scale = 1.4, color = COLOR_HEADER.toColor(), horizontalAlign = HA.CENTER))
            add(Renderable.text("§7${asString()}", scale = 0.8, color = COLOR_SUBHEADER.toColor(), horizontalAlign = HA.CENTER))
        }.let { add(it) }
    }

    private fun buildNavRow(screen: RecipeViewerScreen, recipes: List<PrimitiveRecipe>, currentIndex: Int): Renderable {
        val total = recipes.size

        fun navButton(label: String, active: Boolean, onClick: () -> Unit): Renderable {
            val color = if (active) COLOR_NAV_ACTIVE.toColor() else COLOR_NAV_INACTIVE.toColor()
            val text = Renderable.text(label, scale = 1.2, color = if (active) Color.WHITE else Color.GRAY)
            val button = Renderable.drawInsideRoundedRect(text, color, padding = 3, radius = 6)
            return if (active) Renderable.clickable(button, onClick, bypassChecks = true) else button
        }

        val prevButton = navButton("◄", currentIndex > 0) {
            screen.recipeIndex = currentIndex - 1
            screen.rebuildDisplay()
        }
        val nextButton = navButton("►", currentIndex < total - 1) {
            screen.recipeIndex = currentIndex + 1
            screen.rebuildDisplay()
        }

        val label = Renderable.vertical(spacing = 1, horizontalAlign = HA.CENTER) {
            val currentDisplayName = recipes.getOrNull(currentIndex) ?.recipeType?.displayName ?: "Unknown"
            add(Renderable.text("Recipe ${currentIndex + 1} / $total", color = Color.WHITE, horizontalAlign = HA.CENTER))
            add(Renderable.text("§7$currentDisplayName", scale = 0.8, color = COLOR_SUBHEADER.toColor(), horizontalAlign = HA.CENTER))
        }
        return Renderable.horizontal(spacing = 8, verticalAlign = VA.CENTER, horizontalAlign = HA.CENTER) {
            add(prevButton)
            add(label)
            add(nextButton)
        }
    }

    private fun buildRecipeRenderable(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable = when (recipe.recipeType) {
        NeuRecipeType.CRAFTING -> CraftingRecipeLayout.build(recipe, screen)
        NeuRecipeType.FORGE -> ForgeRecipeLayout.build(recipe, screen)
        NeuRecipeType.KAT_UPGRADE -> KatUpgradeRecipeLayout.build(recipe, screen)
        else -> GenericRecipeLayout.build(recipe, screen)
    }

    private fun buildNoRecipesPlaceholder(internalName: NeuInternalName) =
        Renderable.text("§cNo recipes found for §e${internalName.asString()}", color = Color.WHITE, horizontalAlign = HA.CENTER)

    private fun buildActionsRow(screen: RecipeViewerScreen) = Renderable.horizontal(spacing = 6, verticalAlign = VA.CENTER) {
        if (screen.canNavigateBack) add(buildBackButton(screen))
        add(buildCloseButton(screen))
    }

    private fun buildCloseButton(screen: RecipeViewerScreen) = Renderable.clickable(
        Renderable.drawInsideRoundedRectWithOutline(
            Renderable.text("Close", color = Color.WHITE),
            color = COLOR_CLOSE.toColor(),
            padding = 5,
            radius = 8,
            topOutlineColor = COLOR_CLOSE.toColor().brighter().rgb,
            bottomOutlineColor = COLOR_CLOSE.toColor().darker().rgb,
            borderOutlineThickness = 1,
        ),
        onLeftClick = { screen.onClose() },
        bypassChecks = true,
    )

    private fun buildBackButton(screen: RecipeViewerScreen) = Renderable.clickable(
        Renderable.drawInsideRoundedRectWithOutline(
            Renderable.text("◄ Back", color = Color.WHITE),
            color = COLOR_BACK.toColor(), padding = 5, radius = 8,
            topOutlineColor = COLOR_BACK.toColor().brighter().rgb,
            bottomOutlineColor = COLOR_BACK.toColor().darker().rgb,
            borderOutlineThickness = 1,
        ),
        onLeftClick = { screen.navigateBack() },
        bypassChecks = true,
    )
}
