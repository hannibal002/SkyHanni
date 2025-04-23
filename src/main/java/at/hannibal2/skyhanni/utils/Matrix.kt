package at.hannibal2.skyhanni.utils

import kotlin.math.absoluteValue

class Matrix(private val data: Array<DoubleArray>) {
    val width: Int = data.first().size
    val height: Int = data.size
    private val isSquare get(): Boolean = width == height

    init {
        require(data.isNotEmpty()) { "Matrix can't be empty" }
        require(data.all { it.size == width }) { "All rows must have the same length" }
    }

    operator fun get(index: Int): DoubleArray = data[index]

    operator fun get(row: Int, col: Int): Double = data[row][col]

    operator fun set(index: Int, value: DoubleArray) {
        require(value.size == width) { "Invalid row size" }
        data[index] = value
    }

    operator fun set(row: Int, col: Int, value: Double) {
        data[row][col] = value
    }

    fun copy(): Matrix = Matrix(data.deepCopy())

    fun inverse(): Matrix {
        require(isSquare) { "Matrix must be square" }

        val a = data.deepCopy()
        val b = identity(width).data

        for (c in 0 until width) {
            val rBig = (c until height).maxByOrNull { a[it][c].absoluteValue }
            requireNotNull(rBig) { "Cannot invert matrix" }

            val aBig = a[rBig][c].absoluteValue
            require(aBig != 0.0) { "Cannot invert matrix" }

            if (rBig != c) {
                a.swapRows(c, rBig)
                b.swapRows(c, rBig)
            }

            val pivot = a[c][c]
            for (s in c until width) a[c][s] /= pivot
            for (s in 0 until width) b[c][s] /= pivot

            for (r2 in 0 until height) {
                if (r2 == c) continue
                val factor = -a[r2][c]
                for (s in c until width) a[r2][s] += factor * a[c][s]
                for (s in 0 until width) b[r2][s] += factor * b[c][s]
            }
        }
        return Matrix(b)
    }

    fun transpose(): Matrix {
        val transposed = createArray(width, height) { row, col -> data[col][row] }
        return Matrix(transposed)
    }

    operator fun times(other: Matrix): Matrix {
        require(this.width == other.height) { "Invalid Matrix sizes" }
        val result = createArray(height, other.width) { row, col ->
            (0 until width).sumOf { k -> this[row][k] * other[k][col] }
        }
        return Matrix(result)
    }

    operator fun times(other: Double): Matrix {
        val result = createArray(height, width) { row, col -> data[row][col] * other }
        return Matrix(result)
    }

    operator fun plus(other: Matrix): Matrix {
        require(this.width == other.width && this.height == other.height) { "Invalid Dimensions" }
        val result = createArray(height, width) { row, col ->
            data[row][col] + other[row][col]
        }
        return Matrix(result)
    }

    operator fun minus(other: Matrix): Matrix {
        require(this.width == other.width && this.height == other.height) { "Invalid Dimensions" }

        val result = createArray(height, width) { row, col ->
            data[row][col] - other[row][col]
        }
        return Matrix(result)
    }

    override fun toString(): String {
        return buildString {
            for (row in data) {
                for (cell in row) append(" $cell")
                appendLine()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Matrix) return false
        if (height != other.height || width != other.width) return false
        return data.contentDeepEquals(other.data)
    }

    override fun hashCode(): Int = data.contentDeepHashCode()

    companion object {
        fun identity(size: Int): Matrix {
            val result = createArray(size, size) { row, col -> if (row == col) 1.0 else 0.0 }
            return Matrix(result)
        }

        private fun Array<DoubleArray>.deepCopy() = createArray(size, first().size) { row, col -> this[row][col] }

        private inline fun createArray(
            height: Int,
            width: Int,
            function: (row: Int, column: Int) -> Double
        ): Array<DoubleArray> {
            return Array(height) { row -> DoubleArray(width) { col -> function(row, col) } }
        }

        private fun Array<DoubleArray>.swapRows(i: Int, j: Int) {
            val temp = this[i]
            this[i] = this[j]
            this[j] = temp
        }
    }
}
