package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LorenzUtils.round
import net.minecraft.entity.Entity
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.BlockPos
import net.minecraft.util.Rotations
import net.minecraft.util.Vec3
import kotlin.math.*

data class LorenzVec(
    val x: Double,
    val y: Double,
    val z: Double,
) {

    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())

    constructor(x: Float, y: Float, z: Float) : this(x.toDouble(), y.toDouble(), z.toDouble())

    fun toBlocPos(): BlockPos = BlockPos(x, y, z)

    fun toVec3(): Vec3 = Vec3(x, y, z)

    fun distanceIgnoreY(other: LorenzVec): Double = distanceIgnoreYSq(other).pow(0.5)

    fun distance(other: LorenzVec): Double = distanceSq(other).pow(0.5)

    fun distanceSq(x: Double, y: Double, z: Double): Double = distanceSq(LorenzVec(x, y, z))

    fun distance(x: Double, y: Double, z: Double): Double = distance(LorenzVec(x, y, z))

    fun distanceSq(other: LorenzVec): Double {
        val dx = (other.x - x)
        val dy = (other.y - y)
        val dz = (other.z - z)
        return (dx * dx + dy * dy + dz * dz)
    }

    fun distanceIgnoreYSq(other: LorenzVec): Double {
        val dx = (other.x - x)
        val dz = (other.z - z)
        return (dx * dx + dz * dz)
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

    fun subtract(other: LorenzVec) = LorenzVec(x - other.x, y - other.y, z - other.z)

    fun printWithAccuracy(accuracy: Int): String {
        val x = (round(x * accuracy) / accuracy)
        val y = (round(y * accuracy) / accuracy)
        val z = (round(z * accuracy) / accuracy)
        return LorenzVec(x, y, z).toCleanString()
    }

    private fun toCleanString(): String {
        return "$x $y $z"
    }

    fun length(): Double {
        return sqrt(x * x + y * y + z * z)
    }

    fun isZero(): Boolean = x == 0.0 && y == 0.0 && z == 0.0

    fun clone(): LorenzVec = LorenzVec(x, y, z)

    fun toDoubleArray(): Array<Double> {
        return arrayOf(x, y, z)
    }

    fun equalsIgnoreY(other: LorenzVec) = x == other.x && z == other.z

    fun round(decimals: Int) = LorenzVec(x.round(decimals), y.round(decimals), z.round(decimals))

    companion object {
        fun getFromYawPitch(yaw: Double, pitch: Double): LorenzVec {
            val yaw: Double = (yaw + 90) * Math.PI / 180
            val pitch: Double = (pitch + 90) * Math.PI / 180

            val x = sin(pitch) * cos(yaw)
            val y = sin(pitch) * sin(yaw)
            val z = cos(pitch)
            return LorenzVec(x, z, y)
        }

        // only for migration purposes
        fun decodeFromString(string: String): LorenzVec {
            val (x, y, z) = string.split(":").map { it.toDouble() }
            return LorenzVec(x, y, z)
        }
    }
}

private infix fun Double.multiplyZeroSave(other: Double): Double {
    val result = this * other
    return if (result == -0.0) 0.0 else result
}

fun BlockPos.toLorenzVec(): LorenzVec = LorenzVec(x, y, z)

fun Entity.getLorenzVec(): LorenzVec = LorenzVec(posX, posY, posZ)

fun Vec3.toLorenzVec(): LorenzVec = LorenzVec(xCoord, yCoord, zCoord)

fun Rotations.toLorenzVec(): LorenzVec = LorenzVec(x, y, z)

fun S2APacketParticles.toLorenzVec() = LorenzVec(xCoordinate, yCoordinate, zCoordinate)

fun Array<Double>.toLorenzVec(): LorenzVec {
    return LorenzVec(this[0], this[1], this[2])
}