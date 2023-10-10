package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GardenYawAndPitch {
    private val config get() = SkyHanniMod.feature.garden.yawPitchDisplay
    private var lastChange = SimpleTimeMark.farPast()
    private var lastYaw = 0f
    private var lastPitch = 0f

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
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
            lastChange = SimpleTimeMark.now()
        }
        lastYaw = yaw
        lastPitch = pitch

        if (!config.showAlways && lastChange.passedSince() > config.timeout.seconds) return

        val yawText = yaw.round(config.yawPrecision).addSeparators()
        val pitchText = pitch.round(config.pitchPrecision).addSeparators()
        val displayList = listOf(
            "§aYaw: §f$yawText",
            "§aPitch: §f$pitchText",
        )
        if (GardenAPI.inGarden()) {
            config.pos.renderStrings(displayList, posLabel = "Yaw and Pitch")
        } else {
            config.posOutside.renderStrings(displayList, posLabel = "Yaw and Pitch")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastChange = SimpleTimeMark.farPast()
    }
}