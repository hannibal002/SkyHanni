package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

import java.util.stream.IntStream;

class BitboardUtils {

    static final int ROWS = Position.ROWS + 1;

    static final long FIRST_COLUMN = 2 * top(0) - 1;
    static final long EMPTY = 0;
    static final long BOTTOM_ROW =
        IntStream.range(0, Position.COLUMNS).mapToLong(BitboardUtils::bottom).reduce(0, Long::sum);
    static final long TOP_ROW = BOTTOM_ROW << Position.ROWS;
    static final long PLAYABLE_SPACE = TOP_ROW - BOTTOM_ROW;

    static long top(int col) {

        return 1L << ((col + 1) * ROWS - 1);
    }

    static long bottom(int col) {

        return 1L << (col * ROWS);
    }

    static long heightOfColumn(long heights, int col) {

        return column(col) & heights;
    }

    static long column(int col) {

        return FIRST_COLUMN << (col * ROWS);
    }

    static long mask(long heights) {

        return heights - BitboardUtils.BOTTOM_ROW;
    }

    static long slot(int col, int row) {

        return 1L << (col * ROWS + row);
    }

    /**
     * Return a bitboard representing missing moves that would form four in a row
     */
    static long winningMoves(long bitboard, long heights) {

        return possibleMoves(heights) & winningSlots(bitboard);
    }

    static long possibleMoves(long heights) {

        return heights & PLAYABLE_SPACE;
    }

    /**
     * Get all slots that, if filled, would yield four in a row (including sentinel slots)
     */
    static long winningSlots(long bitboard) {

        long candidates = (bitboard << 1) & (bitboard << 2) & (bitboard << 3);
        candidates |= winningSlots(bitboard, ROWS - 1);
        candidates |= winningSlots(bitboard, ROWS);
        candidates |= winningSlots(bitboard, ROWS + 1);
        return candidates;
    }

    /**
     * Return slots that, if filled, would form four in a row along a specific direction
     * @param offset the direction, in terms of offset between adjacent slots
     */
    private static long winningSlots(long bitboard, int offset) {

        long adj = bitboard & (bitboard >>> offset);

        return (adj >>> offset) & (bitboard >>> (3 * offset))     // XXX_...
            | (adj >>> offset) & (bitboard << offset)             // .XX_X..
            | (adj << (2 * offset)) & (bitboard >>> offset)       // ..X_XX.
            | (adj << (2 * offset)) & (bitboard << (3 * offset)); // ..._XXX
    }

    static boolean hasTwoOrMore(long bitboard) {

        return (bitboard & (bitboard - 1)) != 0;
    }

    public static String toString(long bitboard) {

        StringBuilder builder = new StringBuilder();
        for (int row = ROWS - 1; row >= 0; --row) {
            for (int col = 0; col < Position.COLUMNS; ++col) {
                long slot = slot(col, row);
                builder.append((slot & bitboard) == 0 ? 'O' : 'X');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private BitboardUtils() {
    }
}
