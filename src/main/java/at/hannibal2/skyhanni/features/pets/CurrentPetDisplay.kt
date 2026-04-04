package at.hannibal2.skyhanni.features.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.pets.display.text.TextPetDisplayConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.ExpSharePetOrganizationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.IconConfig.IconRotationConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.PetItemConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RarityBackgroundConfig
import at.hannibal2.skyhanni.config.features.pets.display.visual.RingConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitDirection
import at.hannibal2.skyhanni.utils.renderables.animated.OrbitSystemRenderable.Companion.orbitalSystem
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationPropertyStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AxisRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.decorators.CircularContainerRenderable.Companion.circularContainer
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import java.util.UUID

private typealias TElement = TextPetDisplayConfig.TextElement
private typealias TLO = TextPetDisplayConfig.TextLocationOption
private typealias NFE = TextPetDisplayConfig.NumberFormatEntry
private typealias EXPSharePlace = ExpSharePetOrganizationConfig.ExpShareLocationOption
private typealias EXPShareGO = ExpSharePetOrganizationConfig.GroupOrientation

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private val expShareConfig get() = config.visual.expSharePets
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null
    private val currentRotation: Property<Vec3> = Property.of(Vec3.ZERO)
    private val EXP_SHARE = "PET_ITEM_EXP_SHARE".toInternalName()
    private val previewPet: PetData by lazy {
        // Level 100 with overflow XP, used as a visual fallback when no pet is active during preview
        PetData(petInternalName = "MITHRIL_GOLEM;4".toInternalName(), exp = 25_353_230.0)
    }

    private fun PetData.buildMainIconRenderableOrNull(): Renderable? = with(config.visual) {
        if (!icon.enabled.get()) return null

        val baseItemRenderable = buildBaseItemRenderable(
            rotationConfig = icon.rotation,
            iconScale = icon.scale.get(),
            useSkinAnimations = icon.skinAnimation.get(),
        )

        val petItemWrappedRenderable = baseItemRenderable.wrapInPetItemOrSelf(
            enabled = petItem.enabled.get(),
            petData = this@buildMainIconRenderableOrNull,
            petItemConfig = petItem,
        )

        val backgroundEnabled = rarityBackground.enabled.get()
        val backgroundWrappedRenderable = petItemWrappedRenderable.wrapInBackgroundColorOrSelf(
            enabled = backgroundEnabled,
            backgroundConfig = rarityBackground.customization,
            rarity = rarity,
        )

        val borderRingConfig = rarityBackground.borderRing
        val separatorEnabled = borderRingConfig.separator.enabled.get()
        val xpRingEnabled = borderRingConfig.enabled.get()
        val separatorWrappedRenderable = backgroundWrappedRenderable.wrapInRingOrSelf(
            enabled = backgroundEnabled && xpRingEnabled && separatorEnabled,
            ringConfig = borderRingConfig.separator,
        )

        val shouldUseXpRing = backgroundEnabled && xpRingEnabled
        val xpRingWrappedRenderable = if (!shouldUseXpRing) separatorWrappedRenderable
        else Renderable.circularContainer(
            separatorWrappedRenderable,
            backgroundColor = borderRingConfig.customization.filledColor.get(),
            unfilledColor = borderRingConfig.customization.unfilledColor.get(),
            filledPercentage = levelProgressionPercentage,
            padding = borderRingConfig.customization.padding.get(),
        )

        return xpRingWrappedRenderable
    }

    private fun Renderable.wrapInExpShareIconsOrSelf(currentPetUuid: UUID): Renderable {
        if (!config.visual.expSharePets.enabled.get()) return this
        val storage = ProfileStorageData.petProfiles ?: return this
        val expShareRenderables = storage.pets.filter {
            it.uuid != currentPetUuid && it.uuid in storage.expSharePets || it.heldItemInternalName == EXP_SHARE
        }.map { it.buildExpShareIconRenderable() }.takeIfNotEmpty() ?: return this

        val organization = expShareConfig.organization
        val subOrbit = organization.subOrbit
        val placement: EXPSharePlace = organization.placement.get()
        return if (placement == EXPSharePlace.ORBIT) {
            val orbitDirection = subOrbit.orbitDirection.get()
            val orbitSpeed = when (orbitDirection) {
                OrbitDirection.NONE -> 0
                else -> subOrbit.orbitSpeed.get().toInt()
            }
            Renderable.orbitalSystem(
                this,
                subBodySpacing = subOrbit.orbitDistance.get(),
                orbitSpeed = orbitSpeed,
                orbitDirection = orbitDirection,
                subBodies = expShareRenderables,
            )
        } else {
            val expShareContainer = when (organization.groupOrientation.get()) {
                EXPShareGO.VERTICAL -> Renderable.vertical(
                    expShareRenderables,
                    spacing = expShareConfig.icon.iconSpacing.get(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                )
                EXPShareGO.HORIZONTAL -> Renderable.horizontal(
                    expShareRenderables,
                    spacing = expShareConfig.icon.iconSpacing.get(),
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
                EXPSharePlace.TOP, EXPSharePlace.BOTTOM -> Renderable.vertical(orderedList, spacing = 2)
                EXPSharePlace.LEFT, EXPSharePlace.RIGHT -> Renderable.horizontal(orderedList, spacing = 2)
                else -> this
            }
        }
    }

    private fun PetData.buildExpShareIconRenderable(): Renderable {
        val baseItemRenderable = buildBaseItemRenderable(
            rotationConfig = expShareConfig.icon.rotation,
            iconScale = expShareConfig.icon.scale.get(),
            useSkinAnimations = expShareConfig.icon.skinAnimation.get(),
        )

        val backgroundEnabled = expShareConfig.rarityBackground.enabled.get()
        val backgroundWrappedRenderable = baseItemRenderable.wrapInBackgroundColorOrSelf(
            enabled = backgroundEnabled,
            backgroundConfig = expShareConfig.rarityBackground.customization,
            rarity = rarity,
        )

        val borderEnabled = expShareConfig.rarityBackground.borderRing.enabled.get()
        val borderWrappedRenderable = backgroundWrappedRenderable.wrapInRingOrSelf(
            enabled = borderEnabled,
            ringConfig = expShareConfig.rarityBackground.borderRing.customization,
        )

        return borderWrappedRenderable
    }

    private fun Renderable.wrapInBackgroundColorOrSelf(
        enabled: Boolean,
        backgroundConfig: RarityBackgroundConfig,
        rarity: LorenzRarity,
    ): Renderable = if (!enabled) this else Renderable.circularContainer(
        this,
        backgroundConfig.getRarityBackgroundColor(rarity),
        padding = backgroundConfig.padding.get(),
    )

    private fun Renderable.wrapInRingOrSelf(
        enabled: Boolean,
        ringConfig: RingConfig,
    ): Renderable = if (!enabled) this else Renderable.circularContainer(
        this,
        ringConfig.color.get(),
        padding = ringConfig.padding.get(),
    )

    private fun Renderable.wrapInPetItemOrSelf(
        enabled: Boolean,
        petData: PetData,
        petItemConfig: PetItemConfig,
    ): Renderable = if (!enabled) this else petData.heldItemInternalName?.getItemStackOrNull()?.let {
        Renderable.doubleLayered(
            this,
            Renderable.item(it) {
                scale = petItemConfig.scale.get()
                horizontalAlign = petItemConfig.placement.get().horizontal
                verticalAlign = petItemConfig.placement.get().vertical
            },
            forceBottomRenderFirst = true,
        )
    } ?: this

    private fun PetData.buildBaseItemRenderable(
        rotationConfig: IconRotationConfig,
        iconScale: Double,
        useSkinAnimations: Boolean,
    ): Renderable = Renderable.animatedItemStack {
        frameStorage = AnimatedFrameLocalStorage(
            getAnimatedItemStackSequence(firstFrameOnly = !useSkinAnimations) ?: listOf(
                getItemStackOrNull()?.let { ItemStackAnimatedFrame(it) }
                    ?: ErrorManager.skyHanniError("Could not generate an item stack for pet!")
            ),
        )
        rotationStorage = AnimatedRotationPropertyStorage(
            rotationDefinition = AnimatedRotationDefinition(
                Direction.Axis.X to AxisRotationDefinition(
                    staticRotation = rotationConfig.staticRotation.xRotation.get(),
                    rotationSpeed = rotationConfig.spinRotation.speedX.get(),
                ),
                Direction.Axis.Y to AxisRotationDefinition(
                    rotationSpeed = rotationConfig.spinRotation.speedY.get(),
                    staticRotation = rotationConfig.staticRotation.yRotation.get(),
                ),
                Direction.Axis.Z to AxisRotationDefinition(
                    rotationSpeed = rotationConfig.spinRotation.speedZ.get(),
                    staticRotation = rotationConfig.staticRotation.zRotation.get(),
                ),
            ),
        ) { currentRotation }
        scale = iconScale
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
        verticalAlign = RenderUtils.VerticalAlignment.CENTER
    }

    private fun PetData.buildTextRenderableOrNull(): Renderable? {
        val enabledTexts = config.text.enabledTexts.get().takeIfNotEmpty() ?: return null
        val lines = enabledTexts.mapNotNull {
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
            StringRenderable(
                "$labelFormat$textElementFormat",
                horizontalAlign = config.text.horizontalAlign.get()
            )
        }.takeIfNotEmpty() ?: return null
        return Renderable.vertical(
            lines,
            horizontalAlign = config.text.horizontalAlign.get(),
            verticalAlign = config.text.verticalAlign.get(),
        )
    }

    private fun Double.formatExpByConfigOption() = when (config.text.xpFormat.get()) {
        NFE.DEFAULT, NFE.UNFORMATTED -> toLong().addSeparators()
        NFE.FORMATTED -> toLong().shortFormat()
        else -> ""
    }

    private fun formatExpPairByConfigOption(
        firstExp: Double,
        secondExp: Double,
    ): String = when (config.text.xpFormat.get()) {
        NFE.DEFAULT -> "§b${firstExp.toLong().addSeparators()}§9/§b${secondExp.toLong().shortFormat()}"
        NFE.FORMATTED -> "§b${firstExp.toLong().shortFormat()}§9/§b${secondExp.toLong().shortFormat()}"
        NFE.UNFORMATTED -> "§b${firstExp.toLong().addSeparators()}§9/§b${secondExp.toLong().addSeparators()}"
        else -> ""
    }

    private fun PetData.buildRenderable(): Renderable? {
        lastPetHash = this.hashCode().takeIf { it != lastPetHash } ?: return petOverlay
        val currentPetUuid = CurrentPetApi.currentPet?.uuid

        val itemRenderable = buildMainIconRenderableOrNull()
            ?.let { if (currentPetUuid != null) it.wrapInExpShareIconsOrSelf(currentPetUuid) else it }
        val textRenderable = buildTextRenderableOrNull()

        return if (itemRenderable != null && textRenderable != null) {
            // Technically nullable in the JVM
            val textLocation: TLO = config.text.textLocation.get()
            val orderedList = when (textLocation) {
                TLO.TOP, TLO.LEFT -> listOf(textRenderable, itemRenderable)
                TLO.BOTTOM, TLO.RIGHT -> listOf(itemRenderable, textRenderable)
            }
            when (textLocation) {
                TLO.TOP, TLO.BOTTOM -> Renderable.vertical(orderedList, spacing = 2)
                TLO.LEFT, TLO.RIGHT -> Renderable.horizontal(orderedList, spacing = 2)
            }
        } else listOf(textRenderable, itemRenderable).firstOrNull { it != null }
    }

    @HandleEvent
    fun onConfigLoad() = ConditionalUtils.onAnyToggled(config) {
        lastPetHash = 0
    }

    @HandleEvent
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.HOTBAR) return
        if (!config.enabled.get()) return
        if (PetDisplayConfigGuiManager.isOpen()) {
            val pet = CurrentPetApi.currentPet ?: previewPet
            val renderable = pet.buildRenderable() ?: return
            config.position.renderRenderable(renderable, posLabel = "Current Pet")
            lastPetHash = 0
            return
        }
        @Suppress("InSkyBlockEarlyReturn")
        if (!SkyBlockUtils.inSkyBlock || RiftApi.inRift()) return
        val currentPet = CurrentPetApi.currentPet ?: return run { lastPetHash = 0 }
        petOverlay = currentPet.buildRenderable()?.also {
            config.position.renderRenderable(it, posLabel = "Current Pet")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
        event.move(130, "misc.pets.display", "misc.pets.display.enabled")
        event.move(130, "misc.pets.displayPos", "misc.pets.display.position")
    }
}
