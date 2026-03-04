package at.hannibal2.skyhanni.features.inventory.recipe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color

private typealias HA = RenderUtils.HorizontalAlignment
private typealias VA = RenderUtils.VerticalAlignment

@SkyHanniModule
object RecipeViewerGui {

    private const val GRID_SPACING = 2
    private const val PANEL_PADDING = 10

    private const val ITEM_SCALE = 2.5
    private fun scaledItem(internalName: NeuInternalName, withTip: Boolean = true) = Renderable.item(
        provider = NeuItemStackProvider(internalName),
        scale = ITEM_SCALE
    ).let { if (withTip) it.withTip() else it }

    // Derive slot size from the actual rendered item dimensions so empty and filled slots always match.
    private val SLOT_SIZE by lazy {
        scaledItem(NeuInternalName.MISSING_ITEM).let {
            it.width.coerceAtLeast(it.height)
        }
    }

    private val COLOR_BG = ChromaColour.fromStaticRGB(20, 20, 30, 230)
    private val COLOR_SLOT_EMPTY = ChromaColour.fromStaticRGB(40, 40, 55, 200)
    private val COLOR_SLOT_FILLED = ChromaColour.fromStaticRGB(55, 55, 75, 220)
    private val COLOR_HEADER = ChromaColour.fromStaticRGB(200, 200, 255, 255)
    private val COLOR_SUBHEADER = ChromaColour.fromStaticRGB(150, 150, 190, 255)
    private val COLOR_ARROW = ChromaColour.fromStaticRGB(100, 220, 100, 255)
    private val COLOR_NAV_ACTIVE = ChromaColour.fromStaticRGB(80, 130, 255, 220)
    private val COLOR_NAV_INACTIVE = ChromaColour.fromStaticRGB(50, 50, 70, 180)
    private val COLOR_CLOSE = ChromaColour.fromStaticRGB(200, 60, 60, 210)
    private val COLOR_OUTLINE_TOP = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val COLOR_OUTLINE_BOT = ChromaColour.fromStaticRGB(60, 60, 100, 255)

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
                    // Still open so the user can see the item info even without recipes.
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
        val recipes = EnoughUpdatesManager.getRecipesFor(screen.internalName).toList()
        val index = screen.recipeIndex.coerceIn(0, (recipes.size - 1).coerceAtLeast(0))

        val body = if (recipes.isEmpty()) {
            buildNoRecipesPlaceholder(screen.internalName)
        } else {
            Renderable.vertical(
                listOf(buildNavRow(screen, recipes, index), buildRecipeRenderable(recipes[index])),
                spacing = 6,
                horizontalAlign = HA.CENTER,
            )
        }

        val inner = Renderable.vertical(
            listOf(buildHeaderRow(screen.internalName), body, buildCloseButton(screen)),
            spacing = 8,
            horizontalAlign = HA.CENTER,
        )

        return Renderable.drawInsideFloatingRectWithBorder(
            inner,
            backgroundColor = COLOR_BG,
            lightColor = COLOR_OUTLINE_TOP,
            darkColor = COLOR_OUTLINE_BOT,
            padding = PANEL_PADDING,
            radius = 12,
            smoothness = 2,
            borderThickness = 2,
        )
    }

    private fun buildHeaderRow(internalName: NeuInternalName): Renderable {
        val texts = Renderable.vertical(spacing = 2, horizontalAlign = HA.CENTER) {
            add(Renderable.text(internalName.repoItemName, scale = 1.4, color = COLOR_HEADER.toColor(), horizontalAlign = HA.CENTER))
            val internalFormat = "§7${internalName.asString()}"
            add(Renderable.text(internalFormat, scale = 0.8, color = COLOR_SUBHEADER.toColor(), horizontalAlign = HA.CENTER))
        }
        return Renderable.horizontal(
            listOf(scaledItem(internalName, withTip = false), texts),
            spacing = 6,
            verticalAlign = VA.CENTER,
        )
    }

    private fun buildNavRow(screen: RecipeViewerScreen, recipes: List<PrimitiveRecipe>, currentIndex: Int): Renderable {
        val total = recipes.size

        fun navButton(label: String, active: Boolean, onClick: () -> Unit): Renderable {
            val color = if (active) COLOR_NAV_ACTIVE.toColor() else COLOR_NAV_INACTIVE.toColor()
            val text = Renderable.text(label, scale = 1.2, color = if (active) Color.WHITE else Color.GRAY)
            val button = Renderable.drawInsideRoundedRect(text, color, padding = 3, radius = 6)
            return if (active) Renderable.clickable(button, onClick) else button
        }

        val prevButton = navButton("◄", currentIndex > 0) {
            screen.recipeIndex = currentIndex - 1
            screen.rebuildDisplay()
        }
        val nextButton = navButton("►", currentIndex < total - 1) {
            screen.recipeIndex = currentIndex + 1
            screen.rebuildDisplay()
        }

        val currentType = recipes[currentIndex].recipeType
        val label = Renderable.vertical(
            listOf(
                Renderable.text("Recipe ${currentIndex + 1} / $total", color = Color.WHITE, horizontalAlign = HA.CENTER),
                Renderable.text("§7$currentType", scale = 0.8, color = COLOR_SUBHEADER.toColor(), horizontalAlign = HA.CENTER),
            ),
            spacing = 1,
            horizontalAlign = HA.CENTER,
        )

        return Renderable.horizontal(listOf(prevButton, label, nextButton), spacing = 8, verticalAlign = VA.CENTER)
    }

    private fun buildRecipeRenderable(recipe: PrimitiveRecipe): Renderable = when (recipe.recipeType) {
        NeuRecipeType.CRAFTING -> buildCraftingLayout(recipe)
        NeuRecipeType.FORGE -> buildForgeLayout(recipe)
        else -> buildGenericLayout(recipe)
    }

    private fun buildCraftingLayout(recipe: PrimitiveRecipe): Renderable {
        // Ingredients are ordered by slot position; fill remaining slots with null (empty).
        val ingredients = recipe.ingredients.toList()
        val grid: Array<PrimitiveIngredient?> = Array(9) { ingredients.getOrNull(it) }
        val gridRenderable = grid.toList().chunked(3).map { row ->
            Renderable.horizontal(row.map { buildItemSlot(it) }, spacing = GRID_SPACING)
        }.let { Renderable.vertical(it, spacing = GRID_SPACING) }

        val arrow = Renderable.text(" ──► ", scale = 1.3, color = COLOR_ARROW.toColor(), horizontalAlign = HA.CENTER)
        val output = buildItemSlot(recipe.outputs.firstOrNull())

        return Renderable.horizontal(listOf(gridRenderable, arrow, output), spacing = 6, verticalAlign = VA.CENTER)
    }

    private fun buildForgeLayout(recipe: PrimitiveRecipe): Renderable {
        val inputsPanel = Renderable.vertical(
            buildList {
                add(sectionLabel("Ingredients"))
                addAll(recipe.ingredients.map { buildIngredientRow(it) })
            },
            spacing = 3,
        )
        val outputPanel = Renderable.vertical(
            listOf(sectionLabel("Output"), buildItemSlot(recipe.outputs.firstOrNull())),
            spacing = 4,
            horizontalAlign = HA.CENTER,
        )
        val arrow = Renderable.text(" ──► ", scale = 1.3, color = COLOR_ARROW.toColor(), horizontalAlign = HA.CENTER)

        return Renderable.horizontal(listOf(inputsPanel, arrow, outputPanel), spacing = 8, verticalAlign = VA.CENTER)
    }

    private fun buildGenericLayout(recipe: PrimitiveRecipe): Renderable {
        fun panel(title: String, ingredients: Set<PrimitiveIngredient>) = Renderable.vertical(
            buildList {
                add(sectionLabel(title))
                if (ingredients.isEmpty()) add(Renderable.text("§7(none)", scale = 0.85))
                else addAll(ingredients.map { buildIngredientRow(it) })
            },
            spacing = 3,
        )

        val arrow = Renderable.text(" ──► ", scale = 1.3, color = COLOR_ARROW.toColor(), horizontalAlign = HA.CENTER)

        return Renderable.horizontal(
            listOf(panel("Inputs", recipe.ingredients), arrow, panel("Output", recipe.outputs)),
            spacing = 8,
            verticalAlign = VA.CENTER,
        )
    }

    /**
     * A single item slot: a fixed-size dark square containing an item icon with tooltip.
     * Empty slots render as a dimmer square with no content.
     */
    private fun buildItemSlot(ingredient: PrimitiveIngredient?): Renderable {
        if (ingredient == null) {
            return Renderable.drawInsideRoundedRect(
                Renderable.placeholder(SLOT_SIZE, SLOT_SIZE),
                COLOR_SLOT_EMPTY.toColor(),
                padding = 0,
                radius = (4 * ITEM_SCALE).toInt(),
            )
        }

        // withTip() wraps the item in a hoverTips using the item's own vanilla tooltip.
        val itemWithTip = scaledItem(ingredient.internalName)

        return Renderable.drawInsideRoundedRect(itemWithTip, COLOR_SLOT_FILLED.toColor(), padding = 0, radius = 4)
    }

    private fun buildIngredientRow(ingredient: PrimitiveIngredient): Renderable {
        val name = ingredient.internalName.repoItemName
        val label = Renderable.text("$name §7×${ingredient.count.toInt().addSeparators()}", scale = 0.9, color = Color.WHITE)
        return Renderable.horizontal(listOf(buildItemSlot(ingredient), label), spacing = 4, verticalAlign = VA.CENTER)
    }

    private fun sectionLabel(text: String) =
        Renderable.text("§7$text:", scale = 0.85, color = COLOR_SUBHEADER.toColor())

    private fun buildNoRecipesPlaceholder(internalName: NeuInternalName) =
        Renderable.text("§cNo recipes found for §e${internalName.asString()}", color = Color.WHITE, horizontalAlign = HA.CENTER)

    private fun buildCloseButton(screen: RecipeViewerScreen): Renderable {
        val closeColor = COLOR_CLOSE.toColor()
        val label = Renderable.text("Close", color = Color.WHITE)
        return Renderable.clickable(
            Renderable.drawInsideRoundedRectWithOutline(
                label,
                color = closeColor,
                padding = 5,
                radius = 8,
                topOutlineColor = closeColor.brighter().rgb,
                bottomOutlineColor = closeColor.darker().rgb,
                borderOutlineThickness = 1,
            ),
            onLeftClick = { screen.onClose() },
        )
    }
}
