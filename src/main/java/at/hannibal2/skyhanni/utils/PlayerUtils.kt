package at.hannibal2.skyhanni.utils import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets

import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.StringUtils.toUnDashedUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import java.util.UUID

//#if MC > 1.21
import net.minecraft.world.entity.ai.attributes.Attributes
//#endif

object PlayerUtils {

    // thirdPersonView on 1.8.9
    // 0 == normal
    // 1 == f3 behind
    // 2 == selfie
    fun isFirstPersonView(): Boolean {
        //#if MC < 1.21
        //$$ return MinecraftClient.getInstance().options.thirdPersonView == 0
        //#else
        return Minecraft.getInstance().options.cameraType.isFirstPerson
        //#endif
    }

    fun isThirdPersonView(): Boolean {
        //#if MC < 1.21
        //$$ return MinecraftClient.getInstance().options.thirdPersonView == 1
        //#else
        val perspective = Minecraft.getInstance().options.cameraType
        // for some reason they make you check the other 2 bools instead of giving you a third one
        return !perspective.isMirrored && !perspective.isFirstPerson
        //#endif
    }

    fun isReversedView(): Boolean {
        //#if MC < 1.21
        //$$ return MinecraftClient.getInstance().options.thirdPersonView == 2
        //#else
        return Minecraft.getInstance().options.cameraType.isMirrored
        //#endif
    }

    fun getWalkSpeed(): Float {
        //#if MC < 1.21
        //$$ val speed = MinecraftCompat.localPlayer.capabilities.walkSpeed.toDouble()
        //#else
        val speed = MinecraftCompat.localPlayer.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)
        //#endif

        // Round to avoid floating point inaccuracies (in-game precision is at most 2 decimals anyway)
        return (speed * 1000).roundTo(2).toFloat()
    }

    fun getUuid() = getRawUuid().toUnDashedUUID()

    fun getRawUuid(): UUID = MinecraftCompat.localPlayer.uuid

    fun getName(): String = MinecraftCompat.localPlayer.name.formattedTextCompatLessResets()

    fun inAir(): Boolean = !MinecraftCompat.localPlayer.onGround()

    fun isSneaking(): Boolean = MinecraftCompat.localPlayer.isShiftKeyDown
}
