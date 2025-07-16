package de.hype.bingonet.shared.objects

import kotlin.math.PI
import kotlin.math.sign

class Vector3d(val x: Double, val y: Double, val z: Double) : Comparable<Vector3d> {

    constructor(pos: Position) : this(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

    fun asLong(): Long {
        var l = 0L
        l = l or ((x.toLong() and BITS_X) shl BIT_SHIFT_X)
        l = l or ((y.toLong() and BITS_Y) shl 0)
        l = l or ((z.toLong() and BITS_Z) shl BIT_SHIFT_Z)
        return l
    }

    fun subtractReverse(vec: Vector3d): Vector3d {
        return Vector3d(vec.x - this.x, vec.x - this.y, vec.z - this.z)
    }

    fun normalize(): Vector3d {
        val base = this.x * this.x + this.y * this.y + this.z * this.z
        return if (base < 1.0E-4) Vector3d(0.0, 0.0, 0.0)
        else Vector3d(this.x / base, this.y / base, this.z / base)
    }

    fun crossProduct(vec: Vector3d): Vector3d {
        return Vector3d(
            this.y * vec.z - this.z * vec.y,
            this.z * vec.x - this.x * vec.z,
            this.x * vec.y - this.y * vec.x
        )
    }

    fun subtract(vec: Vector3d): Vector3d = subtract(vec.x, vec.y, vec.z)

    fun subtract(x: Double, y: Double, z: Double): Vector3d = addVector(-x, -y, -z)

    fun add(vec: Vector3d): Vector3d = addVector(vec.x, vec.y, vec.z)

    /** Adds the specified x, y, z vector components to this vector and returns the resulting vector. Does not change this vector. */
    fun addVector(x: Double, y: Double, z: Double): Vector3d =
        Vector3d(this.x + x, this.y + y, this.z + z)

    fun distanceTo(vec: Vector3d): Double {
        val d0 = vec.x - this.x
        val d1 = vec.y - this.y
        val d2 = vec.z - this.z
        return sqrt(d0 * d0 + d1 * d1 + d2 * d2)
    }

    fun getIntermediateWithXValue(vec: Vector3d, x: Double): Vector3d? {
        val d0 = vec.x - this.x
        val d1 = vec.y - this.y
        val d2 = vec.z - this.z
        if (d0 * d0 < 1.0000000116860974E-7) return null
        val d3 = (x - this.x) / d0
        return if (d3 in 0.0..1.0) Vector3d(
            this.x + d0 * d3,
            this.y + d1 * d3,
            this.z + d2 * d3
        )
        else null
    }

    /** Returns a new vector with y value equal to the second parameter, along the line between this vector and the passed in vector, or null if not possible. */
    fun getIntermediateWithYValue(vec: Vector3d, y: Double): Vector3d? {
        val d0 = vec.x - this.x
        val d1 = vec.y - this.y
        val d2 = vec.z - this.z
        if (d1 * d1 < 1.0000000116860974E-7) return null
        val d3 = (y - this.y) / d1
        return if (d3 in 0.0..1.0) Vector3d(
            this.x + d0 * d3,
            this.y + d1 * d3,
            this.z + d2 * d3
        )
        else null
    }

    /** Returns a new vector with z value equal to the second parameter, along the line between this vector and the passed in vector, or null if not possible. */
    fun getIntermediateWithZValue(vec: Vector3d, z: Double): Vector3d? {
        val d0 = vec.x - this.x
        val d1 = vec.y - this.y
        val d2 = vec.z - this.z
        if (d2 * d2 < 1.0000000116860974E-7) return null
        val d3 = (z - this.z) / d2
        return if (d3 in 0.0..1.0) Vector3d(
            this.x + d0 * d3,
            this.y + d1 * d3,
            this.z + d2 * d3
        )
        else null
    }

    fun rotatePitch(pitch: Float): Vector3d {
        val f = cos(pitch)
        val f1 = sin(pitch)
        val d0 = this.x
        val d1 = this.y * f.toDouble() + this.z * f1.toDouble()
        val d2 = this.z * f.toDouble() - this.y * f1.toDouble()
        return Vector3d(d0, d1, d2)
    }

    fun rotateYaw(yaw: Float): Vector3d {
        val f = cos(yaw)
        val f1 = sin(yaw)
        val d0 = this.x * f.toDouble() + this.z * f1.toDouble()
        val d1 = this.y
        val d2 = this.z * f.toDouble() - this.x * f1.toDouble()
        return Vector3d(d0, d1, d2)
    }

    override fun toString(): String = "$x $y $z"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector3d) return false
        return x == other.x && y == other.y && z == other.z
    }

    override fun hashCode(): Int {
        var bits = 1L
        bits = 31L * bits + doubleToLongBits(x)
        bits = 31L * bits + doubleToLongBits(y)
        bits = 31L * bits + doubleToLongBits(z)
        return (bits xor (bits ushr 32)).toInt()
    }

    override fun compareTo(other: Vector3d): Int {
        return if (this.y == other.y)
            if (this.z == other.z)
                (this.x - other.x).toInt()
            else (this.z - other.z).toInt()
        else (this.y - other.y).toInt()
    }

    fun signumEquals(other: Vector3d): Boolean {
        return sign(x) == sign(other.x) &&
                sign(y) == sign(other.y) &&
                sign(z) == sign(other.z)
    }

    fun sqrt(value: Double): Double = sqrt(value)

    companion object {
        val NULL_VECTOR = Vector3d(0.0, 0.0, 0.0)
        private val MULTIPLY_DE_BRUIJN_BIT_POSITION = intArrayOf(
            0, 1, 28, 2, 29, 14, 24, 3,
            30, 22, 20, 15, 25, 17, 4, 8,
            31, 27, 13, 23, 21, 19, 16, 7,
            26, 12, 18, 6, 11, 5, 10, 9
        )
        private val SIZE_BITS_X = 1 + floorLog2(smallestEncompassingPowerOfTwo(30000000))
        private val SIZE_BITS_Z = SIZE_BITS_X
        val SIZE_BITS_Y = 64 - SIZE_BITS_X - SIZE_BITS_Z
        private val BITS_Y = (1L shl SIZE_BITS_Y) - 1L
        private val BIT_SHIFT_Z = SIZE_BITS_Y
        private val BIT_SHIFT_X = SIZE_BITS_Y + SIZE_BITS_Z
        private val BITS_Z = (1L shl SIZE_BITS_Z) - 1L
        private val BITS_X = (1L shl SIZE_BITS_X) - 1L
        private val SIN_TABLE = FloatArray(65536)

        init {
            for (i in 0 until 65536) {
                SIN_TABLE[i] = sin(i * PI.toFloat() * 2.0f / 65536.0f)
            }
        }

        private fun smallestEncompassingPowerOfTwo(value: Int): Int {
            var i = value - 1
            i = i or (i shr 1)
            i = i or (i shr 2)
            i = i or (i shr 4)
            i = i or (i shr 8)
            i = i or (i shr 16)
            return i + 1
        }

        fun floorLog2(value: Int): Int = ceilLog2(value) - if (isPowerOfTwo(value)) 0 else 1

        fun isPowerOfTwo(value: Int): Boolean = value != 0 && (value and (value - 1)) == 0

        fun ceilLog2(value: Int): Int {
            val v = if (isPowerOfTwo(value)) value else smallestEncompassingPowerOfTwo(value)
            return MULTIPLY_DE_BRUIJN_BIT_POSITION[((v.toLong() * 125613361L) shr 27).toInt() and 31]
        }

        private fun doubleToLongBits(d: Double): Long {
            return if (d == 0.0) 0L else java.lang.Double.doubleToLongBits(d)
        }

        fun sin(f: Float): Float = SIN_TABLE[(f * 10430.378f).toInt() and 0xFFFF]

        fun cos(value: Float): Float = SIN_TABLE[((value * 10430.378f + 16384.0f).toInt()) and 0xFFFF]
    }
}
