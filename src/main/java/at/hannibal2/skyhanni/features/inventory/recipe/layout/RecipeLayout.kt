package at.hannibal2.skyhanni.features.inventory.recipe.layout

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.features.inventory.recipe.RecipeViewerScreen
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
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PrimitiveIngredient
import at.hannibal2.skyhanni.utils.PrimitiveRecipe
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import io.github.notenoughupdates.moulconfig.ChromaColour
import java.awt.Color

internal typealias HA = RenderUtils.HorizontalAlignment
internal typealias VA = RenderUtils.VerticalAlignment

internal const val GRID_SPACING = 2
internal const val ITEM_SCALE = 1.25

internal val COIN_ITEM = "SKYBLOCK_COIN".toInternalName()

internal val COLOR_BG = ChromaColour.fromStaticRGB(20, 20, 30, 230)
internal val COLOR_SLOT_EMPTY = ChromaColour.fromStaticRGB(40, 40, 55, 200)
internal val COLOR_SLOT_FILLED = ChromaColour.fromStaticRGB(55, 55, 75, 220)
internal val COLOR_HEADER = ChromaColour.fromStaticRGB(200, 200, 255, 255)
internal val COLOR_SUBHEADER = ChromaColour.fromStaticRGB(150, 150, 190, 255)
internal val COLOR_ARROW = ChromaColour.fromStaticRGB(100, 220, 100, 255)
internal val COLOR_NAV_ACTIVE = ChromaColour.fromStaticRGB(80, 130, 255, 220)
internal val COLOR_NAV_INACTIVE = ChromaColour.fromStaticRGB(50, 50, 70, 180)
internal val COLOR_CLOSE = ChromaColour.fromStaticRGB(200, 60, 60, 210)
internal val COLOR_OUTLINE_TOP = ChromaColour.fromStaticRGB(100, 100, 160, 255)
internal val COLOR_OUTLINE_BOT = ChromaColour.fromStaticRGB(60, 60, 100, 255)
internal val COLOR_BACK = ChromaColour.fromStaticRGB(60, 100, 200, 210)

private val providerCache = HashMap<NeuInternalName, NeuItemStackProvider>()
private val itemRenderableCache = HashMap<Triple<NeuInternalName, Boolean, Int>, Renderable>()

internal fun providerFor(internalName: NeuInternalName) = providerCache.getOrPut(internalName) {
    NeuItemStackProvider(internalName)
}

internal fun NeuInternalName.scaledItem(withTip: Boolean = true, scale: Double = ITEM_SCALE): Renderable =
    itemRenderableCache.getOrPut(Triple(this, withTip, (scale * 100).toInt())) {
        val provider = providerFor(this)
        val glint = asString().startsWith("ENCHANTED_")
        Renderable.item(provider.stack.copy().apply { if (glint) addEnchantGlint() }) {
            this.scale = scale
            this.xSpacing = 0
            this.ySpacing = 0
            this.horizontalAlign = HA.CENTER
        }.let {
            if (withTip) it.withTip() else it
        }
    }

/** Raw rendered pixel size of an item at [scale], excluding slot padding. */
internal fun itemPixelSize(scale: Double) = (15.5 * scale + 0.5).toInt()

internal fun Renderable.drawInSlot(filled: Boolean = true, radiusScalar: Double = 1.0): Renderable =
    Renderable.drawInsideRoundedRect(
        this,
        if (filled) COLOR_SLOT_FILLED.toColor() else COLOR_SLOT_EMPTY.toColor(),
        padding = 1,
        radius = (4 * radiusScalar).toInt(),
        horizontalAlign = HA.CENTER,
    )

internal fun Renderable.withCountOverlay(count: Int): Renderable = if (count <= 1) this else Renderable.doubleLayered(
    this,
    Renderable.text(
        if (count >= 1000) count.shortFormat() else count.toString(),
        scale = 0.75,
        color = Color.WHITE,
        horizontalAlign = HA.RIGHT,
        verticalAlign = VA.BOTTOM,
    ),
    blockBottomHover = false,
    forceBottomRenderFirst = true,
)

internal fun buildItemSlot(
    ingredient: PrimitiveIngredient?,
    screen: RecipeViewerScreen,
    isOutput: Boolean = false,
    scale: Double = ITEM_SCALE,
): Renderable {
    val pixelSize = itemPixelSize(scale)
    return when {
        ingredient == null || ingredient == PrimitiveIngredient.EMPTY || ingredient.internalName == NeuInternalName.NONE ->
            Renderable.placeholder(pixelSize, pixelSize).drawInSlot(filled = false, radiusScalar = scale)

        ingredient.internalName == COIN_ITEM ->
            Renderable.item(ItemUtils.getCoinItemStack(ingredient.count)) {
                this.scale = scale
                this.xSpacing = 0
                this.ySpacing = 0
            }.drawInSlot()

        else -> {
            val canNavigate = !isOutput && ingredient.internalName != screen.internalName
            val canNavigateFor = canNavigate && EnoughUpdatesManager.getRecipesFor(ingredient.internalName).isNotEmpty()
            val canNavigateUsing = canNavigate && EnoughUpdatesManager.getRecipesUsing(ingredient.internalName).isNotEmpty()

            val scaledItem = ingredient.internalName.scaledItem(withTip = false, scale = scale)
            val slot = scaledItem.drawInSlot().withCountOverlay(ingredient.count.toInt())
            val baseTips = providerFor(ingredient.internalName).stack.getTooltipCompat(false)
            val tips = baseTips + listOfNotNull(
                "",
                "§eLeft click to view recipes".takeIf { canNavigateFor },
                "§eRight click for recipe usages".takeIf { canNavigateUsing },
            )

            if (!canNavigateFor && !canNavigateUsing) Renderable.hoverTips(slot, baseTips, bypassChecks = true)
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
}

internal fun buildIngredientRowOrNull(
    ingredient: PrimitiveIngredient,
    screen: RecipeViewerScreen,
    scale: Double = ITEM_SCALE,
): Renderable? = buildIngredientTextOrNull(ingredient)?.let {
    Renderable.horizontal(spacing = 4, verticalAlign = VA.CENTER) {
        add(buildItemSlot(ingredient, screen, scale = scale))
        add(it)
    }
}

/** Text-only ingredient label (no slot icon), used in the crafting ingredient summary. */
internal fun buildIngredientTextOrNull(ingredient: PrimitiveIngredient): Renderable? {
    if (ingredient == PrimitiveIngredient.EMPTY || ingredient.internalName == NeuInternalName.NONE) return null
    val count = ingredient.count.toInt()
    val countSuffix = if (count > 1) " §7×${count.addSeparators()}" else ""
    fun String.buildIngredientText() = Renderable.wrappedText(
        this,
        scale = 0.9,
        setWidth = 175,
        color = Color.WHITE,
        horizontalAlign = HA.CENTER,
        internalAlign = HA.CENTER,
    )

    val format = if (ingredient.internalName == COIN_ITEM) "§6${count.shortFormat(true)} Coins"
    else "${ingredient.internalName.repoItemName}$countSuffix"
    return format.buildIngredientText()
}

internal fun sectionLabel(text: String) =
    Renderable.text("§7$text:", scale = 0.85, color = COLOR_SUBHEADER.toColor())

internal fun arrowRenderable() =
    Renderable.text(" ──► ", scale = 1.3, color = COLOR_ARROW.toColor(), horizontalAlign = HA.CENTER)

/**
 * Base for recipe-type-specific layout renderers.
 * All shared rendering utilities are top-level internal functions in this file.
 */
interface RecipeLayout {
    fun build(recipe: PrimitiveRecipe, screen: RecipeViewerScreen): Renderable
}
