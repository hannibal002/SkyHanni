package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenYawAndPitch {
    private val config get() = SkyHanniMod.feature.garden
    private var lastChange = 0L
    private var lastYaw = 0f
    private var lastPitch = 0f

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.toolInHand == null) return

        val ypList = mutableListOf<String>()
        val player = Minecraft.getMinecraft().thePlayer

        var pYaw = player.rotationYaw % 360
        if (pYaw < 0) pYaw += 360
        if (pYaw > 180) pYaw -= 360
        val pPitch = player.rotationPitch

        if (pYaw != lastYaw || pPitch != lastPitch) {
            lastChange = System.currentTimeMillis()
        }
        lastYaw = pYaw
        lastPitch = pPitch

        if (System.currentTimeMillis() > lastChange + 3_000) return

        ypList.add("§aYaw: §f${pYaw.toDouble().round(4)}")

        ypList.add("§aPitch: §f${pPitch.toDouble().round(4)}")

        config.YawAndPitchDisplayPos.renderStrings(ypList, posLabel = "Yaw and Pitch")
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastChange = System.currentTimeMillis()
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.showYawAndPitch
}
