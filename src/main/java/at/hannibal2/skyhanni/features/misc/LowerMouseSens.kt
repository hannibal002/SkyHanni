package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
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

    private fun toggleSens() {
        val gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        isToggled = !isToggled
        if (isToggled) {
            SkyHanniMod.feature.storage.savedMouseloweredSensitivity = gameSettings.mouseSensitivity
            val newSens = ((SkyHanniMod.feature.storage.savedMouseloweredSensitivity+(1F / 3F))/config.divisorSens)-(1F / 3F)
            gameSettings.mouseSensitivity = newSens
//            LorenzUtils.chat("§bMouse Sensitivity has been lowered from ${SkyHanniMod.feature.storage.savedMouseloweredSensitivity} to $newSens.")
        } else {
            gameSettings.mouseSensitivity = SkyHanniMod.feature.storage.savedMouseloweredSensitivity
//            LorenzUtils.chat("§bMouse Sensitivity has been restored.")
        }
    }
}
