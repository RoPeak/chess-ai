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

    private static final Color CTRL_BG      = new Color( 45,  30,  15);
    private static final Color BTN_RESET    = new Color(180,  60,  60);
    private static final Color BTN_AI_OFF   = new Color( 90,  90,  90);
    private static final Color BTN_AI_ON    = new Color( 60, 150,  60);

    private final ChessSquareComponent[][] squares = new ChessSquareComponent[8][8];
    private final Gameplay game = new Gameplay();
    private final JLabel statusBar = new JLabel(" ", SwingConstants.CENTER);

    // Control-bar widgets (kept as fields so event handlers can mutate them)
    private JToggleButton aiButton;
    private JComboBox<String> diffCombo;

    // Prevent clicks during AI thinking without disabling (which greys out pieces)
    private boolean aiThinking = false;

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

        add(buildControlPanel(), BorderLayout.NORTH);

        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        initialiseBoard(boardPanel);
        add(boardPanel, BorderLayout.CENTER);

        statusBar.setFont(statusBar.getFont().deriveFont(14f));
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(statusBar, BorderLayout.SOUTH);

        registerPromotionCallback();
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

    private JPanel buildControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(CTRL_BG);

        // Reset button
        JButton resetBtn = styledButton("Reset", BTN_RESET);
        resetBtn.addActionListener(e -> resetGame());
        panel.add(resetBtn);

        // Separator
        panel.add(Box.createHorizontalStrut(6));

        // AI toggle
        aiButton = new JToggleButton("AI: OFF");
        styleToggleButton(aiButton, false);
        aiButton.addActionListener(e -> {
            boolean on = aiButton.isSelected();
            styleToggleButton(aiButton, on);
            game.setAiMode(on, currentDepth);
            updateStatusBar();
            if (on && game.getCurrentPlayerColour() == PieceColour.BLACK) {
                triggerAIMove();
            }
        });
        panel.add(aiButton);

        // Difficulty combo
        diffCombo = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        diffCombo.setSelectedIndex(1); // Medium
        diffCombo.setFont(diffCombo.getFont().deriveFont(13f));
        diffCombo.addActionListener(e -> {
            currentDepth = switch (diffCombo.getSelectedIndex()) {
                case 0 -> 2;
                case 2 -> 4;
                default -> 3;
            };
            if (game.isAiMode()) game.setAiMode(true, currentDepth);
        });
        panel.add(diffCombo);

        return panel;
    }

    private JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        btn.setOpaque(true);
        return btn;
    }

    private void styleToggleButton(JToggleButton btn, boolean on) {
        btn.setText(on ? "AI: ON" : "AI: OFF");
        btn.setBackground(on ? BTN_AI_ON : BTN_AI_OFF);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 14, 5, 14));
        btn.setOpaque(true);
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
        clearHighlights();
        refreshBoard();
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
        if (aiThinking) {
            statusBar.setText("AI is thinking...");
            return;
        }
        if (game.isAiMode()) {
            String turn = game.getCurrentPlayerColour() == PieceColour.WHITE ? "Your turn (White)" : "AI is thinking...";
            statusBar.setText(turn);
        } else {
            String turn = game.getCurrentPlayerColour() == PieceColour.WHITE ? "White's turn" : "Black's turn";
            statusBar.setText(turn);
        }
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    private void handleSquareClick(int row, int col) {
        // Ignore clicks while AI is thinking
        if (aiThinking) return;
        if (game.isAiMode() && game.getCurrentPlayerColour() == PieceColour.BLACK) return;

        // Capture the "from" square before the move is executed
        PiecePosition from = game.getSelectedPosition();
        int fromRow = from != null ? from.getRow() : -1;
        int fromCol = from != null ? from.getCol() : -1;

        boolean moveResult = game.handleSquareSelection(row, col);
        clearHighlights();

        if (moveResult && fromRow >= 0) {
            animateMove(fromRow, fromCol, row, col, () -> {
                checkGameState();
                if (checkGameOver()) return;
                updateStatusBar();
                if (game.isAiMode() && game.getCurrentPlayerColour() == PieceColour.BLACK) {
                    triggerAIMove();
                }
            });
        } else if (moveResult) {
            refreshBoard();
            checkGameState();
            if (checkGameOver()) return;
            updateStatusBar();
            if (game.isAiMode() && game.getCurrentPlayerColour() == PieceColour.BLACK) {
                triggerAIMove();
            }
        } else if (game.isPieceSelected()) {
            refreshBoard();
            squares[row][col].setBackground(SELECTED);
            highlightLegalMoves(new PiecePosition(row, col));
        } else {
            refreshBoard();
        }
    }

    // -------------------------------------------------------------------------
    // AI
    // -------------------------------------------------------------------------

    private void triggerAIMove() {
        aiThinking = true;
        statusBar.setText("AI is thinking...");

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
                        animateMove(m[0], m[1], m[2], m[3], () -> {
                            aiThinking = false;
                            checkGameState();
                            checkGameOver();
                            updateStatusBar();
                        });
                    } else {
                        aiThinking = false;
                        updateStatusBar();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    aiThinking = false;
                    updateStatusBar();
                }
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private AnimationLayer getAnimationLayer() {
        Component glass = getRootPane().getGlassPane();
        if (glass instanceof AnimationLayer) return (AnimationLayer) glass;
        AnimationLayer layer = new AnimationLayer();
        getRootPane().setGlassPane(layer);
        layer.setVisible(true);
        return layer;
    }

    /**
     * Plays a smooth animated piece move from (fromRow,fromCol) to (toRow,toCol).
     * The board is refreshed immediately (showing the post-move state), the
     * destination square is blanked during flight, and restored when done.
     */
    private void animateMove(int fromRow, int fromCol, int toRow, int toCol, Runnable onComplete) {
        refreshBoard();

        Piece piece = game.getBoard().getPiece(toRow, toCol);
        if (piece == null) { onComplete.run(); return; }

        String symbol = getPieceSymbol(piece);
        Color fg = piece.getColour() == PieceColour.WHITE
            ? new Color(30, 30, 30) : new Color(180, 40, 40);

        // Hide the destination square's piece during the animation
        squares[toRow][toCol].clearPieceSymbol();

        Font font = squares[toRow][toCol].getFont();
        Point fromPoint = SwingUtilities.convertPoint(
            squares[fromRow][fromCol],
            squares[fromRow][fromCol].getWidth() / 2,
            squares[fromRow][fromCol].getHeight() / 2,
            getRootPane());
        Point toPoint = SwingUtilities.convertPoint(
            squares[toRow][toCol],
            squares[toRow][toCol].getWidth() / 2,
            squares[toRow][toCol].getHeight() / 2,
            getRootPane());

        AnimationLayer layer = getAnimationLayer();
        layer.play(symbol, fg, font, fromPoint, toPoint, 220, () -> {
            squares[toRow][toCol].setPieceSymbol(symbol, fg);
            onComplete.run();
        });
    }

    // Glass pane that draws an animated piece glyph
    private static class AnimationLayer extends JComponent {
        private String symbol;
        private Color  colour;
        private Font   font;
        private Point  from;
        private Point  to;
        private long   startMs;
        private int    durationMs;
        private Runnable onDone;
        private Timer  timer;

        void play(String symbol, Color colour, Font font,
                  Point from, Point to, int durationMs, Runnable onDone) {
            if (timer != null && timer.isRunning()) {
                timer.stop();
                if (this.onDone != null) this.onDone.run();
            }
            this.symbol     = symbol;
            this.colour     = colour;
            this.font       = font;
            this.from       = from;
            this.to         = to;
            this.durationMs = durationMs;
            this.onDone     = onDone;
            this.startMs    = System.currentTimeMillis();

            timer = new Timer(16, e -> {
                long elapsed = System.currentTimeMillis() - startMs;
                if (elapsed >= durationMs) {
                    timer.stop();
                    this.symbol = null;
                    repaint();
                    Runnable cb = this.onDone;
                    this.onDone = null;
                    if (cb != null) cb.run();
                } else {
                    repaint();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (symbol == null || from == null || to == null) return;
            long elapsed = System.currentTimeMillis() - startMs;
            float t = Math.min(1f, (float) elapsed / durationMs);
            // Ease-out cubic: decelerate into destination
            float eased = 1f - (float) Math.pow(1f - t, 3);
            int x = Math.round(from.x + (to.x - from.x) * eased);
            int y = Math.round(from.y + (to.y - from.y) * eased);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font);
            g2.setColor(colour);
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(symbol);
            int sh = fm.getAscent();
            g2.drawString(symbol, x - sw / 2, y + sh / 2 - fm.getDescent());
            g2.dispose();
        }
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
        aiThinking = false;
        game.resetGame();
        clearHighlights();
        refreshBoard();
        updateStatusBar();
    }
}
