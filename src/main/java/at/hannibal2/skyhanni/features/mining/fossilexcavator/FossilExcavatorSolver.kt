package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.utils.ChatUtils

object FossilExcavatorSolver {
    private val startingSequence: Set<Pair<FossilTile, Double>> = setOf(
        Pair(FossilTile(4, 2), 0.515),
        Pair(FossilTile(5, 4), 0.413),
        Pair(FossilTile(3, 3), 0.461),
        Pair(FossilTile(5, 2), 0.387),
        Pair(FossilTile(3, 1), 0.342),
        Pair(FossilTile(7, 3), 0.48),
        Pair(FossilTile(1, 2), 0.846),
        Pair(FossilTile(3, 4), 1.0),
    )

    private fun isPositionInStartSequence(position: FossilTile): Boolean {
        return startingSequence.any { it.first == position }
    }

    fun findBestTile(fossilLocations: Set<Int>, dirtLocations: Set<Int>, percentage: String?) {
        val invalidPositions: MutableSet<FossilTile> = mutableSetOf()
        for (i in 0..53) {
            if (i !in fossilLocations && i !in dirtLocations) {
                invalidPositions.add(FossilTile(i))
            }
        }
        val foundPositions = fossilLocations.map { FossilTile(it) }.toSet()

        val needsMoveSequence = foundPositions.isEmpty() && invalidPositions.all { isPositionInStartSequence(it) }

        if (needsMoveSequence) {
            val movesTaken = invalidPositions.size
            if (movesTaken >= startingSequence.size) {
                FossilExcavator.showError()
                return
            }

            val nextMove = startingSequence.elementAt(movesTaken)
            FossilExcavator.nextData(nextMove.first, nextMove.second)
            return
        }

        // todo remove
        ChatUtils.chat("dirt: ${dirtLocations.size}, fossils: ${fossilLocations.size}, invalid: ${invalidPositions.size}")

        val possibleClickPositions: MutableMap<FossilTile, Int> = mutableMapOf()
        var totalPossibleTiles = 0.0

        val possibleFossilTypes = if (percentage == null) FossilType.entries else FossilType.getByPercentage(percentage)

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

        possibleClickPositions.filter { it.key in foundPositions }.forEach {
            possibleClickPositions.remove(it.key)
        }

        val bestPosition = possibleClickPositions.maxByOrNull { it.value } ?: run {
            FossilExcavator.showError()
            return
        }

        val nextMove = bestPosition.key
        val correctPercentage = bestPosition.value / totalPossibleTiles
        FossilExcavator.nextData(nextMove, correctPercentage)
    }

    private fun isValidFossilPosition(
        fossil: FossilShape,
        invalidPositions: Set<FossilTile>,
        foundPositions: Set<FossilTile>
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
