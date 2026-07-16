package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.text.PetTextDisplaySettings
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import java.awt.Color
import java.util.Locale
import kotlin.math.roundToInt

private typealias TElement = TextPetDisplayConfig.TextElement
private typealias TLO = TextPetDisplayConfig.TextLocationOption
private typealias NFE = TextPetDisplayConfig.NumberFormatEntry
private typealias ESBundledLocation = TextPetDisplayConfig.ExpSharePetTextConfig.BundledTextLocation

internal fun PetData.buildTextRenderableOrNull(
    textConfig: PetTextDisplaySettings,
    opacity: Float = 1.0f,
    textScale: Double = textConfig.textScaleProperty().get().toDouble(),
): Renderable? {
    val enabledTexts = textConfig.enabledTextsProperty().get().takeIfNotEmpty() ?: return null
    val textColor = Color(255, 255, 255, (255 * opacity).roundToInt().coerceIn(0, 255))
    val xpFormat = textConfig.xpFormatProperty().get()
    val lines = enabledTexts.mapNotNull {
        it to when (it) {
            TElement.PET_NAME -> getUserFriendlyName(
                includeLevel = textConfig.nameLevelProperty().get(),
                includeSkinTag = textConfig.nameSkinSymbolProperty().get(),
            )
            TElement.HELD_ITEM -> heldItemInternalName?.repoItemName ?: return@mapNotNull null
            TElement.OVERFLOW_XP -> {
                val overflowXp = overflowXp.takeIf { overflow -> overflow > 1000.0 } ?: return@mapNotNull null
                "§7+§b${overflowXp.formatExpByConfigOption(xpFormat)}"
            }
            TElement.TOTAL_XP -> {
                val totalXp = exp?.takeIf { totalXp -> totalXp > 0.0 } ?: return@mapNotNull null
                "§b${totalXp.formatExpByConfigOption(xpFormat)}"
            }
            TElement.NEXT_LEVEL -> {
                if (level >= PetUtils.getMaxLevel(fauxInternalName)) return@mapNotNull null

                val currentExp = exp ?: 0.0
                val currentXpOverLevel = currentExp - currentLevelXp
                val neededXp = nextLevelXp - currentLevelXp
                val percentageFormat = if (textConfig.nextLevelPercentProperty().get()) {
                    " §7- §e${levelProgressionPercentage.formatLevelProgressionPercentage()}%"
                } else ""
                formatExpPairByConfigOption(currentXpOverLevel, neededXp, xpFormat) + percentageFormat
            }
        }
    }.map { (textElement, textElementFormat) ->
        val labelFormat = textElement.getFormattedLabel().takeIf { textConfig.textLabelsProperty().get() }.orEmpty()
        StringRenderable(
            "$labelFormat$textElementFormat",
            scale = textScale,
            color = textColor,
            horizontalAlign = textConfig.horizontalAlignProperty().get(),
        )
    }.takeIfNotEmpty() ?: return null
    return Renderable.vertical(
        lines,
        horizontalAlign = textConfig.horizontalAlignProperty().get(),
        verticalAlign = textConfig.verticalAlignProperty().get(),
    )
}

internal fun List<ExpSharePetState>.buildBundledExpShareTextRenderables(config: PetDisplayConfig): List<Renderable> {
    val textConfig = config.text.expSharePets
    if (!textConfig.enabled.get()) return emptyList()
    if (textConfig.textMode.get() != TextPetDisplayConfig.ExpSharePetTextConfig.TextMode.BUNDLED_WITH_MAIN) {
        return emptyList()
    }
    return mapNotNull {
        it.petData.buildTextRenderableOrNull(
            textConfig = textConfig,
            opacity = if (it.disabled) config.visual.expSharePets.disabledOpacity.get() else 1.0f,
        )
    }
}

internal fun combineVisualAndTextRenderables(
    itemRenderable: AnchoredRenderable?,
    textRenderable: Renderable?,
    textLocation: TLO,
    centerTarget: TextPetDisplayConfig.EquippedPetTextConfig.CenterTarget,
): Renderable? = if (itemRenderable != null && textRenderable != null) {
    if (centerTarget == TextPetDisplayConfig.EquippedPetTextConfig.CenterTarget.EQUIPPED_PET_VISUALS) {
        combineAnchoredVisualAndTextRenderables(itemRenderable, textRenderable, textLocation)
    } else {
        val visualRenderable = itemRenderable.renderable
        val orderedList = when (textLocation) {
            TLO.TOP, TLO.LEFT -> listOf(textRenderable, visualRenderable)
            TLO.BOTTOM, TLO.RIGHT -> listOf(visualRenderable, textRenderable)
        }
        when (textLocation) {
            TLO.TOP, TLO.BOTTOM -> Renderable.vertical(orderedList, spacing = 2)
            TLO.LEFT, TLO.RIGHT -> Renderable.horizontal(orderedList, spacing = 2)
        }
    }
} else textRenderable ?: itemRenderable?.renderable

private fun combineAnchoredVisualAndTextRenderables(
    itemRenderable: AnchoredRenderable,
    textRenderable: Renderable,
    textLocation: TLO,
): Renderable {
    val visualRenderable = itemRenderable.renderable
    val spacing = 2
    val textX = when (textLocation) {
        TLO.LEFT -> itemRenderable.anchorX - textRenderable.width - spacing
        TLO.RIGHT -> itemRenderable.anchorX + itemRenderable.anchorWidth + spacing
        else -> itemRenderable.anchorX + RenderableUtils.calculateAlignmentXOffset(textRenderable, itemRenderable.anchorWidth)
    }
    val textY = when (textLocation) {
        TLO.TOP -> itemRenderable.anchorY - textRenderable.height - spacing
        TLO.BOTTOM -> itemRenderable.anchorY + itemRenderable.anchorHeight + spacing
        else -> itemRenderable.anchorY + RenderableUtils.calculateAlignmentYOffset(textRenderable, itemRenderable.anchorHeight)
    }
    val minX = minOf(0, textX)
    val minY = minOf(0, textY)
    val visualX = -minX
    val visualY = -minY
    val shiftedTextX = textX - minX
    val shiftedTextY = textY - minY
    val renderTextFirst = textLocation == TLO.TOP || textLocation == TLO.LEFT

    return object : Renderable {
        override val width = maxOf(visualRenderable.width, textX + textRenderable.width) - minX
        override val height = maxOf(visualRenderable.height, textY + textRenderable.height) - minY
        override val horizontalAlign = RenderUtils.HorizontalAlignment.LEFT
        override val verticalAlign = RenderUtils.VerticalAlignment.TOP

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            if (renderTextFirst) {
                textRenderable.renderAt(mouseOffsetX, mouseOffsetY, shiftedTextX, shiftedTextY)
                visualRenderable.renderAt(mouseOffsetX, mouseOffsetY, visualX, visualY)
            } else {
                visualRenderable.renderAt(mouseOffsetX, mouseOffsetY, visualX, visualY)
                textRenderable.renderAt(mouseOffsetX, mouseOffsetY, shiftedTextX, shiftedTextY)
            }
        }
    }
}

private fun Renderable.renderAt(mouseOffsetX: Int, mouseOffsetY: Int, x: Int, y: Int) {
    DrawContextUtils.pushPop {
        DrawContextUtils.translate(x.toFloat(), y.toFloat())
        render(mouseOffsetX + x, mouseOffsetY + y)
    }
}

internal fun combineMainAndExpShareTextRenderables(
    mainTextRenderable: Renderable?,
    expShareTextRenderables: List<Renderable>,
    textConfig: TextPetDisplayConfig,
): Renderable? {
    if (expShareTextRenderables.isEmpty()) return mainTextRenderable
    val renderables = when (textConfig.expSharePets.bundledLocation.get()) {
        ESBundledLocation.ABOVE -> expShareTextRenderables + listOfNotNull(mainTextRenderable)
        ESBundledLocation.BELOW -> listOfNotNull(mainTextRenderable) + expShareTextRenderables
        ESBundledLocation.SPLIT -> {
            val aboveCount = (expShareTextRenderables.size + 1) / 2
            expShareTextRenderables.take(aboveCount) +
                listOfNotNull(mainTextRenderable) +
                expShareTextRenderables.drop(aboveCount)
        }
    }.takeIfNotEmpty() ?: return null

    return Renderable.vertical(
        renderables,
        spacing = textConfig.expSharePets.bundledSpacing.get(),
        horizontalAlign = textConfig.equippedPet.horizontalAlign.get(),
        verticalAlign = textConfig.equippedPet.verticalAlign.get(),
    )
}

internal fun Renderable.withWidgetWarning(
    warning: String?,
    textConfig: TextPetDisplayConfig.EquippedPetTextConfig,
): Renderable {
    warning ?: return this
    val textScale = textConfig.textScale.get().toDouble()
    val horizontalAlign = textConfig.horizontalAlign.get()
    return Renderable.vertical(
        listOf(
            this,
            StringRenderable(
                warning,
                scale = textScale,
                horizontalAlign = horizontalAlign,
            ),
        ),
        spacing = 2,
        horizontalAlign = this.horizontalAlign,
        verticalAlign = this.verticalAlign,
    )
}

private fun Double.formatExpByConfigOption(xpFormat: NFE) = when (xpFormat) {
    NFE.DEFAULT, NFE.UNFORMATTED -> toLong().addSeparators()
    NFE.FORMATTED -> toLong().shortFormat()
}

private fun Double.formatLevelProgressionPercentage(): String {
    val rounded = coerceIn(0.0, 100.0).roundTo(1)
    return String.format(Locale.US, "%.1f", rounded)
}

private fun formatExpPairByConfigOption(
    firstExp: Double,
    secondExp: Double,
    xpFormat: NFE,
): String = when (xpFormat) {
    NFE.DEFAULT -> "§b${firstExp.toLong().addSeparators()}§9/§b${secondExp.toLong().shortFormat()}"
    NFE.FORMATTED -> "§b${firstExp.toLong().shortFormat()}§9/§b${secondExp.toLong().shortFormat()}"
    NFE.UNFORMATTED -> "§b${firstExp.toLong().addSeparators()}§9/§b${secondExp.toLong().addSeparators()}"
}
