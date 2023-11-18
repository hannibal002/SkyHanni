package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LowerMouseSens {
    private val config get() = SkyHanniMod.feature.misc
    private var isToggled = false
    private var keyHeld = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
       if (!keyHeld) {
           if (config.mouseSensKey.isKeyHeld()) {
               keyHeld = true
               toggleSens()
           }
       } else {
           if (!config.mouseSensKey.isKeyHeld()) {
               keyHeld = false
               toggleSens()
           }
       }
    }
    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!keyHeld) return
        config.loweredMouseDisplay.renderString("§eSensitivity Lowered", posLabel = "Sensitivity Lowered")
    }

    fun toggleSens() {
        val gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        isToggled = !isToggled
        if (isToggled) {
            SkyHanniMod.feature.storage.savedMouseSensitivity = gameSettings.mouseSensitivity
            val newSens = ((SkyHanniMod.feature.storage.savedMouseSensitivity + (1/3))/config.divisorSens - (1/3))
            gameSettings.mouseSensitivity = newSens
//            LorenzUtils.chat("§e[SkyHanni] §bMouse Sensitivity has been lowered.")
        } else {
            gameSettings.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseSensitivity
//            LorenzUtils.chat("§e[SkyHanni] §bMouse Sensitivity has been restored.")
        }
    }
}
