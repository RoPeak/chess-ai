package pieces;

public class PiecePosition {
    private int row;
    private int col;

    public PiecePosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PiecePosition other)) return false;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return 8 * row + col;
    }
}
