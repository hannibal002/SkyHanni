package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.features.event.carnival.CarnivalFruitDigging.Fruit
import java.util.EnumMap
import kotlin.random.Random

internal class FruitDiggingBelief(
    private val openCells: List<Int>,
    private val pool: List<Fruit>,
    private val clues: List<Clue>,
    private val random: Random = Random.Default,
) {
    init {
        require(openCells.size == pool.size) {
            "openCells (${openCells.size}) and pool (${pool.size}) must be the same size"
        }
    }

    /** How many sampled boards we create */
    var samples: Int = 0
        private set

    sealed interface Clue

    class BombCount(val cells: List<Int>, val bombs: Int) : Clue

    class FruitFloor(val cells: List<Int>, val minRank: Int, val named: Fruit, val satisfiedByKnown: Boolean) : Clue

    class FruitCeil(val cells: List<Int>, val maxRank: Int, val named: Fruit, val satisfiedByKnown: Boolean) : Clue

    class NoFruitNearby(val cells: List<Int>) : Clue

    class MustBeFruit(val cell: Int) : Clue

    fun estimate(): Map<Int, Map<Fruit, Double>> {
        if (openCells.isEmpty()) {
            samples = 0
            return emptyMap()
        }

        if (clues.isEmpty()) {
            samples = TARGET_SAMPLES
            val marginal = baseRateDistribution()
            return openCells.associateWith { marginal }
        }

        val slotOf = HashMap<Int, Int>(openCells.size * 2)
        openCells.forEachIndexed { slot, cell -> slotOf[cell] = slot }
        val compiledClues = clues.map { it to slotsFor(it, slotOf) }

        val deal = pool.toTypedArray() // shuffled in place each attempt; deal[slot] = content at that cell
        val hits = Array(openCells.size) { EnumMap<Fruit, Int>(Fruit::class.java) } // per slot: content -> times seen

        var accepted = 0
        var attempts = 0
        while (accepted < TARGET_SAMPLES && attempts < MAX_ATTEMPTS) {
            attempts++
            shuffleInPlace(deal)
            if (compiledClues.all { (clue, slots) -> satisfies(clue, slots, deal) }) {
                accepted++
                for (slot in deal.indices) hits[slot].merge(deal[slot], 1, Int::plus)
            }
        }
        samples = accepted

        // if no boards, fall back to base rates so callers still get something
        if (accepted == 0) {
            val marginal = baseRateDistribution()
            return openCells.associateWith { marginal }
        }

        return buildMap {
            openCells.forEachIndexed { slot, cell ->
                put(cell, hits[slot].mapValues { (_, count) -> count.toDouble() / accepted })
            }
        }
    }

    private fun baseRateDistribution(): Map<Fruit, Double> {
        val total = pool.size.toDouble()
        return pool.groupingBy { it }.eachCount().mapValues { (_, count) -> count / total }
    }

    private fun slotsFor(clue: Clue, slotOf: Map<Int, Int>): IntArray = when (clue) {
        is BombCount -> clue.cells.mapNotNull { slotOf[it] }.toIntArray()
        is FruitFloor -> clue.cells.mapNotNull { slotOf[it] }.toIntArray()
        is FruitCeil -> clue.cells.mapNotNull { slotOf[it] }.toIntArray()
        is NoFruitNearby -> clue.cells.mapNotNull { slotOf[it] }.toIntArray()
        is MustBeFruit -> slotOf[clue.cell]?.let { intArrayOf(it) } ?: IntArray(0)
    }

    private fun satisfies(clue: Clue, slots: IntArray, deal: Array<Fruit>): Boolean = when (clue) {
        is BombCount -> {
            var bombs = 0
            for (slot in slots) if (deal[slot] == Fruit.BOMB) bombs++
            bombs == clue.bombs
        }

        is NoFruitNearby -> slots.none { deal[it].isEdible }

        is FruitFloor -> {
            var foundNamed = clue.satisfiedByKnown
            var ok = true
            for (slot in slots) {
                val fruit = deal[slot]
                if (!fruit.isEdible) continue
                if (fruitDowsingRank(fruit) < clue.minRank) {
                    ok = false
                    break
                } // a fruit below the floor
                if (fruit == clue.named) foundNamed = true
            }
            ok && foundNamed
        }

        is FruitCeil -> {
            var foundNamed = clue.satisfiedByKnown
            var ok = true
            for (slot in slots) {
                val fruit = deal[slot]
                if (!fruit.isEdible) continue
                if (fruitDowsingRank(fruit) > clue.maxRank) {
                    ok = false
                    break
                } // a fruit above the cap
                if (fruit == clue.named) foundNamed = true
            }
            ok && foundNamed
        }

        is MustBeFruit -> slots.isEmpty() || deal[slots[0]].isEdible
    }

    private fun shuffleInPlace(array: Array<Fruit>) {
        for (i in array.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val tmp = array[i]
            array[i] = array[j]
            array[j] = tmp
        }
    }

    companion object {
        fun fruitDowsingRank(fruit: Fruit): Int = if (fruit == Fruit.APPLE) 0 else fruit.points

        /** How many boards with clue we aim to collect before reporting probabilities. */
        private const val TARGET_SAMPLES = 500

        private const val MAX_ATTEMPTS = 20_000
    }
}
