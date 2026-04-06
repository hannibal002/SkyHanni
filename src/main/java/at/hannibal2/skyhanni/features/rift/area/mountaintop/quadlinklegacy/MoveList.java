package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

class MoveList {

    static final long[] DEFAULT_COLUMN_ORDERING = new long[Position.COLUMNS];
    static {
        int right = Position.COLUMNS / 2;
        int left = right - 1;
        for (int i = 0; i < Position.COLUMNS; ++i) {
            DEFAULT_COLUMN_ORDERING[i] = BitboardUtils.column(i % 2 == 0 ? right++ : left--);
        }
    }

    private static final int MOVE_BITS = 48;
    private static final int ORDER_BITS = 3;
    private static final long MOVE_MASK = (1L << MOVE_BITS) - 1;

    private final long[] moves = new long[Position.COLUMNS];
    private int size;

    void calculateMoveOrder(Position p, long candidateMoves) {

        size = 0;
        for (int i = 0; i < DEFAULT_COLUMN_ORDERING.length; ++i) {

            long column = DEFAULT_COLUMN_ORDERING[i];

            long move = column & candidateMoves;
            if (move == BitboardUtils.EMPTY) {

                continue;
            }

            // This is a good candidate for Vector API
            int priority = Long.bitCount(p.getRealThreatsIfPlaySlot(move));
            long packedMove = packMove(move, priority, i);
            int idx = size;
            while (idx > 0 && packedMove > moves[idx - 1]) {

                moves[idx] = moves[idx - 1];
                --idx;
            }
            moves[idx] = packedMove;
            ++size;
        }
    }

    int size() {

        return size;
    }

    long get(int idx) {

        return moves[idx] & MOVE_MASK;
    }

    private static long packMove(long move, int priority, int order) {

        return ((long) priority << (MOVE_BITS + ORDER_BITS))   //
            | ((long) (Position.COLUMNS - order) << MOVE_BITS) //
            | move;
    }
}
