package at.hannibal2.skyhanni.features.mining.fossilexcavator.solver

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.mining.glacite.FossilExcavatorSolverConfig.SolverMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FossilSolver {
    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.solver
    data class SolverSnapshot(
        val clickablePositions: Map<FossilTile, Int> = emptyMap(),
        val totalPossibleFossils: Int = 0,
    )

    @Volatile
    var currentSnapshot: SolverSnapshot = SolverSnapshot()

    /*
    to be used when they have less than 18 clicks
     - solves 361/404 in at most 16 clicks
     - solves 396/404 in at most 17 clicks
     - solves 400/404 in at most 18 clicks
     This is why it is not used all the time
     */
    private val riskyStartingSequence: Set<Triple<FossilTile, Double, Int>> = setOf(
        Triple(FossilTile(4, 2), 0.515, 404),
        Triple(FossilTile(5, 3), 0.393, 196),
        Triple(FossilTile(3, 2), 0.513, 119),
        Triple(FossilTile(7, 2), 0.345, 58),
        Triple(FossilTile(1, 3), 0.342, 38),
        Triple(FossilTile(3, 4), 0.6, 25),
        Triple(FossilTile(5, 1), 0.8, 10),
        Triple(FossilTile(4, 3), 1.0, 2),
    )

    // once they have 18 chisels solves all in 18 clicks
    private val safeStartingSequence: Set<Triple<FossilTile, Double, Int>> = setOf(
        Triple(FossilTile(4, 2), 0.515, 404),
        Triple(FossilTile(5, 4), 0.413, 196),
        Triple(FossilTile(3, 3), 0.461, 115),
        Triple(FossilTile(5, 2), 0.387, 62),
        Triple(FossilTile(3, 1), 0.342, 38),
        Triple(FossilTile(7, 3), 0.48, 25),
        Triple(FossilTile(1, 2), 0.846, 13),
        Triple(FossilTile(3, 4), 1.0, 2),
    )

    // Sequence optimized to avoid hitting fossils
    // This could be only 25 steps since that is the max number of possible chisel charges right now
    // But I put the entire 53 steps for future proofing
    private val worstStartingSequence: Set<Triple<FossilTile, Double, Int>> = setOf(
        Triple(FossilTile(8, 5), 0.002, 404),
        Triple(FossilTile(8, 2), 0.005, 403),
        Triple(FossilTile(8, 3), 0.005, 401),
        Triple(FossilTile(8, 4), 0.005, 399),
        Triple(FossilTile(8, 0), 0.013, 397),
        Triple(FossilTile(8, 1), 0.005, 392),
        Triple(FossilTile(7, 5), 0.021, 390),
        Triple(FossilTile(0, 5), 0.024, 382),
        Triple(FossilTile(0, 0), 0.027, 373),
        Triple(FossilTile(7, 0), 0.028, 363),
        Triple(FossilTile(7, 1), 0.031, 353),
        Triple(FossilTile(6, 0), 0.029, 342),
        Triple(FossilTile(7, 2), 0.033, 332),
        Triple(FossilTile(7, 3), 0.031, 321),
        Triple(FossilTile(7, 4), 0.019, 311),
        Triple(FossilTile(6, 5), 0.030, 305),
        Triple(FossilTile(1, 0), 0.041, 296),
        Triple(FossilTile(0, 1), 0.035, 284),
        Triple(FossilTile(0, 2), 0.040, 274),
        Triple(FossilTile(0, 3), 0.038, 263),
        Triple(FossilTile(0, 4), 0.024, 253),
        Triple(FossilTile(1, 5), 0.036, 247),
        Triple(FossilTile(2, 0), 0.050, 238),
        Triple(FossilTile(1, 1), 0.044, 226),
        Triple(FossilTile(3, 0), 0.051, 216),
        Triple(FossilTile(4, 0), 0.049, 205),
        Triple(FossilTile(5, 0), 0.026, 195),
        Triple(FossilTile(6, 1), 0.053, 190),
        Triple(FossilTile(1, 2), 0.061, 180),
        Triple(FossilTile(2, 1), 0.059, 169),
        Triple(FossilTile(1, 3), 0.063, 159),
        Triple(FossilTile(1, 4), 0.040, 149),
        Triple(FossilTile(2, 5), 0.063, 143),
        Triple(FossilTile(3, 1), 0.082, 134),
        Triple(FossilTile(2, 2), 0.073, 123),
        Triple(FossilTile(4, 1), 0.088, 114),
        Triple(FossilTile(5, 1), 0.048, 104),
        Triple(FossilTile(6, 2), 0.091, 99),
        Triple(FossilTile(2, 3), 0.111, 90),
        Triple(FossilTile(2, 4), 0.075, 80),
        Triple(FossilTile(3, 5), 0.108, 74),
        Triple(FossilTile(3, 2), 0.121, 66),
        Triple(FossilTile(4, 2), 0.172, 58),
        Triple(FossilTile(5, 2), 0.104, 48),
        Triple(FossilTile(3, 3), 0.186, 43),
        Triple(FossilTile(3, 4), 0.171, 35),
        Triple(FossilTile(6, 3), 0.241, 29),
        Triple(FossilTile(6, 4), 0.227, 22),
        Triple(FossilTile(4, 3), 0.412, 17),
        Triple(FossilTile(5, 3), 0.200, 10),
        Triple(FossilTile(4, 4), 0.750, 8),
        Triple(FossilTile(5, 5), 0.500, 2),
        Triple(FossilTile(5, 4), 1.000, 1)
    )

    private fun getCurrentSequence(): Set<Triple<FossilTile, Double, Int>> {
        return when (config.mode) {
            SolverMode.FOSSIL -> if (FossilSolverDisplay.maxCharges < 18) riskyStartingSequence else safeStartingSequence
            SolverMode.AVOID -> worstStartingSequence
        }
    }

    private fun isPositionInStartSequence(position: FossilTile): Boolean {
        return getCurrentSequence().any { it.first == position }
    }

    fun getChosenPosition(possibleClickPositions: Map<FossilTile, Int>): Map.Entry<FossilTile, Int>? {
        return when (config.mode) {
            SolverMode.FOSSIL -> possibleClickPositions.maxByOrNull { it.value }
            SolverMode.AVOID -> possibleClickPositions.minByOrNull { it.value }
        }
    }

    private val solvingMutex = Mutex()

    suspend fun findTile(fossilLocations: Set<Int>, dirtLocations: Set<Int>, percentage: String?): Unit = solvingMutex.withLock {
        val invalidPositions: MutableSet<FossilTile> = mutableSetOf()
        for (i in 0..53) {
            if (i !in fossilLocations && i !in dirtLocations) {
                invalidPositions.add(FossilTile(i))
            }
        }
        val foundPositions = fossilLocations.map { FossilTile(it) }.toSet()

        val currentSeq = getCurrentSequence()
        val needsMoveSequence = foundPositions.isEmpty() && invalidPositions.all { isPositionInStartSequence(it) }

        if (needsMoveSequence) {
            currentSnapshot = SolverSnapshot()

            val movesTaken = invalidPositions.size
            if (movesTaken >= currentSeq.size) {
                return FossilSolverDisplay.showError()
            }

            val nextMove = currentSeq.elementAt(movesTaken)
            FossilSolverDisplay.nextData(nextMove.first, nextMove.second, nextMove.third)
            return
        }

        val possibleClickPositions: MutableMap<FossilTile, Int> = mutableMapOf()
        var totalPossibleTiles = 0

        val possibleFossilTypes = if (percentage == null) FossilType.entries else {
            val possibleFossils = FossilType.getByPercentage(percentage)
            FossilSolverDisplay.possibleFossilTypes = possibleFossils.toSet()
            possibleFossils
        }

        for (x in 0..8) {
            for (y in 0..5) {
                for (fossil in possibleFossilTypes) {
                    for (mutation in fossil.possibleMutations) {
                        val newPosition = mutation.modification(fossil.fossilShape).moveTo(x, y)
                        if (!isValidFossilPosition(newPosition, invalidPositions, foundPositions)) {
                            continue
                        }

                        totalPossibleTiles++
                        for (position in newPosition.tiles) {
                            possibleClickPositions.compute(position) { _, v -> v?.plus(1) ?: 1 }
                        }
                    }
                }
            }
        }

        possibleClickPositions
            .filter { it.key in foundPositions }.keys
            .forEach { possibleClickPositions.remove(it) }

        currentSnapshot = SolverSnapshot(
            clickablePositions = possibleClickPositions,
            totalPossibleFossils = totalPossibleTiles
        )

        val chosenPosition = getChosenPosition(possibleClickPositions) ?: run {
            return if (fossilLocations.isNotEmpty()) {
                FossilSolverDisplay.showCompleted()
            } else FossilSolverDisplay.showError()
        }

        val nextMove = chosenPosition.key
        val correctPercentage = chosenPosition.value / totalPossibleTiles.toDouble()
        FossilSolverDisplay.nextData(nextMove, correctPercentage, totalPossibleTiles)
    }

    private fun isValidFossilPosition(
        fossil: FossilShape,
        invalidPositions: Set<FossilTile>,
        foundPositions: Set<FossilTile>,
    ): Boolean {
        if (fossil.tiles.any { !isValidPosition(it, invalidPositions) }) {
            return false
        }

        for (pos in foundPositions) {
            if (!fossil.tiles.contains(pos)) {
                return false
            }
        }
        return true
    }

    private fun isValidPosition(fossil: FossilTile, invalidPositions: Set<FossilTile>): Boolean {
        if (fossil in invalidPositions) return false
        return fossil.x >= 0 && fossil.y >= 0 && fossil.x < 9 && fossil.y < 6
    }
}
