package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenYawAndPitch {
    private val config get() = SkyHanniMod.feature.garden.yawPitchDisplay
    private var lastChange = 0L
    private var lastYaw = 0f
    private var lastPitch = 0f

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (!GardenAPI.inGarden() && !config.showEverywhere) return
        if (GardenAPI.toolInHand == null && !config.showWithoutTool) return

        val player = Minecraft.getMinecraft().thePlayer

        var yaw = player.rotationYaw % 360
        if (yaw < 0) yaw += 360
        if (yaw > 180) yaw -= 360
        val pitch = player.rotationPitch

        if (yaw != lastYaw || pitch != lastPitch) {
            lastChange = System.currentTimeMillis()
        }
        lastYaw = yaw
        lastPitch = pitch

        if (!config.showAlways && System.currentTimeMillis() > lastChange + (config.timeout * 1000)) return

        val displayList = listOf(
            "§aYaw: §f${yaw.round(config.yawPrecision)}",
            "§aPitch: §f${pitch.round(config.pitchPrecision)}",
        )
        if (GardenAPI.inGarden()) {
            config.pos.renderStrings(displayList, posLabel = "Yaw and Pitch")
        } else {
            config.posOutside.renderStrings(displayList, posLabel = "Yaw and Pitch")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastChange = System.currentTimeMillis()
    }
}