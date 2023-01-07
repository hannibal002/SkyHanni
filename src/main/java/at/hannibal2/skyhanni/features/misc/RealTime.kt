package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat

class RealTime {

    private val format = SimpleDateFormat("HH:mm:ss")

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return

        SkyHanniMod.feature.misc.realTimePos.renderString(format.format(System.currentTimeMillis()))
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.realTime
    }
}