package pieces;

public class Pawn extends Piece {
    public Pawn(PieceColour colour, PiecePosition position) {
        super(colour, position);
    }    

    @Override
    public boolean isValidMove(PiecePosition newPosition, Piece[][] board) {
        // Cannot move to the square it is already on
        if (newPosition.equals(this.position)) {
            return false;
        }

        // Set what 'forward' means for this colour pawn
        int forwardDirection = (colour == PieceColour.WHITE ? -1 : 1);
        
        // Calculate difference between current pos and new pos
        int rowDiff = (newPosition.getRow() - position.getRow()) * forwardDirection;
        int colDiff = (newPosition.getCol() - position.getCol());

        // Move forward one square (rowDiff is one and target square is free)
        if (colDiff == 0 && rowDiff == 1 && board[newPosition.getRow()][newPosition.getCol()] == null) {
            return true;
        }
        
        // Move forward two squares (rowDiff is two, target square is empty 
        // and pawn has not moved yet)
        boolean isStartingPos = (colour == PieceColour.WHITE && position.getRow() == 6) || 
                                (colour == PieceColour.BLACK && position.getRow() == 1);
        if (colDiff == 0 && rowDiff == 2 && board[newPosition.getRow()][newPosition.getCol()] == null && isStartingPos) {
            // Check that square inbetween is not blocking
            int middleRow = position.getRow() + forwardDirection;
            if (board[middleRow][position.getCol()] == null) {
                return true;
            }
        }

        // Diagonal capture (there is an enemy piece one square diagonally above left or right)
        if (Math.abs(colDiff) == 1 && rowDiff == 1 && board[newPosition.getRow()][newPosition.getCol()] != null && board[newPosition.getRow()][newPosition.getCol()].colour != this.colour) {
            return true;
        }

        // En passant is handled in Gameplay.addPawnMoves / makeMove, not here.
        // isValidMove is only used for check detection, where en passant never applies.

        // Invalid move if no other conditions have been met
        return false;
    }
}
