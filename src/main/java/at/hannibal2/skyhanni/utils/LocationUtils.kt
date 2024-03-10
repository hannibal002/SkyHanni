package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB

object LocationUtils {

    fun canSee(a: LorenzVec, b: LorenzVec) =
        Minecraft.getMinecraft().theWorld.rayTraceBlocks(a.toVec3(), b.toVec3(), false, true, false) == null

    fun playerLocation() = Minecraft.getMinecraft().thePlayer.getLorenzVec()

    fun distanceFromPreviousTick(): Double = with(Minecraft.getMinecraft().thePlayer) {
        val oldPos = LorenzVec(prevPosX, prevPosY, prevPosZ)
        val newPos = LorenzVec(posX, posY, posZ)

        (oldPos.distance(newPos) * 20).round(2)
    }

    fun LorenzVec.distanceToPlayer() = distance(playerLocation())

    fun LorenzVec.distanceToPlayerIgnoreY() = distanceIgnoreY(playerLocation())

    fun LorenzVec.distanceSqToPlayer() = distanceSq(playerLocation())

    fun LorenzVec.distanceToPlayerSqIgnoreY() = distanceSqIgnoreY(playerLocation())

    fun Entity.distanceToPlayer() = getLorenzVec().distanceToPlayer()

    fun Entity.distanceTo(location: LorenzVec) = getLorenzVec().distance(location)

    fun playerEyeLocation(): LorenzVec {
        val player = Minecraft.getMinecraft().thePlayer
        val vec = player.getLorenzVec()
        return vec.add(y = player.getEyeHeight().toDouble())
    }

    fun AxisAlignedBB.isInside(vec: LorenzVec) = isVecInside(vec.toVec3())

    fun AxisAlignedBB.isPlayerInside() = isInside(playerLocation())

    fun LorenzVec.canBeSeen(radius: Double = 150.0): Boolean {
        val a = playerEyeLocation()
        val b = this
        val noBlocks = canSee(a, b)
        val notTooFar = a.distance(b) < radius
        val inFov = true // TODO add Frustum "Frustum().isBoundingBoxInFrustum(entity.entityBoundingBox)"
        return noBlocks && notTooFar && inFov
    }
}
