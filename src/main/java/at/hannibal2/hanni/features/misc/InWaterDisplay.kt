package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat

@HanniModule
object InWaterDisplay {

    private val config get() = HanniMod.feature.misc.stranded

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val text = "§7In Water: " + if (MinecraftCompat.localPlayer.isInWater) "§aTrue" else "§cFalse"
        config.inWaterPosition.renderStrings(listOf(text), posLabel = "In Water Display")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.inWaterDisplay
}
