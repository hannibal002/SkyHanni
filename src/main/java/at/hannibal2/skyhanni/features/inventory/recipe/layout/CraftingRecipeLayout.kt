package at.hannibal2.skyhanni.features.inventory.recipe.layout

import at.hannibal2.skyhanni.features.inventory.recipe.RecipeViewerScreen
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import java.awt.Color

object CraftingRecipeLayout : RecipeLayout {

    override fun build(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        val ingredients = recipe.ingredients.toList()
        val grid: Array<PrimitiveIngredient?> = Array(9) { ingredients.getOrNull(it) }
        val gridRenderable = grid.toList().chunked(3).map { row ->
            Renderable.horizontal(row.map { buildItemSlot(it, screen) }, spacing = GRID_SPACING)
        }.let { Renderable.vertical(it, spacing = GRID_SPACING) }

        val rawOutput = buildItemSlot(recipe.outputs.firstOrNull(), screen, isOutput = true)
        val outputName = recipe.outputs.firstOrNull()?.internalName?.repoItemName
        val outputColumn = if (outputName != null) {
            Renderable.vertical(spacing = 2, horizontalAlign = HA.CENTER) {
                add(rawOutput)
                add(Renderable.wrappedText(outputName, setWidth = 80, scale = 0.8, color = Color.WHITE))
            }
        } else rawOutput
        val output = Renderable.fixedSizeColumn(
            object : Renderable by outputColumn {
                override val verticalAlign = VA.CENTER
            },
            gridRenderable.height.coerceAtLeast(outputColumn.height),
        )
        val craftingRow = Renderable.horizontal(listOf(gridRenderable, arrowRenderable(), output), spacing = 6, verticalAlign = VA.CENTER)

        val aggregated = ingredients.groupBy { it.internalName }.map { (name, group) ->
            PrimitiveIngredient(name, group.sumOf { it.count })
        }.takeIfNotEmpty() ?: return craftingRow

        val ingredientList = Renderable.vertical(spacing = 3) {
            add(sectionLabel("Ingredients"))
            addAll(aggregated.mapNotNull { buildIngredientTextOrNull(it) })
        }
        return Renderable.vertical(listOf(craftingRow, ingredientList), spacing = 8, horizontalAlign = HA.CENTER)
    }
}
