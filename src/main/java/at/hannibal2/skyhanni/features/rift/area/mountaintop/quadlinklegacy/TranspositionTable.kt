package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

import java.util.*

internal class TranspositionTable(private val statistics: Statistics) {
    private val table = LongArray(NUM_BUCKETS)

    fun set(key: Long, value: Int, numMoves: Int) {
        statistics.recordTTSet()

        val handle = 2 * hash(key)

        val packed1 = table[handle]
        val key1 = Entry.getKey(packed1)
        val value1 = Entry.getValue(packed1)

        val packed2 = table[handle + 1]
        val key2 = Entry.getKey(packed2)
        val value2 = Entry.getValue(packed2)

        if (key == key2) {
            if (value < value2) {
                table[handle + 1] = Entry.toLong(key, value, numMoves)
            }
            return
        }

        if (key != key1 || value < value1) {
            table[handle] = Entry.toLong(key, value, numMoves)

            if (key != key1 && key1 != 0L) {
                if (key2 != 0L && key1 != key2) {
                    statistics.incrementTTCacheEvictions()
                }
                if (key2 == 0L || Entry.getNumMoves(packed1) <= Entry.getNumMoves(packed2)) {
                    table[handle + 1] = packed1
                }
            }
        }
    }

    fun getValueOrDefault(key: Long, defaultValue: Int): Int {
        statistics.recordTTGet()

        val handle = 2 * hash(key)

        val packed1 = table[handle]
        val key1 = Entry.getKey(packed1)
        if (key1 == key) {
            statistics.incrementTTHits()
            return Entry.getValue(packed1)
        }

        val packed2 = table[handle + 1]
        val key2 = Entry.getKey(packed2)
        if (key2 == key) {
            statistics.incrementTTHits()
            return Entry.getValue(packed2)
        }
        statistics.incrementTTMisses()
        return defaultValue
    }

    private fun hash(key: Long): Int {
        return ((key * GOLDEN_GAMMA) ushr (Long.SIZE_BITS - INDEX_SIZE)).toInt()
    }

    val entriesUsed: Int
        get() {
            var count = 0
            for (tableEntry in table) {
                if (tableEntry != 0L) {
                    ++count
                }
            }
            return count
        }

    fun reset() {
        Arrays.fill(table, 0L)
    }

    internal object Entry {
        private const val KEY_SIZE = 48
        private const val SCORE_SIZE = 7
        private const val NUM_MOVES_SIZE = 6

        private const val KEY_MASK = (1L shl KEY_SIZE) - 1
        private const val SCORE_MASK = (1L shl SCORE_SIZE) - 1
        private const val NUM_MOVES_MASK = (1L shl NUM_MOVES_SIZE) - 1

        fun toLong(key: Long, value: Int, numMoves: Int): Long {
            return (numMoves.toLong() shl (KEY_SIZE + SCORE_SIZE)) or (key shl SCORE_SIZE) or packScore(value)
        }

        fun getKey(packed: Long): Long {
            return (packed ushr SCORE_SIZE) and KEY_MASK
        }

        fun getValue(packed: Long): Int {
            return unpackScore(packed and SCORE_MASK)
        }

        fun getNumMoves(packed: Long): Int {
            return ((packed ushr (KEY_SIZE + SCORE_SIZE)) and NUM_MOVES_MASK).toInt()
        }

        private fun packScore(score: Int): Long {
            return score - Solver.SCORE_MIN + 1L
        }

        private fun unpackScore(bits: Long): Int {
            return bits.toInt() + Solver.SCORE_MIN - 1
        }
    }

    companion object {
        const val NUM_BUCKETS: Int = (Config.TABLE_SIZE_MIB * 1024 * 1024) / Long.SIZE_BYTES
        private const val NUM_ROWS: Int = NUM_BUCKETS / 2

        private val INDEX_SIZE = NUM_ROWS.toLong().countTrailingZeroBits()

        private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    }
}
