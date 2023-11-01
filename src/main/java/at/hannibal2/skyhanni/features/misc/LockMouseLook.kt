package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LockMouseLook {
    private var lockedMouse = false
    private var oldSensitivity = 0F
    private val lockedPosition = -1F / 3F

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (lockedMouse) toggleLock()
        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (gameSettings.mouseSensitivity == lockedPosition) {
            gameSettings.mouseSensitivity = 0.5f
            LorenzUtils.chat("§e[SkyHanni] §bReset your mouse sensitivity to 100%.")
        }
    }

    fun toggleLock() {
        lockedMouse = !lockedMouse

        val gameSettings = Minecraft.getMinecraft().gameSettings
        if (lockedMouse) {
            oldSensitivity = gameSettings.mouseSensitivity
            gameSettings.mouseSensitivity = lockedPosition
            LorenzUtils.chat("§e[SkyHanni] §bMouse rotation is now locked. Type /shmouselock to unlock your rotation")
        } else {
            gameSettings.mouseSensitivity = oldSensitivity
            LorenzUtils.chat("§e[SkyHanni] §bMouse rotation is now unlocked.")
        }
    }
}
