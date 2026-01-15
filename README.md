# Chess-AI

A Java-based chess game featuring a graphical user interface built with Swing. Play chess with complete rule enforcement and a clean, intuitive interface.

## Features

-  Full chess implementation with all standard rules
-  Graphical user interface using Java Swing
-  All chess pieces with proper movement validation
-  Visual board representation
-  Two-player local gameplay
-  Complete rule enforcement

## Project Structure

```
Chess-AI/
├── src/
│   └── main/
│       └── java/
│           ├── Main.java                    # Application entry point
│           ├── game/
│           │   ├── Board.java               # Chess board logic
│           │   ├── ChessGUI.java            # Main GUI component
│           │   ├── ChessSquareComponent.java # Individual square rendering
│           │   └── Gameplay.java            # Game state management
│           └── pieces/
│               ├── Piece.java               # Abstract base piece class
│               ├── PieceColour.java         # Enum for piece colors
│               ├── PiecePosition.java       # Position representation
│               ├── Pawn.java                # Pawn implementation
│               ├── Rook.java                # Rook implementation
│               ├── Knight.java              # Knight implementation
│               ├── Bishop.java              # Bishop implementation
│               ├── Queen.java               # Queen implementation
│               └── King.java                # King implementation
├── pom.xml                                   # Maven build configuration
└── README.md
```

## Requirements

- **Java Development Kit (JDK)** 22 or higher
- **Apache Maven** 3.6+ for building

## Building and Running

### Using Maven (Recommended)

1. **Build the project:**
   ```bash
   mvn package
   ```
   or for a clean build:
   ```bash
   mvn clean install
   ```

2. **Run the game:**
   ```bash
   java -jar target/chess-ai-1.0.0.jar
   ```

### From IDE

1. Import the project as a Maven project
2. Run the `Main.java` class

## How to Play

1. Launch the application
2. The chess board will appear with pieces in their starting positions
3. Click on a piece to select it
4. Click on a valid destination square to move the piece
5. Players alternate turns (White moves first)
6. The game enforces all standard chess rules

## Chess Pieces

| Piece | Symbol | Movement |
|-------|--------|----------|
| Pawn | ♟ | Forward one square (two on first move), captures diagonally |
| Rook | ♜ | Any number of squares horizontally or vertically |
| Knight | ♞ | L-shape: 2 squares in one direction, 1 perpendicular |
| Bishop | ♝ | Any number of squares diagonally |
| Queen | ♛ | Combination of Rook and Bishop |
| King | ♚ | One square in any direction |

## Architecture

### Game Package
- **Board**: Manages the 8x8 chess board and piece positions
- **ChessGUI**: Main window and UI components
- **ChessSquareComponent**: Renders individual board squares
- **Gameplay**: Handles turn management and game state

### Pieces Package
- **Piece**: Abstract base class defining common piece behavior
- **PieceColour**: Enum for WHITE and BLACK
- **PiecePosition**: Represents board coordinates
- Individual piece classes implement specific movement rules

## Technologies Used

- **Java 22** - Core programming language
- **Java Swing** - GUI framework
- **Maven** - Build automation and dependency management

## Future Enhancements

- [ ] AI opponent with different difficulty levels
- [ ] Move validation highlighting
- [ ] Check and checkmate detection
- [ ] Castling and en passant special moves
- [ ] Move history and undo functionality
- [ ] Save/load game state
- [ ] Timer for timed games
- [ ] Captured pieces display

## Development

To contribute or modify:

1. Clone the repository
2. Make your changes to the source files
3. Build with `mvn package` to verify compilation
4. Test thoroughly before committing

## License

This is a personal educational project.