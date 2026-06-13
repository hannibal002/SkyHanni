package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.api.pet.PetStorageApi
import at.hannibal2.skyhanni.config.features.pets.display.PetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.text.PetTextDisplaySettings
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.BorderRingConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetOrganizationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig.IconRotationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.PetItemConfig
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
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
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
import at.hannibal2.skyhanni.utils.renderables.primitives.CircularRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private typealias TElement = TextPetDisplayConfig.TextElement
private typealias TLO = TextPetDisplayConfig.TextLocationOption
private typealias NFE = TextPetDisplayConfig.NumberFormatEntry
private typealias ESTextMode = TextPetDisplayConfig.ExpSharePetTextConfig.TextMode
private typealias ETextCenter = TextPetDisplayConfig.EquippedPetTextConfig.CenterTarget
private typealias ESBundledLocation = TextPetDisplayConfig.ExpSharePetTextConfig.BundledTextLocation
private typealias EXPSharePlace = ExpSharePetOrganizationConfig.ExpShareLocationOption
private typealias EXPShareGO = ExpSharePetOrganizationConfig.GroupOrientation

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

    private data class AnchoredRenderable(
        val renderable: Renderable,
        val anchorX: Int,
        val anchorY: Int,
        val anchorWidth: Int,
        val anchorHeight: Int,
    )

    private fun Renderable.anchorToSelf() = AnchoredRenderable(
        renderable = this,
        anchorX = 0,
        anchorY = 0,
        anchorWidth = width,
        anchorHeight = height,
    )

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
        val activeExpSharePets = PetStorageApi.getActiveExpSharePetUuids()
        val disabledExpSharePets = PetStorageApi.getDisabledExpSharePetUuids()
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
            val subBodyWidth = expShareRenderables.maxOfOrNull { it.width } ?: 0
            val subBodyHeight = expShareRenderables.maxOfOrNull { it.height } ?: 0
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

            val orderedList = when (placement) {
                EXPSharePlace.TOP, EXPSharePlace.LEFT -> listOf(expShareContainer, this)
                EXPSharePlace.BOTTOM, EXPSharePlace.RIGHT -> listOf(this, expShareContainer)
                else -> return anchorToSelf()
            }

            val iconGroupSpacing = 2
            val renderable = when (placement) {
                EXPSharePlace.TOP, EXPSharePlace.BOTTOM -> Renderable.vertical(orderedList, spacing = iconGroupSpacing)
                EXPSharePlace.LEFT, EXPSharePlace.RIGHT -> Renderable.horizontal(orderedList, spacing = iconGroupSpacing)
                else -> return anchorToSelf()
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

    private fun buildCircularContainer(
        root: Renderable,
        backgroundColor: ChromaColour,
        filledPercentage: Double = 100.0,
        unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
        padding: Int = 2,
        preview: Boolean,
    ): Renderable = if (preview) {
        PreviewCircularContainerRenderable(root, backgroundColor, filledPercentage, unfilledColor, padding)
    } else {
        PetDisplayCircularContainerRenderable(root, backgroundColor, filledPercentage, unfilledColor, padding)
    }

    private fun Renderable.wrapInPetItemOrSelf(
        enabled: Boolean,
        petData: PetData,
        petItemConfig: PetItemConfig,
        opacity: Float = 1.0f,
    ): Renderable = if (!enabled) this else petData.heldItemInternalName?.getItemStackOrNull()?.let {
        PetItemOverlayRenderable(
            root = this,
            item = Renderable.item(it) {
                scale = petItemConfig.scale.get().toDouble()
                alpha = opacity
            },
            placement = petItemConfig.placement.get(),
        )
    } ?: this

    private class PetItemOverlayRenderable(
        private val root: Renderable,
        private val item: Renderable,
        private val placement: PetItemConfig.PetItemPlacement,
    ) : Renderable {
        override val width = root.width
        override val height = root.height
        override val horizontalAlign = root.horizontalAlign
        override val verticalAlign = root.verticalAlign

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            root.render(mouseOffsetX, mouseOffsetY)
            val itemX = anchorX() - item.width / 2
            val itemY = anchorY() - item.height / 2
            DrawContextUtils.translated(itemX, itemY) {
                item.render(mouseOffsetX + itemX, mouseOffsetY + itemY)
            }
        }

        private fun anchorX(): Int = when (placement.horizontal) {
            RenderUtils.HorizontalAlignment.LEFT -> 0
            RenderUtils.HorizontalAlignment.CENTER -> width / 2
            RenderUtils.HorizontalAlignment.RIGHT -> width
            RenderUtils.HorizontalAlignment.DONT_ALIGN -> width / 2
        }

        private fun anchorY(): Int = when (placement.vertical) {
            RenderUtils.VerticalAlignment.TOP -> 0
            RenderUtils.VerticalAlignment.CENTER -> height / 2
            RenderUtils.VerticalAlignment.BOTTOM -> height
            RenderUtils.VerticalAlignment.DONT_ALIGN -> height / 2
        }
    }

    private class PreviewCircularContainerRenderable(
        private val root: Renderable,
        private val backgroundColor: ChromaColour,
        private val filledPercentage: Double,
        private val unfilledColor: ChromaColour,
        private val padding: Int,
    ) : Renderable {
        private val radius = (max(root.width, root.height) / 2) + padding
        private val takenSpace = 2 * (radius - padding)
        override val width = radius * 2
        override val height = radius * 2
        override val horizontalAlign = root.horizontalAlign
        override val verticalAlign = root.verticalAlign
        private val circleSegments = buildCircleSegments()

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            circleSegments.forEach { it.render() }
            DrawContextUtils.translated(padding.toFloat(), padding.toFloat()) {
                root.renderCentered(mouseOffsetX + padding, mouseOffsetY + padding, takenSpace, takenSpace)
            }
        }

        private fun buildCircleSegments(): List<CircleSegment> {
            if (filledPercentage >= 100.0) {
                return buildCirclePixels(backgroundColor.toColor().rgb) { _, _ -> true }
            }

            val baseAngle = Math.PI.toFloat() * 3f / 2f
            val endAngle = (baseAngle + ((100.0 - filledPercentage) / 50.0 * Math.PI).toFloat())
                .mod(2f * Math.PI.toFloat())
            return buildCircleArc(backgroundColor.toColor().rgb, baseAngle, endAngle) +
                buildCircleArc(unfilledColor.toColor().rgb, endAngle, baseAngle)
        }

        private fun buildCircleArc(color: Int, startAngle: Float, endAngle: Float): List<CircleSegment> {
            return buildCirclePixels(color) { dx, dy ->
                angleBetween(
                    atan2(-dy, -dx).toFloat().mod(TAU),
                    startAngle,
                    endAngle,
                )
            }
        }

        private fun buildCirclePixels(color: Int, includeSample: (Double, Double) -> Boolean): List<CircleSegment> = buildList {
            val radiusSquared = radius.toDouble() * radius
            val alpha = color ushr 24 and 0xFF
            val rgb = color and 0x00FFFFFF
            for (y in 0 until width) {
                var runStart: Int? = null
                var runColor = 0
                for (x in 0 until width) {
                    var samplesInside = 0
                    for (sampleY in SAMPLE_OFFSETS) {
                        for (sampleX in SAMPLE_OFFSETS) {
                            val dx = x + sampleX - radius
                            val dy = y + sampleY - radius
                            if (dx * dx + dy * dy <= radiusSquared && includeSample(dx, dy)) samplesInside++
                        }
                    }

                    val sampleColor = if (samplesInside > 0) {
                        ((alpha * samplesInside / SAMPLE_COUNT) shl 24) or rgb
                    } else 0

                    if (sampleColor == runColor) continue
                    runStart?.let { add(CircleSegment(it, y, x, runColor)) }
                    runStart = if (sampleColor == 0) null else x
                    runColor = sampleColor
                }
                runStart?.let {
                    add(CircleSegment(it, y, width, runColor))
                }
            }
        }

        private data class CircleSegment(
            val startX: Int,
            val y: Int,
            val endX: Int,
            val color: Int,
        ) {
            fun render() {
                GuiRenderUtils.drawRect(startX, y, endX, y + 1, color)
            }
        }

        private fun angleBetween(angle: Float, startAngle: Float, endAngle: Float): Boolean {
            return if (startAngle <= endAngle) {
                angle in startAngle..endAngle
            } else {
                angle >= startAngle || angle <= endAngle
            }
        }

        private companion object {
            private val SAMPLE_OFFSETS = DoubleArray(4) { (it + 0.5) / 4.0 }
            private const val SAMPLE_COUNT = 16
            private val TAU = (2.0 * Math.PI).toFloat()
        }
    }

    private class PetDisplayCircularContainerRenderable(
        private val root: Renderable,
        backgroundColor: ChromaColour,
        filledPercentage: Double = 100.0,
        unfilledColor: ChromaColour = Color.LIGHT_GRAY.toChromaColor(255),
        private val padding: Int = 2,
    ) : CircularRenderable(
        backgroundColor,
        radius = (max(root.width, root.height) / 2) + padding,
        1f,
        filledPercentage,
        unfilledColor,
        root.horizontalAlign,
        root.verticalAlign,
    ) {
        private val takenSpace = 2 * (radius - padding)

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            super.render(mouseOffsetX, mouseOffsetY)
            DrawContextUtils.translated(padding.toFloat(), padding.toFloat()) {
                root.renderCentered(mouseOffsetX + padding, mouseOffsetY + padding, takenSpace, takenSpace)
            }
        }
    }

    private fun Renderable.renderCentered(mouseOffsetX: Int, mouseOffsetY: Int, xSpace: Int, ySpace: Int) {
        val xOffset = (xSpace - width) / 2f
        val yOffset = (ySpace - height) / 2f
        DrawContextUtils.translated(xOffset, yOffset) {
            render(mouseOffsetX + xOffset.roundToInt(), mouseOffsetY + yOffset.roundToInt())
        }
    }

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

    private fun PetData.buildTextRenderableOrNull(
        textConfig: PetTextDisplaySettings,
        opacity: Float = 1.0f,
        textScale: Double = textConfig.textScale.get().toDouble(),
    ): Renderable? {
        val enabledTexts = textConfig.enabledTexts.get().takeIfNotEmpty() ?: return null
        val textColor = Color(255, 255, 255, (255 * opacity).roundToInt().coerceIn(0, 255))
        val xpFormat = textConfig.xpFormat.get()
        val lines = enabledTexts.mapNotNull {
            it to when (it) {
                TElement.PET_NAME -> getUserFriendlyName(
                    includeLevel = textConfig.nameLevel.get(),
                    includeSkinTag = textConfig.nameSkinSymbol.get(),
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
                    val percentageFormat = if (textConfig.nextLevelPercent.get()) {
                        " §7- §e${levelProgressionPercentage.formatLevelProgressionPercentage()}%"
                    } else ""
                    formatExpPairByConfigOption(currentXpOverLevel, neededXp, xpFormat) + percentageFormat
                }
            }
        }.map { (textElement, textElementFormat) ->
            val labelFormat = textElement.getFormattedLabel().takeIf { textConfig.textLabels.get() }.orEmpty()
            StringRenderable(
                "$labelFormat$textElementFormat",
                scale = textScale,
                color = textColor,
                horizontalAlign = textConfig.horizontalAlign.get()
            )
        }.takeIfNotEmpty() ?: return null
        return Renderable.vertical(
            lines,
            horizontalAlign = textConfig.horizontalAlign.get(),
            verticalAlign = textConfig.verticalAlign.get(),
        )
    }

    private fun List<ExpSharePetState>.buildBundledExpShareTextRenderables(): List<Renderable> {
        val textConfig = config.text.expSharePets
        if (!textConfig.enabled.get()) return emptyList()
        if (textConfig.textMode.get() != ESTextMode.BUNDLED_WITH_MAIN) return emptyList()
        return mapNotNull {
            it.petData.buildTextRenderableOrNull(
                textConfig = textConfig,
                opacity = it.opacity,
            )
        }
    }

    private fun Double.formatExpByConfigOption(xpFormat: NFE) = when (xpFormat) {
        NFE.DEFAULT, NFE.UNFORMATTED -> toLong().addSeparators()
        NFE.FORMATTED -> toLong().shortFormat()
    }

    private fun Double.formatLevelProgressionPercentage(): String {
        val rounded = coerceIn(0.0, 100.0).roundTo(1)
        return String.format(Locale.US, "%.1f", rounded)
    }

    private fun ChromaColour.withOpacity(opacity: Float): ChromaColour {
        val color = toColor()
        val alpha = (color.alpha * opacity).roundToInt().coerceIn(0, 255)
        return color.toChromaColor(alpha)
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

    private fun combineVisualAndTextRenderables(
        itemRenderable: AnchoredRenderable?,
        textRenderable: Renderable?,
        textLocation: TLO,
        centerTarget: ETextCenter,
    ): Renderable? {
        return if (itemRenderable != null && textRenderable != null) {
            if (centerTarget == ETextCenter.EQUIPPED_PET_VISUALS) {
                return combineAnchoredVisualAndTextRenderables(itemRenderable, textRenderable, textLocation)
            }

            val visualRenderable = itemRenderable.renderable
            val orderedList = when (textLocation) {
                TLO.TOP, TLO.LEFT -> listOf(textRenderable, visualRenderable)
                TLO.BOTTOM, TLO.RIGHT -> listOf(visualRenderable, textRenderable)
            }
            when (textLocation) {
                TLO.TOP, TLO.BOTTOM -> Renderable.vertical(orderedList, spacing = 2)
                TLO.LEFT, TLO.RIGHT -> Renderable.horizontal(orderedList, spacing = 2)
            }
        } else textRenderable ?: itemRenderable?.renderable
    }

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

    private fun combineMainAndExpShareTextRenderables(
        mainTextRenderable: Renderable?,
        expShareTextRenderables: List<Renderable>,
    ): Renderable? {
        if (expShareTextRenderables.isEmpty()) return mainTextRenderable
        val renderables = when (config.text.expSharePets.bundledLocation.get()) {
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
            spacing = config.text.expSharePets.bundledSpacing.get(),
            horizontalAlign = config.text.equippedPet.horizontalAlign.get(),
            verticalAlign = config.text.equippedPet.verticalAlign.get(),
        )
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
            storage?.pets,
            expSharePets.map { it.petData.uuid to it.petData.exp to it.disabled },
        ).hashCode()
        val cache = if (preview) previewRenderCache else liveRenderCache
        if (cache?.key == displayHash) return cache.renderable

        val itemRenderable = buildMainIconRenderableOrNull(preview)
            ?.let { if (currentPetUuid != null) it.wrapInExpShareIconsOrSelf(expSharePets, preview) else it.anchorToSelf() }
        val mainTextRenderable = buildTextRenderableOrNull(config.text.equippedPet)
        val expShareTextRenderables = expSharePets.buildBundledExpShareTextRenderables()
        val textRenderable = combineMainAndExpShareTextRenderables(
            mainTextRenderable,
            expShareTextRenderables,
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

    private fun IconConfig.renderHash(): Int = listOf(
        enabled.get(),
        skinAnimation.get(),
        skinAnimationSpeed.get(),
        scale.get(),
        rotation.renderHash(),
    ).hashCode()

    private fun IconRotationConfig.renderHash(): Int = listOf(
        staticRotation.xRotation.get(),
        staticRotation.yRotation.get(),
        staticRotation.zRotation.get(),
        spinRotation.speedX.get(),
        spinRotation.speedY.get(),
        spinRotation.speedZ.get(),
    ).hashCode()

    private fun PetItemConfig.renderHash(): Int = listOf(
        enabled.get(),
        placement.get(),
        scale.get(),
    ).hashCode()

    private fun VisualPetDisplayConfig.BackgroundColorConfig.renderHash(): Int = listOf(
        enabled.get(),
        customization.renderHash(),
        borderRing.renderHash(),
    ).hashCode()

    private fun RarityBackgroundConfig.renderHash(): Int = listOf(
        padding.get(),
        commonColor.get(),
        uncommonColor.get(),
        rareColor.get(),
        epicColor.get(),
        legendaryColor.get(),
        mythicColor.get(),
    ).hashCode()

    private fun BorderRingConfig.renderHash(): Int = listOf(
        enabled.get(),
        customization.padding.get(),
        customization.filledColor.get(),
        customization.unfilledColor.get(),
        separator.enabled.get(),
        separator.padding.get(),
        separator.color.get(),
    ).hashCode()

    private fun ExpSharePetDisplayConfig.renderHash(): Int = listOf(
        enabled.get(),
        organization.placement.get(),
        organization.groupOrientation.get(),
        organization.subOrbit.orbitDistance.get(),
        organization.subOrbit.orbitDirection.get(),
        organization.subOrbit.orbitSpeed.get(),
        icon.renderHash(),
        icon.iconSpacing.get(),
        petItem.renderHash(),
        rarityBackground.renderHash(),
        activeSlotsOnly.get(),
        disabledOpacity.get(),
    ).hashCode()

    private fun PetTextDisplaySettings.settingsRenderHash(): Int = listOf(
        enabledTexts.get().toList(),
        textLabels.get(),
        nameLevel.get(),
        nameSkinSymbol.get(),
        nextLevelPercent.get(),
        xpFormat.get(),
        textScale.get(),
        textLocation.get(),
        verticalAlign.get(),
        horizontalAlign.get(),
    ).hashCode()

    private fun TextPetDisplayConfig.ExpSharePetTextConfig.renderHash(): Int = listOf(
        enabled.get(),
        textMode.get(),
        bundledLocation.get(),
        bundledSpacing.get(),
        settingsRenderHash(),
    ).hashCode()

    private fun TextPetDisplayConfig.renderHash(): Int = listOf(
        equippedPet.settingsRenderHash(),
        equippedPet.centerTarget.get(),
        expSharePets.renderHash(),
    ).hashCode()

    private fun VisualPetDisplayConfig.renderHash(): Int = listOf(
        icon.renderHash(),
        rarityBackground.renderHash(),
        petItem.renderHash(),
    ).hashCode()

    private fun PetDisplayConfig.renderHash(): Int = listOf(
        general.enabled.get(),
        visual.equippedPet.renderHash(),
        visual.expSharePets.renderHash(),
        text.renderHash(),
    ).hashCode()

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
        if (!PetStorageApi.isExpSharingInventory(InventoryUtils.openInventoryName())) return
        val slot = event.slot?.containerSlot ?: return
        if (!PetStorageApi.isExpShareSlotDisabled(slot)) return

        event.toolTip.add("")
        event.toolTip.add("§cThis Exp Share slot is disabled.")
        event.toolTip.add("§7Diana's §d${Perk.SHARING_IS_CARING.perkName} §7perk is not active.")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.HOTBAR) return
        if (RiftApi.inRift() || !config.general.enabled.get()) return
        val currentPet = CurrentPetApi.currentPet ?: return invalidateRenderable()
        currentPet.withAnimatedExp().buildRenderable()?.also {
            config.general.position.renderRenderable(it, posLabel = "Pet Display")
        }
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

    private fun MutableList<String>.addIconDebug(label: String, icon: IconConfig) {
        add("$label.enabled: ${icon.enabled.get()}")
        add("$label.skinAnimation: ${icon.skinAnimation.get()}")
        add("$label.skinAnimationSpeed: ${icon.skinAnimationSpeed.get()}")
        add("$label.scale: ${icon.scale.get()}")
    }

    private data class RenderCache(val key: Int, val renderable: Renderable?)
    private data class VisualIconLayer(val renderable: Renderable, val backgroundEnabled: Boolean)
    private data class ExpSharePetState(val petData: PetData, val disabled: Boolean) {
        val opacity get() = if (disabled) expShareConfig.disabledOpacity.get() else 1.0f
    }

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
