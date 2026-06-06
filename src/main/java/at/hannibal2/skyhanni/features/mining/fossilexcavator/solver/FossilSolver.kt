package at.hannibal2.skyhanni.features.mining.fossilexcavator.solver

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FossilSolver {
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
    private val worstStartingSequence: Set<Triple<FossilTile, Double, Int>> = setOf(
        Triple(FossilTile(0, 0), 0.023, 174),
        Triple(FossilTile(0, 1), 0.024, 170),
        Triple(FossilTile(0, 5), 0.024, 166),
        Triple(FossilTile(0, 4), 0.025, 162),
        Triple(FossilTile(0, 2), 0.025, 158),
        Triple(FossilTile(0, 3), 0.019, 154),
        Triple(FossilTile(1, 0), 0.026, 151),
        Triple(FossilTile(1, 1), 0.027, 147),
    )

    private fun getCurrentSequence(): Set<Triple<FossilTile, Double, Int>> =
        if (FossilSolverDisplay.maxCharges < 18) riskyStartingSequence else safeStartingSequence

    private fun isPositionInStartSequence(position: FossilTile): Boolean {
        return getCurrentSequence().any { it.first == position }
    }

    private val solvingMutex = Mutex()

    suspend fun findBestTile(
        fossilLocations: Set<Int>,
        dirtLocations: Set<Int>,
        percentage: String?,
    ) {
        solve(fossilLocations, dirtLocations, percentage, isWorst = false)
    }

    suspend fun findWorstTile(fossilLocations: Set<Int>, dirtLocations: Set<Int>, percentage: String?) =
        solve(fossilLocations, dirtLocations, percentage, isWorst = true)

    private suspend fun solve(
        fossilLocations: Set<Int>,
        dirtLocations: Set<Int>,
        percentage: String?,
        isWorst: Boolean
    ) = solvingMutex.withLock {
        val invalidPositions: MutableSet<FossilTile> = mutableSetOf()
        for (i in 0..53) {
            if (i !in fossilLocations && i !in dirtLocations) {
                invalidPositions.add(FossilTile(i))
            }
        }
        val foundPositions = fossilLocations.map { FossilTile(it) }.toSet()

        val currentSeq = if (isWorst) worstStartingSequence else getCurrentSequence()
        val needsMoveSequence = foundPositions.isEmpty() && invalidPositions.all { pos -> currentSeq.any { it.first == pos } }

        if (needsMoveSequence) {
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

        if (!isWorst) {
            possibleClickPositions
                .filter { it.key in foundPositions }.keys
                .forEach { possibleClickPositions.remove(it) }

            val bestPosition = possibleClickPositions.maxByOrNull { it.value } ?: run {
                return if (fossilLocations.isNotEmpty()) {
                    FossilSolverDisplay.showCompleted()
                } else FossilSolverDisplay.showError()
            }

            val nextMove = bestPosition.key
            val correctPercentage = bestPosition.value / totalPossibleTiles.toDouble()
            FossilSolverDisplay.nextData(nextMove, correctPercentage, totalPossibleTiles)
        } else {
            val remainingTiles = (0..53).map { FossilTile(it) }
                .filter { it !in invalidPositions && it !in foundPositions }

            if (remainingTiles.isEmpty()) {
                return if (fossilLocations.isNotEmpty()) {
                    FossilSolverDisplay.showCompleted()
                } else FossilSolverDisplay.showError()
            }

            val worstPosition = remainingTiles.minByOrNull { possibleClickPositions[it] ?: 0 } ?: run {
                return FossilSolverDisplay.showError()
            }

            val nextMove = worstPosition
            val occurrences = possibleClickPositions[worstPosition] ?: 0
            val correctPercentage = occurrences / totalPossibleTiles.toDouble()
            FossilSolverDisplay.nextData(nextMove, correctPercentage, totalPossibleTiles)
        }
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
