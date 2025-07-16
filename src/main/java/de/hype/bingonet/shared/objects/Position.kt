package de.hype.bingonet.shared.objects

class Position : Vector3i {
    constructor(x: Int, y: Int, z: Int) : super(x, y, z)

    constructor(vector: Vector3i) : super(vector.x, vector.y, vector.z)

    override fun toString(): String {
        return "$x $y $z"
    }

    fun toFullString(): String {
        return "X:$x Y:$y Z:$z"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Position) return false
        return (other.x == x && other.y == y && other.z == z)
    }

    fun getDistanceBetween(pos: Position): Double {
        val x = pos.x - this.x
        val y = pos.y - this.y
        val z = pos.z - this.z
        return kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
    }

    fun isInRange(otherPos: Position, range: Int): Boolean {
        return getDistanceBetween(otherPos) < range
    }

    companion object {
        val ORIGIN: Position = Position(0, 0, 0)

        /**
         * @param string needs to be `x y z` formating!
         */
        @JvmStatic
        fun fromString(string: String): Position {
            val temp: Array<String> = string.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Position(temp[0].toInt(), temp[1].toInt(), temp[2].toInt())
        }
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}
