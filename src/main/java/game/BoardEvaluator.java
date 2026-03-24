package game;

import pieces.*;

/**
 * Stateless board evaluation function.
 * Returns a score from WHITE's perspective: positive = good for white, negative = good for black.
 * Uses material values (centipawns) plus piece-square table (PST) bonuses.
 * PST tables are defined from white's perspective where row 0 = rank 8, row 7 = rank 1.
 * For black pieces the row index is mirrored: pstRow = 7 - boardRow.
 */
public class BoardEvaluator {

    // Material values in centipawns
    private static final int PAWN_VALUE   = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE   = 500;
    private static final int QUEEN_VALUE  = 900;
    private static final int KING_VALUE   = 20_000;

    // Piece-square tables — Tomasz Michniewski's simplified evaluation function
    private static final int[][] PAWN_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        { 50, 50, 50, 50, 50, 50, 50, 50 },
        { 10, 10, 20, 30, 30, 20, 10, 10 },
        {  5,  5, 10, 25, 25, 10,  5,  5 },
        {  0,  0,  0, 20, 20,  0,  0,  0 },
        {  5, -5,-10,  0,  0,-10, -5,  5 },
        {  5, 10, 10,-20,-20, 10, 10,  5 },
        {  0,  0,  0,  0,  0,  0,  0,  0 }
    };

    private static final int[][] KNIGHT_TABLE = {
        { -50,-40,-30,-30,-30,-30,-40,-50 },
        { -40,-20,  0,  0,  0,  0,-20,-40 },
        { -30,  0, 10, 15, 15, 10,  0,-30 },
        { -30,  5, 15, 20, 20, 15,  5,-30 },
        { -30,  0, 15, 20, 20, 15,  0,-30 },
        { -30,  5, 10, 15, 15, 10,  5,-30 },
        { -40,-20,  0,  5,  5,  0,-20,-40 },
        { -50,-40,-30,-30,-30,-30,-40,-50 }
    };

    private static final int[][] BISHOP_TABLE = {
        { -20,-10,-10,-10,-10,-10,-10,-20 },
        { -10,  0,  0,  0,  0,  0,  0,-10 },
        { -10,  0,  5, 10, 10,  5,  0,-10 },
        { -10,  5,  5, 10, 10,  5,  5,-10 },
        { -10,  0, 10, 10, 10, 10,  0,-10 },
        { -10, 10, 10, 10, 10, 10, 10,-10 },
        { -10,  5,  0,  0,  0,  0,  5,-10 },
        { -20,-10,-10,-10,-10,-10,-10,-20 }
    };

    private static final int[][] ROOK_TABLE = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        {  5, 10, 10, 10, 10, 10, 10,  5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        {  0,  0,  0,  5,  5,  0,  0,  0 }
    };

    private static final int[][] QUEEN_TABLE = {
        { -20,-10,-10, -5, -5,-10,-10,-20 },
        { -10,  0,  0,  0,  0,  0,  0,-10 },
        { -10,  0,  5,  5,  5,  5,  0,-10 },
        {  -5,  0,  5,  5,  5,  5,  0, -5 },
        {   0,  0,  5,  5,  5,  5,  0, -5 },
        { -10,  5,  5,  5,  5,  5,  0,-10 },
        { -10,  0,  5,  0,  0,  0,  0,-10 },
        { -20,-10,-10, -5, -5,-10,-10,-20 }
    };

    private static final int[][] KING_TABLE = {
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -30,-40,-40,-50,-50,-40,-40,-30 },
        { -20,-30,-30,-40,-40,-30,-30,-20 },
        { -10,-20,-20,-20,-20,-20,-20,-10 },
        {  20, 20,  0,  0,  0,  0, 20, 20 },
        {  20, 30, 10,  0,  0, 10, 30, 20 }
    };

    public static int evaluate(Piece[][] board) {
        int score = 0;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board[row][col];
                if (piece == null) continue;

                int material = getMaterialValue(piece);
                int pst      = getPSTValue(piece, row, col);

                if (piece.getColour() == PieceColour.WHITE) {
                    score += material + pst;
                } else {
                    score -= material + pst;
                }
            }
        }
        return score;
    }

    private static int getMaterialValue(Piece piece) {
        return switch (piece.getClass().getSimpleName()) {
            case "Pawn"   -> PAWN_VALUE;
            case "Knight" -> KNIGHT_VALUE;
            case "Bishop" -> BISHOP_VALUE;
            case "Rook"   -> ROOK_VALUE;
            case "Queen"  -> QUEEN_VALUE;
            case "King"   -> KING_VALUE;
            default       -> 0;
        };
    }

    private static int getPSTValue(Piece piece, int row, int col) {
        // Black pieces mirror white's table vertically
        int pstRow = (piece.getColour() == PieceColour.WHITE) ? row : 7 - row;
        return switch (piece.getClass().getSimpleName()) {
            case "Pawn"   -> PAWN_TABLE[pstRow][col];
            case "Knight" -> KNIGHT_TABLE[pstRow][col];
            case "Bishop" -> BISHOP_TABLE[pstRow][col];
            case "Rook"   -> ROOK_TABLE[pstRow][col];
            case "Queen"  -> QUEEN_TABLE[pstRow][col];
            case "King"   -> KING_TABLE[pstRow][col];
            default       -> 0;
        };
    }
}
