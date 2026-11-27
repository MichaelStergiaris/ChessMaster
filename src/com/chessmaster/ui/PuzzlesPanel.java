package com.chessmaster.ui;

import com.chessmaster.data.Puzzle;
import com.chessmaster.data.PuzzleService;
import com.chessmaster.data.UserProfile;
import com.chessmaster.model.*;
import com.chessmaster.model.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Unlimited Lichess-style Tactics Trainer connected live to Lichess Public Puzzle API,
 * Lichess Daily Puzzle, Chess.com Random Puzzles, and Curated Masterpieces.
 */
public final class PuzzlesPanel extends JPanel {

    private final PuzzleService puzzleService;
    private final SoundManager sound;
    private final UserProfile profile;
    private final Consumer<String> onOpenAnalysis;

    private final ChessBoardPanel boardPanel = new ChessBoardPanel();

    private Puzzle currentPuzzle;
    private BoardState currentState = new BoardState();
    private Color playerColor = Color.WHITE;
    private int solutionIdx = 0;
    private boolean puzzleFinished = false;
    private boolean failedThisPuzzle = false;
    private boolean ratingRecorded = false;
    private int hintStage = 0;
    private int loadSeq = 0;
    private final List<String> solvedSanMoves = new ArrayList<>();

    // UI Controls
    private final JLabel promptBanner = new JLabel("Loading live puzzle from Lichess...");
    private final JLabel ratingBigLabel = new JLabel("1400");
    private final JLabel streakLabel = new JLabel("Streak: 0  |  Best: 0  |  Solved: 0");
    private final JLabel deltaBadge = new JLabel(" ");

    private final JComboBox<String> sourceCombo = new JComboBox<>(new String[]{
        PuzzleService.SOURCE_LICHESS_UNLIMITED,
        PuzzleService.SOURCE_LICHESS_DAILY,
        PuzzleService.SOURCE_CHESSCOM,
        PuzzleService.SOURCE_CURATED
    });
    private final JComboBox<String> themeCombo;
    private final JComboBox<String> diffCombo;

    private final JLabel puzzleTitleLabel = new JLabel("—");
    private final JLabel puzzleMetaLabel = new JLabel("—");
    private final JLabel puzzlePlayersLabel = new JLabel(" ");
    private final JLabel puzzleThemesLabel = new JLabel(" ");
    private final JTextArea moveLogArea = new JTextArea(4, 24);
    private final JButton nextBtn = Theme.makePrimaryButton("Next Puzzle →");

    private final ExecutorService loaderExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PuzzleLoader");
        t.setDaemon(true);
        return t;
    });

    public PuzzlesPanel(PuzzleService puzzleService, SoundManager sound, UserProfile profile,
                        Consumer<String> onOpenAnalysis) {
        this.puzzleService = puzzleService;
        this.sound = sound;
        this.profile = profile;
        this.onOpenAnalysis = onOpenAnalysis;

        setLayout(new BorderLayout(16, 0));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(14, 18, 14, 18));

        themeCombo = new JComboBox<>(PuzzleService.LICHESS_THEMES.keySet().toArray(new String[0]));
        diffCombo = new JComboBox<>(PuzzleService.LICHESS_DIFFICULTIES.keySet().toArray(new String[0]));
        Theme.styleComboBox(sourceCombo);
        Theme.styleComboBox(themeCombo);
        Theme.styleComboBox(diffCombo);

        add(buildBoardArea(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        boardPanel.setMoveListener(this::handlePlayerMove);
        refreshProfileLabels();
        loadNextPuzzle();
    }

    public ChessBoardPanel getBoardPanel() {
        return boardPanel;
    }

    private JPanel buildBoardArea() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel topBanner = Theme.makeCardPanel(new BorderLayout(10, 0));
        topBanner.setBorder(new EmptyBorder(10, 16, 10, 16));
        promptBanner.setFont(Theme.FONT_HEADER);
        promptBanner.setForeground(Theme.TEXT_PRIMARY);
        deltaBadge.setFont(Theme.FONT_HEADER);
        deltaBadge.setForeground(Theme.ACCENT_GREEN);
        topBanner.add(promptBanner, BorderLayout.WEST);
        topBanner.add(deltaBadge, BorderLayout.EAST);

        p.add(topBanner, BorderLayout.NORTH);
        p.add(boardPanel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(390, 650));

        // 1. Stats Card
        JPanel statsCard = Theme.makeCardPanel(new BorderLayout(8, 6));
        statsCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel hdr = new JLabel("PUZZLE RATING");
        hdr.setFont(Theme.FONT_SMALL_BOLD);
        hdr.setForeground(Theme.TEXT_MUTED);
        ratingBigLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        ratingBigLabel.setForeground(Theme.ACCENT_GREEN);
        topRow.add(hdr, BorderLayout.NORTH);
        topRow.add(ratingBigLabel, BorderLayout.WEST);

        streakLabel.setFont(Theme.FONT_SMALL_BOLD);
        streakLabel.setForeground(Theme.TEXT_SECONDARY);
        statsCard.add(topRow, BorderLayout.NORTH);
        statsCard.add(streakLabel, BorderLayout.SOUTH);

        // 2. Database & Filter Card
        JPanel filterCard = Theme.makeCardPanel(new GridBagLayout());
        filterCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.insets = new Insets(3, 0, 3, 0);

        JLabel dbTitle = new JLabel("Puzzle Database & Filters");
        dbTitle.setFont(Theme.FONT_HEADER);
        gc.gridy = 0;
        filterCard.add(dbTitle, gc);

        gc.gridy = 1;
        filterCard.add(makeField("Puzzle Database Source", sourceCombo), gc);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 8, 0));
        row2.setOpaque(false);
        row2.add(makeField("Tactical Theme", themeCombo));
        row2.add(makeField("Difficulty", diffCombo));
        gc.gridy = 2;
        filterCard.add(row2, gc);

        JButton fetchBtn = Theme.makeBlueButton("Fetch Matching Puzzle");
        fetchBtn.addActionListener(e -> loadNextPuzzle());
        gc.gridy = 3;
        gc.insets = new Insets(6, 0, 2, 0);
        filterCard.add(fetchBtn, gc);

        // 3. Current Puzzle Details Card
        JPanel infoCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        infoCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel metaStack = new JPanel();
        metaStack.setLayout(new BoxLayout(metaStack, BoxLayout.Y_AXIS));
        metaStack.setOpaque(false);

        puzzleTitleLabel.setFont(Theme.FONT_HEADER);
        puzzleTitleLabel.setForeground(Theme.TEXT_PRIMARY);
        puzzleMetaLabel.setFont(Theme.FONT_SMALL_BOLD);
        puzzleMetaLabel.setForeground(Theme.ACCENT_AMBER);
        puzzlePlayersLabel.setFont(Theme.FONT_SMALL);
        puzzlePlayersLabel.setForeground(Theme.TEXT_SECONDARY);
        puzzleThemesLabel.setFont(Theme.FONT_SMALL);
        puzzleThemesLabel.setForeground(Theme.ACCENT_BLUE);

        metaStack.add(puzzleTitleLabel);
        metaStack.add(Box.createVerticalStrut(4));
        metaStack.add(puzzleMetaLabel);
        metaStack.add(Box.createVerticalStrut(3));
        metaStack.add(puzzlePlayersLabel);
        metaStack.add(Box.createVerticalStrut(4));
        metaStack.add(puzzleThemesLabel);

        moveLogArea.setEditable(false);
        moveLogArea.setLineWrap(true);
        moveLogArea.setWrapStyleWord(true);
        moveLogArea.setBackground(Theme.BG_INPUT);
        moveLogArea.setForeground(Theme.TEXT_PRIMARY);
        moveLogArea.setFont(Theme.FONT_MONO);
        moveLogArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane logScroll = new JScrollPane(moveLogArea);
        Theme.styleScrollPane(logScroll);

        infoCard.add(metaStack, BorderLayout.NORTH);
        infoCard.add(logScroll, BorderLayout.CENTER);

        // 4. Controls Card
        JPanel controls = Theme.makeCardPanel(new BorderLayout(0, 8));
        controls.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel grid = new JPanel(new GridLayout(2, 2, 6, 6));
        grid.setOpaque(false);
        JButton hintBtn = Theme.makeBlueButton("Hint");
        JButton solBtn = Theme.makeSecondaryButton("Show Solution");
        JButton retryBtn = Theme.makeSecondaryButton("Retry");
        JButton analyzeBtn = Theme.makeSecondaryButton("Analyze Stockfish");

        hintBtn.addActionListener(e -> showHint());
        solBtn.addActionListener(e -> revealSolution());
        retryBtn.addActionListener(e -> {
            if (currentPuzzle != null) startPuzzle(currentPuzzle);
        });
        analyzeBtn.addActionListener(e -> {
            if (onOpenAnalysis != null && currentPuzzle != null) {
                onOpenAnalysis.accept(currentState.toFen());
            }
        });

        grid.add(hintBtn);
        grid.add(solBtn);
        grid.add(retryBtn);
        grid.add(analyzeBtn);

        nextBtn.addActionListener(e -> loadNextPuzzle());
        controls.add(grid, BorderLayout.CENTER);
        controls.add(nextBtn, BorderLayout.SOUTH);

        JPanel northGroup = new JPanel(new BorderLayout(0, 10));
        northGroup.setOpaque(false);
        northGroup.add(statsCard, BorderLayout.NORTH);
        northGroup.add(filterCard, BorderLayout.CENTER);

        side.add(northGroup, BorderLayout.NORTH);
        side.add(infoCard, BorderLayout.CENTER);
        side.add(controls, BorderLayout.SOUTH);
        return side;
    }

    private JPanel makeField(String lbl, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(lbl);
        l.setFont(Theme.FONT_SMALL_BOLD);
        l.setForeground(Theme.TEXT_SECONDARY);
        p.add(l, BorderLayout.NORTH);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public void loadNextPuzzle() {
        final int seq = ++loadSeq;
        final String source = (String) sourceCombo.getSelectedItem();
        final String themeName = (String) themeCombo.getSelectedItem();
        final String diffName = (String) diffCombo.getSelectedItem();
        final String themeKey = PuzzleService.LICHESS_THEMES.getOrDefault(themeName, "mix");
        final String diffKey = PuzzleService.LICHESS_DIFFICULTIES.getOrDefault(diffName, "normal");

        promptBanner.setForeground(Theme.ACCENT_AMBER);
        promptBanner.setText("Fetching puzzle from " + source + "...");
        deltaBadge.setText(" ");
        boardPanel.setInteractive(false);

        loaderExec.submit(() -> {
            Puzzle p = puzzleService.fetchNextPuzzle(source, themeKey, diffKey);
            SwingUtilities.invokeLater(() -> {
                if (seq != loadSeq) return;
                if (p != null) {
                    startPuzzle(p);
                } else {
                    promptBanner.setForeground(Theme.ACCENT_RED);
                    promptBanner.setText("Could not load puzzle. Please try again.");
                }
            });
        });
    }

    private void startPuzzle(Puzzle puzzle) {
        this.currentPuzzle = puzzle;
        this.solutionIdx = 0;
        this.puzzleFinished = false;
        this.failedThisPuzzle = false;
        this.ratingRecorded = false;
        this.hintStage = 0;
        this.solvedSanMoves.clear();

        BoardState targetStart = BoardState.fromFen(puzzle.startFen);
        this.playerColor = targetStart.getSideToMove();
        boardPanel.setFlipped(playerColor == Color.BLACK);
        boardPanel.setAllowedMoveColor(playerColor);

        puzzleTitleLabel.setText(puzzle.title);
        puzzleMetaLabel.setText(puzzle.source + "  •  Rating: " + puzzle.rating + "  •  #" + puzzle.id);
        puzzlePlayersLabel.setText(puzzle.playersInfo.isEmpty() ? "Verified Tactical Position" : puzzle.playersInfo);
        puzzleThemesLabel.setText("Themes: " + String.join(" • ", puzzle.themes.subList(0, Math.min(4, puzzle.themes.size()))));
        moveLogArea.setText("");
        deltaBadge.setText(" ");

        // If we have the position BEFORE the opponent's setup blunder, animate that move onto the board like Lichess!
        if (puzzle.fenBeforeSetup != null && puzzle.setupMoveUci != null) {
            BoardState before = BoardState.fromFen(puzzle.fenBeforeSetup);
            Move setupMove = before.parseUci(puzzle.setupMoveUci);
            if (setupMove != null) {
                currentState = before;
                boardPanel.setBoardState(before, -1, -1, false);
                boardPanel.setInteractive(false);
                promptBanner.setForeground(Theme.TEXT_SECONDARY);
                promptBanner.setText("Opponent played " + before.moveToSan(setupMove) + "...");

                int seq = loadSeq;
                javax.swing.Timer t = new javax.swing.Timer(420, e -> {
                    if (seq != loadSeq) return;
                    currentState = targetStart;
                    boardPanel.setBoardState(currentState, setupMove.from, setupMove.to, true);
                    boardPanel.setInteractive(true);
                    sound.playMove();
                    setYourTurnPrompt();
                });
                t.setRepeats(false);
                t.start();
                return;
            }
        }

        // Otherwise show startFen directly (highlighting lastMove if available)
        int lf = -1, lt = -1;
        if (puzzle.setupMoveUci != null && puzzle.setupMoveUci.length() >= 4) {
            lf = BoardState.algebraicToSquare(puzzle.setupMoveUci.substring(0, 2));
            lt = BoardState.algebraicToSquare(puzzle.setupMoveUci.substring(2, 4));
        }
        currentState = targetStart;
        boardPanel.setBoardState(currentState, lf, lt, false);
        boardPanel.setInteractive(true);
        setYourTurnPrompt();
    }

    private void setYourTurnPrompt() {
        promptBanner.setForeground(Theme.TEXT_PRIMARY);
        promptBanner.setText("Find the best move for " + playerColor.displayName() + "!");
    }

    private void handlePlayerMove(Move move) {
        if (puzzleFinished || currentPuzzle == null || solutionIdx >= currentPuzzle.solutionUci.size()) return;

        String expectedUci = currentPuzzle.solutionUci.get(solutionIdx);
        BoardState afterMove = currentState.applyMove(move);

        // Accept exact solution move OR any immediate checkmate!
        boolean isCorrect = move.matchesUci(expectedUci)
                || (afterMove.getGameStatus() == GameStatus.WHITE_WINS_CHECKMATE
                || afterMove.getGameStatus() == GameStatus.BLACK_WINS_CHECKMATE);

        if (!isCorrect) {
            sound.playError();
            boardPanel.flashErrorSquare(move.to);
            failedThisPuzzle = true;
            if (!ratingRecorded) {
                ratingRecorded = true;
                int delta = profile.recordPuzzleResult(currentPuzzle.rating, false);
                deltaBadge.setForeground(Theme.ACCENT_RED);
                deltaBadge.setText(delta + " Elo");
                refreshProfileLabels();
            }
            promptBanner.setForeground(Theme.ACCENT_RED);
            promptBanner.setText("Not quite! Try another move or click Hint.");
            return;
        }

        // Correct move!
        String san = currentState.moveToSan(move);
        solvedSanMoves.add(san);
        updateMoveLog();

        currentState = afterMove;
        boardPanel.setBoardState(currentState, move.from, move.to, false);
        hintStage = 0;

        if (currentState.isInCheck()) sound.playCheck();
        else if (move.isCapture) sound.playCapture();
        else sound.playMove();

        solutionIdx++;

        // Check if puzzle is completely solved
        boolean isMate = currentState.getGameStatus() == GameStatus.WHITE_WINS_CHECKMATE
                || currentState.getGameStatus() == GameStatus.BLACK_WINS_CHECKMATE;
        if (isMate || solutionIdx >= currentPuzzle.solutionUci.size()) {
            completePuzzle();
            return;
        }

        // Play opponent's automatic reply after short visual pause
        boardPanel.setInteractive(false);
        promptBanner.setForeground(Theme.ACCENT_GREEN);
        promptBanner.setText("Good move! Keep going...");

        int seq = loadSeq;
        String oppUci = currentPuzzle.solutionUci.get(solutionIdx);
        javax.swing.Timer replyTimer = new javax.swing.Timer(360, e -> {
            if (seq != loadSeq) return;
            Move oppMove = currentState.parseUci(oppUci);
            if (oppMove == null) {
                completePuzzle();
                return;
            }
            solvedSanMoves.add(currentState.moveToSan(oppMove));
            updateMoveLog();
            currentState = currentState.applyMove(oppMove);
            boardPanel.setBoardState(currentState, oppMove.from, oppMove.to, true);
            sound.playMove();
            solutionIdx++;

            if (solutionIdx >= currentPuzzle.solutionUci.size()) {
                completePuzzle();
            } else {
                boardPanel.setInteractive(true);
                promptBanner.setForeground(Theme.ACCENT_GREEN);
                promptBanner.setText("Great! Find the winning follow-up for " + playerColor.displayName() + ":");
            }
        });
        replyTimer.setRepeats(false);
        replyTimer.start();
    }

    private void completePuzzle() {
        puzzleFinished = true;
        boardPanel.setInteractive(false);
        sound.playSuccess();

        if (!ratingRecorded) {
            ratingRecorded = true;
            int delta = profile.recordPuzzleResult(currentPuzzle.rating, !failedThisPuzzle);
            deltaBadge.setForeground(delta >= 0 ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
            deltaBadge.setText((delta >= 0 ? "+" : "") + delta + " Elo");
            refreshProfileLabels();
        }

        promptBanner.setForeground(Theme.ACCENT_GREEN);
        promptBanner.setText(failedThisPuzzle
                ? "Puzzle Completed! Click Next Puzzle to continue."
                : "Brilliant! Puzzle Solved Cleanly!");
    }

    private void showHint() {
        if (puzzleFinished || currentPuzzle == null || solutionIdx >= currentPuzzle.solutionUci.size()) return;
        Move m = currentState.parseUci(currentPuzzle.solutionUci.get(solutionIdx));
        if (m == null) return;
        failedThisPuzzle = true;
        hintStage++;
        if (hintStage == 1) {
            boardPanel.setHintSquare(m.from);
            promptBanner.setForeground(Theme.ACCENT_BLUE);
            promptBanner.setText("Hint: Look closely at the highlighted piece on " + BoardState.squareToAlgebraic(m.from) + ".");
        } else {
            boardPanel.setHintArrow(m.from, m.to, new java.awt.Color(0x4C, 0xAF, 0x50, 220));
            promptBanner.setForeground(Theme.ACCENT_BLUE);
            promptBanner.setText("Hint: Play " + currentState.moveToSan(m) + "!");
        }
    }

    private void revealSolution() {
        if (currentPuzzle == null || puzzleFinished) return;
        failedThisPuzzle = true;
        if (!ratingRecorded) {
            ratingRecorded = true;
            int delta = profile.recordPuzzleResult(currentPuzzle.rating, false);
            deltaBadge.setForeground(Theme.ACCENT_RED);
            deltaBadge.setText(delta + " Elo");
            refreshProfileLabels();
        }
        puzzleFinished = true;
        boardPanel.setInteractive(false);

        int seq = loadSeq;
        javax.swing.Timer stepTimer = new javax.swing.Timer(550, null);
        stepTimer.addActionListener(e -> {
            if (seq != loadSeq || solutionIdx >= currentPuzzle.solutionUci.size()) {
                stepTimer.stop();
                promptBanner.setForeground(Theme.ACCENT_AMBER);
                promptBanner.setText("Solution shown. Click Next Puzzle for a fresh challenge!");
                return;
            }
            Move m = currentState.parseUci(currentPuzzle.solutionUci.get(solutionIdx));
            if (m == null) {
                stepTimer.stop();
                return;
            }
            solvedSanMoves.add(currentState.moveToSan(m));
            updateMoveLog();
            currentState = currentState.applyMove(m);
            boardPanel.setBoardState(currentState, m.from, m.to, true);
            sound.playMove();
            solutionIdx++;
        });
        stepTimer.setInitialDelay(100);
        stepTimer.start();
    }

    private void updateMoveLog() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solvedSanMoves.size(); i++) {
            if (i > 0) sb.append("  ");
            sb.append((i + 1)).append(". ").append(solvedSanMoves.get(i));
        }
        moveLogArea.setText(sb.toString());
    }

    private void refreshProfileLabels() {
        ratingBigLabel.setText(String.valueOf(profile.puzzleRating));
        streakLabel.setText("Streak: " + profile.currentStreak
                + "   •   Best: " + profile.bestStreak
                + "   •   Solved: " + profile.puzzlesSolved);
    }

    public void shutdown() {
        loaderExec.shutdownNow();
    }
}
