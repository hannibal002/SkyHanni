package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LockMouseLook {
    private var lockedMouse = false
    private var oldSensitivity = 0F

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (lockedMouse) toggleLock()
    }

    fun toggleLock() {
        lockedMouse = !lockedMouse

        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (lockedMouse) {
            oldSensitivity = gameSettings.mouseSensitivity
            gameSettings.mouseSensitivity = -1F / 3F
            LorenzUtils.chat("§bMouse rotation is now locked. Type /shmouselock to unlock your rotation")
        } else {
            gameSettings.mouseSensitivity = oldSensitivity
            LorenzUtils.chat("§bMouse rotation is now unlocked.")
        }
    }
}
