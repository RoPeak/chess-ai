package game;

// Custom imports
import pieces.Piece;
import pieces.PieceColour;
import pieces.PiecePosition;
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;
import pieces.Queen;
import pieces.Rook;

public class Board {
    private Piece[][] board;

    public Board() {
        board = new Piece[8][8];
        setupPieces();
    }

    public void setupPieces() {
        // Initialise all piece placements
        // Rook placement
        board[0][0] = new Rook(PieceColour.BLACK, new PiecePosition(0, 0));
        board[0][7] = new Rook(PieceColour.BLACK, new PiecePosition(0, 7));
        board[7][0] = new Rook(PieceColour.WHITE, new PiecePosition(7, 0));
        board[7][7] = new Rook(PieceColour.WHITE, new PiecePosition(7, 7));


        board[0][1] = new Knight(PieceColour.BLACK, new PiecePosition(0, 1));
        board[0][6] = new Knight(PieceColour.BLACK, new PiecePosition(0, 6));
        board[7][1] = new Knight(PieceColour.WHITE, new PiecePosition(7, 1));
        board[7][6] = new Knight(PieceColour.WHITE, new PiecePosition(7, 6));

        // Bishop placement
        board[0][2] = new Bishop(PieceColour.BLACK, new PiecePosition(0, 2));
        board[0][5] = new Bishop(PieceColour.BLACK, new PiecePosition(0, 5));
        board[7][2] = new Bishop(PieceColour.WHITE, new PiecePosition(7, 2));
        board[7][5] = new Bishop(PieceColour.WHITE, new PiecePosition(7, 5));

        // Queen placement
        board[0][3] = new Queen(PieceColour.BLACK, new PiecePosition(0, 3));
        board[7][3] = new Queen(PieceColour.WHITE, new PiecePosition(7, 3));

        // King placement
        board[0][4] = new King(PieceColour.BLACK, new PiecePosition(0, 4));
        board[7][4] = new King(PieceColour.WHITE, new PiecePosition(7, 4));

        // Pawn placement
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Pawn(PieceColour.BLACK, new PiecePosition(1, i));
            board[6][i] = new Pawn(PieceColour.WHITE, new PiecePosition(6, i));
        }
    }

    public void movePiece(PiecePosition start, PiecePosition end) {
        // Check if there is a piece at the start position
        // and that the move is valid
        if (board[start.getRow()][start.getCol()] != null && 
            board[start.getRow()][start.getCol()].isValidMove(end, board)) {
                // Perform the move - place piece at end position
                board[end.getRow()][end.getCol()] = board[start.getRow()][start.getCol()];
                
                // Update piece's position
                board[end.getRow()][end.getCol()].setPosition(end);

                // Clear start position
                board[start.getRow()][start.getCol()] = null;
            }
    }

    public Piece[][] getBoard() {
        return this.board;
    }
    
    public Piece getPiece(int row, int col) {
        return board[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        board[row][col] = piece;
        if (piece != null) {
            piece.setPosition(new PiecePosition(row, col));
        }
    }
}