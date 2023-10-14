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

    fun AxisAlignedBB.rayIntersects(origin: LorenzVec, direction: LorenzVec): Boolean {
        val boxSize = LorenzVec((this.maxX - this.minX),(this.maxY - this.minY),(this.maxZ - this.minZ))
        val offset = LorenzVec(this.maxX,this.maxY,this.maxZ).subtract(boxSize.multiply(0.5))
        val ro = origin.subtract(offset)
        val rd = direction
        //LorenzDebug.log("Boxsize: $boxSize, Origin: $ro, Direction: $rd, Offset: $offset")

        // The MIT License
        // Copyright © 2014 Inigo Quilez
        // Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
        // https://iquilezles.org/articles/intersectors
        val m = rd.inverse() // can precompute if traversing a set of aligned boxes

        val n = m.multiply(ro) // can precompute if traversing a set of aligned boxes

        val k = boxSize.multiply(m.absolut())
        val t1 = (k.add(n)).multiply(-1)
        val t2 = k.subtract(n)
        val tN = max(max(t1.x, t1.y), t1.z)
        val tF = min(min(t2.x, t2.y), t2.z)
        return tN <= tF && tF >= 0.0 // no intersection
        // End of Copyright © 2014 Inigo Quilez
    }
}