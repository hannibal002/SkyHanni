package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

internal class MoveList {
    private val moves = LongArray(Position.COLUMNS)
    private var size = 0

    fun calculateMoveOrder(p: Position, candidateMoves: Long) {
        size = 0
        for (i in DEFAULT_COLUMN_ORDERING.indices) {
            val column: Long = DEFAULT_COLUMN_ORDERING[i]

            val move = column and candidateMoves
            if (move == BitboardUtils.EMPTY) {
                continue
            }

            // This is a good candidate for Vector API
            val priority = p.getRealThreatsIfPlaySlot(move).countOneBits()
            val packedMove: Long = packMove(move, priority, i)
            var idx = size
            while (idx > 0 && packedMove > moves[idx - 1]) {
                moves[idx] = moves[idx - 1]
                --idx
            }
            moves[idx] = packedMove
            ++size
        }
    }

    fun size(): Int {
        return size
    }

    fun get(idx: Int): Long {
        return moves[idx] and MOVE_MASK
    }

    companion object {
        val DEFAULT_COLUMN_ORDERING: LongArray = LongArray(Position.COLUMNS)

        init {
            var right: Int = Position.COLUMNS / 2
            var left = right - 1
            for (i in 0..<Position.COLUMNS) {
                DEFAULT_COLUMN_ORDERING[i] = BitboardUtils.column(if (i % 2 == 0) right++ else left--)
            }
        }

        private const val MOVE_BITS = 48
        private const val ORDER_BITS = 3
        private const val MOVE_MASK = (1L shl MOVE_BITS) - 1

        private fun packMove(move: Long, priority: Int, order: Int): Long {
            return ((priority.toLong() shl (MOVE_BITS + ORDER_BITS)) //
                    or ((Position.COLUMNS - order).toLong() shl MOVE_BITS) //
                    or move)
        }
    }
}
