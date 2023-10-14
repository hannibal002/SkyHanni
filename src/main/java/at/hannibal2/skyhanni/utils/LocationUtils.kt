package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import kotlin.math.max
import kotlin.math.min

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

    fun Entity.disanceToIngoreY(location: LorenzVec) = getLorenzVec().distanceIgnoreY(location)

    fun playerEyeLocation(): LorenzVec {
        val player = Minecraft.getMinecraft().thePlayer
        val vec = player.getLorenzVec()
        return vec.add(0.0, 0.0 + player.getEyeHeight(), 0.0)
    }

    fun AxisAlignedBB.isVecInside(vec: LorenzVec) = isVecInside(vec.toVec3())

    fun AxisAlignedBB.isPlayerInside() = isVecInside(playerLocation())

    fun AxisAlignedBB.minBox() = LorenzVec(minX,minY,minZ)

    fun AxisAlignedBB.maxBox() = LorenzVec(maxX,maxY,maxZ)

    fun AxisAlignedBB.rayIntersects(origin: LorenzVec, direction: LorenzVec): Boolean {
        //Reference for Algorithm https://tavianator.com/2011/ray_box.html
        val rayDirectionInverse = direction.inverse()
        val t1 = (this.minBox().subtract(origin)).multiply(rayDirectionInverse)
        val t2 = (this.maxBox().subtract(origin)).multiply(rayDirectionInverse)

        val tmin = max(t1.minOfEachElement(t2).max(), Double.NEGATIVE_INFINITY)
        val tmax = min(t1.maxOfEachElement(t2).min(), Double.POSITIVE_INFINITY)
        return tmax >= tmin && tmax >= 0.0
    }
}