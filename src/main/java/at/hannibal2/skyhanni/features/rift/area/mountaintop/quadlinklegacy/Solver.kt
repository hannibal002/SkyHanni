package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

import java.util.concurrent.atomic.AtomicReference
import java.util.function.BooleanSupplier
import kotlin.math.max
import kotlin.math.min

/**
 * This class can be reused to score multiple positions. It is *not* thread-safe.
 */
internal class Solver {
    private val statistics = Statistics()
    private val tt: TranspositionTable = TranspositionTable(statistics)

    private val moveLists: Array<MoveList> = Array(Position.TOTAL_SLOTS) { MoveList() }

    fun recommendMovesUntil(
        boardSnapshot: String,
        pieceCount: Int,
        response: AtomicReference<QuadLinkLegacySolver.QLLResponse?>,
        shouldStop: BooleanSupplier,
    ) {
        val p: Position = Position.fromBoardSnapshot(boardSnapshot, pieceCount)
        if (p.numMoves <= 1 || p.onlyMiddlePlayed()) {
            response.set(QuadLinkLegacySolver.QLLResponse(pieceCount, 3))
        }
        if (p.hasWinningMove()) {
            val winningMove = p.anyWinningMove
            response.set(QuadLinkLegacySolver.QLLResponse(pieceCount, winningMove))
            return
        }

        var bestScore: Int = SCORE_MIN
        var bestCol = -1

        for (i in COL_SEARCH_ORDER) {
            if (!p.canPlayMove(i)) {
                continue
            }
            p.playMove(i)
            val score = -mtdf(p, SCORE_MIN, -bestScore, shouldStop)
            p.undoMove(i)
            if (score == STOP_SEARCH) {
                return
            }
            if (p.numMoves < 8 && score > 0) { // can play any winning move in the first 4 turns

                response.set(QuadLinkLegacySolver.QLLResponse(pieceCount, i))
            }
            if (bestCol == -1 || score > bestScore) {
                bestScore = score
                bestCol = i
                if (score > 0) {
                    response.set(QuadLinkLegacySolver.QLLResponse(pieceCount, bestCol))
                }
            }
        }

        if (bestCol != -1) {
            response.set(QuadLinkLegacySolver.QLLResponse(pieceCount, bestCol))
        }
    }

    private fun mtdf(p: Position, min: Int, max: Int, shouldStop: BooleanSupplier): Int {
        if (shouldStop.asBoolean) {
            return STOP_SEARCH
        }
        if (p.hasWinningMove()) {
            return p.emptySlotsCount
        }

        var g = max
        var upper = max
        var lower = min

        while (lower < upper) {
            val beta = max(g, lower + 1)
            g = negamax(p, beta - 1, beta, shouldStop)
            if (g == STOP_SEARCH) {
                return STOP_SEARCH
            }

            if (g < beta) {
                upper = g
            } else {
                lower = g
            }
        }
        return g
    }

    /**
     * @param p a non-terminal position with no winning moves
     */
    private fun negamax(p: Position, alphaP: Int, betaP: Int, shouldStop: BooleanSupplier): Int {

        var alpha = alphaP
        var beta = betaP
        if (shouldStop.asBoolean) {
            return STOP_SEARCH
        }

        statistics.incrementExploredNodes()

        if (p.emptySlotsCount == 0) {
            return 0
        }

        val scoreIfAnyMoveLoses = -(p.emptySlotsCount - 1)

        val possibleMoves = p.possibleMoves
        val opponentThreats = p.opponentThreats

        val forcedMoves = possibleMoves and opponentThreats
        if (BitboardUtils.hasTwoOrMore(forcedMoves)) {
            return scoreIfAnyMoveLoses
        }

        val losingSlots = (opponentThreats and BitboardUtils.PLAYABLE_SPACE) ushr 1
        var nonLosingPossibleMoves = possibleMoves and losingSlots.inv()
        if (forcedMoves != BitboardUtils.EMPTY) {
            nonLosingPossibleMoves = nonLosingPossibleMoves and forcedMoves
        }

        if (nonLosingPossibleMoves == BitboardUtils.EMPTY) {
            return scoreIfAnyMoveLoses
        }

        beta = min(beta, max(0, p.emptySlotsCount - 2))
        beta = min(beta, tt.getValueOrDefault(p.key(), SCORE_MAX))
        alpha = max(alpha, min(0, scoreIfAnyMoveLoses + 2))

        if (alpha >= beta) {
            return beta
        }

        var bestScore = alpha

        val moveList = moveLists[p.numMoves]
        moveList.calculateMoveOrder(p, nonLosingPossibleMoves)

        for (i in 0..<moveList.size()) {
            val candidateSlot = moveList.get(i)
            if (candidateSlot != BitboardUtils.EMPTY) {
                p.playMoveInSlot(candidateSlot)
                val childScore = negamax(p, -beta, -bestScore, shouldStop)
                p.undoMoveInSlot(candidateSlot)
                if (childScore == STOP_SEARCH) {
                    return STOP_SEARCH
                }
                val candidateScore = -childScore

                if (candidateScore >= beta) {
                    return candidateScore
                }

                bestScore = max(bestScore, candidateScore)
            }
        }

        tt.set(p.key(), bestScore, p.numMoves)
        return bestScore
    }

    companion object {
        const val SCORE_MIN: Int = -Position.TOTAL_SLOTS + 7
        const val SCORE_MAX: Int = Position.TOTAL_SLOTS - 6
        private const val STOP_SEARCH = Int.MIN_VALUE

        private val COL_SEARCH_ORDER = intArrayOf(3, 2, 4, 1, 5, 0, 6)
    }
}
