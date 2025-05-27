package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.pets.PetDisplayConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.CircularContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.AnimatedItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackAnimationFrame
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRotationDefinition
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.util.EnumFacing
import java.awt.Color

typealias VElement = PetDisplayConfig.VisualPetDisplayConfig.VisualElement
typealias TElement = PetDisplayConfig.TextPetDisplayConfig.TextElement
typealias SDirection = PetDisplayConfig.VisualPetDisplayConfig.SpinDirection
typealias TLO = PetDisplayConfig.TextPetDisplayConfig.TextLocationOption
typealias NFE = PetDisplayConfig.TextPetDisplayConfig.NumberFormatEntry

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val customColorConfig get() = config.visual.colorCustomization
    private val rarityColorConfig get() = customColorConfig.rarityBackground
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null

    private fun LorenzRarity.getRarityBackgroundColor(): ChromaColour = when (this) {
        LorenzRarity.COMMON -> rarityColorConfig.commonColor
        LorenzRarity.UNCOMMON -> rarityColorConfig.uncommonColor
        LorenzRarity.RARE -> rarityColorConfig.rareColor
        LorenzRarity.EPIC -> rarityColorConfig.epicColor
        LorenzRarity.LEGENDARY -> rarityColorConfig.legendaryColor
        LorenzRarity.MYTHIC -> rarityColorConfig.mythicColor
        else -> this.color.toChromaColor()
    }

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        val enabledVisuals = config.visual.enabledVisuals.get()
        if (VElement.PET_ICON !in enabledVisuals) return null

        val spinDirection = config.visual.spinDirection.get()
        val spinFrequency = config.visual.spinFrequency.get()
        val spinMultiplier = if (spinDirection == SDirection.CLOCKWISE) -1 else 1
        val degreesPerSecond = if (spinDirection != SDirection.NONE) ((360 / spinFrequency) * spinMultiplier) else 0.0

        val baseItemRenderable = AnimatedItemStackRenderable(
            frames = getAnimatedItemStackSequence(
                firstFrameOnly = !(config.visual.skinAnimation.get())
            ) ?: listOf(ItemStackAnimationFrame(getItemStackOrNull() ?: return null)),
            scale = config.visual.iconScale.get(),
            rotation = ItemStackRotationDefinition(
                axis = EnumFacing.Axis.Y,
                rotationSpeed = degreesPerSecond.toDouble(),
            ),
        )

        if (VElement.RARITY_BACKGROUND !in enabledVisuals) return baseItemRenderable
        val rarityBackgroundRenderable = CircularContainerRenderable(
            baseItemRenderable,
            rarity.getRarityBackgroundColor().toColor(),
            padding = 4,
        )
        val separatorRingEnabled = VElement.XP_RING in enabledVisuals && VElement.SEPARATOR_RING in enabledVisuals
        val borderedRarityBackgroundRenderable = if (separatorRingEnabled) CircularContainerRenderable(
            rarityBackgroundRenderable,
            customColorConfig.separatorColor.toColor(),
            padding = 6,
        ) else rarityBackgroundRenderable
        if (VElement.XP_RING !in enabledVisuals) return borderedRarityBackgroundRenderable
        val xpRingCompleteRenderable = CircularContainerRenderable(
            borderedRarityBackgroundRenderable,
            backgroundColor = customColorConfig.xpRing.filledColor.toColor(),
            unfilledColor = customColorConfig.xpRing.unfilledColor.toColor(),
            filledPercentage = levelProgressionPercentage,
            padding = 2,
        )
        return xpRingCompleteRenderable
    }

    private fun PetData.buildTextRenderableOrNull(): Renderable? = VerticalContainerRenderable(
        buildList {
            val enabledTexts = config.text.enabledTexts.get().takeIfNotEmpty() ?: return null
            enabledTexts.mapNotNull {
                it to when (it) {
                    // These are "parts" of other elements, so they themselves don't do anything.
                    TElement.PET_LEVEL, TElement.SKIN_SYMBOL -> return@mapNotNull null

                    TElement.PET_NAME -> {
                        getUserFriendlyName(
                            includeLevel = TElement.PET_LEVEL in enabledTexts,
                            includeSkinTag = TElement.SKIN_SYMBOL in enabledTexts,
                        )
                    }
                    TElement.HELD_ITEM -> heldItemInternalName?.repoItemName ?: return@mapNotNull null
                    TElement.OVERFLOW_XP -> {
                        // 1000.0 to account for double rounding errors between Hypixel's stored data, and our calculation
                        val overflowXp = overflowXp.takeIf { overflow -> overflow > 1000.0 } ?: return@mapNotNull null
                        "§7+§b${overflowXp.formatExpByConfigOption()}"
                    }
                    TElement.TOTAL_XP -> {
                        val totalXp = exp?.takeIf { totalXp -> totalXp > 0.0 } ?: return@mapNotNull null
                        "§b${totalXp.formatExpByConfigOption()}"
                    }
                    TElement.NEXT_LEVEL -> {
                        if (level >= PetUtils.getMaxLevel(fauxInternalName)) return@mapNotNull null

                        val currentExp = exp ?: 0.0
                        val currentXpOverLevel = currentExp - currentLevelXp
                        val neededXp = nextLevelXp - currentLevelXp
                        val percentageFormat = if (config.text.nextLevelPercent.get()) {
                            " §7- §e${levelProgressionPercentage.shortFormat()}%"
                        } else ""
                        formatExpPairByConfigOption(currentXpOverLevel, neededXp) + percentageFormat
                    }
                }
            }.map { (textElement, textElementFormat) ->
                val labelFormat = if (config.text.textLabels.get()) {
                    when (textElement) {
                        // These are "parts" of other elements, so they themselves don't have labels.
                        TElement.PET_LEVEL, TElement.SKIN_SYMBOL -> ""
                        // No label needed
                        TElement.PET_NAME -> ""
                        else -> "§e$textElement§7: "
                    }
                } else ""
                RenderableString(
                    "$labelFormat$textElementFormat",
                    horizontalAlign = config.text.horizontalAlign.get()
                )
            }.forEach { add(it) }
        },
        horizontalAlign = config.text.horizontalAlign.get(),
        verticalAlign = config.text.verticalAlign.get(),
    )

    private fun Double.formatExpByConfigOption() = when (config.text.xpFormat.get()) {
        NFE.DEFAULT, NFE.UNFORMATTED -> toInt().addSeparators()
        NFE.FORMATTED -> toInt().shortFormat()
        else -> ""
    }

    private fun formatExpPairByConfigOption(
        firstExp: Double,
        secondExp: Double,
    ): String = when (config.text.xpFormat.get()) {
        NFE.DEFAULT -> "§b${firstExp.toInt().addSeparators()}§9/§b${secondExp.toInt().shortFormat()}"
        NFE.FORMATTED -> "§b${firstExp.toInt().shortFormat()}§9/§b${secondExp.toInt().shortFormat()}"
        NFE.UNFORMATTED -> "§b${firstExp.toInt().addSeparators()}§9/§b${secondExp.toInt().addSeparators()}"
        else -> ""
    }

    private fun PetData.buildRenderable(): Renderable? {
        lastPetHash = this.hashCode().takeIf { it != lastPetHash } ?: return petOverlay
        CurrentPetApi.currentPet ?: return null

        val itemRenderable = buildItemRenderableOrNull()
        val textRenderable = buildTextRenderableOrNull()

        return if (itemRenderable != null && textRenderable != null) {
            // Technically nullable in the JVM
            val textLocation = config.text.textLocation.get() ?: return null
            val orderedList = when (textLocation) {
                TLO.TOP, TLO.LEFT -> listOf(textRenderable, itemRenderable)
                TLO.BOTTOM, TLO.RIGHT -> listOf(itemRenderable, textRenderable)
            }
            when (textLocation) {
                TLO.TOP, TLO.BOTTOM -> VerticalContainerRenderable(orderedList, spacing = 2)
                TLO.LEFT, TLO.RIGHT -> HorizontalContainerRenderable(orderedList, spacing = 2)
            }
        } else listOf(textRenderable, itemRenderable).firstOrNull { it != null }
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.enabled,

            config.visual.enabledVisuals,
            config.visual.iconScale,
            config.visual.skinAnimation,
            config.visual.spinDirection,
            config.visual.spinFrequency,

            config.text.enabledTexts,
            config.text.textLabels,
            config.text.nextLevelPercent,
            config.text.xpFormat,
            config.text.textLocation,
            config.text.verticalAlign,
            config.text.horizontalAlign,
        ) { lastPetHash = 0 }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (RiftApi.inRift() || !config.enabled.get()) return
        petOverlay = CurrentPetApi.currentPet?.buildRenderable()
        petOverlay?.let {
            config.position.renderRenderable(it, posLabel = "Current Pet")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
        event.move(88, "misc.pets.display", "misc.pets.display.enabled")
        event.move(88, "misc.pets.displayPos", "misc.pets.display.pos")
    }
}
