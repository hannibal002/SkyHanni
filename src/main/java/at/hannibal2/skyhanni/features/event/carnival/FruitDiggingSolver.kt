package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.features.event.carnival.CarnivalFruitDigging.Fruit

/**
 *
 * Steps:
 *  1. Read the board. Sort every cell into: known content, a still-diggable unknown,
 *    a fruit destroyed by a bomb and gather the dowsing clues we collected
 *  2. Estimate beliefs. Give the unknowns and clues to [FruitDiggingBelief], which returns
 *    the probability
 *  3. Score and pick
 */
class FruitDiggingSolver(private val size: Int = 7) {

    data class CellInput(
        val content: Fruit?,
        val diggable: Boolean,
        val ghost: Boolean,
        val mustFruit: Boolean,
        val minesCount: Int?,
        val treasure: Fruit?,
        val anchor: Fruit?,
    )

    data class Recommendation(
        val targetRow: Int,
        val targetCol: Int,
        val shovel: String,
        val expectedPoints: Double,
        val pBomb: Double,
        val pRum: Double,
        val samples: Int,
    )

    // Pre-compute each cell up-to-8 neighbors once
    private val neighborsOf: Array<IntArray> = run {
        val n = size
        Array(n * n) { id ->
            val row = id / n
            val col = id % n
            buildList {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val r = row + dr
                        val c = col + dc
                        if (r in 0 until n && c in 0 until n) add(r * n + c)
                    }
                }
            }.toIntArray()
        }
    }

    private val allContents: List<Fruit> = Fruit.entries.filter { it.isEdible || it == Fruit.BOMB || it == Fruit.RUM }

    fun recommend(
        grid: Array<Array<CellInput>>,
        digsUsed: Int,
        nextMultiplier: Double,
        coconutProtection: Boolean,
    ): Recommendation? = Advisor(grid, digsUsed, nextMultiplier, coconutProtection).recommend()

    private inner class Advisor(
        grid: Array<Array<CellInput>>,
        private val digsUsed: Int,
        private val nextMultiplier: Double,
        private val coconutProtection: Boolean,
    ) {

        private val knownContent = arrayOfNulls<Fruit>(size * size)

        private val diggable = BooleanArray(size * size)

        private val ghost = BooleanArray(size * size)

        private val candidates = ArrayList<Int>()

        private val openCells = ArrayList<Int>()

        private val openCellsOnBoard = HashSet<Int>()

        private var applesCollected = 0
        private var cherriesCollected = 0

        private val beliefs: Map<Int, Map<Fruit, Double>>
        private val sampleCount: Int

        init {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val id = row * size + col
                    val cell = grid[row][col]
                    val content = cell.content?.takeIf { it != Fruit.UNKNOWN && it != Fruit.NO_FRUIT }

                    knownContent[id] = content
                    diggable[id] = cell.diggable
                    ghost[id] = cell.ghost && content == null

                    if (cell.diggable) {
                        candidates.add(id)
                        if (content == null) {
                            openCells.add(id)
                            openCellsOnBoard.add(id)
                        }
                    } else if (content == null) {
                        openCells.add(id) // off-board but still an unknown
                    }

                    if (content != null && !cell.diggable) {
                        if (content == Fruit.APPLE) applesCollected++
                        if (content == Fruit.CHERRY) cherriesCollected++
                    }
                }
            }

            val belief = FruitDiggingBelief(openCells, remainingBag(), gatherClues(grid))
            beliefs = belief.estimate()
            sampleCount = belief.samples
        }

        private fun remainingBag(): List<Fruit> {
            val counts = HashMap<Fruit, Int>()
            for (content in allContents) counts[content] = content.count
            for (id in knownContent.indices) knownContent[id]?.let { counts[it] = (counts[it] ?: 0) - 1 }
            return buildList {
                for ((content, n) in counts) repeat(n.coerceAtLeast(0)) { add(content) }
            }
        }

        private fun gatherClues(grid: Array<Array<CellInput>>): List<FruitDiggingBelief.Clue> = buildList {
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val id = row * size + col
                    val cell = grid[row][col]
                    val hiddenNeighbors = neighborsOf[id].filter { it in openCellsOnBoard }

                    cell.minesCount?.let { add(FruitDiggingBelief.BombCount(hiddenNeighbors, it)) }
                    cell.treasure?.let { add(orderClue(id, hiddenNeighbors, it, isAnchor = false)) }
                    cell.anchor?.let { add(orderClue(id, hiddenNeighbors, it, isAnchor = true)) }

                    if (ghost[id] || cell.mustFruit) add(FruitDiggingBelief.MustBeFruit(id))
                }
            }
        }

        private fun orderClue(
            center: Int,
            hiddenNeighbors: List<Int>,
            named: Fruit,
            isAnchor: Boolean,
        ): FruitDiggingBelief.Clue {
            if (named == Fruit.NO_FRUIT) return FruitDiggingBelief.NoFruitNearby(hiddenNeighbors)

            // If a neighbor we already know IS the named fruit, the "named fruit
            // exists nearby" half of the clue is satisfied without sampling
            val satisfiedByKnown = neighborsOf[center].any { knownContent[it] == named && diggable[it] }
            val rank = FruitDiggingBelief.fruitDowsingRank(named)
            return if (isAnchor) {
                FruitDiggingBelief.FruitFloor(hiddenNeighbors, rank, named, satisfiedByKnown)
            } else {
                FruitDiggingBelief.FruitCeil(hiddenNeighbors, rank, named, satisfiedByKnown)
            }
        }


        private fun pointValue(content: Fruit): Double = when (content) {
            Fruit.APPLE -> 100.0 * (applesCollected + 1)
            Fruit.CHERRY -> 200.0 + 300.0 * (cherriesCollected + 1)
            Fruit.BOMB, Fruit.RUM, Fruit.UNKNOWN, Fruit.NO_FRUIT -> 0.0
            else -> content.points.toDouble()
        }

        private fun distribution(cell: Int): Map<Fruit, Double> = beliefs[cell].orEmpty()

        private fun probabilityOf(cell: Int, content: Fruit): Double {
            knownContent[cell]?.let { return if (it == content) 1.0 else 0.0 }
            return distribution(cell)[content] ?: 0.0
        }

        private fun bombChance(cell: Int) = probabilityOf(cell, Fruit.BOMB)
        private fun rumChance(cell: Int) = probabilityOf(cell, Fruit.RUM)

        private fun expectedPoints(cell: Int): Double {
            val known = knownContent[cell]
            val raw = if (known != null) {
                valueIncludingWatermelon(known, cell)
            } else {
                distribution(cell).entries.sumOf { (content, p) -> p * valueIncludingWatermelon(content, cell) }
            }
            return raw * nextMultiplier
        }

        private fun valueIncludingWatermelon(content: Fruit, cell: Int): Double {
            if (content != Fruit.WATERMELON) return pointValue(content)
            val nearby = averageNearbyFruitValue(cell)
            val target = if (nearby > 0) nearby else averageRemainingFruitValue()
            return pointValue(content) + 0.5 * target
        }

        private fun averageNearbyFruitValue(cell: Int): Double {
            var value = 0.0
            var weight = 0.0
            for (nb in neighborsOf[cell]) {
                if (!diggable[nb]) continue // dug / destroyed / ghost: not a nearby fruit any more
                val known = knownContent[nb]
                if (known != null) {
                    if (known.isEdible) {
                        value += pointValue(known)
                        weight += 1
                    }
                } else {
                    for ((content, p) in distribution(nb)) {
                        if (content.isEdible) {
                            value += p * pointValue(content)
                            weight += p
                        }
                    }
                }
            }
            return if (weight > 0) value / weight else 0.0
        }

        private fun averageRemainingFruitValue(): Double {
            var count = 0
            var value = 0.0
            for (content in allContents) {
                if (!content.isEdible) continue
                val n = remainingFruitCount(content)
                count += n
                value += pointValue(content) * n
            }
            return if (count > 0) value / count else 0.0
        }

        private fun remainingFruitCount(content: Fruit): Int {
            var n = content.count
            for (id in knownContent.indices) if (knownContent[id] == content) n--
            return n
        }

        /** Total fruit value a bomb here would destroy (used to penalize risky digs). */
        private fun fruitValueAtRisk(cell: Int): Double {
            var total = 0.0
            for (nb in neighborsOf[cell]) {
                if (!diggable[nb]) continue
                val known = knownContent[nb]
                if (known != null) {
                    if (known.isEdible) total += pointValue(known)
                } else {
                    for ((content, p) in distribution(nb)) if (content.isEdible) total += p * pointValue(content)
                }
            }
            return total
        }

        /**
         * The value of a good dig we lose by wasting this turn on a bomb/rum.
         * Used as the "cost" of risk: the best among clearly-safe candidates,
         * or just the average remaining fruit if nothing is safe
         */
        private fun opportunityCost(): Double {
            val bestSafe = candidates
                .filter { bombChance(it) < 0.05 && rumChance(it) < 0.05 }
                .maxOfOrNull { expectedPoints(it) }
                ?: 0.0
            return if (bestSafe > 0) bestSafe else averageRemainingFruitValue()
        }

        private fun score(cell: Int, opportunityCost: Double): Double {
            val expected = expectedPoints(cell)

            var risk = (bombChance(cell) + rumChance(cell)) * opportunityCost * RISK_WEIGHT
            if (!coconutProtection) risk += bombChance(cell) * DESTRUCTION_WEIGHT * fruitValueAtRisk(cell)

            val setup = (probabilityOf(cell, Fruit.POMEGRANATE) - probabilityOf(cell, Fruit.DURIAN)) *
                SETUP_WEIGHT * opportunityCost

            val hiddenNeighbors = neighborsOf[cell].count { it in openCellsOnBoard }
            val information = hiddenNeighbors * INFO_WEIGHT

            return expected - risk + setup + information
        }

        private fun shovelFor(cell: Int): String {
            var expectedFruitNearby = 0.0
            for (nb in neighborsOf[cell]) {
                if (!diggable[nb]) continue
                val known = knownContent[nb]
                if (known != null) {
                    if (known.isEdible) expectedFruitNearby += 1
                } else {
                    for ((content, p) in distribution(nb)) if (content.isEdible) expectedFruitNearby += p
                }
            }
            return if (expectedFruitNearby >= 0.25) "Anchor" else "Mines"
        }

        fun recommend(): Recommendation? {
            if (candidates.isEmpty()) return null
            val opportunityCost = opportunityCost()

            var ranked = candidates.sortedByDescending { score(it, opportunityCost) }

            // If a durian already halved our next dig, don't waste a high-value cell
            // on it — spend the penalty on the cheapest safe dig and save the good
            // cells for full value next turn (only worth it with digs to spare)
            if (nextMultiplier < 1.0 && (CarnivalFruitDigging.MAX_DIGS - digsUsed) > 1) {
                val absorber = candidates
                    .filter { bombChance(it) < 0.15 && rumChance(it) < 0.15 }
                    .minByOrNull { expectedPoints(it) }
                if (absorber != null) ranked = listOf(absorber) + ranked.filter { it != absorber }
            }

            val best = ranked.first()
            return Recommendation(
                targetRow = best / size,
                targetCol = best % size,
                shovel = shovelFor(best),
                expectedPoints = expectedPoints(best),
                pBomb = bombChance(best),
                pRum = rumChance(best),
                samples = sampleCount,
            )
        }
    }

    private companion object {
        const val RISK_WEIGHT = 1.0 // cost of a wasted dig, scaled by P(bomb)+P(rum)
        const val DESTRUCTION_WEIGHT = 0.10 // extra penalty: fraction of neighbor fruit a bomb would destroy
        const val SETUP_WEIGHT = 0.5 // worth of the multiplier an ability cell sets up
        const val INFO_WEIGHT = 15.0 // bonus per hidden neighbor a dig would reveal
    }
}
