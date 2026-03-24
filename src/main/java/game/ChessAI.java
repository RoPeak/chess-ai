package game;

import java.util.List;
import pieces.*;

/**
 * Minimax AI with alpha-beta pruning.
 * The AI always plays BLACK (minimises the evaluation score).
 * Uses undo-based simulation: the live board is temporarily mutated and restored,
 * so no object copying is needed.
 */
public class ChessAI {

    private final Gameplay gameplay;
    private final int depth;

    public ChessAI(Gameplay gameplay, int depth) {
        this.gameplay = gameplay;
        this.depth = depth;
    }

    /**
     * Returns the best move for BLACK as {fromRow, fromCol, toRow, toCol},
     * or null if BLACK has no legal moves.
     */
    public int[] getBestMove() {
        List<int[]> moves = gameplay.getAllLegalMoves(PieceColour.BLACK);
        if (moves.isEmpty()) return null;

        sortMoves(moves);

        int bestScore = Integer.MAX_VALUE;
        int[] bestMove = moves.get(0);
        int alpha = Integer.MIN_VALUE;
        int beta  = Integer.MAX_VALUE;

        for (int[] move : moves) {
            SearchState state = applySearchMove(move);
            // After BLACK's move, WHITE (maximising) responds
            int score = minimax(depth - 1, alpha, beta, true);
            undoSearchMove(move, state);

            if (score < bestScore) {
                bestScore = score;
                bestMove  = move;
            }
            beta = Math.min(beta, score);
        }
        return bestMove;
    }

    private int minimax(int depth, int alpha, int beta, boolean maximising) {
        if (depth == 0) {
            return BoardEvaluator.evaluate(gameplay.getBoard().getBoard());
        }

        PieceColour colour = maximising ? PieceColour.WHITE : PieceColour.BLACK;
        List<int[]> moves  = gameplay.getAllLegalMoves(colour);

        if (moves.isEmpty()) {
            // No legal moves: checkmate or stalemate
            if (gameplay.isInCheck(colour)) {
                return maximising ? -1_000_000 : 1_000_000;
            }
            return 0; // Stalemate
        }

        sortMoves(moves);

        if (maximising) {
            int maxEval = Integer.MIN_VALUE;
            for (int[] move : moves) {
                SearchState state = applySearchMove(move);
                int eval = minimax(depth - 1, alpha, beta, false);
                undoSearchMove(move, state);
                maxEval = Math.max(maxEval, eval);
                alpha   = Math.max(alpha, eval);
                if (beta <= alpha) break; // Beta cut-off
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int[] move : moves) {
                SearchState state = applySearchMove(move);
                int eval = minimax(depth - 1, alpha, beta, true);
                undoSearchMove(move, state);
                minEval = Math.min(minEval, eval);
                beta    = Math.min(beta, eval);
                if (beta <= alpha) break; // Alpha cut-off
            }
            return minEval;
        }
    }

    // Sort moves so captures are examined first, improving alpha-beta pruning
    private void sortMoves(List<int[]> moves) {
        moves.sort((a, b) -> {
            boolean aCapture = gameplay.getBoard().getPiece(a[2], a[3]) != null;
            boolean bCapture = gameplay.getBoard().getPiece(b[2], b[3]) != null;
            return Boolean.compare(bCapture, aCapture);
        });
    }

    // -------------------------------------------------------------------------
    // Undo-based simulation helpers
    // -------------------------------------------------------------------------

    /** All mutable state that must be saved before applying a search move. */
    private static class SearchState {
        // Piece sitting at the destination before the move (normal capture, may be null)
        final Piece capturedAtDest;
        // En passant target before this move
        final PiecePosition savedEnPassant;
        // En passant special: the pawn captured by en passant
        int   epCapturedRow = -1, epCapturedCol = -1;
        Piece epCapturedPawn = null;
        // Castling: rook columns (>= 0 means castling happened)
        int rookFromCol = -1, rookToCol = -1;
        // hasMoved flags for King / Rook
        boolean movingHadMoved   = false;
        boolean castleRookHadMoved = false;
        // Promotion: original pawn object (non-null means promotion happened)
        Piece promotedFromPawn = null;

        SearchState(Piece capturedAtDest, PiecePosition savedEnPassant) {
            this.capturedAtDest  = capturedAtDest;
            this.savedEnPassant  = savedEnPassant;
        }
    }

    private SearchState applySearchMove(int[] move) {
        int fromRow = move[0], fromCol = move[1], toRow = move[2], toCol = move[3];
        Board board = gameplay.getBoard();
        Piece movingPiece = board.getPiece(fromRow, fromCol);

        SearchState state = new SearchState(board.getPiece(toRow, toCol), gameplay.getEnPassantTarget());

        // Save hasMoved for King / Rook
        if      (movingPiece instanceof King) state.movingHadMoved = ((King) movingPiece).hasMoved();
        else if (movingPiece instanceof Rook) state.movingHadMoved = ((Rook) movingPiece).hasMoved();

        // En passant capture: remove the captured pawn from its square
        PiecePosition ep = gameplay.getEnPassantTarget();
        if (movingPiece instanceof Pawn && ep != null && toRow == ep.getRow() && toCol == ep.getCol()) {
            state.epCapturedRow  = fromRow;
            state.epCapturedCol  = toCol;
            state.epCapturedPawn = board.getPiece(fromRow, toCol);
            board.setPiece(fromRow, toCol, null);
        }

        // Castling: move the rook to its new square
        if (movingPiece instanceof King && Math.abs(toCol - fromCol) == 2) {
            state.rookFromCol = toCol > fromCol ? 7 : 0;
            state.rookToCol   = toCol > fromCol ? 5 : 3;
            Rook rook = (Rook) board.getPiece(fromRow, state.rookFromCol);
            state.castleRookHadMoved = rook.hasMoved();
            board.simulateMove(fromRow, state.rookFromCol, fromRow, state.rookToCol);
            rook.markMoved();
        }

        // Mark the moving piece as having moved
        if      (movingPiece instanceof King) ((King) movingPiece).markMoved();
        else if (movingPiece instanceof Rook) ((Rook) movingPiece).markMoved();

        // Move the piece to its destination
        board.simulateMove(fromRow, fromCol, toRow, toCol);

        // Update en passant target for the next ply
        gameplay.setEnPassantTarget(null);
        if (movingPiece instanceof Pawn && Math.abs(toRow - fromRow) == 2) {
            gameplay.setEnPassantTarget(new PiecePosition((fromRow + toRow) / 2, fromCol));
        }

        // Pawn promotion — AI always promotes to Queen
        if (movingPiece instanceof Pawn) {
            boolean promotes = (movingPiece.getColour() == PieceColour.WHITE && toRow == 0)
                            || (movingPiece.getColour() == PieceColour.BLACK && toRow == 7);
            if (promotes) {
                state.promotedFromPawn = movingPiece;
                board.setPiece(toRow, toCol, new Queen(movingPiece.getColour(), new PiecePosition(toRow, toCol)));
            }
        }

        return state;
    }

    private void undoSearchMove(int[] move, SearchState state) {
        int fromRow = move[0], fromCol = move[1], toRow = move[2], toCol = move[3];
        Board board = gameplay.getBoard();

        // Restore en passant target
        gameplay.setEnPassantTarget(state.savedEnPassant);

        // If promotion happened, put the original pawn back at the destination
        // so that undoSimulatedMove moves it back to fromRow/fromCol correctly
        if (state.promotedFromPawn != null) {
            board.setPiece(toRow, toCol, state.promotedFromPawn);
        }

        // Undo the main piece move
        board.undoSimulatedMove(fromRow, fromCol, toRow, toCol, state.capturedAtDest);

        // Restore hasMoved for the moving piece (now back at fromRow/fromCol)
        Piece movedPiece = board.getPiece(fromRow, fromCol);
        if      (movedPiece instanceof King) ((King) movedPiece).setHasMoved(state.movingHadMoved);
        else if (movedPiece instanceof Rook) ((Rook) movedPiece).setHasMoved(state.movingHadMoved);

        // Undo the castling rook move
        if (state.rookFromCol >= 0) {
            Rook rook = (Rook) board.getPiece(fromRow, state.rookToCol);
            board.undoSimulatedMove(fromRow, state.rookFromCol, fromRow, state.rookToCol, null);
            rook.setHasMoved(state.castleRookHadMoved);
        }

        // Restore en passant captured pawn
        if (state.epCapturedPawn != null) {
            board.setPiece(state.epCapturedRow, state.epCapturedCol, state.epCapturedPawn);
        }
    }
}
