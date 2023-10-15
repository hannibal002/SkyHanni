package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB

object LocationUtils {

    fun canSee(a: LorenzVec, b: LorenzVec): Boolean {
        return Minecraft.getMinecraft().theWorld.rayTraceBlocks(a.toVec3(), b.toVec3(), false, true, false) == null
    }

    fun playerLocation() = Minecraft.getMinecraft().thePlayer.getLorenzVec()

    fun LorenzVec.distanceToPlayer() = distance(playerLocation())

    fun LorenzVec.distanceSqToPlayer() = distanceSq(playerLocation())

    fun LorenzVec.distanceToPlayerSqIgnoreY() = distanceSqIgnoreY(playerLocation())

    fun Entity.distanceToPlayer() = getLorenzVec().distance(playerLocation())

    fun Entity.distanceTo(location: LorenzVec) = getLorenzVec().distance(location)

    fun playerEyeLocation(): LorenzVec {
        val player = Minecraft.getMinecraft().thePlayer
        val vec = player.getLorenzVec()
        return vec.add(0.0, 0.0 + player.getEyeHeight(), 0.0)
    }

    fun AxisAlignedBB.isVecInside(vec: LorenzVec) = isVecInside(vec.toVec3())

    fun AxisAlignedBB.isPlayerInside() = isVecInside(playerLocation())
}