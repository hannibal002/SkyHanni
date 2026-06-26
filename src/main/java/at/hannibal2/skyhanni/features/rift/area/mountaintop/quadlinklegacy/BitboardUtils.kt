package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

import java.util.stream.IntStream

internal object BitboardUtils {
    const val ROWS: Int = Position.ROWS + 1

    val FIRST_COLUMN: Long = 2 * top(0) - 1
    const val EMPTY: Long = 0
    val BOTTOM_ROW: Long = IntStream.range(0, Position.COLUMNS)
        .mapToLong { col: Int -> bottom(col) }
        .reduce(0) { a: Long, b: Long -> a + b }
    val TOP_ROW: Long = BOTTOM_ROW shl Position.ROWS
    val PLAYABLE_SPACE: Long = TOP_ROW - BOTTOM_ROW

    fun top(col: Int): Long {
        return 1L shl ((col + 1) * ROWS - 1)
    }

    fun bottom(col: Int): Long {
        return 1L shl (col * ROWS)
    }

    fun heightOfColumn(heights: Long, col: Int): Long {
        return column(col) and heights
    }

    fun column(col: Int): Long {
        return FIRST_COLUMN shl (col * ROWS)
    }

    fun mask(heights: Long): Long {
        return heights - BOTTOM_ROW
    }

    fun slot(col: Int, row: Int): Long {
        return 1L shl (col * ROWS + row)
    }

    /**
     * Return a bitboard representing missing moves that would form four in a row
     */
    fun winningMoves(bitboard: Long, heights: Long): Long {
        return possibleMoves(heights) and winningSlots(bitboard)
    }

    fun possibleMoves(heights: Long): Long {
        return heights and PLAYABLE_SPACE
    }

    /**
     * Get all slots that, if filled, would yield four in a row (including sentinel slots)
     */
    fun winningSlots(bitboard: Long): Long {
        var candidates = (bitboard shl 1) and (bitboard shl 2) and (bitboard shl 3)
        candidates = candidates or winningSlots(bitboard, ROWS - 1)
        candidates = candidates or winningSlots(bitboard, ROWS)
        candidates = candidates or winningSlots(bitboard, ROWS + 1)
        return candidates
    }

    /**
     * Return slots that, if filled, would form four in a row along a specific direction
     * @param offset the direction, in terms of offset between adjacent slots
     */
    private fun winningSlots(bitboard: Long, offset: Int): Long {
        val adj = bitboard and (bitboard ushr offset)

        return (
            (adj ushr offset) and (bitboard ushr (3 * offset)) // XXX_...
                or ((adj ushr offset) and (bitboard shl offset)) // .XX_X..
                or ((adj shl (2 * offset)) and (bitboard ushr offset)) // ..X_XX.
                or ((adj shl (2 * offset)) and (bitboard shl (3 * offset))) // ..._XXX
            )
    }

    fun hasTwoOrMore(bitboard: Long): Boolean {
        return (bitboard and (bitboard - 1)) != 0L
    }

    fun toString(bitboard: Long): String {
        val builder = StringBuilder()
        for (row in ROWS - 1 downTo 0) {
            for (col in 0..<Position.COLUMNS) {
                val slot = slot(col, row)
                builder.append(if ((slot and bitboard) == 0L) 'O' else 'X')
            }
            builder.append('\n')
        }
        return builder.toString()
    }
}
