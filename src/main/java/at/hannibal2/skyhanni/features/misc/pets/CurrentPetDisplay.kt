package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.pets.PetDisplayConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.CircularContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.AnimatedItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRotationDefinition
import net.minecraft.util.EnumFacing
import java.awt.Color

typealias VElement = PetDisplayConfig.VisualPetDisplayConfig.VisualElement
typealias TElement = PetDisplayConfig.TextPetDisplayConfig.TextElement
typealias SDirection = PetDisplayConfig.VisualPetDisplayConfig.SpinDirection
typealias TLO = PetDisplayConfig.TextPetDisplayConfig.TextLocationOption
typealias NFE = PetDisplayConfig.TextPetDisplayConfig.NumberFormatEntry

@SkyHanniModule
object CurrentPetDisplay {

    private const val BASE_ITEM_SCALE = 1.7

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        val enabledVisuals = config.visual.enabledVisuals.get()
        if (VElement.PET_ICON !in enabledVisuals) return null
        val itemStack = getItemStackOrNull() ?: return null
        val spinDirection = config.visual.spinDirection.get()

        val baseItemRenderable: ItemStackRenderable = when {
            spinDirection != SDirection.NONE -> {
                val multiplier = if (spinDirection == SDirection.CLOCKWISE) -1 else 1
                val degreesPerSecond = (360 / config.visual.spinFrequency.get()) * multiplier
                AnimatedItemStackRenderable(
                    itemStack,
                    scale = BASE_ITEM_SCALE,
                    rotation = ItemStackRotationDefinition(
                        axis = EnumFacing.Axis.Y,
                        rotationSpeed = degreesPerSecond,
                    ),
                )
            }
            else -> ItemStackRenderable(
                itemStack,
                scale = BASE_ITEM_SCALE,
            )
        }
        baseItemRenderable.renderBounds(Color.RED)
        if (VElement.RARITY_BACKGROUND !in enabledVisuals) return baseItemRenderable
        val rarityBackgroundRenderable = CircularContainerRenderable(
            baseItemRenderable,
            rarity.color.toColor(),
            padding = 4,
        )
        val borderedRarityBackgroundRenderable = CircularContainerRenderable(
            rarityBackgroundRenderable,
            Color.GRAY,
            padding = 6,
        )
        if (VElement.XP_RING !in enabledVisuals) return borderedRarityBackgroundRenderable
        val xpRingCompleteRenderable = CircularContainerRenderable(
            borderedRarityBackgroundRenderable,
            Color.cyan,
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
                    TElement.PET_LEVEL, TElement.SKIN_SYMBOL, TElement.NEXT_LEVEL_PERCENTAGE -> return@mapNotNull null

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
                        val overflowFormat = overflowXp.formatExpByConfigOption()
                        "§7+§b$overflowFormat"
                    }
                    TElement.TOTAL_XP -> {
                        val totalXp = exp?.takeIf { totalXp -> totalXp > 0.0 } ?: return@mapNotNull null
                        "§b$totalXp"
                    }
                    TElement.NEXT_LEVEL_PROGRESS -> {
                        if (level == PetUtils.getMaxLevel(petInternalName)) return@mapNotNull null

                        val currentExp = exp ?: 0.0
                        val currentXpOverLevel = currentExp - currentLevelXp
                        val percentageFormat = if (TElement.NEXT_LEVEL_PERCENTAGE in enabledTexts) {
                            " §7- §e${levelProgressionPercentage.shortFormat()}"
                        } else ""
                        formatExpPairByConfigOption(currentXpOverLevel, nextLevelXp) + percentageFormat
                    }
                }
            }.map { (textElement, textElementFormat) ->
                val labelFormat = if (config.text.textLabels.get()) {
                    when (textElement) {
                        TElement.PET_LEVEL,
                        TElement.SKIN_SYMBOL,
                        TElement.NEXT_LEVEL_PERCENTAGE,
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
            val textLocation = config.text.textLocation.get()
            val orderedList = when (textLocation) {
                TLO.TOP, TLO.LEFT -> listOf(textRenderable, itemRenderable)
                TLO.BOTTOM, TLO.RIGHT -> listOf(itemRenderable, textRenderable)
                else -> listOf()
            }
            when (textLocation) {
                TLO.TOP, TLO.BOTTOM -> VerticalContainerRenderable(orderedList, spacing = 2)
                TLO.LEFT, TLO.RIGHT -> HorizontalContainerRenderable(orderedList, spacing = 2)
                else -> return null
            }
        } else listOf(textRenderable, itemRenderable).firstOrNull { it != null }
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.enabled,

            config.visual.enabledVisuals,
            config.visual.spinDirection,
            config.visual.spinFrequency,
            config.visual.skinAnimation,

            config.text.enabledTexts,
            config.text.textLabels,
            config.text.xpFormat,
            config.text.textLocation,
            config.text.verticalAlign,
            config.text.horizontalAlign,
        ) { lastPetHash = 0 }
    }

    private val seedRenderable by lazy {
        AnimatedItemStackRenderable(
            "BOX_OF_SEEDS".toInternalName().getItemStack(),
            scale = 1.9,
            rotation = ItemStackRotationDefinition(
                axis = EnumFacing.Axis.Y,
                rotationSpeed = 10.0,
            ),
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        Position(100, 100).renderRenderable(seedRenderable, posLabel = "Seed")
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
