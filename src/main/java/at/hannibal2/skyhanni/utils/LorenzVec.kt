package at.hannibal2.skyhanni.utils

import net.minecraft.entity.Entity
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.pow

data class LorenzVec(
    val x: Double,
    val y: Double,
    val z: Double
) {
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun toBlocPos(): BlockPos = BlockPos(x, y, z)

    fun toVec3(): Vec3 = Vec3(x, y, z)

    fun distance(other: LorenzVec): Double = distanceSq(other).pow(0.5)

    fun distanceSq(x: Double, y: Double, z: Double): Double = distanceSq(LorenzVec(x, y, z))

    fun distance(x: Double, y: Double, z: Double): Double = distance(LorenzVec(x, y, z))

    fun distanceSq(other: LorenzVec): Double {
        val dx = (other.x - x)
        val dy = (other.y - y)
        val dz = (other.z - z)
        return (dx * dx + dy * dy + dz * dz)
    }

    fun add(x: Double, y: Double, z: Double): LorenzVec = LorenzVec(this.x + x, this.y + y, this.z + z)

    fun add(x: Int, y: Int, z: Int): LorenzVec = LorenzVec(this.x + x, this.y + y, this.z + z)

    override fun toString(): String {
        return "LorenzVec{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}'
    }

    fun multiply(d: Double): LorenzVec = LorenzVec(x multiplyZeroSave d, y multiplyZeroSave d, z multiplyZeroSave d)

    fun multiply(d: Int): LorenzVec =
        LorenzVec(x multiplyZeroSave d.toDouble(), y multiplyZeroSave d.toDouble(), z multiplyZeroSave d.toDouble())

    fun add(other: LorenzVec) = LorenzVec(x + other.x, y + other.y, z + other.z)
}

private infix fun Double.multiplyZeroSave(other: Double): Double {
    val result = this * other
    return if (result == -0.0) 0.0 else result
}

fun BlockPos.toLorenzVec(): LorenzVec = LorenzVec(x, y, z)

fun Entity.getLorenzVec(): LorenzVec = LorenzVec(posX, posY, posZ)

fun Vec3.toLorenzVec(): LorenzVec = LorenzVec(xCoord, yCoord, zCoord)