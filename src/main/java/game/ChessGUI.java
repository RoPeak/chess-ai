package game;

// Standard imports
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

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


public class ChessGUI extends JFrame {
    private final ChessSquareComponent[][] squares = new ChessSquareComponent[8][8];
    private final Gameplay game = new Gameplay();
    private final Map<Class<? extends Piece>, String> pieceMap = new HashMap<>() {
        {
            put(Pawn.class, "P");
            put(Rook.class, "R");
            put(Knight.class, "N");
            put(Bishop.class, "B");
            put(Queen.class, "Q");
            put(King.class, "K");
        }
    };

    public ChessGUI() {
        setTitle("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(8, 8));
        registerPromotionCallback();
        initialiseBoard();
        addGameResetOption();
        pack();
        setVisible(true);
    }

    private void registerPromotionCallback() {
        game.setPromotionCallback((colour, position) -> {
            String[] options = {"Queen", "Rook", "Bishop", "Knight"};
            int choice = JOptionPane.showOptionDialog(
                this, "Choose promotion piece:", "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
            return switch (choice) {
                case 1 -> new Rook(colour, position);
                case 2 -> new Bishop(colour, position);
                case 3 -> new Knight(colour, position);
                default -> new Queen(colour, position);
            };
        });
    }

    private void initialiseBoard() {
        // Initialise each square of the board to be a component with a mouse listener
        for (int row = 0; row < squares.length; row++) {
            for (int col = 0; col < squares[row].length; col++) {
                final int finalRow = row;
                final int finalCol = col;
                ChessSquareComponent square = new ChessSquareComponent(row, col);

                square.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSquareClick(finalRow, finalCol);
                    }
                });

                add(square);
                squares[row][col] = square;
            }
        }

        // Draw the board
        refreshBoard();
    }

    private void refreshBoard() {
        // Iterate over every square, checking for piece presence
        // then use the map to update that square with the appropriate symbol
        Board board = game.getBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null) {
                    String symbol = pieceMap.get(piece.getClass());
                    Color colour = (piece.getColour() == PieceColour.WHITE) ? Color.WHITE : Color.BLUE;
                    squares[row][col].setPieceSymbol(symbol, colour);
                } else {
                    squares[row][col].clearPieceSymbol();
                }
            }
        }
    }

    private void handleSquareClick(int row, int col) {
        boolean moveResult = game.handleSquareSelection(row, col);
        clearHighlights();

        if (moveResult) {
            refreshBoard();
            checkGameState();
            checkGameOver();
        } else if (game.isPieceSelected()) {
            highlightLegalMoves(new PiecePosition(row, col));
        }
        refreshBoard();
    }

    private void checkGameState() {
        PieceColour currentPlayer = game.getCurrentPlayerColour();
        if (game.isInCheck(currentPlayer)) {
            JOptionPane.showMessageDialog(this, currentPlayer + " is in check.");
        }
    }

    private boolean checkGameOver() {
        PieceColour currentPlayer = game.getCurrentPlayerColour();

        if (game.isCheckmate(currentPlayer)) {
            int response = JOptionPane.showConfirmDialog(this,
                "Checkmate! Would you like to play again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                System.exit(0);
            }
            return true;
        }

        if (game.isStalemate(currentPlayer)) {
            int response = JOptionPane.showConfirmDialog(this,
                "Stalemate! It's a draw. Would you like to play again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                System.exit(0);
            }
            return true;
        }

        return false;
    }

    private void highlightLegalMoves(PiecePosition position) {
        List<PiecePosition> legalMoves = game.getLegalMovesForPiece(position);
        for (PiecePosition move : legalMoves) {
            squares[move.getRow()][move.getCol()].setBackground(Color.GREEN);
        }
    }

    private void clearHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setBackground((row + col) % 2 == 0 ? Color.LIGHT_GRAY : Color.BLACK);
            }
        }
    }

    private void addGameResetOption() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem resetItem = new JMenuItem("Reset");

        resetItem.addActionListener(e -> resetGame());
        gameMenu.add(resetItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    private void resetGame() {
        game.resetGame();
        refreshBoard();
    }
}
