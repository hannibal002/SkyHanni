package at.hannibal2.skyhanni.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CurrentPetApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import java.awt.Color

@SkyHanniModule
object CurrentPetDisplay {

    private val config get() = SkyHanniMod.feature.misc.pets.display

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (RiftApi.inRift() || !config.enabled) return

        val currentPet = CurrentPetApi.currentPet ?: return
        val displayName = currentPet.getUserFriendlyName(includeLevel = false)
        val itemStack = currentPet.getItemStackOrNull() ?: return
        val rarityColor = currentPet.rarity?.color ?: return

        val nameRender = Renderable.string(displayName, color = rarityColor.toColor())
        val circle = Renderable.CircularRenderable(
            rarityColor.toColor(),
            25,
            itemStack = itemStack,
            border = Renderable.CircularRenderable(
                Color.LIGHT_GRAY,
                30,
                border = if (config.levelRing) Renderable.CircularRenderable(
                    backgroundColor = Color.BLUE,
                    radius = 32,
                    filledPercentage = currentPet.levelProgressionPercentage ?: 0.0
                ) else null
            )
        )

        val container = Renderable.verticalContainer(
            listOf(nameRender, circle),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        config.position.renderRenderable(container, posLabel = "Current Pet")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
        event.move(75, "misc.pets.display", "misc.pets.display.enabled")
        event.move(75, "misc.pets.displayPos", "misc.pets.display.pos")
    }
}
