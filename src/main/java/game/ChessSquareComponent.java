package game;

// Standard imports
import javax.swing.*;
import java.awt.*;

public class ChessSquareComponent extends JButton {
    static final Color LIGHT = new Color(240, 217, 181);
    static final Color DARK  = new Color(181, 136,  99);

    private final int row;
    private final int col;

    public ChessSquareComponent(int row, int col) {
        this.row = row;
        this.col = col;
        initButton();
    }

    private void initButton() {
        setPreferredSize(new Dimension(72, 72));
        setBackground((row + col) % 2 == 0 ? LIGHT : DARK);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFont(chooseBestFont(38));
        // Remove visual button decorations so it looks like a plain square
        setContentAreaFilled(false);
        setOpaque(true);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    /**
     * Tries fonts known to include the chess glyph block (U+2654–U+265F).
     * Falls back to Serif if none are found.
     */
    static Font chooseBestFont(int size) {
        for (String name : new String[]{"Segoe UI Symbol", "Segoe UI Emoji", "FreeSerif", "Symbola"}) {
            Font f = new Font(name, Font.PLAIN, size);
            if (f.canDisplay('\u2654')) return f;
        }
        return new Font(Font.SERIF, Font.BOLD, size);
    }

    /** Returns true if the system has a font that can render chess Unicode glyphs. */
    static boolean canDisplayChessGlyphs() {
        for (String name : new String[]{"Segoe UI Symbol", "Segoe UI Emoji", "FreeSerif", "Symbola"}) {
            Font f = new Font(name, Font.PLAIN, 36);
            if (f.canDisplay('\u2654')) return true;
        }
        return false;
    }

    public void setPieceSymbol(String symbol, Color colour) {
        setText(symbol);
        setForeground(colour);
    }

    public void clearPieceSymbol() {
        setText("");
    }
}
