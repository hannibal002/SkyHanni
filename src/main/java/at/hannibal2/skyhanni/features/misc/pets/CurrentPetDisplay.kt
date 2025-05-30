package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.pets.display.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.misc.pets.display.VisualPetDisplayConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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

typealias TElement = TextPetDisplayConfig.TextElement
typealias SDirection = VisualPetDisplayConfig.SpinDirection
typealias TLO = TextPetDisplayConfig.TextLocationOption
typealias NFE = TextPetDisplayConfig.NumberFormatEntry

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val customizationConfig get() = config.visual.customization
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null

    private fun LorenzRarity.getRarityBackgroundColor(): ChromaColour = with(customizationConfig.rarityBackground) {
        when (this@getRarityBackgroundColor) {
            LorenzRarity.COMMON -> commonColor.get()
            LorenzRarity.UNCOMMON -> uncommonColor.get()
            LorenzRarity.RARE -> rareColor.get()
            LorenzRarity.EPIC -> epicColor.get()
            LorenzRarity.LEGENDARY -> legendaryColor.get()
            LorenzRarity.MYTHIC -> mythicColor.get()
            else -> this@getRarityBackgroundColor.color.toChromaColor()
        }
    }

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        if (!config.visual.petIcon.get()) return null

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

        if (!config.visual.rarityBackground.get()) return baseItemRenderable
        val rarityBackgroundRenderable = CircularContainerRenderable(
            baseItemRenderable,
            rarity.getRarityBackgroundColor(),
            padding = 4,
        )
        val separatorRingEnabled = config.visual.xpRing.get() && config.visual.separatorRing.get()
        val borderedRarityBackgroundRenderable = if (separatorRingEnabled) CircularContainerRenderable(
            rarityBackgroundRenderable,
            customizationConfig.separatorRing.color.get(),
            padding = customizationConfig.separatorRing.padding.get(),
        ) else rarityBackgroundRenderable
        if (!config.visual.xpRing.get()) return borderedRarityBackgroundRenderable
        val xpRingCompleteRenderable = CircularContainerRenderable(
            borderedRarityBackgroundRenderable,
            backgroundColor = customizationConfig.xpRing.filledColor.get(),
            unfilledColor = customizationConfig.xpRing.unfilledColor.get(),
            filledPercentage = levelProgressionPercentage,
            padding = customizationConfig.xpRing.padding.get(),
        )
        return xpRingCompleteRenderable
    }

    private fun PetData.buildTextRenderableOrNull(): Renderable? = VerticalContainerRenderable(
        buildList {
            val enabledTexts = config.text.enabledTexts.get().takeIfNotEmpty() ?: return null
            enabledTexts.mapNotNull {
                it to when (it) {
                    TElement.PET_NAME -> getUserFriendlyName(
                        includeLevel = config.text.nameLevel.get(),
                        includeSkinTag = config.text.nameSkinSymbol.get(),
                    )
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
                val labelFormat = textElement.getFormattedLabel().takeIf { config.text.textLabels.get() }.orEmpty()
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

            config.visual.petIcon,
            config.visual.rarityBackground,
            config.visual.xpRing,
            config.visual.separatorRing,
            config.visual.iconScale,
            config.visual.skinAnimation,
            config.visual.spinDirection,
            config.visual.spinFrequency,

            customizationConfig.separatorRing.padding,
            customizationConfig.separatorRing.color,

            customizationConfig.xpRing.padding,
            customizationConfig.xpRing.filledColor,
            customizationConfig.xpRing.unfilledColor,

            customizationConfig.rarityBackground.padding,
            customizationConfig.rarityBackground.commonColor,
            customizationConfig.rarityBackground.uncommonColor,
            customizationConfig.rarityBackground.rareColor,
            customizationConfig.rarityBackground.epicColor,
            customizationConfig.rarityBackground.legendaryColor,
            customizationConfig.rarityBackground.mythicColor,

            config.text.enabledTexts,
            config.text.nameLevel,
            config.text.nameSkinSymbol,
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
