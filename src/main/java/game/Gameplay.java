package game;

// Standard imports
import java.util.List;
import java.util.ArrayList;

// Custom imports
import pieces.PieceColour;
import pieces.PiecePosition;
import pieces.King;
import pieces.Piece;

public class Gameplay {
    private Board board;
    private boolean whiteTurn = true;
    private PiecePosition selectedPiecePosition;

    public Gameplay() {
        this.board = new Board();
    }

    public Board getBoard() {
        return this.board;
    }

    public boolean makeMove(PiecePosition start, PiecePosition end) {
        Piece movingPiece = board.getPiece(start.getRow(), start.getCol());
        
        // Ensure space is not empty or piece is wrong colour
        if (movingPiece == null || movingPiece.getColour() != (whiteTurn ? PieceColour.WHITE : PieceColour.BLACK)) {
            return false;
        }

        // Ensure desired move is valid for this given piece
        if (movingPiece.isValidMove(end, board.getBoard())) {
            // Move piece and swap turn
            board.movePiece(start, end);
            whiteTurn = !whiteTurn;

            return true;
        }

        // Incorrect move if no condition has been met
        return false;
    }

    private PiecePosition findKingPosition(PieceColour kingColour) {
        // Iterate through the board and find the given coloured king
        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece instanceof King && piece.getColour() == kingColour) {
                    return new PiecePosition(row, col);
                }
            } 
        }
        throw new RuntimeException("King not on board");
    }

    private boolean isPositionOnBoard(PiecePosition position) {
        // Return whether the given position is a valid position in our 2D grid
        return position.getRow() >= 0 && position.getRow() < board.getBoard().length 
        && position.getCol() >= 0 && position.getCol() < board.getBoard()[0].length;
    }

    public boolean isInCheck(PieceColour kingColour) {
        // Iterate through each square, checking if that piece is checking the king
        PiecePosition kingPosition = findKingPosition(kingColour);
        for (int row = 0; row < board.getBoard().length; row++) {
            for (int col = 0; col < board.getBoard()[row].length; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColour() != kingColour) {
                    if (piece.isValidMove(kingPosition, board.getBoard())) {
                        // A piece of the opposite colour is able to capture the king (!)
                        return true;
                    }
                }
            }
        }
        // No piece checking the king was found
        return false;
    }

    private boolean wouldBeInCheckAfterMove(PieceColour kingColour, PiecePosition from, PiecePosition to) {
        // Simulate move
        Piece temp = board.getPiece(to.getRow(), to.getCol());
        board.setPiece(to.getRow(), to.getCol(), board.getPiece(from.getRow(), from.getCol()));
        board.setPiece(from.getRow(), from.getCol(), null);
        boolean inCheck = isInCheck(kingColour);

        // Undo move
        board.setPiece(from.getRow(), from.getCol(), board.getPiece(to.getRow(), to.getCol()));
        board.setPiece(to.getRow(), to.getCol(), temp);

        return inCheck;
    }

    public boolean isCheckmate(PieceColour kingColour) {
        if (!isInCheck(kingColour)) {
            // Not in check so not checkmate
            return false;
        }

        // Select the king
        PiecePosition kingPosition = findKingPosition(kingColour);
        King king = (King) board.getPiece(kingPosition.getRow(), kingPosition.getCol());

        // Attempt to find a move that gets the king out of check
        for (int rowOffSet = -1; rowOffSet <= 1; rowOffSet++) {
            for (int colOffSet = -1; colOffSet <= 1; colOffSet++) {
                if (rowOffSet == 0 && colOffSet == 0) {
                    // Skip current king position
                    continue;
                }
                
                // Check if this new move is valid and does not result in check
                PiecePosition newPosition = new PiecePosition(kingPosition.getRow() + rowOffSet, kingPosition.getCol() + colOffSet);
                if (isPositionOnBoard(newPosition) && king.isValidMove(newPosition, board.getBoard())
                    && !wouldBeInCheckAfterMove(kingColour, kingPosition, newPosition)) {
                        // Appropriate move found, so not checkmate
                        return false;
                }
            }
        }

        // No legal moves available, game over
        return true;
    }

    public void resetGame() {
        // Re-initialise board and reset turn to white
        this.board = new Board();
        this.whiteTurn = true;
    }

    public PieceColour getCurrentPlayerColour() {
        // Return White or Black depending on who's turn it is
        return whiteTurn ? PieceColour.WHITE : PieceColour.BLACK;
    }

    public boolean isPieceSelected() {
        return selectedPiecePosition != null;
    }

    public boolean handleSquareSelection(int row, int col) {
        // If no piece is selected, this click will likely be to
        // try and select one - only if there is a piece on this square
        // AND it is the current player's colour
        if (selectedPiecePosition == null) {
            Piece selectedPiece = board.getPiece(row, col);
            if (selectedPiece != null && selectedPiece.getColour() == getCurrentPlayerColour()) {
                selectedPiecePosition = new PiecePosition(row, col);
                // Indicate that a piece has been selected but not moved
                return false;
            }
        } 
        // If a piece has been selected, this click will likely be to move it
        else {
            // Attempt to make move, reset the selected piece regardless of result,
            // then return success/failure
            boolean moveMade = makeMove(selectedPiecePosition, new PiecePosition(row, col));
            selectedPiecePosition = null; 
            return moveMade;
        }

        // Return false if no accepting condition was met
        return false;
    }

    private void addLineMoves(PiecePosition position, int[][] directions, List<PiecePosition> legalMoves) {
        // Computes legal moves along straight and diagonal lines by iterating through specified directions
        // from the given position, until a blocking piece or the board edge is reached 
        for (int[] d: directions) {
            PiecePosition newPos = new PiecePosition(position.getRow() + d[0], position.getCol() + d[1]);
            while (isPositionOnBoard(newPos)) {
                if (board.getPiece(newPos.getRow(), newPos.getCol()) == null) {
                    // Empty square in path = legal move has been found
                    legalMoves.add(new PiecePosition(newPos.getRow(), newPos.getCol()));

                    // Move on to next square in sequence
                    newPos = new PiecePosition(newPos.getRow() + d[0], newPos.getCol() + d[1]);
                } 
                // For mon-empty squares, check if it is a capturable piece
                else {
                    if (board.getPiece(newPos.getRow(), newPos.getCol()).getColour() != board.getPiece(position.getRow(), position.getCol()).getColour()) {
                        // Piece of opposing colour reached = legal move has been found
                        legalMoves.add(newPos);
                    }
                    break;
                }
            }
        }
    }

    public void addSingleMoves(PiecePosition position, int[][] moves, List<PiecePosition> legalMoves) {
        // Computes legal single moves by iterating through specified directions
        // from the given position, until a blocking piece or the board edge is reached 
        for (int[] move : moves) {
            PiecePosition newPos = new PiecePosition(position.getRow() + move[0], position.getCol() + move[1]);
            
            // If this is a valid square on the board that is either empty or contains a piece of the opposite colour,
            // then it is a legal move
            if (isPositionOnBoard(newPos) && (board.getPiece(newPos.getRow(), newPos.getCol()) == null || 
                board.getPiece(newPos.getRow(), newPos.getCol()).getColour() != board.getPiece(position.getRow(), position.getCol()).getColour())) {
                    legalMoves.add(newPos);
                }
        }
    }

    public void addPawnMoves(PiecePosition position, PieceColour colour, List<PiecePosition> legalMoves) {
        // Pawns are slightly more complex to compute as they can take either one or two moves depending on
        // whether they have already moved.
        // They also capture diagonally i.e. a direction different to how they move
        int direction = colour == PieceColour.WHITE ? -1 : 1;

        // Generate move
        PiecePosition newPos = new PiecePosition(position.getRow() + direction, position.getCol());
        
        // Conditions for a standard single move (valid board position and square ahead is free)
        if (isPositionOnBoard(newPos) && board.getPiece(newPos.getRow(), newPos.getCol()) == null) {
            legalMoves.add(newPos);
        }

        // Conditions for a double move (valid board position, pawn has not moved, and 2 squares ahead are free)
        // This is only checked if the pawn has not moved before
        if ((colour == PieceColour.WHITE && position.getRow() == 6) || (colour == PieceColour.BLACK && position.getRow() == 1)) {
            newPos = new PiecePosition(position.getRow() + 2 * direction, position.getCol());
            PiecePosition middleStep = new PiecePosition(position.getRow() + direction, position.getCol());

            // Valid board position and no pieces blocking = legal move found
            if (isPositionOnBoard(newPos) && board.getPiece(newPos.getRow(), newPos.getCol()) == null 
                && board.getPiece(middleStep.getRow(), middleStep.getCol()) == null) {
                    legalMoves.add(newPos);
            }
        }

        // Conditions for a capture (valid board position and there is an opposing piece on a diagonal square)
        // Determine the two possible spaces for capture - edge case of one of them not being on the board
        int[] captureCols = {position.getCol() -1, position.getCol() + 1};
        for (int col : captureCols) {
            newPos = new PiecePosition(position.getRow() + direction, col);
            // If either space is on the board and has an opposing piece on it = legal move found
            if (isPositionOnBoard(newPos) && board.getPiece(newPos.getRow(), newPos.getCol()) != null 
                && board.getPiece(newPos.getRow(), newPos.getCol()).getColour() != colour) {
                    legalMoves.add(newPos);
                }
        }
    }

    public List<PiecePosition> getLegalMovesForPiece(PiecePosition position) {
        Piece selectedPiece = board.getPiece(position.getRow(), position.getCol());

        // Base case of no piece being on square
        if (selectedPiece == null) {
            return new ArrayList<>();
        }

        // Determine which piece is selected and then add the legal moves to an ArrayList
        // based on 2D grid coords
        List<PiecePosition> legalMoves = new ArrayList<>();
        switch (selectedPiece.getClass().getSimpleName()) {
            case "Pawn":
                addPawnMoves(position, selectedPiece.getColour(), legalMoves);
                break;
            case "Rook":
                addLineMoves(position, new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}, legalMoves);
                break;
            case "Knight":
                addSingleMoves(position, new int[][]{{2,1}, {2,-1}, {-2, 1}, {-2, -1}, {1, 2}, {-1, 2}, {1, -2}, {-1, -2}}, legalMoves);
                break;
            case "Bishop":
                addLineMoves(position, new int[][]{{1,1}, {-1,-1}, {1,-1}, {-1,1}}, legalMoves);
                break;
            case "Queen":
                addLineMoves(position, new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}, legalMoves);
                break;
            case "King":
                addSingleMoves(position, new int[][]{{1,0}, {-1,0}, {0,1},{0,-1}, {1,1}, {-1,-1}, {1,-1}, {-1,1}}, legalMoves);
                break;
        }
        return legalMoves;
    }
}