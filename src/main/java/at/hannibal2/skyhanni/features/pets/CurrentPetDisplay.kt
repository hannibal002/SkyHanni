package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.api.pet.PetStorageApi
import at.hannibal2.skyhanni.api.pet.PetStorageExpShare
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetOrganizationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig.IconRotationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RarityBackgroundConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RingConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.VisualPetDisplayConfig
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.events.pets.PetChangeEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitSystemRenderable.Companion.orbitalSystem
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.FrameTickRateProvider
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationPropertyStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AxisRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private typealias ESTextMode = TextPetDisplayConfig.ExpSharePetTextConfig.TextMode
private typealias ETextCenter = TextPetDisplayConfig.EquippedPetTextConfig.CenterTarget
private typealias EXPSharePlace = ExpSharePetOrganizationConfig.ExpShareLocationOption
private typealias EXPShareGO = ExpSharePetOrganizationConfig.GroupOrientation

internal fun PetDisplayConfig.requiresExactCurrentPetXp(): Boolean {
    val enabledTexts = text.equippedPet.enabledTexts.get()
    return TextPetDisplayConfig.TextElement.OVERFLOW_XP in enabledTexts ||
        TextPetDisplayConfig.TextElement.TOTAL_XP in enabledTexts
}

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val equippedVisualConfig get() = config.visual.equippedPet
    private val expShareConfig get() = config.visual.expSharePets
    private var liveRenderCache: RenderCache? = null
    private var previewRenderCache: RenderCache? = null
    private var observedConfig: PetDisplayConfig? = null
    private var xpAnimation: XpAnimation? = null
    private val expShareXpAnimations = mutableMapOf<UUID, XpAnimation>()
    private var expShareOrbitAngle = 0f
    private var expShareOrbitLastRenderTime = SimpleTimeMark.now()
    private val currentRotation: Property<Vec3> = Property.of(Vec3.ZERO)
    private val EXP_SHARE = "PET_ITEM_EXP_SHARE".toInternalName()
    private val previewPet: PetData by lazy {
        PetData(
            petInternalName = "BEE;4".toInternalName(),
            skinInternalName = "PET_SKIN_BEE_RGBEE".toInternalName(),
            heldItemInternalName = EXP_SHARE,
            exp = 25_353_230.0,
        )
    }

    private fun PetData.buildMainIconRenderableOrNull(preview: Boolean): Renderable? = with(equippedVisualConfig) {
        val iconLayer = buildVisualIconLayerOrNull(this, preview) ?: return null

        val borderRingConfig = rarityBackground.borderRing
        val xpRingEnabled = borderRingConfig.enabled.get()
        val separatorWrappedRenderable = iconLayer.renderable.wrapInRingOrSelf(
            enabled = iconLayer.backgroundEnabled && xpRingEnabled && borderRingConfig.separator.enabled.get(),
            ringConfig = borderRingConfig.separator,
            preview = preview,
        )

        return if (!iconLayer.backgroundEnabled || !xpRingEnabled) separatorWrappedRenderable
        else buildCircularContainer(
            separatorWrappedRenderable,
            backgroundColor = borderRingConfig.customization.filledColor.get(),
            unfilledColor = borderRingConfig.customization.unfilledColor.get(),
            filledPercentage = levelProgressionPercentage,
            padding = borderRingConfig.customization.padding.get().roundToInt(),
            preview = preview,
        )
    }

    private fun getVisibleExpSharePetStates(currentPetUuid: UUID): List<ExpSharePetState> {
        val storage = ProfileStorageData.petProfiles ?: return emptyList()
        val activeExpSharePets = PetStorageExpShare.getActivePetUuids()
        val disabledExpSharePets = PetStorageExpShare.getDisabledPetUuids()
        return storage.pets.mapNotNull {
            it.visibleExpShareStateOrNull(currentPetUuid, activeExpSharePets, disabledExpSharePets)
        }
    }

    private fun Renderable.wrapInExpShareIconsOrSelf(expSharePets: List<ExpSharePetState>, preview: Boolean): AnchoredRenderable {
        if (!expShareConfig.enabled.get()) return anchorToSelf()
        val expShareRenderables = expSharePets.mapNotNull {
            it.buildExpShareRenderable(preview)
        }.takeIfNotEmpty() ?: return anchorToSelf()

        val organization = expShareConfig.organization
        val subOrbit = organization.subOrbit
        val placement: EXPSharePlace = organization.placement.get()
        return if (placement == EXPSharePlace.ORBIT) {
            val subBodySpacing = subOrbit.orbitDistance.get().roundToInt()
            val subBodyWidth = expShareRenderables.maxOf { it.width }
            val subBodyHeight = expShareRenderables.maxOf { it.height }
            val renderable = Renderable.orbitalSystem(
                this,
                subBodySpacing = subBodySpacing,
                orbitSpeed = subOrbit.orbitSpeed.get().toInt(),
                orbitDirection = subOrbit.orbitDirection.get(),
                subBodies = expShareRenderables,
                initialAngle = expShareOrbitAngle,
                initialLastRenderTime = expShareOrbitLastRenderTime,
                onRenderStateChange = { angle, lastRenderTime ->
                    expShareOrbitAngle = angle
                    expShareOrbitLastRenderTime = lastRenderTime
                },
            )
            AnchoredRenderable(
                renderable = renderable,
                anchorX = subBodyWidth + subBodySpacing,
                anchorY = subBodyHeight + subBodySpacing,
                anchorWidth = width,
                anchorHeight = height,
            )
        } else {
            val expShareContainer = when (organization.groupOrientation.get()) {
                EXPShareGO.VERTICAL -> Renderable.vertical(
                    expShareRenderables,
                    spacing = expShareConfig.icon.iconSpacing.get().roundToInt(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                )
                EXPShareGO.HORIZONTAL -> Renderable.horizontal(
                    expShareRenderables,
                    spacing = expShareConfig.icon.iconSpacing.get().roundToInt(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                )
            }

            val iconGroupSpacing = 2
            val orderedList = if (placement == EXPSharePlace.TOP || placement == EXPSharePlace.LEFT) {
                listOf(expShareContainer, this)
            } else listOf(this, expShareContainer)
            val renderable = if (placement == EXPSharePlace.TOP || placement == EXPSharePlace.BOTTOM) {
                Renderable.vertical(orderedList, spacing = iconGroupSpacing)
            } else {
                Renderable.horizontal(orderedList, spacing = iconGroupSpacing)
            }
            val anchorX = when (placement) {
                EXPSharePlace.LEFT -> expShareContainer.width + iconGroupSpacing
                EXPSharePlace.RIGHT -> 0
                else -> RenderableUtils.calculateAlignmentXOffset(this, renderable.width)
            }
            val anchorY = when (placement) {
                EXPSharePlace.TOP -> expShareContainer.height + iconGroupSpacing
                EXPSharePlace.BOTTOM -> 0
                else -> RenderableUtils.calculateAlignmentYOffset(this, renderable.height)
            }

            AnchoredRenderable(
                renderable = renderable,
                anchorX = anchorX,
                anchorY = anchorY,
                anchorWidth = width,
                anchorHeight = height,
            )
        }
    }

    private fun PetData.visibleExpShareStateOrNull(
        currentPetUuid: UUID,
        activeExpSharePets: Set<UUID>,
        disabledExpSharePets: Set<UUID>,
    ): ExpSharePetState? {
        val petUuid = uuid ?: return null
        if (petUuid == currentPetUuid) return null
        if (petUuid in activeExpSharePets) {
            return ExpSharePetState(this, disabled = false)
        }
        if (petUuid in disabledExpSharePets && !expShareConfig.activeSlotsOnly.get()) {
            return ExpSharePetState(this, disabled = true)
        }
        return null
    }

    private fun ExpSharePetState.buildExpShareRenderable(preview: Boolean): Renderable? {
        val itemRenderable = petData.buildExpShareIconRenderable(preview, opacity)
        val textConfig = config.text.expSharePets
        val textRenderable = if (
            textConfig.enabled.get() &&
            textConfig.textMode.get() == ESTextMode.ATTACHED_TO_ICONS
        ) petData.buildTextRenderableOrNull(
            textConfig = textConfig,
            opacity = opacity,
            textScale = textConfig.textScale.get().toDouble(),
        ) else null
        return combineVisualAndTextRenderables(
            itemRenderable?.anchorToSelf(),
            textRenderable,
            textConfig.textLocation.get(),
            ETextCenter.ALL_PET_VISUALS,
        )
    }

    private fun PetData.buildExpShareIconRenderable(preview: Boolean, opacity: Float): Renderable? {
        val iconLayer = buildVisualIconLayerOrNull(
            visualConfig = expShareConfig,
            preview = preview,
            rotationPropGetter = null,
            opacity = opacity,
        ) ?: return null

        val borderRingConfig = expShareConfig.rarityBackground.borderRing
        val xpRingEnabled = borderRingConfig.enabled.get()
        val separatorWrappedRenderable = iconLayer.renderable.wrapInRingOrSelf(
            enabled = iconLayer.backgroundEnabled && xpRingEnabled && borderRingConfig.separator.enabled.get(),
            ringConfig = borderRingConfig.separator,
            preview = preview,
            opacity = opacity,
        )

        return if (!iconLayer.backgroundEnabled || !xpRingEnabled) separatorWrappedRenderable
        else buildCircularContainer(
            separatorWrappedRenderable,
            backgroundColor = borderRingConfig.customization.filledColor.get().withOpacity(opacity),
            unfilledColor = borderRingConfig.customization.unfilledColor.get().withOpacity(opacity),
            filledPercentage = levelProgressionPercentage,
            padding = borderRingConfig.customization.padding.get().roundToInt(),
            preview = preview,
        )
    }

    private fun PetData.buildVisualIconLayerOrNull(
        visualConfig: VisualPetDisplayConfig,
        preview: Boolean,
        rotationPropGetter: (() -> Property<Vec3>)? = { currentRotation },
        opacity: Float = 1.0f,
    ): VisualIconLayer? = with(visualConfig) {
        if (!icon.enabled.get()) return null
        val baseItemRenderable = buildBaseItemRenderable(
            rotationConfig = icon.rotation,
            iconScale = icon.scale.get().toDouble(),
            useSkinAnimations = icon.skinAnimation.get(),
            skinAnimationSpeed = icon.skinAnimationSpeed.get(),
            rotationPropGetter = rotationPropGetter,
            opacity = opacity,
        ) ?: return null

        val petItemWrappedRenderable = baseItemRenderable.wrapInPetItemOrSelf(
            enabled = petItem.enabled.get(),
            petData = this@buildVisualIconLayerOrNull,
            petItemConfig = petItem,
            opacity = opacity,
        )

        val backgroundEnabled = rarityBackground.enabled.get()
        val backgroundWrappedRenderable = petItemWrappedRenderable.wrapInBackgroundColorOrSelf(
            enabled = backgroundEnabled,
            backgroundConfig = rarityBackground.customization,
            rarity = rarity,
            preview = preview,
            opacity = opacity,
        )
        VisualIconLayer(backgroundWrappedRenderable, backgroundEnabled)
    }

    private fun Renderable.wrapInBackgroundColorOrSelf(
        enabled: Boolean,
        backgroundConfig: RarityBackgroundConfig,
        rarity: LorenzRarity,
        preview: Boolean,
        opacity: Float,
    ): Renderable = if (!enabled) this else buildCircularContainer(
        this,
        backgroundConfig.getRarityBackgroundColor(rarity).withOpacity(opacity),
        padding = backgroundConfig.padding.get().roundToInt(),
        preview = preview,
    )

    private fun Renderable.wrapInRingOrSelf(
        enabled: Boolean,
        ringConfig: RingConfig,
        preview: Boolean,
        opacity: Float = 1.0f,
    ): Renderable = if (!enabled) this else buildCircularContainer(
        this,
        ringConfig.color.get().withOpacity(opacity),
        padding = ringConfig.padding.get().roundToInt(),
        preview = preview,
    )

    private fun PetData.buildBaseItemRenderable(
        rotationConfig: IconRotationConfig,
        iconScale: Double,
        useSkinAnimations: Boolean,
        skinAnimationSpeed: Float,
        rotationPropGetter: (() -> Property<Vec3>)? = { currentRotation },
        opacity: Float = 1.0f,
    ): Renderable? {
        val frames = getAnimatedItemStackSequence(
            firstFrameOnly = !useSkinAnimations,
            animationSpeed = skinAnimationSpeed,
        ) ?: getItemStackOrNull()?.let { listOf(ItemStackAnimatedFrame(it)) } ?: return null
        return Renderable.animatedItemStack {
            frameStorage = AnimatedFrameLocalStorage(frames, FrameTickRateProvider.ofFrame())
            val rotationDefinition = AnimatedRotationDefinition(
                Direction.Axis.X to AxisRotationDefinition(
                    staticRotation = rotationConfig.staticRotation.xRotation.get().toDouble(),
                    rotationSpeed = rotationConfig.spinRotation.speedX.get().toDouble(),
                ),
                Direction.Axis.Y to AxisRotationDefinition(
                    rotationSpeed = rotationConfig.spinRotation.speedY.get().toDouble(),
                    staticRotation = rotationConfig.staticRotation.yRotation.get().toDouble(),
                ),
                Direction.Axis.Z to AxisRotationDefinition(
                    rotationSpeed = rotationConfig.spinRotation.speedZ.get().toDouble(),
                    staticRotation = rotationConfig.staticRotation.zRotation.get().toDouble(),
                ),
            )
            rotationStorage = if (rotationPropGetter != null) {
                AnimatedRotationPropertyStorage(rotationDefinition, rotationPropGetter)
            } else {
                AnimatedRotationLocalStorage(rotationDefinition)
            }
            scale = iconScale
            xSpacing = 1
            ySpacing = 1
            rescaleSkulls = false
            alpha = opacity
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
            verticalAlign = RenderUtils.VerticalAlignment.CENTER
        }
    }

    private fun PetData.buildRenderable(preview: Boolean = false): Renderable? {
        val storage = ProfileStorageData.petProfiles
        val currentPetUuid = uuid
        val expSharePets = currentPetUuid?.let(::getVisibleExpSharePetStates).orEmpty().withAnimatedExpShare()
        val displayHash = listOf(
            hashCode(),
            ProfileStorageData.profileSpecific?.currentPetUuid,
            config.renderHash(),
            storage?.expSharePets,
            expSharePets.map { it.petData to it.disabled },
        ).hashCode()
        val cache = if (preview) previewRenderCache else liveRenderCache
        if (cache?.key == displayHash) return cache.renderable

        val itemRenderable = buildMainIconRenderableOrNull(preview)
            ?.let { if (currentPetUuid != null) it.wrapInExpShareIconsOrSelf(expSharePets, preview) else it.anchorToSelf() }
        val mainTextRenderable = buildTextRenderableOrNull(config.text.equippedPet)
        val expShareTextRenderables = expSharePets.buildBundledExpShareTextRenderables(config)
        val textRenderable = combineMainAndExpShareTextRenderables(
            mainTextRenderable,
            expShareTextRenderables,
            config.text,
        )
        val renderable = combineVisualAndTextRenderables(
            itemRenderable,
            textRenderable,
            config.text.equippedPet.textLocation.get(),
            config.text.equippedPet.centerTarget.get(),
        )

        val newCache = RenderCache(displayHash, renderable)
        if (preview) previewRenderCache = newCache else liveRenderCache = newCache
        return renderable
    }

    private fun PetData.withAnimatedExp(): PetData {
        val targetExp = exp ?: return this
        val petUuid = uuid
        val currentAnimation = xpAnimation
        if (currentAnimation == null || currentAnimation.uuid != petUuid || targetExp < currentAnimation.targetExp) {
            xpAnimation = XpAnimation(petUuid, targetExp, targetExp)
            return this
        }

        if (targetExp > currentAnimation.targetExp) {
            xpAnimation = XpAnimation(petUuid, currentAnimation.currentExp(), targetExp)
        }

        val displayedExp = xpAnimation?.currentExp() ?: targetExp
        if (displayedExp >= targetExp) {
            xpAnimation = XpAnimation(petUuid, targetExp, targetExp)
            return this
        }

        return copy(exp = displayedExp)
    }

    private fun List<ExpSharePetState>.withAnimatedExpShare(): List<ExpSharePetState> {
        val activeUuids = mapNotNull { it.petData.uuid }.toSet()
        expShareXpAnimations.keys.removeAll { it !in activeUuids }
        return map { it.copy(petData = it.petData.withAnimatedExpShare()) }
    }

    private fun PetData.withAnimatedExpShare(): PetData {
        val targetExp = exp ?: return this
        val petUuid = uuid ?: return this
        val currentAnimation = expShareXpAnimations[petUuid]
        if (currentAnimation == null || targetExp < currentAnimation.targetExp) {
            expShareXpAnimations[petUuid] = XpAnimation(petUuid, targetExp, targetExp)
            return this
        }

        if (targetExp > currentAnimation.targetExp) {
            expShareXpAnimations[petUuid] = XpAnimation(petUuid, currentAnimation.currentExp(), targetExp)
        }

        val displayedExp = expShareXpAnimations[petUuid]?.currentExp() ?: targetExp
        if (displayedExp >= targetExp) {
            expShareXpAnimations[petUuid] = XpAnimation(petUuid, targetExp, targetExp)
            return this
        }

        return copy(exp = displayedExp)
    }

    fun invalidateRenderable() {
        liveRenderCache = null
        previewRenderCache = null
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        val currentConfig = config
        if (observedConfig === currentConfig) return
        observedConfig = currentConfig
        ConditionalUtils.onAnyToggled(currentConfig) {
            invalidateRenderable()
        }
    }

    @HandleEvent(PetChangeEvent::class)
    fun onPetChange() {
        invalidateRenderable()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!PetStorageExpShare.isInventory(InventoryUtils.openInventoryName())) return
        val slot = event.slot?.containerSlot ?: return
        if (!PetStorageExpShare.isSlotDisabled(slot)) return

        event.toolTip.add("")
        event.toolTip.add("§cThis Exp Share slot is disabled.")
        event.toolTip.add("§7Diana's §d${Perk.SHARING_IS_CARING.perkName} §7perk is not active.")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.HOTBAR) return
        if (RiftApi.inRift() || !config.general.enabled.get()) return
        val widgetWarning = PetStorageApi.getPetWidgetWarning(
            config.text.equippedPet.requiresOverflowXpForAccuracy(),
        )
        val currentPet = CurrentPetApi.currentPet ?: return invalidateRenderable()
        currentPet.withAnimatedExp()
            .buildRenderable()
            ?.withWidgetWarning(widgetWarning, config.text.equippedPet)
            ?.also {
                config.general.position.renderRenderable(it, posLabel = "Pet Display")
            }
    }

    private fun TextPetDisplayConfig.EquippedPetTextConfig.requiresOverflowXpForAccuracy(): Boolean {
        val enabledTexts = enabledTexts.get()
        return TextPetDisplayConfig.TextElement.OVERFLOW_XP in enabledTexts ||
            TextPetDisplayConfig.TextElement.TOTAL_XP in enabledTexts
    }

    @HandleEvent
    fun onRenderingTick(event: RenderingTickEvent) {
        if (event.startPhase) return
        if (!PetDisplayConfigGuiManager.isOpen()) return
        if (!config.general.enabled.get()) return
        val petData = CurrentPetApi.currentPet ?: previewPet
        val renderable = petData.withAnimatedExp().buildRenderable(preview = true) ?: return
        PetDisplayConfigGuiManager.renderPreview(renderable)
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Pet Display")
        event.addIrrelevant {
            val currentPet = CurrentPetApi.currentPet
            add("enabled: ${config.general.enabled.get()}")
            add("currentPetUuid: ${currentPet?.uuid}")
            add("currentPet: ${currentPet?.getUserFriendlyName() ?: "<none>"}")
            addIconDebug("mainIcon", equippedVisualConfig.icon)
            addIconDebug("expShareIcon", expShareConfig.icon)
            add("petItemEnabled: ${equippedVisualConfig.petItem.enabled.get()}")
            add("petItemScale: ${equippedVisualConfig.petItem.scale.get()}")
            add("petItemPlacement: ${equippedVisualConfig.petItem.placement.get()}")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
        event.move(138, "misc.pets.display", "misc.pets.display.general.enabled")
        event.move(138, "misc.pets.displayPos", "misc.pets.display.general.position")
    }

    private fun MutableList<String>.addIconDebug(label: String, icon: IconConfig) {
        add("$label.enabled: ${icon.enabled.get()}")
        add("$label.skinAnimation: ${icon.skinAnimation.get()}")
        add("$label.skinAnimationSpeed: ${icon.skinAnimationSpeed.get()}")
        add("$label.scale: ${icon.scale.get()}")
    }

    private data class RenderCache(val key: Int, val renderable: Renderable?)
    private val ExpSharePetState.opacity get() = if (disabled) expShareConfig.disabledOpacity.get() else 1.0f

    private data class XpAnimation(
        val uuid: UUID?,
        val startExp: Double,
        val targetExp: Double,
        val startedAt: SimpleTimeMark = SimpleTimeMark.now(),
    ) {
        fun currentExp(): Double {
            if (startExp == targetExp) return targetExp
            val progress = (startedAt.passedSince() / XP_ANIMATION_DURATION).coerceIn(0.0, 1.0)
            return startExp + (targetExp - startExp) * easeOutCubic(progress)
        }
    }

    private val XP_ANIMATION_DURATION = 750.milliseconds

    private fun easeOutCubic(progress: Double): Double {
        val inverse = 1.0 - progress
        return 1.0 - inverse * inverse * inverse
    }
}
