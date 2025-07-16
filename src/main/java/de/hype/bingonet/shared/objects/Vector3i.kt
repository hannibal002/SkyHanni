package de.hype.bingonet.shared.objects

import java.lang.Double.doubleToLongBits
import kotlin.math.sign

open class Vector3i : Comparable<Vector3i> {
    @JvmField
    var x: Int

    @JvmField
    var y: Int

    @JvmField
    var z: Int

    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(vec: Vector3d) {
        this.x = vec.x.toInt()
        this.y = vec.y.toInt()
        this.z = vec.z.toInt()
    }

    override fun toString(): String {
        return "$x $y $z"
    }

    fun asLong(): Long {
        var l = 0L
        l = l or ((x.toLong() and BITS_X) shl BIT_SHIFT_X)
        l = l or ((y.toLong() and BITS_Y) shl 0)
        return l or ((z.toLong() and BITS_Z) shl BIT_SHIFT_Z)
    }

    fun subtractReverse(vec: Vector3i): Vector3i {
        return Vector3i(vec.x - this.x, vec.x - this.y, vec.z - this.z)
    }

    fun normalize(): Vector3i {
        val base = this.x * this.x + this.y * this.y + this.z * this.z
        return if (base < 1.0E-4) Vector3i(0, 0, 0) else Vector3i(this.x / base, this.y / base, this.z / base)
    }

    fun crossProduct(vec: Vector3i): Vector3i {
        return Vector3i(
            this.y * vec.z - this.z * vec.y,
            this.z * vec.x - this.x * vec.z,
            this.x * vec.y - this.y * vec.x
        )
    }

    fun subtract(vec: Vector3i): Vector3i {
        return this.subtract(vec.x, vec.y, vec.z)
    }

    fun subtract(x: Int, y: Int, z: Int): Vector3i {
        return this.addVector(-x, -y, -z)
    }

    fun add(vec: Vector3i): Vector3i {
        return this.addVector(vec.x, vec.y, vec.z)
    }

    /**
     * Adds the specified x,y,z vector components to this vector and returns the resulting vector. Does not change this vector.
     */
    fun addVector(x: Int, y: Int, z: Int): Vector3i {
        return Vector3i(this.x + x, this.y + y, this.z + z)
    }

    fun distanceTo(vec: Vector3i): Double {
        val d0 = (vec.x - this.x).toDouble()
        val d1 = (vec.y - this.y).toDouble()
        val d2 = (vec.z - this.z).toDouble()
        return sqrt(d0 * d0 + d1 * d1 + d2 * d2)
    }

    fun rotatePitch(pitch: Float): Vector3i {
        val f: Float = cos(pitch)
        val f1: Float = sin(pitch)
        val d1 = this.y * f.toInt() + this.z * f1.toInt()
        val d2 = this.z * f.toInt() - this.y * f1.toInt()
        return Vector3i(this.x, d1, d2)
    }

    fun rotateYaw(yaw: Float): Vector3i {
        val f: Float = cos(yaw)
        val f1: Float = sin(yaw)
        val d0 = this.x * f.toInt() + this.z * f1.toInt()
        val d2 = this.z * f.toInt() - this.x * f1.toInt()
        return Vector3i(d0, this.y, d2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other !is Vector3i) {
            return false
        } else {
            val Vectorc = other
            return this.x == Vectorc.x && this.y == Vectorc.y && this.z == Vectorc.z
        }
    }

    override fun hashCode(): Int {
        var bits = 1L
        bits = 31L * bits + doubleToLongBits(x.toDouble())
        bits = 31L * bits + doubleToLongBits(y.toDouble())
        bits = 31L * bits + doubleToLongBits(z.toDouble())
        return (bits xor (bits shr 32)).toInt()
    }

    override fun compareTo(other: Vector3i): Int {
        return if (this.y == other.y)
            (if (this.z == other.z)
                (this.x - other.x)
            else
                (this.z - other.z))
        else
            (this.y - other.y)
    }

    fun signumEquals(other: Vector3i): Boolean {
        return sign(x.toFloat()) == sign(other.x.toFloat()) && sign(y.toFloat()) == sign(other.y.toFloat()) && sign(z.toFloat()) == sign(
            other.z.toFloat()
        )
    }

    fun sqrt(value: Double): Double {
        return kotlin.math.sqrt(value)
    }

    companion object {
        val NULL_VECTOR: Vector3i = Vector3i(0, 0, 0)
        private val MULTIPLY_DE_BRUIJN_BIT_POSITION = intArrayOf(
            0,
            1,
            28,
            2,
            29,
            14,
            24,
            3,
            30,
            22,
            20,
            15,
            25,
            17,
            4,
            8,
            31,
            27,
            13,
            23,
            21,
            19,
            16,
            7,
            26,
            12,
            18,
            6,
            11,
            5,
            10,
            9
        )
        private val SIZE_BITS_X: Int = 1 + floorLog2(smallestEncompassingPowerOfTwo(30000000))
        private val SIZE_BITS_Z: Int = SIZE_BITS_X
        val SIZE_BITS_Y: Int = 64 - SIZE_BITS_X - SIZE_BITS_Z
        private val BITS_Y = (1L shl SIZE_BITS_Y) - 1L
        private val BIT_SHIFT_Z: Int = SIZE_BITS_Y
        private val BIT_SHIFT_X: Int = SIZE_BITS_Y + SIZE_BITS_Z
        private val BITS_Z = (1L shl SIZE_BITS_Z) - 1L
        private val BITS_X = (1L shl SIZE_BITS_X) - 1L
        private val SIN_TABLE = FloatArray(65536)

        init {
            var i: Int = 0
            while (i < 65536) {
                SIN_TABLE[i] = kotlin.math.sin(i.toDouble() * 3.141592653589793 * 2.0 / 65536.0).toFloat()
                ++i
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

        fun floorLog2(value: Int): Int {
            return ceilLog2(value) - (if (isPowerOfTwo(value)) 0 else 1)
        }

        fun isPowerOfTwo(value: Int): Boolean {
            return value != 0 && (value and value - 1) == 0
        }

        fun ceilLog2(value: Int): Int {
            var value = value
            value = if (isPowerOfTwo(value)) value else smallestEncompassingPowerOfTwo(value)
            return MULTIPLY_DE_BRUIJN_BIT_POSITION[(value.toLong() * 125613361L shr 27).toInt() and 31]
        }

        fun sin(f: Float): Float {
            return SIN_TABLE[(f * 10430.378f).toInt() and '\uffff'.code]
        }

        /**
         * cos looked up in the sin table with the appropriate offset
         */
        fun cos(value: Float): Float {
            return SIN_TABLE[(value * 10430.378f + 16384.0f).toInt() and '\uffff'.code]
        }
    }
}
