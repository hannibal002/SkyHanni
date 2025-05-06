package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
//#if MC > 1.21
//$$ import net.minecraft.entity.attribute.EntityAttributes
//#endif

object PlayerUtils {

    // thirdPersonView on 1.8.9
    // 0 == normal
    // 1 == f3 behind
    // 2 == selfie
    fun isFirstPersonView(): Boolean {
        //#if MC < 1.21
        return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0
        //#else
        //$$ return MinecraftClient.getInstance().options.perspective.isFirstPerson
        //#endif
    }

    fun isThirdPersonView(): Boolean {
        //#if MC < 1.21
        return Minecraft.getMinecraft().gameSettings.thirdPersonView == 1
        //#else
        //$$ val perspective = MinecraftClient.getInstance().options.perspective
        //$$ // for some reason they make you check the other 2 bools instead of giving you a third one
        //$$ return !perspective.isFrontView && !perspective.isFirstPerson
        //#endif
    }

    fun isReversedView(): Boolean {
        //#if MC < 1.21
        return Minecraft.getMinecraft().gameSettings.thirdPersonView == 2
        //#else
        //$$ return MinecraftClient.getInstance().options.perspective.isFrontView
        //#endif
    }

    fun getWalkSpeed(): Int {
        //#if MC < 1.21
        return (MinecraftCompat.localPlayer.capabilities.walkSpeed * 1000).toInt()
        //#else
        //$$ return MinecraftCompat.localPlayer.getAttributeValue(EntityAttributes.MOVEMENT_SPEED).toInt()
        //#endif
    }
}
