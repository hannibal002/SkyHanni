package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

import java.util.Arrays;

class TranspositionTable {

    private static final int NUM_ENTRIES = (Config.TABLE_SIZE_MIB * 1024 * 1024) / Long.BYTES;
    private static final int NUM_ROWS = NUM_ENTRIES / 2;

    private static final int INDEX_SIZE = Long.numberOfTrailingZeros(NUM_ROWS);

    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;

    private final long[] table = new long[NUM_ENTRIES];

    private final Statistics statistics;

    TranspositionTable(Statistics statistics) {

        this.statistics = statistics;
    }

    void set(long key, int value, int numMoves) {

        statistics.recordTTSet(key);

        int handle = 2 * hash(key);

        long packed1 = table[handle];
        long key1 = Entry.getKey(packed1);
        int value1 = Entry.getValue(packed1);

        long packed2 = table[handle + 1];
        long key2 = Entry.getKey(packed2);
        int value2 = Entry.getValue(packed2);

        if (key == key2) {
            if (value < value2) {
                table[handle + 1] = Entry.toLong(key, value, numMoves);
            }
            return;
        }

        if (key != key1 || value < value1) {

            table[handle] = Entry.toLong(key, value, numMoves);

            if (key != key1 && key1 != 0) {

                if (key2 != 0 && key1 != key2) {
                    statistics.incrementTTCacheEvictions();
                }
                if (key2 == 0 || Entry.getNumMoves(packed1) <= Entry.getNumMoves(packed2)) {
                    table[handle + 1] = packed1;
                }
            }
        }
    }

    int getValueOrDefault(long key, int defaultValue) {

        statistics.recordTTGet(key);

        int handle = 2 * hash(key);

        long packed1 = table[handle];
        long key1 = Entry.getKey(packed1);
        if (key1 == key) {

            statistics.incrementTTHits();
            return Entry.getValue(packed1);
        }

        long packed2 = table[handle + 1];
        long key2 = Entry.getKey(packed2);
        if (key2 == key) {

            statistics.incrementTTHits();
            return Entry.getValue(packed2);
        }
        statistics.incrementTTMisses();
        return defaultValue;
    }

    private int hash(long key) {

        return (int) ((key * GOLDEN_GAMMA) >>> (Long.SIZE - INDEX_SIZE));
    }

    int getEntriesUsed() {

        int count = 0;
        for (long tableEntry : table) {

            if (tableEntry != 0) {
                ++count;
            }
        }
        return count;
    }

    int getCapacity() {

        return NUM_ENTRIES;
    }

    void reset() {

        Arrays.fill(table, 0);
    }

    static class Entry {

        private static final int KEY_SIZE = 48;
        private static final int SCORE_SIZE = 7;
        private static final int NUM_MOVES_SIZE = 6;

        private static final long KEY_MASK = (1L << KEY_SIZE) - 1;
        private static final long SCORE_MASK = (1L << SCORE_SIZE) - 1;
        private static final long NUM_MOVES_MASK = (1L << NUM_MOVES_SIZE) - 1;

        static long toLong(long key, int value, int numMoves) {

            return ((long) numMoves << (KEY_SIZE + SCORE_SIZE)) | (key << SCORE_SIZE) | packScore(value);
        }

        static long getKey(long packed) {

            return (packed >>> SCORE_SIZE) & KEY_MASK;
        }

        static int getValue(long packed) {

            return unpackScore(packed & SCORE_MASK);
        }

        static int getNumMoves(long packed) {

            return (int) ((packed >>> (KEY_SIZE + SCORE_SIZE)) & NUM_MOVES_MASK);
        }

        private static long packScore(int score) {

            return score - Solver.SCORE_MIN + 1L;
        }

        private static int unpackScore(long bits) {

            return (int) bits + Solver.SCORE_MIN - 1;
        }
    }
}
