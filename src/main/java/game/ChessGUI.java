package game;

// Standard imports
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// Custom imports
import pieces.*;


public class ChessGUI extends JFrame {
    private static final Color LIGHT_SQUARE = ChessSquareComponent.LIGHT;
    private static final Color DARK_SQUARE  = ChessSquareComponent.DARK;
    private static final Color HIGHLIGHT    = new Color(130, 190,  60);
    private static final Color SELECTED     = new Color(200, 200,  80);

    private final ChessSquareComponent[][] squares = new ChessSquareComponent[8][8];
    private final Gameplay game = new Gameplay();
    private final JLabel statusBar = new JLabel(" ", SwingConstants.CENTER);

    // Unicode chess glyphs — only used when the system font supports them
    private static final Map<Class<? extends Piece>, String> WHITE_UNICODE = new HashMap<>() {{
        put(King.class,   "\u2654"); put(Queen.class,  "\u2655"); put(Rook.class,   "\u2656");
        put(Bishop.class, "\u2657"); put(Knight.class, "\u2658"); put(Pawn.class,   "\u2659");
    }};
    private static final Map<Class<? extends Piece>, String> BLACK_UNICODE = new HashMap<>() {{
        put(King.class,   "\u265A"); put(Queen.class,  "\u265B"); put(Rook.class,   "\u265C");
        put(Bishop.class, "\u265D"); put(Knight.class, "\u265E"); put(Pawn.class,   "\u265F");
    }};
    // ASCII fallback (colour is conveyed by foreground colour)
    private static final Map<Class<? extends Piece>, String> LETTER_SYMBOLS = new HashMap<>() {{
        put(King.class, "K"); put(Queen.class, "Q"); put(Rook.class, "R");
        put(Bishop.class, "B"); put(Knight.class, "N"); put(Pawn.class, "P");
    }};

    private final boolean useUnicode = ChessSquareComponent.canDisplayChessGlyphs();

    // Current difficulty depth (default medium)
    private int currentDepth = 3;

    public ChessGUI() {
        setTitle("Chess AI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        initialiseBoard(boardPanel);
        add(boardPanel, BorderLayout.CENTER);

        statusBar.setFont(statusBar.getFont().deriveFont(14f));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);

        registerPromotionCallback();
        addMenuBar();
        updateStatusBar();
        pack();
        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

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

    private void initialiseBoard(JPanel boardPanel) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                final int r = row, c = col;
                ChessSquareComponent square = new ChessSquareComponent(row, col);
                square.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { handleSquareClick(r, c); }
                });
                boardPanel.add(square);
                squares[row][col] = square;
            }
        }
        clearHighlights(); // ensure warm colours from the start
        refreshBoard();
    }

    private void addMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");

        // Reset
        JMenuItem resetItem = new JMenuItem("Reset");
        resetItem.addActionListener(e -> resetGame());
        gameMenu.add(resetItem);

        gameMenu.addSeparator();

        // AI toggle
        JCheckBoxMenuItem aiItem = new JCheckBoxMenuItem("AI Opponent");
        aiItem.addActionListener(e -> {
            game.setAiMode(aiItem.isSelected(), currentDepth);
            updateStatusBar();
            // If AI is now on and it's black's turn, trigger the AI immediately
            if (aiItem.isSelected() && game.getCurrentPlayerColour() == PieceColour.BLACK) {
                triggerAIMove();
            }
        });
        gameMenu.add(aiItem);

        gameMenu.addSeparator();

        // Difficulty sub-menu
        JMenu diffMenu = new JMenu("Difficulty");
        ButtonGroup group = new ButtonGroup();
        String[][] levels = {{"Easy", "2"}, {"Medium", "3"}, {"Hard", "4"}};
        for (String[] level : levels) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(level[0]);
            if (level[0].equals("Medium")) item.setSelected(true);
            int d = Integer.parseInt(level[1]);
            item.addActionListener(e -> {
                currentDepth = d;
                if (game.isAiMode()) game.setAiMode(true, currentDepth);
            });
            group.add(item);
            diffMenu.add(item);
        }
        gameMenu.add(diffMenu);

        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    // -------------------------------------------------------------------------
    // Board display
    // -------------------------------------------------------------------------

    private void refreshBoard() {
        Board board = game.getBoard();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null) {
                    String symbol = getPieceSymbol(piece);
                    Color fg = piece.getColour() == PieceColour.WHITE
                        ? new Color(30, 30, 30) : new Color(180, 40, 40);
                    squares[row][col].setPieceSymbol(symbol, fg);
                } else {
                    squares[row][col].clearPieceSymbol();
                }
            }
        }
    }

    private void clearHighlights() {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                squares[row][col].setBackground((row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE);
    }

    private void highlightLegalMoves(PiecePosition position) {
        List<PiecePosition> legalMoves = game.getLegalMovesForPiece(position);
        for (PiecePosition move : legalMoves)
            squares[move.getRow()][move.getCol()].setBackground(HIGHLIGHT);
    }

    private String getPieceSymbol(Piece piece) {
        if (useUnicode) {
            Map<Class<? extends Piece>, String> map =
                piece.getColour() == PieceColour.WHITE ? WHITE_UNICODE : BLACK_UNICODE;
            return map.getOrDefault(piece.getClass(), "?");
        }
        return LETTER_SYMBOLS.getOrDefault(piece.getClass(), "?");
    }

    private void updateStatusBar() {
        if (game.isAiMode()) {
            String turn = game.getCurrentPlayerColour() == PieceColour.WHITE ? "Your turn (White)" : "AI is thinking...";
            statusBar.setText(turn);
        } else {
            String turn = game.getCurrentPlayerColour() == PieceColour.WHITE ? "White's turn" : "Black's turn";
            statusBar.setText(turn);
        }
    }

    private void setSquaresEnabled(boolean enabled) {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                squares[row][col].setEnabled(enabled);
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    private void handleSquareClick(int row, int col) {
        // Ignore clicks while it's the AI's turn
        if (game.isAiMode() && game.getCurrentPlayerColour() == PieceColour.BLACK) return;

        boolean moveResult = game.handleSquareSelection(row, col);
        clearHighlights();

        if (moveResult) {
            refreshBoard();
            checkGameState();
            if (checkGameOver()) return;
            updateStatusBar();

            if (game.isAiMode() && game.getCurrentPlayerColour() == PieceColour.BLACK) {
                triggerAIMove();
                return;
            }
        } else if (game.isPieceSelected()) {
            squares[row][col].setBackground(SELECTED);
            highlightLegalMoves(new PiecePosition(row, col));
        }
        refreshBoard();
    }

    // -------------------------------------------------------------------------
    // AI
    // -------------------------------------------------------------------------

    private void triggerAIMove() {
        statusBar.setText("AI is thinking...");
        setSquaresEnabled(false);

        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                return game.getAI().getBestMove();
            }

            @Override
            protected void done() {
                try {
                    int[] m = get();
                    if (m != null) {
                        // Suppress the promotion dialog for AI — it always queens (default)
                        Gameplay.PromotionCallback saved = game.getPromotionCallback();
                        game.setPromotionCallback(null);
                        game.makeMove(new PiecePosition(m[0], m[1]), new PiecePosition(m[2], m[3]));
                        game.setPromotionCallback(saved);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    refreshBoard();
                    checkGameState();
                    checkGameOver();
                    updateStatusBar();
                    setSquaresEnabled(true);
                }
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // Game state checks
    // -------------------------------------------------------------------------

    private void checkGameState() {
        PieceColour current = game.getCurrentPlayerColour();
        // Only show "in check" if it's not already checkmate (checkGameOver handles that separately)
        if (game.isInCheck(current) && !game.isCheckmate(current)) {
            String who;
            if (game.isAiMode()) {
                who = current == PieceColour.WHITE ? "You are" : "The AI is";
            } else {
                who = current + " is";
            }
            JOptionPane.showMessageDialog(this, who + " in check.");
        }
    }

    private boolean checkGameOver() {
        PieceColour current = game.getCurrentPlayerColour();

        if (game.isCheckmate(current)) {
            String msg = game.isAiMode()
                ? (current == PieceColour.WHITE ? "Checkmate — the AI wins!" : "Checkmate — you win!")
                : "Checkmate! " + current + " loses.";
            int response = JOptionPane.showConfirmDialog(this,
                msg + "\nPlay again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) resetGame();
            else System.exit(0);
            return true;
        }

        if (game.isStalemate(current)) {
            int response = JOptionPane.showConfirmDialog(this,
                "Stalemate — it's a draw!\nPlay again?", "Game Over", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) resetGame();
            else System.exit(0);
            return true;
        }

        return false;
    }

    private void resetGame() {
        game.resetGame();
        clearHighlights();
        refreshBoard();
        updateStatusBar();
    }
}
