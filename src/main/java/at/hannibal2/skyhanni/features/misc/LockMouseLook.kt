package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object LockMouseLook {
    private var lockedMouse = false
    private var oldSensitivity = 0F;

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (lockedMouse) toggleLock()
    }

    fun toggleLock() {
        lockedMouse = !lockedMouse

        if (lockedMouse) {
            oldSensitivity = Minecraft.getMinecraft().gameSettings.mouseSensitivity;
            Minecraft.getMinecraft().gameSettings.mouseSensitivity = -1F/3F;
            LorenzUtils.chat("§b[SkyHanni] Mouse rotation is now locked. Type /shmouselock to unlock your rotation")
        } else {
            Minecraft.getMinecraft().gameSettings.mouseSensitivity = oldSensitivity
            LorenzUtils.chat("§b[SkyHanni] Mouse rotation is now unlocked.")
        }
    }
}