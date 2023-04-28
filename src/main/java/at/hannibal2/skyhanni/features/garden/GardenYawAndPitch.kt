package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenYawAndPitch {
    private val config get() = SkyHanniMod.feature.garden
    private var yawandpitchDisplay = listOf<String>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        val ypList = mutableListOf<String>()
        val player = Minecraft.getMinecraft().thePlayer

        var pYaw = player.rotationYaw % 360
        if (pYaw < 0) pYaw += 360
        if (pYaw > 180) pYaw -= 360
        ypList.add("§aYaw: §e${pYaw.toDouble().round(2)}")

        val pPitch = player.rotationPitch
        ypList.add("§aPitch: §e${pPitch.toDouble().round(2)}")

        yawandpitchDisplay = ypList

        config.YawAndPitchDisplayPos.renderStrings(yawandpitchDisplay, posLabel = "Yaw and Pitch")
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.showYawAndPitch
}
