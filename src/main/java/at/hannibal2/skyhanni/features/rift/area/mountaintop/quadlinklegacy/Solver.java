package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * This class can be reused to score multiple positions. It is <i>not</i> thread-safe.
 */
public class Solver {

    public static final int SCORE_MIN = -Position.TOTAL_SLOTS + 7;
    public static final int SCORE_MAX = Position.TOTAL_SLOTS - 6;
    private static final int STOP_SEARCH = Integer.MIN_VALUE;

    private static final int[] COL_SEARCH_ORDER = new int[]{ 3, 2, 4, 1, 5, 0, 6};

    private final Statistics statistics = new Statistics();
    TranspositionTable tt = new TranspositionTable(statistics);

    MoveList[] moveLists = Stream.generate(MoveList::new).limit(Position.TOTAL_SLOTS).toArray(MoveList[] ::new);

    public void recommendMovesUntil(String boardSnapshot, int pieceCount, AtomicReference<QuadLinkLegacySolver.QLLResponse> response, BooleanSupplier shouldStop) {

        Position p = Position.fromBoardSnapshot(boardSnapshot, pieceCount);
        if (p.getNumMoves() <= 1 || p.onlyMiddlePlayed()) {

            response.set(new QuadLinkLegacySolver.QLLResponse(pieceCount, 3));
        }
        if (p.hasWinningMove()) {

            int winningMove = p.getAnyWinningMove();
            response.set(new QuadLinkLegacySolver.QLLResponse(pieceCount, winningMove));
            return;
        }

        int bestScore = SCORE_MIN;
        int bestCol = -1;

        for (int i : COL_SEARCH_ORDER) {

            if (!p.canPlayMove(i)) {

                continue;
            }
            p.playMove(i);
            int score = -mtdf(p, SCORE_MIN, -bestScore, shouldStop);
            p.undoMove(i);
            if (score == STOP_SEARCH) {

                return;
            }
            if (p.getNumMoves() < 8 && score > 0) { // can play any winning move in the first 4 turns

                response.set(new QuadLinkLegacySolver.QLLResponse(pieceCount, i));
            }
            if (bestCol == -1 || score > bestScore) {

                bestScore = score;
                bestCol = i;
                if (score > 0) {

                    response.set(new QuadLinkLegacySolver.QLLResponse(pieceCount, bestCol));
                }
            }
        }

        if (bestCol != -1) {
            response.set(new QuadLinkLegacySolver.QLLResponse(pieceCount, bestCol));
        }
    }

    private int mtdf(Position p, int min, int max, BooleanSupplier shouldStop) {

        if (shouldStop.getAsBoolean()) {

            return STOP_SEARCH;
        }
        if (p.hasWinningMove()) {

            return p.getEmptySlotsCount();
        }

        int g = max;
        int upper = max;
        int lower = min;

        while (lower < upper) {

            int beta = Math.max(g, lower + 1);
            g = negamax(p, beta - 1, beta, shouldStop);
            if (g == STOP_SEARCH) {

                return STOP_SEARCH;
            }

            if (g < beta) {

                upper = g;
            }
            else {

                lower = g;
            }
        }
        return g;
    }

    /**
     * @param p a non-terminal position with no winning moves
     */
    private int negamax(Position p, int alpha, int beta, BooleanSupplier shouldStop) {

        if (shouldStop.getAsBoolean()) {

            return STOP_SEARCH;
        }

        statistics.incrementExploredNodes();

        if (p.getEmptySlotsCount() == 0) {

            return 0;
        }

        final int scoreIfAnyMoveLoses = -(p.getEmptySlotsCount() - 1);

        long possibleMoves = p.getPossibleMoves();
        long opponentThreats = p.getOpponentThreats();

        long forcedMoves = possibleMoves & opponentThreats;
        if (BitboardUtils.hasTwoOrMore(forcedMoves)) {

            return scoreIfAnyMoveLoses;
        }

        long losingSlots = (opponentThreats & BitboardUtils.PLAYABLE_SPACE) >>> 1;
        long nonLosingPossibleMoves = possibleMoves & ~losingSlots;
        if (forcedMoves != BitboardUtils.EMPTY) {

            nonLosingPossibleMoves &= forcedMoves;
        }

        if (nonLosingPossibleMoves == BitboardUtils.EMPTY) {

            return scoreIfAnyMoveLoses;
        }

        beta = Math.min(beta, Math.max(0, p.getEmptySlotsCount() - 2));
        beta = Math.min(beta, tt.getValueOrDefault(p.key(), SCORE_MAX));
        alpha = Math.max(alpha, Math.min(0, scoreIfAnyMoveLoses + 2));

        if (alpha >= beta) {
            return beta;
        }

        int bestScore = alpha;

        MoveList moveList = moveLists[p.getNumMoves()];
        moveList.calculateMoveOrder(p, nonLosingPossibleMoves);

        for (int i = 0; i < moveList.size(); ++i) {

            long candidateSlot = moveList.get(i);
            if (candidateSlot != BitboardUtils.EMPTY) {

                p.playMoveInSlot(candidateSlot);
                int childScore = negamax(p, -beta, -bestScore, shouldStop);
                p.undoMoveInSlot(candidateSlot);
                if (childScore == STOP_SEARCH) {

                    return STOP_SEARCH;
                }
                int candidateScore = -childScore;

                if (candidateScore >= beta) {
                    return candidateScore;
                }

                bestScore = Math.max(bestScore, candidateScore);
            }
        }

        tt.set(p.key(), bestScore, p.getNumMoves());
        return bestScore;
    }
}
