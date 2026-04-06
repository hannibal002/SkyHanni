package at.hannibal2.skyhanni.features.inventory.recipe.layout

import at.hannibal2.skyhanni.features.inventory.recipe.RecipeViewerScreen
import at.hannibal2.skyhanni.utils.DurationPrimitiveRecipe
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color

/**
 * Shared layout for recipes with a single timed operation (forge, kat upgrade).
 * Subclasses declare [timeLabel] and whether [alwaysShowOutput].
 */
abstract class DurationRecipeLayout : RecipeLayout {

    abstract val timeLabel: String
    open val alwaysShowOutput: Boolean get() = false

    override fun build(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        require(recipe is DurationPrimitiveRecipe)

        val inputsPanel = Renderable.vertical(spacing = 3) {
            add(sectionLabel("Ingredients"))
            addAll(recipe.ingredients.mapNotNull { buildIngredientRowOrNull(it, screen) })
        }
        val timeRenderable = Renderable.vertical(spacing = 2, horizontalAlign = HA.CENTER) {
            add(sectionLabel(timeLabel))
            add(Renderable.text("§e${recipe.duration.format()}", scale = 0.85, color = Color.WHITE, horizontalAlign = HA.CENTER))
        }

        if (!alwaysShowOutput && recipe.outputs.size <= 1 && screen.viewMode != RecipeViewerScreen.RecipeViewMode.RECIPES_USING) {
            return Renderable.vertical(listOf(inputsPanel, timeRenderable), spacing = 8, horizontalAlign = HA.CENTER)
        }

        val centeredArrow = Renderable.fixedSizeColumn(
            object : Renderable by arrowRenderable() {
                override val verticalAlign = VA.CENTER
            },
            inputsPanel.height,
        )
        val mainRow = Renderable.horizontal(
            listOf(inputsPanel, centeredArrow, buildOutputPanel(recipe, screen, inputsPanel.height)),
            spacing = 8,
            verticalAlign = VA.TOP,
        )
        return Renderable.vertical(listOf(mainRow, timeRenderable), spacing = 8, horizontalAlign = HA.CENTER)
    }

    /**
     * For a single output, the item is vertically centred within [containerHeight].
     */
    private fun buildOutputPanel(recipe: PrimitiveRecipe, screen: RecipeViewerScreen, containerHeight: Int): Renderable {
        return if (recipe.outputs.size == 1) {
            val rawOutput = buildItemSlot(recipe.outputs.first(), screen, isOutput = true)
            val repoItemName = recipe.outputs.first().internalName.repoItemName
            val nameText = Renderable.wrappedText(repoItemName, setWidth = 80, scale = 0.8, color = Color.WHITE)
            val itemWithName = Renderable.vertical(listOf(rawOutput, nameText), spacing = 2, horizontalAlign = HA.CENTER)
            val centeredContent = Renderable.fixedSizeColumn(
                object : Renderable by itemWithName {
                    override val verticalAlign = VA.CENTER
                },
                containerHeight.coerceAtLeast(itemWithName.height),
            )
            Renderable.vertical(listOf(centeredContent), spacing = 3, horizontalAlign = HA.CENTER)
        } else Renderable.vertical(spacing = 3, horizontalAlign = HA.CENTER) {
            addAll(recipe.outputs.mapNotNull { buildIngredientRowOrNull(it, screen) })
        }
    }
}

object ForgeRecipeLayout : DurationRecipeLayout() {
    override val timeLabel = "Forge time"
}

object KatUpgradeRecipeLayout : DurationRecipeLayout() {
    override val timeLabel = "Upgrade time"
    override val alwaysShowOutput = true
}
