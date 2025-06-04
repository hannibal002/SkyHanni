package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RarityBackgroundConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.VisualCustomizationConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.CircularContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.OrbitDirection
import at.hannibal2.skyhanni.utils.renderables.OrbitSystemRenderable
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
typealias TLO = TextPetDisplayConfig.TextLocationOption
typealias NFE = TextPetDisplayConfig.NumberFormatEntry
typealias EXPSharePlace = ExpSharePetConfig.ExpShareLocationOption
typealias EXPShareGO = ExpSharePetDisplayConfig.GroupOrientation

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val customizationConfig get() = config.visual.customization
    private val expShareConfig get() = customizationConfig.expSharePet
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null
    private val EXP_SHARE = "PET_ITEM_EXP_SHARE".toInternalName()

    private fun LorenzRarity.getRarityBackgroundColor(
        backgroundConfig: RarityBackgroundConfig
    ): ChromaColour = with(backgroundConfig) {
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

    private fun PetData.buildMainIconRenderableOrNull(): Renderable? {
        if (!config.visual.icon.get()) return null

        val baseItemRenderable = buildBaseItemRenderable(
            spinDirection = config.visual.iconSpin.direction.get(),
            spinFrequency = config.visual.iconSpin.frequency.get(),
            iconScale = config.visual.iconScale.get(),
            skinAnimation = config.visual.skinAnimation.get(),
        )

        val backgroundEnabled = config.visual.rarityBackground.get()
        val backgroundWrappedRenderable = baseItemRenderable.wrapInBackgroundColorOrSelf(
            enabled = backgroundEnabled,
            backgroundConfig = customizationConfig.rarityBackground,
            rarity = rarity,
        )

        val separatorEnabled = config.visual.separatorRing.get()
        val xpRingEnabled = config.visual.xpRing.get()
        val separatorWrappedRenderable = backgroundWrappedRenderable.wrapInRingOrSelf(
            enabled = xpRingEnabled && separatorEnabled,
            ringConfig = customizationConfig.separatorRing
        )

        val xpRingWrappedRenderable = if (!xpRingEnabled) separatorWrappedRenderable else CircularContainerRenderable(
            separatorWrappedRenderable,
            backgroundColor = customizationConfig.xpRing.filledColor.get(),
            unfilledColor = customizationConfig.xpRing.unfilledColor.get(),
            filledPercentage = levelProgressionPercentage,
            padding = customizationConfig.xpRing.padding.get(),
        )

        return xpRingWrappedRenderable
    }

    private fun Renderable.wrapInExpShareIconsOrSelf(): Renderable {
        if (!config.visual.expSharePet.get()) return this
        val storage = ProfileStorageData.petProfiles ?: return this
        val expShareRenderables = storage.pets.filter {
            it.uuid in storage.expSharePets || it.heldItemInternalName == EXP_SHARE
        }.mapNotNull { it.buildExpShareIconRenderableOrNull() }.takeIfNotEmpty() ?: return this

        return when (val placement: EXPSharePlace = expShareConfig.placement.get()) {
            EXPSharePlace.ORBIT -> OrbitSystemRenderable(
                this,
                subBodySpacing = expShareConfig.subOrbit.orbitDistance.get(),
                orbitSpeed = expShareConfig.subOrbit.orbitSpeed.get(),
                orbitDirection = expShareConfig.subOrbit.orbitDirection.get(),
                subBodies = expShareRenderables,
            )
            else -> {
                val expShareOrientation: EXPShareGO = expShareConfig.displayCustomization.groupOrientation.get()
                val expShareContainer = when (expShareOrientation) {
                    EXPShareGO.VERTICAL -> VerticalContainerRenderable(
                        expShareRenderables,
                        spacing = expShareConfig.displayCustomization.iconSpacing.get(),
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                    )
                    EXPShareGO.HORIZONTAL -> HorizontalContainerRenderable(
                        expShareRenderables,
                        spacing = expShareConfig.displayCustomization.iconSpacing.get(),
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                    )
                }

                val orderedList = when (placement) {
                    EXPSharePlace.TOP, EXPSharePlace.LEFT -> listOf(expShareContainer, this)
                    EXPSharePlace.BOTTOM, EXPSharePlace.RIGHT -> listOf(this, expShareContainer)
                    else -> return this
                }

                when (placement) {
                    EXPSharePlace.TOP, EXPSharePlace.BOTTOM -> VerticalContainerRenderable(orderedList, spacing = 2)
                    EXPSharePlace.LEFT, EXPSharePlace.RIGHT -> HorizontalContainerRenderable(orderedList, spacing = 2)
                    else -> this
                }
            }
        }
    }

    private fun PetData.buildExpShareIconRenderableOrNull(): Renderable? {
        val baseItemRenderable = buildBaseItemRenderable(
            spinDirection = expShareConfig.displayCustomization.iconSpin.direction.get(),
            spinFrequency = expShareConfig.displayCustomization.iconSpin.frequency.get(),
            iconScale = expShareConfig.displayCustomization.iconScale.get(),
            skinAnimation = expShareConfig.displayCustomization.skinAnimation.get(),
        )

        val backgroundEnabled = expShareConfig.displayCustomization.rarityBackground.enabled.get()
        val backgroundWrappedRenderable = baseItemRenderable.wrapInBackgroundColorOrSelf(
            enabled = backgroundEnabled,
            backgroundConfig = expShareConfig.displayCustomization.rarityBackground.customization,
            rarity = rarity,
        )

        val borderEnabled = expShareConfig.displayCustomization.borderRing.enabled.get()
        val borderWrappedRenderable = backgroundWrappedRenderable.wrapInRingOrSelf(
            enabled = borderEnabled,
            ringConfig = expShareConfig.displayCustomization.borderRing.customization,
        )

        return borderWrappedRenderable
    }

    private fun Renderable.wrapInBackgroundColorOrSelf(
        enabled: Boolean,
        backgroundConfig: RarityBackgroundConfig,
        rarity: LorenzRarity,
    ): Renderable = if (!enabled) this else CircularContainerRenderable(
        this,
        rarity.getRarityBackgroundColor(backgroundConfig),
        padding = backgroundConfig.padding.get(),
    )

    private fun Renderable.wrapInRingOrSelf(
        enabled: Boolean,
        ringConfig: VisualCustomizationConfig.RingConfig,
    ): Renderable = if (!enabled) this else CircularContainerRenderable(
        this,
        ringConfig.color.get(),
        padding = ringConfig.padding.get(),
    )

    private fun PetData.buildBaseItemRenderable(
        spinDirection: OrbitDirection,
        spinFrequency: Float,
        iconScale: Double,
        skinAnimation: Boolean,
    ): Renderable {
        val spinMultiplier = if (spinDirection == OrbitDirection.CLOCKWISE) -1 else 1
        val degreesPerSecond = if (spinDirection != OrbitDirection.NONE) ((360 / spinFrequency) * spinMultiplier) else 0.0f
        val itemStack = getItemStackOrNull()
            ?: ErrorManager.skyHanniError("Could not generate an item stack for pet!")
        return AnimatedItemStackRenderable(
            frames = getAnimatedItemStackSequence(
                firstFrameOnly = !skinAnimation
            ) ?: listOf(ItemStackAnimationFrame(itemStack)),
            scale = iconScale,
            rotation = ItemStackRotationDefinition(
                axis = EnumFacing.Axis.Y,
                rotationSpeed = degreesPerSecond.toDouble(),
            ),
        )
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

        val itemRenderable = buildMainIconRenderableOrNull()?.wrapInExpShareIconsOrSelf()
        val textRenderable = buildTextRenderableOrNull()

        return if (itemRenderable != null && textRenderable != null) {
            // Technically nullable in the JVM
            val textLocation: TLO = config.text.textLocation.get()
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
        ConditionalUtils.onToggleAll(config) {
            lastPetHash = 0
        }
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
