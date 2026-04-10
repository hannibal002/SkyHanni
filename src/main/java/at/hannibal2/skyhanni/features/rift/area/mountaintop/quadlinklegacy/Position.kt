package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

internal class Position {
    /**
     * Bitmask of the pieces placed by the next player to move
     */
    private var board = BitboardUtils.EMPTY

    /**
     * Bitmask of the bottom-most empty cell in each column
     */
    private var heights = BitboardUtils.BOTTOM_ROW

    /**
     * @return the number of moves so far in this position
     */
    var numMoves: Int = 0
        private set

    /**
     * Add a move to the position in the specified slot
     */
    fun playMoveInSlot(slot: Long) {
        board = board xor mask()
        heights += slot
        ++numMoves
    }

    /**
     * "Undo" the last move in the specified slot.
     * 
     * @param slot the most recent slot that was played
     */
    fun undoMoveInSlot(slot: Long) {
        heights -= slot
        board = board xor mask()
        --numMoves
    }

    /**
     * Return whether the current player can instantly win the game on the next move
     * @return true, if the current player can form four in a row on the next move
     */
    fun hasWinningMove(): Boolean {
        return BitboardUtils.winningMoves(board, heights) != BitboardUtils.EMPTY
    }

    val anyWinningMove: Int
        get() {
            val winningMoves =
                BitboardUtils.winningMoves(
                    board,
                    heights
                )
            for (i in 0..<COLUMNS) {
                if (canPlayMove(i) && (winningMoves and BitboardUtils.column(
                        i
                    )) != 0L
                ) {
                    return i
                }
            }
            return -1
        }

    val possibleMoves: Long
        get() = BitboardUtils.possibleMoves(heights)

    val opponentThreats: Long
        /**
         * May include sentinel slots or slots already played
         */
        get() = BitboardUtils.winningSlots(mask() and board.inv())

    /**
     * May include sentinel slots, doesn't include slots already played
     */
    fun getRealThreatsIfPlaySlot(slot: Long): Long {
        return BitboardUtils.winningSlots(board or slot) and mask().inv() and BitboardUtils.PLAYABLE_SPACE
    }

    val emptySlotsCount: Int
        /**
         * @return The number of empty slots in the position
         */
        get() = TOTAL_SLOTS - this.numMoves

    /**
     * Return unique key representing current position
     */
    fun key(): Long {
        return (board + heights) ushr 1
    }

    private fun mask(): Long {
        return BitboardUtils.mask(heights)
    }

    fun playMove(col: Int) {
        board = board xor mask()
        heights += heightOfColumn(col)
        ++numMoves
    }

    fun undoMove(col: Int) {
        --numMoves
        heights -= heightOfColumn(col) / 2
        board = board xor mask()
    }

    private fun heightOfColumn(col: Int): Long {
        return BitboardUtils.heightOfColumn(heights, col)
    }

    fun canPlayMove(col: Int): Boolean {
        return (heights and BitboardUtils.top(col)) == 0L
    }

    fun onlyMiddlePlayed(): Boolean {
        return mask() == (mask() and BitboardUtils.column(3))
    }

    override fun toString(): String {
        val builder = StringBuilder()
        for (row in ROWS - 1 downTo 0) {
            for (col in 0..<COLUMNS) {
                builder.append(toChar(row, col))
            }
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun toChar(row: Int, col: Int): Char {
        val slot = BitboardUtils.slot(col, row)
        if ((slot and mask()) == 0L) {
            return QuadLinkLegacySolver.EMPTY_PIECE
        }
        return if ((slot and board) == 0L) QuadLinkLegacySolver.PLAYER_PIECE else QuadLinkLegacySolver.WIZARD_PIECE
    }

    companion object {
        const val COLUMNS: Int = 7
        const val ROWS: Int = 6
        const val TOTAL_SLOTS: Int = COLUMNS * ROWS

        fun fromBoardSnapshot(boardSnapshot: String, pieceCount: Int): Position {
            val position = Position()
            position.heights = BitboardUtils.EMPTY

            for (col in 0..<COLUMNS) {
                var row: Int = 0
                while (row < ROWS) {
                    val cell = boardSnapshot[(ROWS - 1 - row) * COLUMNS + col]

                    if (cell == QuadLinkLegacySolver.EMPTY_PIECE) {
                        break
                    } else if (cell == (if (pieceCount % 2 == 1) QuadLinkLegacySolver.PLAYER_PIECE else QuadLinkLegacySolver.WIZARD_PIECE)) {
                        position.board = position.board or BitboardUtils.slot(col, row)
                    }
                    ++position.numMoves
                    ++row
                }
                position.heights = position.heights or BitboardUtils.slot(col, row)
            }

            return position
        }
    }
}
