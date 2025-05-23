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
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import java.awt.Color

typealias VElement = PetDisplayConfig.VisualPetDisplayConfig.VisualElement
typealias TElement = PetDisplayConfig.TextPetDisplayConfig.TextElement

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display

    private fun PetData.buildItemRenderableOrNull(): Renderable? {
        val enabledVisuals = config.visual.enabledVisuals.get()
        if (VElement.ITEM_STACK !in enabledVisuals) return null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (RiftApi.inRift() || !config.enabled) return

        val currentPet = CurrentPetApi.currentPet ?: return
        val displayName = currentPet.getUserFriendlyName(includeLevel = true)
        val itemStack = currentPet.getItemStackOrNull() ?: return
        val rarityColor = currentPet.rarity.color

        val nameRender = RenderableString(displayName, color = rarityColor.toColor())
        val circle = Renderable.CircularRenderable(
            rarityColor.toColor(),
            20,
            itemStack = itemStack,
            border = Renderable.CircularRenderable(
                Color.GRAY,
                26,
                border = if (config.levelRing) Renderable.CircularRenderable(
                    backgroundColor = Color.cyan,
                    radius = 29,
                    filledPercentage = currentPet.levelProgressionPercentage
                ) else null
            )
        )

        val container = VerticalContainerRenderable(
            listOf(nameRender, circle),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        config.position.renderRenderable(container, posLabel = "Current Pet")
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
