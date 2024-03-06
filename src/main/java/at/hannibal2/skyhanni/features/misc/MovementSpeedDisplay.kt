package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MovementSpeedDisplay {

    private val config get() = SkyHanniMod.feature.misc

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        currentBPS = with(Minecraft.getMinecraft().thePlayer) {
            val oldPos = LorenzVec(prevPosX, prevPosY, prevPosZ)
            val newPos = LorenzVec(posX, posY, posZ)

            // Distance from previous tick, multiplied by TPS
            (oldPos.distance(newPos) * 20).round(2)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.playerMovementSpeedPos.renderString("Movement Speed: $currentBPS", posLabel = "Movement Speed")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.playerMovementSpeed

    companion object {
        var currentBPS: Double = 0.0
    }
}
