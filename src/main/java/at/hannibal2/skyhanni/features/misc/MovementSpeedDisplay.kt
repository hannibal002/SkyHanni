package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MovementSpeedDisplay {

    private val config get() = SkyHanniMod.feature.misc

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.playerMovementSpeedPos.renderString("Movement Speed: ${LocationUtils.distanceFromPreviousTick()}", posLabel = "Movement Speed")
    }

    fun isEnabled() = LorenzUtils.onHypixel &&
        (LorenzUtils.inSkyBlock || OutsideSbFeature.MOVEMENT_SPEED.isSelected()) &&
        config.playerMovementSpeed
}
