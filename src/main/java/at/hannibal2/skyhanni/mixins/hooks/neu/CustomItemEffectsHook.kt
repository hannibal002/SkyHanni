package at.hannibal2.skyhanni.mixins.hooks.neu

import at.hannibal2.skyhanni.features.misc.LockMouseLook
import net.minecraft.client.Minecraft

object CustomItemEffectsHook {

    @JvmStatic
    fun getSensMultiplier(): Float {
        return if (LockMouseLook.lockedMouse) {
            //        -1F / 3F
            0f
        } else {
            Minecraft.getMinecraft().gameSettings.mouseSensitivity
        }
    }
}
