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
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.addEnchantGlint
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
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
    private const val ITEM_SCALE = 1.25

    private val providerCache = HashMap<NeuInternalName, NeuItemStackProvider>()
    private fun providerFor(internalName: NeuInternalName) = providerCache.getOrPut(internalName) {
        NeuItemStackProvider(internalName)
    }

    private val itemProviderCache = HashMap<Triple<NeuInternalName, Int, Boolean>, NeuItemStackProvider>()
    private fun itemProviderFor(internalName: NeuInternalName, stackCount: Int): NeuItemStackProvider {
        val glint = internalName.asString().startsWith("ENCHANTED_")
        return itemProviderCache.getOrPut(Triple(internalName, stackCount, glint)) {
            NeuItemStackProvider(internalName) {
                if (glint) addEnchantGlint()
                count = stackCount
            }
        }
    }

    private val itemRenderableCache = HashMap<Triple<NeuInternalName, Int, Boolean>, Renderable>()
    private fun NeuInternalName.scaledItem(
        withTip: Boolean = true,
        stackCount: Int = 1
    ): Renderable = itemRenderableCache.getOrPut(Triple(this, stackCount, withTip)) {
        Renderable.item(itemProviderFor(this, stackCount), scale = ITEM_SCALE).let {
            if (withTip) it.withTip() else it
        }
    }

    private val COIN_ITEM = "SKYBLOCK_COIN".toInternalName()
    private val SLOT_SIZE by lazy {
        NeuInternalName.MISSING_ITEM.scaledItem().let {
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
    private val COLOR_BACK = ChromaColour.fromStaticRGB(60, 100, 200, 210)

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
                add(body)
                add(buildActionsRow(screen))
            },
            backgroundColor = COLOR_BG,
            lightColor = COLOR_OUTLINE_TOP,
            darkColor = COLOR_OUTLINE_BOT,
            padding = PANEL_PADDING,
            radius = 12,
            smoothness = 2,
            borderThickness = 2,
        )
    }

    private fun NeuInternalName.buildHeaderRow(): Renderable {
        val texts = Renderable.vertical(spacing = 2, horizontalAlign = HA.CENTER) {
            add(Renderable.text(repoItemName, scale = 1.4, color = COLOR_HEADER.toColor(), horizontalAlign = HA.CENTER))
            add(Renderable.text("§7${asString()}", scale = 0.8, color = COLOR_SUBHEADER.toColor(), horizontalAlign = HA.CENTER))
        }
        return Renderable.horizontal(
            listOf(this@buildHeaderRow.scaledItem(withTip = false), texts),
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

        val currentType = recipes[currentIndex].recipeType.displayName
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

    private fun buildRecipeRenderable(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable = when (recipe.recipeType) {
        NeuRecipeType.CRAFTING -> buildCraftingLayout(recipe, screen)
        NeuRecipeType.FORGE -> buildForgeLayout(recipe, screen)
        else -> buildGenericLayout(recipe, screen)
    }

    private fun buildCraftingLayout(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        val ingredients = recipe.ingredients.toList()
        val grid: Array<PrimitiveIngredient?> = Array(9) { ingredients.getOrNull(it) }
        val gridRenderable = grid.toList().chunked(3).map { row ->
            Renderable.horizontal(row.map { buildItemSlot(it, screen) }, spacing = GRID_SPACING)
        }.let { Renderable.vertical(it, spacing = GRID_SPACING) }

        val arrow = arrowRenderable()
        val output = buildItemSlot(recipe.outputs.firstOrNull(), screen)

        return Renderable.horizontal(listOf(gridRenderable, arrow, output), spacing = 6, verticalAlign = VA.CENTER)
    }

    private fun buildForgeLayout(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        val inputsPanel = Renderable.vertical(
            buildList {
                add(sectionLabel("Ingredients"))
                addAll(recipe.ingredients.map { buildIngredientRow(it, screen) })
            },
            spacing = 3,
        )

        if (recipe.outputs.size <= 1) return inputsPanel

        val outputPanel = Renderable.vertical(
            buildList {
                add(sectionLabel("Output"))
                addAll(recipe.outputs.map { buildIngredientRow(it, screen) })
            },
            spacing = 3,
            horizontalAlign = HA.CENTER,
        )

        return Renderable.horizontal(listOf(inputsPanel, arrowRenderable(), outputPanel), spacing = 8, verticalAlign = VA.TOP)
    }

    private fun buildGenericLayout(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable {
        fun panel(title: String, ingredients: Set<PrimitiveIngredient>) = Renderable.vertical(
            buildList {
                add(sectionLabel(title))
                if (ingredients.isEmpty()) add(Renderable.text("§7(none)", scale = 0.85))
                else addAll(ingredients.map { buildIngredientRow(it, screen) })
            },
            spacing = 3,
        )

        val inputsPanel = panel("Inputs", recipe.ingredients)
        return if (recipe.outputs.size <= 1) inputsPanel
        else Renderable.horizontal(
            listOf(
                inputsPanel,
                arrowRenderable(),
                panel("Outputs", recipe.outputs)
            ),
            spacing = 8,
            verticalAlign = VA.TOP,
        )
    }

    private fun Renderable.drawInSlot(
        filled: Boolean = true,
        radiusScalar: Double = 1.0,
    ): Renderable = Renderable.drawInsideRoundedRect(
        this,
        if (filled) COLOR_SLOT_FILLED.toColor() else COLOR_SLOT_EMPTY.toColor(),
        padding = 0,
        radius = (4 * radiusScalar).toInt(),
    )

    /**
     * A single item slot: a fixed-size dark square containing an item icon with tooltip.
     * Empty slots render as a dimmer square with no content.
     */
    private fun buildItemSlot(
        ingredient: PrimitiveIngredient?,
        screen: RecipeViewerScreen,
        isOutput: Boolean = false,
    ): Renderable = when {
        ingredient == null ->
            Renderable.placeholder(SLOT_SIZE, SLOT_SIZE).drawInSlot(filled = false, radiusScalar = ITEM_SCALE)

        ingredient.internalName == COIN_ITEM ->
            Renderable.item(ItemUtils.getCoinItemStack(ingredient.count), scale = ITEM_SCALE).drawInSlot()

        else -> run {
            val canNavigate = !isOutput && ingredient.internalName != screen.internalName
            val canNavigateFor = canNavigate && EnoughUpdatesManager.getRecipesFor(ingredient.internalName).isNotEmpty()
            val canNavigateUsing = canNavigate && EnoughUpdatesManager.getRecipesUsing(ingredient.internalName).isNotEmpty()

            val slot = ingredient.internalName.scaledItem(withTip = false, stackCount = ingredient.count.toInt()).drawInSlot()
            val baseTips = providerFor(ingredient.internalName).stack.getTooltipCompat(false)
            val tips = baseTips + listOfNotNull(
                "",
                "§eLeft click to view recipes".takeIf { canNavigateFor },
                "§eRight click for recipe usages".takeIf { canNavigateUsing },
            )

            return@run if (!canNavigateFor && !canNavigateUsing) Renderable.hoverTips(slot, baseTips, bypassChecks = true)
            else Renderable.clickable(
                slot,
                onAnyClick = buildMap {
                    if (canNavigateFor) put(LEFT_MOUSE) { screen.navigateTo(ingredient.internalName) }
                    if (canNavigateUsing) put(RIGHT_MOUSE) { screen.navigateToUsages(ingredient.internalName) }
                },
                tips = tips,
                bypassChecks = true,
            )
        }
    }

    private fun buildIngredientRow(ingredient: PrimitiveIngredient, screen: RecipeViewerScreen): Renderable {
        val name = ingredient.internalName.repoItemName
        val count = ingredient.count.toInt()
        val countSuffix = if (count > 1) " §7×${count.addSeparators()}" else ""
        val label = Renderable.text("$name$countSuffix", scale = 0.9, color = Color.WHITE)
        return Renderable.horizontal(listOf(buildItemSlot(ingredient, screen), label), spacing = 4, verticalAlign = VA.CENTER)
    }

    private fun sectionLabel(text: String) =
        Renderable.text("§7$text:", scale = 0.85, color = COLOR_SUBHEADER.toColor())

    private fun arrowRenderable() =
        Renderable.text(" ──► ", scale = 1.3, color = COLOR_ARROW.toColor(), horizontalAlign = HA.CENTER)

    private fun buildNoRecipesPlaceholder(internalName: NeuInternalName) =
        Renderable.text("§cNo recipes found for §e${internalName.asString()}", color = Color.WHITE, horizontalAlign = HA.CENTER)

    private fun buildActionsRow(screen: RecipeViewerScreen) = Renderable.horizontal(
        spacing = 6,
        verticalAlign = VA.CENTER
    ) {
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
