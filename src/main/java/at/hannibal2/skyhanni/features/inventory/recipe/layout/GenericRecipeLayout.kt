package at.hannibal2.skyhanni.features.inventory.recipe.layout

import at.hannibal2.skyhanni.features.inventory.recipe.RecipeViewerScreen
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color

object GenericRecipeLayout : RecipeLayout {

    override fun build(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        val outputsPanel = panel("Outputs", recipe.outputs, screen)

        if (recipe.outputs.size <= 1) {
            return if (recipe.ingredients.size == 1) buildSingleIngredientNoOutput(recipe.ingredients.first(), screen)
            else panel("Inputs", recipe.ingredients, screen)
        }

        if (recipe.ingredients.size == 1) return buildSingleIngredientWithOutputs(recipe.ingredients.first(), outputsPanel, screen)

        return Renderable.horizontal(spacing = 8, verticalAlign = VA.TOP) {
            add(panel("Inputs", recipe.ingredients, screen))
            add(arrowRenderable())
            add(outputsPanel)
        }
    }

    private fun panel(title: String, ingredients: Collection<PrimitiveIngredient>, screen: RecipeViewerScreen) = Renderable.vertical(
        buildList {
            add(sectionLabel(title))
            if (ingredients.isEmpty()) add(Renderable.text("§7(none)", scale = 0.85))
            else addAll(ingredients.mapNotNull { buildIngredientRowOrNull(it, screen) })
        },
        spacing = 3,
    )

    private fun PrimitiveIngredient.buildWrappedText() = Renderable.wrappedText(
        internalName.repoItemName,
        setWidth = 80,
        scale = 0.8,
        color = Color.WHITE,
        horizontalAlign = HA.CENTER,
        internalAlign = HA.CENTER,
    )

    private fun buildSingleIngredientNoOutput(ingredient: PrimitiveIngredient, screen: RecipeViewerScreen) = Renderable.vertical(
        listOf(
            buildItemSlot(ingredient, screen),
            ingredient.buildWrappedText(),
        ),
        spacing = 2,
        horizontalAlign = HA.CENTER,
    )

    private fun buildSingleIngredientWithOutputs(
        ingredient: PrimitiveIngredient,
        outputsPanel: Renderable,
        screen: RecipeViewerScreen,
    ): Renderable {
        val slot = buildItemSlot(ingredient, screen)
        val topPad = ((outputsPanel.height - slot.height) / 2).coerceAtLeast(0)
        val inputColumn = Renderable.vertical(
            buildList {
                if (topPad > 0) add(Renderable.placeholder(0, topPad))
                add(slot)
                add(ingredient.buildWrappedText())
            },
            spacing = 2,
            horizontalAlign = HA.CENTER,
        )
        val centeredArrow = Renderable.fixedSizeColumn(
            object : Renderable by arrowRenderable() {
                override val verticalAlign = VA.CENTER
            },
            outputsPanel.height,
        )
        return Renderable.horizontal(listOf(inputColumn, centeredArrow, outputsPanel), spacing = 8, verticalAlign = VA.TOP)
    }
}
