package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

class Position {

    static final int COLUMNS = 7;
    static final int ROWS = 6;
    static final int TOTAL_SLOTS = COLUMNS * ROWS;

    /**
     * Bitmask of the pieces placed by the next player to move
     */
    private long board = BitboardUtils.EMPTY;
    /**
     * Bitmask of the bottom-most empty cell in each column
     */
    private long heights = BitboardUtils.BOTTOM_ROW;
    private int numMoves = 0;

    static Position fromBoardSnapshot(String boardSnapshot, int pieceCount) {

        Position position = new Position();
        position.heights = BitboardUtils.EMPTY;

        for (int col = 0; col < COLUMNS; ++col) {

            int row;
            for (row = 0; row < ROWS; ++row) {
                char cell = boardSnapshot.charAt((ROWS - 1 - row) * COLUMNS + col);

                if (cell == QuadLinkLegacySolver.EMPTY_PIECE) {
                    break;
                }
                else if (cell == (pieceCount % 2 == 1 ? QuadLinkLegacySolver.PLAYER_PIECE : QuadLinkLegacySolver.WIZARD_PIECE)) {
                    position.board |= BitboardUtils.slot(col, row);
                }
                ++position.numMoves;
            }
            position.heights |= BitboardUtils.slot(col, row);
        }

        return position;
    }

    /**
     * Add a move to the position in the specified slot
     */
    void playMoveInSlot(long slot) {

        board ^= mask();
        heights += slot;
        ++numMoves;
    }

    /**
     * "Undo" the last move in the specified slot.
     *
     * @param slot the most recent slot that was played
     */
    void undoMoveInSlot(long slot) {

        heights -= slot;
        board ^= mask();
        --numMoves;
    }

    /**
     * Return whether the current player can instantly win the game on the next move
     * @return true, if the current player can form four in a row on the next move
     */
    boolean hasWinningMove() {

        return BitboardUtils.winningMoves(board, heights) != BitboardUtils.EMPTY;
    }

    int getAnyWinningMove() {

        long winningMoves = BitboardUtils.winningMoves(board, heights);
        for (int i = 0; i < COLUMNS; ++i) {

            if (canPlayMove(i) && (winningMoves & BitboardUtils.column(i)) != 0) {

                return i;
            }
        }
        return -1;
    }

    long getPossibleMoves() {

        return BitboardUtils.possibleMoves(heights);
    }

    /**
     * May include sentinel slots or slots already played
     */
    long getOpponentThreats() {

        return BitboardUtils.winningSlots(mask() & ~board);
    }

    /**
     * May include sentinel slots, doesn't include slots already played
     */
    long getRealThreatsIfPlaySlot(long slot) {

        return BitboardUtils.winningSlots(board | slot) & ~mask() & BitboardUtils.PLAYABLE_SPACE;
    }

    /**
     * @return the number of moves so far in this position
     */
    int getNumMoves() {

        return numMoves;
    }

    /**
     * @return The number of empty slots in the position
     */
    int getEmptySlotsCount() {

        return Position.TOTAL_SLOTS - getNumMoves();
    }

    /**
     * Return unique key representing current position
     */
    long key() {

        return (board + heights) >>> 1;
    }

    private long mask() {

        return BitboardUtils.mask(heights);
    }

    void playMove(int col) {

        board ^= mask();
        heights += heightOfColumn(col);
        ++numMoves;
    }

    void undoMove(int col) {

        --numMoves;
        heights -= heightOfColumn(col) / 2;
        board ^= mask();
    }

    private long heightOfColumn(int col) {

        return BitboardUtils.heightOfColumn(heights, col);
    }

    boolean canPlayMove(int col) {

        return (heights & BitboardUtils.top(col)) == 0;
    }

    boolean onlyMiddlePlayed() {

        return mask() == (mask() & BitboardUtils.column(3));
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();
        for (int row = ROWS - 1; row >= 0; --row) {
            for (int col = 0; col < COLUMNS; ++col) {
                builder.append(toChar(row, col));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private char toChar(int row, int col) {

        long slot = BitboardUtils.slot(col, row);
        if ((slot & mask()) == 0) {
            return QuadLinkLegacySolver.EMPTY_PIECE;
        }
        return (slot & board) == 0 ? QuadLinkLegacySolver.PLAYER_PIECE : QuadLinkLegacySolver.WIZARD_PIECE;
    }
}
