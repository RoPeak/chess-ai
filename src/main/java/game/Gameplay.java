package game;

// Standard imports
import java.util.List;
import java.util.ArrayList;

// Custom imports
import pieces.*;

public class Gameplay {

    @FunctionalInterface
    public interface PromotionCallback {
        Piece choose(PieceColour colour, PiecePosition position);
    }

    private Board board;
    private boolean whiteTurn = true;
    private PiecePosition selectedPiecePosition;
    private PiecePosition enPassantTarget = null;
    private PromotionCallback promotionCallback = null;
    private boolean aiMode = false;
    private int aiDepth = 3;
    private ChessAI ai = null;

    public Gameplay() {
        this.board = new Board();
    }

    public Board getBoard() {
        return this.board;
    }

    public void setPromotionCallback(PromotionCallback callback) {
        this.promotionCallback = callback;
    }

    public PromotionCallback getPromotionCallback() {
        return this.promotionCallback;
    }

    public boolean makeMove(PiecePosition start, PiecePosition end) {
        Piece movingPiece = board.getPiece(start.getRow(), start.getCol());

        // Ensure piece exists and belongs to current player
        if (movingPiece == null || movingPiece.getColour() != getCurrentPlayerColour()) {
            return false;
        }

        // Validate against the legal move list (already filtered for check)
        List<PiecePosition> legal = getLegalMovesForPiece(start);
        if (!legal.contains(end)) {
            return false;
        }

        // En passant capture: remove the captured pawn before moving
        if (movingPiece instanceof Pawn && enPassantTarget != null && end.equals(enPassantTarget)) {
            board.setPiece(start.getRow(), end.getCol(), null);
        }

        // Castling: also move the rook
        if (movingPiece instanceof King) {
            int colDiff = end.getCol() - start.getCol();
            if (Math.abs(colDiff) == 2) {
                int rookFromCol = colDiff > 0 ? 7 : 0;
                int rookToCol   = colDiff > 0 ? 5 : 3;
                Rook rook = (Rook) board.getPiece(start.getRow(), rookFromCol);
                board.setPiece(start.getRow(), rookToCol, rook);
                board.setPiece(start.getRow(), rookFromCol, null);
                rook.markMoved();
            }
            ((King) movingPiece).markMoved();
        }

        if (movingPiece instanceof Rook) {
            ((Rook) movingPiece).markMoved();
        }

        // Track en passant target for next move
        PiecePosition nextEnPassantTarget = null;
        if (movingPiece instanceof Pawn) {
            int rowDiff = end.getRow() - start.getRow();
            if (Math.abs(rowDiff) == 2) {
                nextEnPassantTarget = new PiecePosition(start.getRow() + rowDiff / 2, start.getCol());
            }
        }

        // Perform the move
        board.setPiece(end.getRow(), end.getCol(), movingPiece);
        board.setPiece(start.getRow(), start.getCol(), null);
        enPassantTarget = nextEnPassantTarget;

        // Pawn promotion
        if (movingPiece instanceof Pawn) {
            boolean promotes = (movingPiece.getColour() == PieceColour.WHITE && end.getRow() == 0)
                            || (movingPiece.getColour() == PieceColour.BLACK && end.getRow() == 7);
            if (promotes) {
                Piece promoted = handlePromotion(end, movingPiece.getColour());
                board.setPiece(end.getRow(), end.getCol(), promoted);
            }
        }

        whiteTurn = !whiteTurn;
        return true;
    }

    private Piece handlePromotion(PiecePosition position, PieceColour colour) {
        if (promotionCallback != null) {
            return promotionCallback.choose(colour, position);
        }
        return new Queen(colour, position);
    }

    private PiecePosition findKingPosition(PieceColour kingColour) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece instanceof King && piece.getColour() == kingColour) {
                    return new PiecePosition(row, col);
                }
            }
        }
        throw new RuntimeException("King not on board");
    }

    private boolean isPositionOnBoard(PiecePosition position) {
        return position.getRow() >= 0 && position.getRow() < 8
            && position.getCol() >= 0 && position.getCol() < 8;
    }

    public boolean isInCheck(PieceColour kingColour) {
        // Iterate through each square, checking if that piece is checking the king
        PiecePosition kingPosition = findKingPosition(kingColour);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColour() != kingColour) {
                    if (piece.isValidMove(kingPosition, board.getBoard())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean wouldBeInCheckAfterMove(PieceColour colour, PiecePosition from, PiecePosition to) {
        // Simulate move
        Piece temp = board.getPiece(to.getRow(), to.getCol());
        board.setPiece(to.getRow(), to.getCol(), board.getPiece(from.getRow(), from.getCol()));
        board.setPiece(from.getRow(), from.getCol(), null);
        boolean inCheck = isInCheck(colour);

        // Undo move
        board.setPiece(from.getRow(), from.getCol(), board.getPiece(to.getRow(), to.getCol()));
        board.setPiece(to.getRow(), to.getCol(), temp);

        return inCheck;
    }

    private boolean wouldBeInCheckAfterEnPassant(PieceColour colour, PiecePosition from, PiecePosition to) {
        // Temporarily remove the captured pawn (same row as moving pawn, same col as landing square)
        int capturedRow = from.getRow();
        int capturedCol = to.getCol();
        Piece capturedPawn = board.getPiece(capturedRow, capturedCol);
        board.setPiece(capturedRow, capturedCol, null);
        boolean inCheck = wouldBeInCheckAfterMove(colour, from, to);
        board.setPiece(capturedRow, capturedCol, capturedPawn);
        return inCheck;
    }

    private boolean canCastle(PieceColour colour, boolean kingSide) {
        int row = (colour == PieceColour.WHITE) ? 7 : 0;
        Piece kingPiece = board.getPiece(row, 4);
        if (!(kingPiece instanceof King) || ((King) kingPiece).hasMoved()) return false;
        if (isInCheck(colour)) return false;

        if (kingSide) {
            Piece rook = board.getPiece(row, 7);
            if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;
            if (board.getPiece(row, 5) != null || board.getPiece(row, 6) != null) return false;
            PiecePosition kingPos = new PiecePosition(row, 4);
            if (wouldBeInCheckAfterMove(colour, kingPos, new PiecePosition(row, 5))) return false;
            if (wouldBeInCheckAfterMove(colour, kingPos, new PiecePosition(row, 6))) return false;
        } else {
            Piece rook = board.getPiece(row, 0);
            if (!(rook instanceof Rook) || ((Rook) rook).hasMoved()) return false;
            if (board.getPiece(row, 1) != null || board.getPiece(row, 2) != null || board.getPiece(row, 3) != null) return false;
            PiecePosition kingPos = new PiecePosition(row, 4);
            if (wouldBeInCheckAfterMove(colour, kingPos, new PiecePosition(row, 3))) return false;
            if (wouldBeInCheckAfterMove(colour, kingPos, new PiecePosition(row, 2))) return false;
        }
        return true;
    }

    public boolean isCheckmate(PieceColour colour) {
        return isInCheck(colour) && getAllLegalMoves(colour).isEmpty();
    }

    public boolean isStalemate(PieceColour colour) {
        return !isInCheck(colour) && getAllLegalMoves(colour).isEmpty();
    }

    public void setAiMode(boolean enabled, int depth) {
        this.aiMode  = enabled;
        this.aiDepth = depth;
        this.ai      = enabled ? new ChessAI(this, depth) : null;
    }

    public boolean isAiMode() { return aiMode; }
    public ChessAI getAI()    { return ai; }

    public void resetGame() {
        this.board = new Board();
        this.whiteTurn = true;
        this.enPassantTarget = null;
        this.selectedPiecePosition = null;
        // Rebuild AI instance so it works against the fresh board
        if (aiMode) ai = new ChessAI(this, aiDepth);
    }

    public PieceColour getCurrentPlayerColour() {
        return whiteTurn ? PieceColour.WHITE : PieceColour.BLACK;
    }

    public boolean isPieceSelected() {
        return selectedPiecePosition != null;
    }

    public boolean handleSquareSelection(int row, int col) {
        if (selectedPiecePosition == null) {
            Piece selectedPiece = board.getPiece(row, col);
            if (selectedPiece != null && selectedPiece.getColour() == getCurrentPlayerColour()) {
                selectedPiecePosition = new PiecePosition(row, col);
                return false;
            }
        } else {
            PiecePosition previousSelection = selectedPiecePosition;
            boolean moveMade = makeMove(selectedPiecePosition, new PiecePosition(row, col));
            selectedPiecePosition = null;

            if (!moveMade) {
                // If the user clicked a different friendly piece, re-select it rather than
                // leaving them with nothing selected (clicking the same square deselects)
                Piece clicked = board.getPiece(row, col);
                boolean isDifferentFriendly = clicked != null
                    && clicked.getColour() == getCurrentPlayerColour()
                    && !new PiecePosition(row, col).equals(previousSelection);
                if (isDifferentFriendly) {
                    selectedPiecePosition = new PiecePosition(row, col);
                }
            }
            return moveMade;
        }
        return false;
    }

    private void addLineMoves(PiecePosition position, int[][] directions, List<PiecePosition> legalMoves) {
        // Computes legal moves along straight and diagonal lines by iterating through specified directions
        // from the given position, until a blocking piece or the board edge is reached
        for (int[] d : directions) {
            PiecePosition newPos = new PiecePosition(position.getRow() + d[0], position.getCol() + d[1]);
            while (isPositionOnBoard(newPos)) {
                if (board.getPiece(newPos.getRow(), newPos.getCol()) == null) {
                    legalMoves.add(new PiecePosition(newPos.getRow(), newPos.getCol()));
                    newPos = new PiecePosition(newPos.getRow() + d[0], newPos.getCol() + d[1]);
                } else {
                    if (board.getPiece(newPos.getRow(), newPos.getCol()).getColour() != board.getPiece(position.getRow(), position.getCol()).getColour()) {
                        legalMoves.add(newPos);
                    }
                    break;
                }
            }
        }
    }

    public void addSingleMoves(PiecePosition position, int[][] moves, List<PiecePosition> legalMoves) {
        // Computes legal single-step moves, skipping squares off the board or occupied by a friendly piece
        for (int[] move : moves) {
            PiecePosition newPos = new PiecePosition(position.getRow() + move[0], position.getCol() + move[1]);
            if (isPositionOnBoard(newPos) && (board.getPiece(newPos.getRow(), newPos.getCol()) == null
                || board.getPiece(newPos.getRow(), newPos.getCol()).getColour() != board.getPiece(position.getRow(), position.getCol()).getColour())) {
                legalMoves.add(newPos);
            }
        }
    }

    public void addPawnMoves(PiecePosition position, PieceColour colour, List<PiecePosition> legalMoves) {
        int direction = colour == PieceColour.WHITE ? -1 : 1;

        // Single push
        PiecePosition singlePush = new PiecePosition(position.getRow() + direction, position.getCol());
        if (isPositionOnBoard(singlePush) && board.getPiece(singlePush.getRow(), singlePush.getCol()) == null) {
            legalMoves.add(singlePush);

            // Double push from starting rank (only if single push was clear)
            if ((colour == PieceColour.WHITE && position.getRow() == 6)
                    || (colour == PieceColour.BLACK && position.getRow() == 1)) {
                PiecePosition doublePush = new PiecePosition(position.getRow() + 2 * direction, position.getCol());
                if (isPositionOnBoard(doublePush) && board.getPiece(doublePush.getRow(), doublePush.getCol()) == null) {
                    legalMoves.add(doublePush);
                }
            }
        }

        // Diagonal captures (including en passant)
        for (int dc : new int[]{-1, 1}) {
            PiecePosition capturePos = new PiecePosition(position.getRow() + direction, position.getCol() + dc);
            if (!isPositionOnBoard(capturePos)) continue;

            Piece target = board.getPiece(capturePos.getRow(), capturePos.getCol());
            if (target != null && target.getColour() != colour) {
                // Normal diagonal capture
                legalMoves.add(capturePos);
            } else if (capturePos.equals(enPassantTarget)) {
                // En passant capture
                legalMoves.add(capturePos);
            }
        }
    }

    public List<PiecePosition> getLegalMovesForPiece(PiecePosition position) {
        Piece selectedPiece = board.getPiece(position.getRow(), position.getCol());

        // Base case of no piece being on square
        if (selectedPiece == null) {
            return new ArrayList<>();
        }

        // Generate candidate moves based on piece type
        List<PiecePosition> rawMoves = new ArrayList<>();
        PieceColour colour = selectedPiece.getColour();

        switch (selectedPiece.getClass().getSimpleName()) {
            case "Pawn":
                addPawnMoves(position, colour, rawMoves);
                break;
            case "Rook":
                addLineMoves(position, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}, rawMoves);
                break;
            case "Knight":
                addSingleMoves(position, new int[][]{{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{-1,2},{1,-2},{-1,-2}}, rawMoves);
                break;
            case "Bishop":
                addLineMoves(position, new int[][]{{1,1},{-1,-1},{1,-1},{-1,1}}, rawMoves);
                break;
            case "Queen":
                addLineMoves(position, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}}, rawMoves);
                break;
            case "King":
                addSingleMoves(position, new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}}, rawMoves);
                if (canCastle(colour, true))  rawMoves.add(new PiecePosition(position.getRow(), 6));
                if (canCastle(colour, false)) rawMoves.add(new PiecePosition(position.getRow(), 2));
                break;
        }

        // Filter out any move that leaves the king in check
        List<PiecePosition> legalMoves = new ArrayList<>();
        for (PiecePosition move : rawMoves) {
            boolean inCheck;
            if (selectedPiece instanceof Pawn && enPassantTarget != null && move.equals(enPassantTarget)) {
                inCheck = wouldBeInCheckAfterEnPassant(colour, position, move);
            } else {
                inCheck = wouldBeInCheckAfterMove(colour, position, move);
            }
            if (!inCheck) legalMoves.add(move);
        }

        return legalMoves;
    }

    public List<int[]> getAllLegalMoves(PieceColour colour) {
        // Returns {fromRow, fromCol, toRow, toCol} for every legal move for the given colour
        List<int[]> moves = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColour() == colour) {
                    PiecePosition from = new PiecePosition(row, col);
                    for (PiecePosition to : getLegalMovesForPiece(from)) {
                        moves.add(new int[]{row, col, to.getRow(), to.getCol()});
                    }
                }
            }
        }
        return moves;
    }

    // Expose en passant state for the AI to save/restore during search
    public PiecePosition getEnPassantTarget() { return enPassantTarget; }
    public void setEnPassantTarget(PiecePosition target) { enPassantTarget = target; }
}
