package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

@SkyHanniModule
object InWaterDisplay {

    private val config get() = SkyHanniMod.feature.misc.stranded

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val text = "§7In Water: " + if (MinecraftCompat.localPlayer.isInWater) "§aTrue" else "§cFalse"
        config.inWaterPosition.renderStrings(listOf(text), posLabel = "In Water Display")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.inWaterDisplay
}
