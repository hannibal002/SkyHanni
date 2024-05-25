package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InWaterDisplay {

    private val config get() = SkyHanniMod.feature.misc.stranded
    private var playerInWater = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        playerInWater = Minecraft.getMinecraft().thePlayer.isInWater
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val text = "In Water: " + if (playerInWater) "§aTrue" else "§cFalse"
        config.inWaterPosition.renderStrings(listOf(text), posLabel = "In Water Display")
    }

    private fun isEnabled() = config.inWaterDisplay && LorenzUtils.inSkyBlock
}
