package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import at.hannibal2.skyhanni.utils.GuiRender.renderString
import at.hannibal2.skyhanni.utils.LorenzLogger
import net.minecraftforge.client.event.RenderGameOverlayEvent

class LorenzTest {

    var log = LorenzLogger("debug/packets")

    companion object {
        var enabled = false
        var text = ""

        val debugLogger = LorenzLogger("debug/test")
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.debug.enabled) return

        if (enabled) {
            SkyHanniMod.feature.debug.testPos.renderString(text)
        }
    }
}