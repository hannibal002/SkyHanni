package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LocationUtils.calculateEdges
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.VectorUtils.plus
import at.hannibal2.skyhanni.utils.VectorUtils.times
import com.google.gson.annotations.Expose
import net.minecraft.core.BlockPos
import net.minecraft.core.Rotations
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin

// TODO refactor instead of suppressing TooManyFunctions
// TODO remove unused suppress once LorenzVec is gone
@Suppress("TooManyFunctions", "unused")
object VectorUtils {
    private val edgeCache = mutableMapOf<Vec3, Set<Pair<Vec3, Vec3>>>()

    val Vec3.edges: Set<Pair<Vec3, Vec3>>
        get() = edgeCache.getOrPut(this) {
            boundingToOffset(1.0, 1.0, 1.0)
                .inflate(0.0001, 0.0001, 0.0001)
                .calculateEdges()
        }

    fun Vec3.toBlockPos(): BlockPos =
        BlockPos(floor(x).toInt(), floor(y).toInt(), floor(z).toInt())

    fun Vec3.distanceIgnoreY(other: Vec3): Double = distanceSqIgnoreY(other).pow(0.5)

    // distance -> distanceTo

    // distanceSq -> distanceToSqr

    fun Vec3.distanceChebyshevIgnoreY(other: Vec3) = max(abs(x - other.x), abs(z - other.z))

    fun Vec3.distanceSqIgnoreY(other: Vec3): Double {
        val dx = other.x - x
        val dz = other.z - z
        return (dx * dx + dz * dz)
    }

    fun Vec3.distanceSqOnlyY(other: Vec3): Double {
        val dy = other.y - y
        return (dy * dy)
    }

    operator fun Vec3.plus(other: Vec3): Vec3 = add(other)

    operator fun Vec3.minus(other: Vec3): Vec3 = subtract(other)

    operator fun Vec3.times(other: Vec3): Vec3 = multiply(other)
    operator fun Vec3.times(other: Double): Vec3 = multiply(other, other, other)

    operator fun Vec3.div(other: Vec3) = Vec3(x / other.x, y / other.y, z / other.z)
    operator fun Vec3.div(other: Double) = Vec3(x / other, y / other, z / other)

    operator fun Vec3.component1() = x
    operator fun Vec3.component2() = y
    operator fun Vec3.component3() = y

    fun Vec3.angleAsCos(other: Vec3) = normalize().dot(other.normalize())

    fun Vec3.angleInRad(other: Vec3) = acos(angleAsCos(other))

    fun Vec3.angleInDeg(other: Vec3) = Math.toDegrees(angleInRad(other))

    fun Vec3.scaledTo(other: Vec3) = this.normalize() * other.length()

    fun Vec3.inverse() = Vec3(1.0 / x, 1.0 / y, 1.0 / z)

    fun Vec3.min(): Double = minOf(x, y, z)
    fun Vec3.max(): Double = maxOf(x, y, z)

    fun Vec3.add(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3 = add(x, y, z)

    fun Vec3.subtract(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vec3 = subtract(x, y, z)

    fun Vec3.minOfEachElement(other: Vec3) =
        Vec3(min(x, other.x), min(y, other.y), min(z, other.z))

    fun Vec3.maxOfEachElement(other: Vec3) =
        Vec3(max(x, other.x), max(y, other.y), max(z, other.z))

    fun Vec3.printWithAccuracy(accuracy: Int, splitChar: String = " "): String {
        return if (accuracy == 0) {
            val x = round(x).toInt()
            val y = round(y).toInt()
            val z = round(z).toInt()
            "$x$splitChar$y$splitChar$z"
        } else {
            val x = (round(x * accuracy) / accuracy)
            val y = (round(y * accuracy) / accuracy)
            val z = (round(z * accuracy) / accuracy)
            "$x$splitChar$y$splitChar$z"
        }
    }

    fun Vec3.toCleanString(separator: String = ", "): String =
        doubleArrayOf(x, y, z).joinToString(separator)

    fun Vec3.asStoredString(): String = "$x:$y:$z"

    fun Vec3.isNormalized(tolerance: Double = 0.01) = (lengthSqr() - 1.0).absoluteValue < tolerance

    fun Vec3.isZero(): Boolean = this == Vec3.ZERO

    fun Vec3.with(x: Double? = null, y: Double? = null, z: Double? = null): Vec3 {
        require(x != null || y != null || z != null) {
            "Vec3.with() must have at least one argument"
        }
        return Vec3(x ?: this.x, y ?: this.y, z ?: this.z)
    }

    fun Vec3.toDoubleArray() = doubleArrayOf(x, y, z)
    fun Vec3.toFloatArray() = floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())

    fun Vec3.equalsIgnoreY(other: Vec3) = x == other.x && z == other.z

    fun Vec3.roundTo(precision: Int) =
        Vec3(x.roundTo(precision), y.roundTo(precision), z.roundTo(precision))

    fun Vec3.roundToBlock() = Vec3(floor(x), floor(y), floor(z))

    fun Vec3.blockCenter(): Vec3 = roundToBlock().add(0.5)

    fun Vec3.slope(other: Vec3, factor: Double) = this + (other - this).scale(factor)

    // TODO better name. don't confuse with roundTo()
    fun Vec3.roundLocation() = Vec3(
        if (x < 0) x - 1 else x,
        y - 1,
        if (z < 0) z - 1 else z,
    )

    fun Vec3.ceil(): Vec3 = Vec3(ceil(x), ceil(y), ceil(z))

    fun Vec3.boundingCenter(expand: Double): AABB =
        AABB(x - expand, y - expand, z - expand, x + expand, y + expand, z + expand)

    fun Vec3.boundingToOffset(offX: Double, offY: Double, offZ: Double) =
        AABB(x, y, z, x + offX, y + offY, z + offZ)

    fun Vec3.axisAlignedTo(other: Vec3) = AABB(x, y, z, other.x, other.y, other.z)

    fun Vec3.up(offset: Double = 1.0): Vec3 = add(0.0, y + offset, 0.0)

    fun Vec3.down(offset: Double = 1.0): Vec3 = add(0.0, y - offset, 0.0)

    fun Vec3.interpolate(other: Vec3, factor: Double): Vec3 {
        require(factor in 0.0..1.0) { "Percentage must be between 0 and 1: $factor" }

        val x = (1 - factor) * x + factor * other.x
        val y = (1 - factor) * y + factor * other.y
        val z = (1 - factor) * z + factor * other.z

        return Vec3(x, y, z)
    }

    fun Vec3.rotateXY(theta: Double) =
        Vec3(x * cos(theta) - y * sin(theta), x * sin(theta) + y * cos(theta), z)

    fun Vec3.rotateXZ(theta: Double) =
        Vec3(x * cos(theta) + z * sin(theta), y, -x * sin(theta) + z * cos(theta))

    fun Vec3.rotateYZ(theta: Double) =
        Vec3(x, y * cos(theta) - z * sin(theta), y * sin(theta) + z * cos(theta))

    fun Vec3.nearestPointOnLine(startPos: Vec3, endPos: Vec3): Vec3 {
        var d = endPos - startPos
        val w = this - startPos

        val dp = d.lengthSqr()
        var dt = 0.0

        if (dp != dt) dt = (w.dot(d) / dp).coerceIn(0.0, 1.0)

        d *= dt
        d += startPos
        return d
    }

    fun Vec3.distanceToLine(startPos: Vec3, endPos: Vec3): Double =
        (nearestPointOnLine(startPos, endPos) - this).lengthSqr()

    fun Vec3.middle(other: Vec3): Vec3 = this + ((other - this) / 2.0)

    // format we use to send to all/party chat
    fun Vec3.toChatFormat(): String = "x: ${x.toInt()}, y: ${y.toInt()}, z: ${z.toInt()}"

    // format we show in local chat or for local commands
    fun Vec3.toLocalFormat(): String = "${x.toInt()} ${y.toInt()} ${z.toInt()}"

    val directions = setOf(
        Vec3(1.0, 0.0, 0.0),
        Vec3(-1.0, 0.0, 0.0),
        Vec3(0.0, 1.0, 0.0),
        Vec3(0.0, -1.0, 0.0),
        Vec3(0.0, 0.0, 1.0),
        Vec3(0.0, 0.0, -1.0),
    )

    fun getFromYawPitch(yawDegrees: Double, pitchDegrees: Double): Vec3 {
        val yawRad: Double = (yawDegrees + 90) * Math.PI / 180
        val pitchRad: Double = (pitchDegrees + 90) * Math.PI / 180

        val x = sin(pitchRad) * cos(yawRad)
        val y = sin(pitchRad) * sin(yawRad)
        val z = cos(pitchRad)
        return Vec3(x, z, y)
    }

    // Format: "x:y:z"
    fun fromStoredString(string: String): Vec3 =
        string.split(":").map(String::toDouble).toVec3()

    fun readListFromClipboard(): List<Vec3> =
        OSUtils.readFromClipboard()?.split("\n")?.map { line ->
            fromStoredString(line.replace("\"", "").replace(",", ""))
        } ?: emptyList()

    fun List<Vec3>.copyLocations() {
        OSUtils.copyToClipboard(joinToString(",\n") { it.asStoredString() })
    }

    fun List<Double>.toVec3(): Vec3 {
        require(size == 3) { "Can not transform a list of size $size (!= 3) to Vec3" }
        return Vec3(this[0], this[1], this[2])
    }

    val expandVector = Vec3(0.002, 0.002, 0.002)

    fun BlockPos.toVec3(): Vec3 = Vec3.atLowerCornerOf(this)

    val Entity.serverPosition: Vec3
        get() = Vec3(positionCodec.base.x, positionCodec.base.y, positionCodec.base.z)

    fun Entity.getPositionLog() = PositionLog(
        tick = tickCount,
        position = position(),
        prev = oldPosition(),
        server = serverPosition,
        motion = deltaMovement,
        yaw = yRot,
        pitch = xRot,
    )

    data class PositionLog(
        @Expose val tick: Int,
        @Expose val position: Vec3,
        @Expose val prev: Vec3,
        @Expose val server: Vec3,
        @Expose val motion: Vec3,
        @Expose val yaw: Float,
        @Expose val pitch: Float,
    )

    fun Rotations.toVec3(): Vec3 = Vec3(x().toDouble(), y().toDouble(), z().toDouble())

    fun ClientboundLevelParticlesPacket.toVec3() = Vec3(x, y, z)

    fun AABB.inflate(vec: Vec3): AABB = inflate(vec.x, vec.y, vec.z)
}
