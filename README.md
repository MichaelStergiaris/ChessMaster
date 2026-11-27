# ChessMaster Studio

A native **Java desktop application** for practicing and mastering chess, featuring a Lichess / Chess.com–inspired dark graphical interface, **unlimited live tactical puzzles**, adjustable-difficulty **Stockfish 19** gameplay, a **48-opening interactive study & drill studio**, and a **MultiPV Analysis Board**.

Built in pure standard Java (JDK 21+ / JDK 25) with **zero third-party JAR dependencies**.

---

## Features

### 1. Play vs. Stockfish 19
- **UCI Stockfish 19 Integration:** Plays directly against the official Stockfish engine (with an automatic pure-Java alpha-beta fallback if the engine binary is absent).
- **Adjustable Difficulty:** Choose from **9 curated presets**—from **Level 1 Beginner (400 Elo)** up to **Stockfish 19 Full Strength (MAX)**—or fine-tune any target strength with the **400–3200 Elo slider**.
- **Time Controls:** Unlimited, Bullet (`1+0`), Blitz (`3+2`, `5+0`), and Rapid (`10+0`, `15+10`) with live clocks and increment support.
- **Training Tools:** Animated live **Evaluation Bar**, Stockfish **Hint** arrows, takebacks, captured pieces with material balance, clickable move history table with `< | < > | >` review navigation, FEN copy, and one-click handoff to the Analysis Board.

### 2. Unlimited Tactical Puzzles
- **Live Popular Puzzle Databases:**
  - **Lichess Unlimited Public Puzzle API** (`/api/puzzle/next`) with **18 tactical theme filters** (*Fork, Pin, Skewer, Sacrifice, Discovered Attack, Deflection, Attraction, Mate in 1/2/3, Back Rank Mate, Smothered Mate, Kingside Attack, Endgame, Trapped Piece, Quiet Move, Crushing, Healthy Mix*) and **5 difficulty tiers** (*Easiest* to *Hardest*).
  - **Lichess Daily Puzzle** (`/api/puzzle/daily`).
  - **Chess.com Random Puzzles** (`/pub/puzzle/random`).
  - **18 Curated Masterpiece Classics** (Morphy's Opera Game, Philidor's Legacy, Anastasia's Mate, Boden's Mate, Légal's Mate, Damiano's Mate, Arabian Mate, Pawn Breakthrough, Knight Underpromotion, and more) that also work **100% offline**.
- **Lichess-Style Experience:** Background prefetch queue for instant puzzle loading, animated opponent blunder setup moves, 2-stage hints (highlight piece -> draw solution arrow), solution playback, and persistent local **Elo puzzle rating & streak tracking** (`~/.chessmaster/profile.json`).

### 3. Opening Studio (Study & Interactive Drill)
- **48 Popular Chess Openings & Key Variations** organized across all ECO categories:
  - **Open Games (`1.e4 e5`):** Ruy Lopez (Morphy, Berlin, Marshall), Italian Game (Giuoco Piano, Evans Gambit, Two Knights), Scotch Game, Vienna Gambit, King's Gambit, Petrov's Defense, Philidor Defense, Four Knights, Ponziani.
  - **Semi-Open Games (`1.e4 ...`):** Sicilian Defense (Najdorf, Dragon, Accelerated Dragon, Sveshnikov, Alapin, Rossolimo, Closed), French Defense (Advance, Winawer, Tarrasch), Caro-Kann (Classical, Advance), Scandinavian, Pirc, Alekhine.
  - **Closed & Queen's Gambit (`1.d4 d5`):** Queen's Gambit Declined, Queen's Gambit Accepted, Slav, Semi-Slav (Meran), London System, Jobava London, Catalan, Trompowsky.
  - **Indian Defenses (`1.d4 Nf6`):** King's Indian (Classical), Nimzo-Indian (Rubinstein), Queen's Indian, Grünfeld (Exchange), Modern Benoni, Benko Gambit, Leningrad Dutch.
  - **Flank Openings:** English (Symmetrical, Reversed Sicilian), Réti, Bird's Opening, Nimzowitsch-Larsen Attack.
- **Study & Explore Mode:** Move-by-move coaching commentary explaining the strategic and tactical ideas behind every move, Auto-Play, searchable ECO library, and live **Lichess Masters Explorer** Win/Draw/Loss continuation bars.
- **Interactive Drill Mode:** Practice lines from White or Black's perspective with instant feedback, hints, mistake counter, persistent **Mastered (`✓`)** badges, and one-click **"Play vs Stockfish"** from any opening position.

### 4. Analysis Board
- Free interactive board with live **Stockfish 19 MultiPV (Top 3 Lines)** evaluation, clickable principal variations in SAN notation, animated Evaluation Bar, custom FEN loader/exporter, and right-click board annotations.

### 5. Lichess / Chess.com–Style Interface
- Custom **high-DPI anti-aliased vector Staunton pieces** rendered with `Graphics2D` (crisp at any window size).
- Smooth **drag-and-drop** and **click-to-move** with ease-out piece slide animations.
- Legal move dots, capture corner rings, check radial halo, and visual pawn promotion picker.
- **Right-click board annotations:** Drag right-click to draw colored arrows (`Shift` = red, `Alt` = blue, `Ctrl` = amber, default = green) or right-click a square to circle it.
- **5 Board Themes:** *Lichess Wood*, *Chess.com Green*, *Midnight Slate*, *Tournament Blue*, and *Walnut Dark*.
- Synthesized wooden move, capture, castle, check, and puzzle audio effects (toggleable in the top bar).

---

## Requirements

- **Java JDK 21 or newer** (tested on **JDK 25**)
- **Windows** (for the included `setup-stockfish.ps1`, `build.bat`, and `run.bat` helper scripts; the Java source itself is cross-platform and works on macOS/Linux by placing the platform's Stockfish binary at `engine/stockfish.exe` or `stockfish`)
- Internet connection (optional) for live Lichess / Chess.com puzzles and Lichess Masters opening statistics

---

## Quick Start (Windows)

1. **Clone the repository:**
   ```bash
   git clone https://github.com/MichaelStergiaris/ChessMaster.git
   cd ChessMaster
   ```

2. **Download the official Stockfish 19 binary:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup-stockfish.ps1
   ```
   *(The binary is excluded from Git due to its ~98 MB size; this script automatically downloads the latest official Windows universal release into `engine/stockfish.exe`.)*

3. **Build the executable JAR:**
   ```cmd
   build.bat
   ```

4. **Launch ChessMaster:**
   ```cmd
   run.bat
   ```
   Or directly via:
   ```cmd
   java -jar ChessMaster.jar
   ```

---

## Running the Automated Verification Suite

ChessMaster includes a self-test suite ([ChessMasterSelfTest.java](src/com/chessmaster/ChessMasterSelfTest.java)) that validates:
- **FIDE Chess Rules & Move Generator** via exact Perft (`Startpos Perft(4) = 197,281`, `Kiwipete Perft(3) = 97,862`)
- All **48 opening lines** and coaching annotations
- All **18 curated tactical puzzles** (including mate verification)
- Live **Lichess**, **Chess.com**, and **Lichess Masters Explorer** API parsing
- **Stockfish 19 UCI** difficulty scaling and MultiPV analysis
- Vector piece rendering and GUI frame initialization

Run it after building:
```cmd
java -cp out com.chessmaster.ChessMasterSelfTest
```

---

## Project Structure

```text
ChessMaster/
├── src/com/chessmaster/
│   ├── ChessMasterApp.java          # Desktop application entry point
│   ├── ChessMasterSelfTest.java     # Automated Perft, opening, puzzle, API & engine tests
│   ├── model/
│   │   ├── BoardState.java          # Complete FIDE rules, legal moves, FEN, SAN, PGN parser
│   │   ├── Color.java
│   │   ├── GameStatus.java
│   │   ├── Move.java
│   │   ├── Piece.java
│   │   └── PieceType.java
│   ├── engine/
│   │   └── StockfishEngine.java     # UCI Stockfish 19 controller, Elo scaling, MultiPV, fallback AI
│   ├── data/
│   │   ├── JsonParser.java          # Zero-dependency RFC 8259 JSON parser/serializer
│   │   ├── Opening.java
│   │   ├── OpeningDatabase.java     # 48 popular openings across all ECO families
│   │   ├── OpeningExplorerClient.java # Live Lichess Masters API client
│   │   ├── Puzzle.java
│   │   ├── PuzzleService.java       # Lichess Unlimited, Lichess Daily, Chess.com & Curated bank
│   │   └── UserProfile.java         # Local JSON persistence for ratings, streaks & mastery
│   └── ui/
│       ├── MainFrame.java           # Main window & top navigation bar
│       ├── ChessBoardPanel.java     # Interactive drag/click board, animations, right-click arrows
│       ├── PieceRenderer.java       # High-DPI custom vector Staunton chess pieces
│       ├── EvalBarPanel.java        # Animated Lichess-style evaluation bar
│       ├── PlayStockfishPanel.java  # Play vs. Stockfish mode
│       ├── PuzzlesPanel.java        # Unlimited Puzzles mode
│       ├── OpeningsPanel.java       # Opening Study & Interactive Drill mode
│       ├── AnalysisPanel.java       # Stockfish MultiPV Analysis Studio
│       ├── SoundManager.java        # Synthesized chess sound effects
│       └── Theme.java               # Dark studio theme & 5 board palettes
├── engine/                          # Place stockfish.exe here (via setup-stockfish.ps1)
├── setup-stockfish.ps1              # Downloads official Stockfish binary
├── build.bat                        # Compiles sources & packages ChessMaster.jar
├── run.bat                          # One-click desktop launcher
└── LICENSE
```

---

## License

This project is released under the [MIT License](LICENSE).  
[Stockfish](https://stockfishchess.org/) is licensed under the GNU GPLv3 and downloaded separately from its official releases.
