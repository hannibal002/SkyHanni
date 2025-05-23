package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.pets.PetDisplayConfig
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
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

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display
    private var rotationContext: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var lastPetHash: Int = 0
    private var petOverlay: Renderable? = null

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        val enabledVisuals = config.visual.enabledVisuals.get()
        if (VElement.ITEM_STACK !in enabledVisuals) return null
        val itemStack = getItemStackOrNull() ?: return null
        val spinDirection = config.visual.spinDirection.get()

        val baseItemRenderable = when {
            spinDirection != SDirection.NONE -> {
                val multiplier = if (spinDirection == SDirection.CLOCKWISE) -1 else 1
                val degreesPerSecond = (360 / config.visual.spinFrequency.get()) * multiplier
                AnimatedItemStackRenderable(
                    itemStack,
                    rotation = ItemStackRotationDefinition(
                        axis = EnumFacing.Axis.Y,
                        rotationSpeed = degreesPerSecond,
                    ),
                    initialRotation = rotationContext
                )
            }
            else -> ItemStackRenderable(itemStack)
        }

        val augmentedRenderable = when {
            VElement.RARITY_BACKGROUND !in enabledVisuals -> baseItemRenderable
            else -> Renderable.CircularRenderable(
                baseItemRenderable,
                rarity.color.toColor(),
                20,
                border = Renderable.CircularRenderable(
                    content = null,
                    Color.GRAY,
                    26,
                    border = when {
                        VElement.XP_RING !in enabledVisuals -> null
                        else -> Renderable.CircularRenderable(
                            content = null,
                            backgroundColor = Color.cyan,
                            radius = 29,
                            filledPercentage = levelProgressionPercentage
                        )
                    }
                )
            )
        }

        val petItemRenderable = when {
            VElement.PET_HELD_ITEM !in enabledVisuals -> augmentedRenderable
            else -> {
                // Todo
                augmentedRenderable
            }
        }

        return petItemRenderable
    }

    private fun PetData.buildTextRenderableOrNull(): Renderable? = VerticalContainerRenderable(
        buildList {
            val enabledTexts = config.text.enabledTexts.get().takeIfNotEmpty() ?: return null
            enabledTexts.mapNotNull {
                when(it) {
                    TElement.PET_LEVEL, TElement.SKIN_SYMBOL -> return@mapNotNull null
                    TElement.PET_NAME -> {
                        getUserFriendlyName(
                            includeLevel = TElement.PET_LEVEL in enabledTexts,
                            includeSkinTag = TElement.SKIN_SYMBOL in enabledTexts,
                        )
                    }
                    TElement.HELD_ITEM -> {
                        val item = heldItemInternalName ?: return@mapNotNull null
                        "§eHeld item: ${item.repoItemName}"
                    }
                    TElement.NEXT_LEVEL_PROGRESS, TElement.OVERFLOW_XP, TElement.TOTAL_XP -> {
                        // Todo
                        ""
                    }
                }
            }.map {
                RenderableString(
                    it,
                    horizontalAlign = config.text.horizontalAlign
                )
            }.forEach { add(it) }
        },
        horizontalAlign = config.text.horizontalAlign,
        verticalAlign = config.text.verticalAlign,
    )

    private fun PetData.buildRenderable(): Renderable? {
        val itemRenderable = buildItemRenderableOrNull()
        val textRenderable = buildTextRenderableOrNull()

        return if (itemRenderable != null && textRenderable != null) {
            val textLocation = config.text.textLocation.get()
            val orderedList = when (textLocation) {
                TLO.TOP, TLO.LEFT -> listOf(textRenderable, itemRenderable)
                TLO.BOTTOM, TLO.RIGHT -> listOf(itemRenderable, textRenderable)
            }
            when (textLocation) {
                TLO.TOP, TLO.BOTTOM -> VerticalContainerRenderable(orderedList)
                TLO.LEFT, TLO.RIGHT -> HorizontalContainerRenderable(orderedList)
            }
        } else listOf(textRenderable, itemRenderable).firstOrNull { it != null }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (RiftApi.inRift() || !config.enabled) return

        lastPetHash = (CurrentPetApi.currentPet?.hashCode() ?: 0).takeIf { it != lastPetHash } ?: return
        val currentPet = CurrentPetApi.currentPet ?: run {
            petOverlay = null
            return
        }
        petOverlay = currentPet.buildRenderable()
        config.position.renderRenderable(petOverlay, posLabel = "Current Pet")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
        event.move(88, "misc.pets.display", "misc.pets.display.enabled")
        event.move(88, "misc.pets.displayPos", "misc.pets.display.pos")
        event.move(88, "misc.pets.petItemDisplay", "misc.pets.display.visual.enabledVisuals") { old ->
            val newList = mutableListOf(
                VElement.ITEM_STACK,
                VElement.RARITY_BACKGROUND,
                VElement.XP_RING,
            )
            if (old.asJsonArray.toList().isNotEmpty()) {
                newList.add(VElement.PET_HELD_ITEM)
            }
            ConfigManager.gson.toJsonTree(newList)
        }
    }
}
