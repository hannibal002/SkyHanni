package at.hannibal2.hanni.features.pets

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.pet.CurrentPetApi
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderString

@HanniModule
object CurrentPetDisplay {

    private val config get() = HanniMod.feature.misc.pets

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onRenderOverlay() {
        if (RiftApi.inRift() || !config.display) return

        val displayName = CurrentPetApi.currentPet?.getUserFriendlyName(includeLevel = false) ?: return
        config.displayPos.renderString(displayName, posLabel = "Current Pet")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.petDisplay", "misc.pets.display")
        event.move(9, "misc.petDisplayPos", "misc.pets.displayPos")
    }
}
