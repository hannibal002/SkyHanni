package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.StringUtils.toUnDashedUUID
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Avatar
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.Attributes
import java.util.UUID

object PlayerUtils {

    val STANDING_EYE_HEIGHT = Avatar.POSES.getValue(Pose.STANDING).eyeHeight
    val SNEAKING_EYE_HEIGHT = Avatar.POSES.getValue(Pose.CROUCHING).eyeHeight

    // thirdPersonView on 1.8.9
    // 0 == normal
    // 1 == f3 behind
    // 2 == selfie
    fun isFirstPersonView(): Boolean {
        return Minecraft.getInstance().options.cameraType.isFirstPerson
    }

    fun isThirdPersonView(): Boolean {
        val perspective = Minecraft.getInstance().options.cameraType
        // for some reason they make you check the other 2 booleans instead of giving you a third one
        return !perspective.isMirrored && !perspective.isFirstPerson
    }

    fun isReversedView(): Boolean {
        return Minecraft.getInstance().options.cameraType.isMirrored
    }

    fun getWalkSpeed(): Float {
        val speed = MinecraftCompat.localPlayer.getAttributeBaseValue(Attributes.MOVEMENT_SPEED)

        // Round to avoid floating point inaccuracies (in-game precision is at most 2 decimals anyway)
        return (speed * 1000).roundTo(2).toFloat()
    }

    fun getUuid() = getRawUuid().toUnDashedUUID()

    fun getRawUuid(): UUID = MinecraftCompat.localPlayer.uuid

    fun getName(): String = MinecraftCompat.localPlayer.plainTextName

    fun onGround(): Boolean = MinecraftCompat.localPlayer.onGround()
    fun inAir(): Boolean = !onGround()

    fun blockPosition() = MinecraftCompat.localPlayer.blockPosition().toLorenzVec()

    fun getLocation() = MinecraftCompat.localPlayer.getLorenzVec()

    fun isSneaking(): Boolean = MinecraftCompat.localPlayer.isShiftKeyDown
}
