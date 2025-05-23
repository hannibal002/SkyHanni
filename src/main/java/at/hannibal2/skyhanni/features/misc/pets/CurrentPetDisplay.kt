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
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.AnimatedItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRotationDefinition
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import java.awt.Color

typealias VElement = PetDisplayConfig.VisualPetDisplayConfig.VisualElement
typealias TElement = PetDisplayConfig.TextPetDisplayConfig.TextElement
typealias SDirection = PetDisplayConfig.VisualPetDisplayConfig.SpinDirection
typealias TLO = PetDisplayConfig.TextPetDisplayConfig.TextLocationOption
typealias NFE = PetDisplayConfig.TextPetDisplayConfig.NumberFormatEntry

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private var rotationContext: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null
    private var currentSpinningRenderable: AnimatedItemStackRenderable? = null

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        val enabledVisuals = config.visual.enabledVisuals.get()
        if (VElement.PET_ICON !in enabledVisuals) return null
        val itemStack = getItemStackOrNull() ?: return null
        val spinDirection = config.visual.spinDirection.get()

        val baseItemRenderable = when {
            spinDirection != SDirection.NONE -> {
                val multiplier = if (spinDirection == SDirection.CLOCKWISE) -1 else 1
                val degreesPerSecond = (360 / config.visual.spinFrequency.get()) * multiplier
                currentSpinningRenderable = AnimatedItemStackRenderable(
                    itemStack,
                    scale = 1.9,
                    rotation = ItemStackRotationDefinition(
                        axis = EnumFacing.Axis.Y,
                        rotationSpeed = degreesPerSecond,
                    ),
                    initialRotation = rotationContext
                )
                currentSpinningRenderable
            }
            else -> {
                rotationContext = currentSpinningRenderable?.currentRotation ?: rotationContext
                currentSpinningRenderable = null
                ItemStackRenderable(
                    itemStack,
                    scale = 1.9,
                )
            }
        }

        return when {
            VElement.RARITY_BACKGROUND !in enabledVisuals -> baseItemRenderable
            else -> Renderable.CircularRenderable(
                itemRenderable = baseItemRenderable,
                rarity.color.toColor(),
                20,
                border = Renderable.CircularRenderable(
                    itemRenderable = null,
                    Color.GRAY,
                    26,
                    border = when {
                        VElement.XP_RING !in enabledVisuals -> null
                        else -> Renderable.CircularRenderable(
                            itemRenderable = null,
                            backgroundColor = Color.cyan,
                            radius = 29,
                            filledPercentage = levelProgressionPercentage
                        )
                    }
                )
            )
        }
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
                        ChatUtils.chat("overflowXp: $overflowXp")
                        val overflowXp = overflowXp.takeIf { overflow -> overflow > 0.0 } ?: return@mapNotNull null
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
    }

    private fun formatExpPairByConfigOption(
        firstExp: Double,
        secondExp: Double,
    ): String = when (config.text.xpFormat.get()) {
        NFE.DEFAULT -> "§b${firstExp.toInt().addSeparators()}§9/§b${secondExp.toInt().shortFormat()}"
        NFE.FORMATTED -> "§b${firstExp.toInt().shortFormat()}§9/§b${secondExp.toInt().shortFormat()}"
        NFE.UNFORMATTED -> "§b${firstExp.toInt().addSeparators()}§9/§b${secondExp.toInt().addSeparators()}"
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
