package com.chessmaster.ui;

import com.chessmaster.data.UserProfile;
import com.chessmaster.engine.StockfishEngine;
import com.chessmaster.model.*;
import com.chessmaster.model.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Play vs. Stockfish mode with adjustable Elo (400 - 3200 / Full Strength),
 * time controls with increment, live Eval Bar, hints, takebacks, move history review, and clocks.
 */
public final class PlayStockfishPanel extends JPanel {

    private static final class TimeControl {
        final String label;
        final int initialSeconds;
        final int incrementSeconds;

        TimeControl(String label, int initialSeconds, int incrementSeconds) {
            this.label = label;
            this.initialSeconds = initialSeconds;
            this.incrementSeconds = incrementSeconds;
        }

        @Override public String toString() { return label; }
    }

    private static final List<TimeControl> TIME_CONTROLS = List.of(
        new TimeControl("Unlimited (No Clock)", 0, 0),
        new TimeControl("Bullet 1+0", 60, 0),
        new TimeControl("Blitz 3+2", 180, 2),
        new TimeControl("Blitz 5+0", 300, 0),
        new TimeControl("Rapid 10+0", 600, 0),
        new TimeControl("Rapid 15+10", 900, 10)
    );

    private final StockfishEngine engine;
    private final SoundManager sound;
    private final UserProfile profile;
    private final Consumer<String> onOpenAnalysis;

    private final ChessBoardPanel boardPanel = new ChessBoardPanel();
    private final EvalBarPanel evalBar = new EvalBarPanel();

    // Game history (index 0 = starting position, index k = after k plies)
    private final List<BoardState> historyStates = new ArrayList<>();
    private final List<Move> historyMoves = new ArrayList<>();
    private final List<String> historySan = new ArrayList<>();
    private int viewPly = 0;

    private Color humanColor = Color.WHITE;
    private GameStatus status = GameStatus.IN_PROGRESS;
    private boolean engineThinking = false;
    private int gameGeneration = 0;

    // Clocks
    private boolean timedGame = false;
    private long whiteMs = 0;
    private long blackMs = 0;
    private int incrementMs = 0;
    private long lastTickMs = 0;
    private final javax.swing.Timer clockTimer;

    // UI widgets
    private final JLabel topNameLabel = new JLabel("Stockfish 19");
    private final JLabel topCapLabel = new JLabel("");
    private final JLabel topClockLabel = new JLabel("--:--");
    private final JLabel bottomNameLabel = new JLabel("You");
    private final JLabel bottomCapLabel = new JLabel("");
    private final JLabel bottomClockLabel = new JLabel("--:--");
    private final JLabel statusBanner = new JLabel("Ready to play");

    private final JComboBox<StockfishEngine.DifficultyPreset> presetCombo;
    private final JSlider eloSlider = new JSlider(400, 3200, 1350);
    private final JLabel eloValueLabel = new JLabel("Elo: 1350");
    private final JCheckBox maxStrengthCheck = new JCheckBox("Full Stockfish 19 Strength (MAX)");
    private final JComboBox<String> sideCombo = new JComboBox<>(new String[]{"White", "Random", "Black"});
    private final JComboBox<TimeControl> timeCombo;
    private final JCheckBox showEvalCheck = new JCheckBox("Show Live Evaluation Bar", true);

    private final DefaultTableModel moveTableModel;
    private final JTable moveTable;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PlayEngineWorker");
        t.setDaemon(true);
        return t;
    });

    public PlayStockfishPanel(StockfishEngine engine, SoundManager sound, UserProfile profile,
                              Consumer<String> onOpenAnalysis) {
        this.engine = engine;
        this.sound = sound;
        this.profile = profile;
        this.onOpenAnalysis = onOpenAnalysis;

        setLayout(new BorderLayout(16, 0));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(14, 18, 14, 18));

        presetCombo = new JComboBox<>(StockfishEngine.PRESETS.toArray(new StockfishEngine.DifficultyPreset[0]));
        presetCombo.setSelectedIndex(3); // Level 4 Club 1350
        Theme.styleComboBox(presetCombo);

        timeCombo = new JComboBox<>(TIME_CONTROLS.toArray(new TimeControl[0]));
        timeCombo.setSelectedIndex(4); // Rapid 10+0
        Theme.styleComboBox(timeCombo);
        Theme.styleComboBox(sideCombo);

        moveTableModel = new DefaultTableModel(new String[]{"#", "White", "Black"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        moveTable = new JTable(moveTableModel);

        add(buildBoardArea(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        boardPanel.setMoveListener(this::handleHumanMove);

        clockTimer = new javax.swing.Timer(100, e -> tickClock());
        startNewGame(BoardState.START_FEN);
    }

    public ChessBoardPanel getBoardPanel() {
        return boardPanel;
    }

    private JPanel buildBoardArea() {
        JPanel wrapper = new JPanel(new BorderLayout(10, 8));
        wrapper.setOpaque(false);

        JPanel topBar = buildPlayerBar(topNameLabel, topCapLabel, topClockLabel);
        JPanel bottomBar = buildPlayerBar(bottomNameLabel, bottomCapLabel, bottomClockLabel);

        JPanel centerBox = new JPanel(new BorderLayout(10, 0));
        centerBox.setOpaque(false);
        centerBox.add(evalBar, BorderLayout.WEST);
        centerBox.add(boardPanel, BorderLayout.CENTER);

        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(centerBox, BorderLayout.CENTER);
        wrapper.add(bottomBar, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildPlayerBar(JLabel nameLbl, JLabel capLbl, JLabel clockLbl) {
        JPanel bar = Theme.makeCardPanel(new BorderLayout(12, 0));
        bar.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        nameLbl.setFont(Theme.FONT_HEADER);
        nameLbl.setForeground(Theme.TEXT_PRIMARY);
        capLbl.setFont(Theme.FONT_BODY);
        capLbl.setForeground(Theme.TEXT_SECONDARY);
        left.add(nameLbl);
        left.add(capLbl);

        clockLbl.setFont(Theme.FONT_CLOCK);
        clockLbl.setForeground(Theme.TEXT_PRIMARY);
        clockLbl.setOpaque(true);
        clockLbl.setBackground(Theme.BG_INPUT);
        clockLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(3, 12, 3, 12)));

        bar.add(left, BorderLayout.WEST);
        bar.add(clockLbl, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(385, 650));

        // 1. Setup Card
        JPanel setupCard = Theme.makeCardPanel(new GridBagLayout());
        setupCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.insets = new Insets(3, 0, 3, 0);

        JLabel title = new JLabel("Play vs. Stockfish");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.TEXT_PRIMARY);
        gc.gridy = 0;
        setupCard.add(title, gc);

        JLabel engineBadge = new JLabel(engine.isStockfishReady()
                ? "Engine: " + engine.getEngineName() + " (UCI Active)"
                : "Engine: Built-in Fallback");
        engineBadge.setFont(Theme.FONT_SMALL);
        engineBadge.setForeground(engine.isStockfishReady() ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
        gc.gridy = 1;
        setupCard.add(engineBadge, gc);

        gc.gridy = 2;
        setupCard.add(makeFieldRow("Difficulty Preset", presetCombo), gc);

        // Elo slider row
        JPanel eloBox = new JPanel(new BorderLayout(6, 2));
        eloBox.setOpaque(false);
        eloValueLabel.setFont(Theme.FONT_SMALL_BOLD);
        eloValueLabel.setForeground(Theme.ACCENT_GREEN);
        eloSlider.setOpaque(false);
        eloSlider.setMajorTickSpacing(700);
        eloSlider.setMinorTickSpacing(100);
        eloSlider.setFocusable(false);
        eloBox.add(eloValueLabel, BorderLayout.NORTH);
        eloBox.add(eloSlider, BorderLayout.CENTER);
        gc.gridy = 3;
        setupCard.add(eloBox, gc);

        maxStrengthCheck.setOpaque(false);
        maxStrengthCheck.setForeground(Theme.TEXT_SECONDARY);
        maxStrengthCheck.setFont(Theme.FONT_SMALL);
        maxStrengthCheck.setFocusable(false);
        gc.gridy = 4;
        setupCard.add(maxStrengthCheck, gc);

        // Sync preset <-> slider <-> maxStrength
        final boolean[] syncing = {false};
        presetCombo.addActionListener(e -> {
            if (syncing[0]) return;
            syncing[0] = true;
            StockfishEngine.DifficultyPreset p = (StockfishEngine.DifficultyPreset) presetCombo.getSelectedItem();
            if (p != null) {
                maxStrengthCheck.setSelected(p.fullStrength);
                eloSlider.setEnabled(!p.fullStrength);
                eloSlider.setValue(Math.min(3200, p.elo));
                eloValueLabel.setText(p.fullStrength ? "Elo: MAX (~3600+)" : "Custom Elo: " + p.elo);
            }
            syncing[0] = false;
        });

        eloSlider.addChangeListener(e -> {
            if (syncing[0]) return;
            eloValueLabel.setText("Custom Elo: " + eloSlider.getValue());
        });

        maxStrengthCheck.addActionListener(e -> {
            boolean max = maxStrengthCheck.isSelected();
            eloSlider.setEnabled(!max);
            eloValueLabel.setText(max ? "Elo: MAX (~3600+)" : "Custom Elo: " + eloSlider.getValue());
        });

        JPanel twoCol = new JPanel(new GridLayout(1, 2, 8, 0));
        twoCol.setOpaque(false);
        twoCol.add(makeFieldRow("Play As", sideCombo));
        twoCol.add(makeFieldRow("Time Control", timeCombo));
        gc.gridy = 5;
        setupCard.add(twoCol, gc);

        showEvalCheck.setOpaque(false);
        showEvalCheck.setForeground(Theme.TEXT_SECONDARY);
        showEvalCheck.setFont(Theme.FONT_SMALL);
        showEvalCheck.setFocusable(false);
        showEvalCheck.addActionListener(e -> evalBar.setVisible(showEvalCheck.isSelected()));
        gc.gridy = 6;
        setupCard.add(showEvalCheck, gc);

        JButton newGameBtn = Theme.makePrimaryButton("Start New Game");
        newGameBtn.addActionListener(e -> startNewGame(BoardState.START_FEN));
        gc.gridy = 7;
        gc.insets = new Insets(6, 0, 2, 0);
        setupCard.add(newGameBtn, gc);

        // 2. Status + Move Table Card
        JPanel midCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        midCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        statusBanner.setFont(Theme.FONT_BODY_BOLD);
        statusBanner.setForeground(Theme.ACCENT_GREEN);
        statusBanner.setHorizontalAlignment(SwingConstants.CENTER);
        statusBanner.setOpaque(true);
        statusBanner.setBackground(Theme.BG_CARD);
        statusBanner.setBorder(new EmptyBorder(7, 10, 7, 10));
        midCard.add(statusBanner, BorderLayout.NORTH);

        styleMoveTable();
        JScrollPane tableScroll = new JScrollPane(moveTable);
        Theme.styleScrollPane(tableScroll);
        midCard.add(tableScroll, BorderLayout.CENTER);

        // History navigation bar
        JPanel navBar = new JPanel(new GridLayout(1, 4, 6, 0));
        navBar.setOpaque(false);
        JButton bFirst = Theme.makeSecondaryButton("|<");
        JButton bPrev = Theme.makeSecondaryButton("<");
        JButton bNext = Theme.makeSecondaryButton(">");
        JButton bLast = Theme.makeSecondaryButton(">|");
        bFirst.addActionListener(e -> jumpToPly(0));
        bPrev.addActionListener(e -> jumpToPly(viewPly - 1));
        bNext.addActionListener(e -> jumpToPly(viewPly + 1));
        bLast.addActionListener(e -> jumpToPly(historyStates.size() - 1));
        navBar.add(bFirst);
        navBar.add(bPrev);
        navBar.add(bNext);
        navBar.add(bLast);
        midCard.add(navBar, BorderLayout.SOUTH);

        // 3. Action Buttons Card
        JPanel actionCard = Theme.makeCardPanel(new GridLayout(2, 3, 6, 6));
        actionCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton hintBtn = Theme.makeBlueButton("Hint");
        JButton undoBtn = Theme.makeSecondaryButton("Takeback");
        JButton flipBtn = Theme.makeSecondaryButton("Flip");
        JButton analyzeBtn = Theme.makeSecondaryButton("Analyze");
        JButton fenBtn = Theme.makeSecondaryButton("Copy FEN");
        JButton resignBtn = Theme.makeDangerButton("Resign");

        hintBtn.addActionListener(e -> requestHint());
        undoBtn.addActionListener(e -> performTakeback());
        flipBtn.addActionListener(e -> flipBoard());
        analyzeBtn.addActionListener(e -> {
            if (onOpenAnalysis != null && !historyStates.isEmpty()) {
                onOpenAnalysis.accept(historyStates.get(viewPly).toFen());
            }
        });
        fenBtn.addActionListener(e -> {
            if (!historyStates.isEmpty()) {
                String fen = historyStates.get(viewPly).toFen();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fen), null);
                statusBanner.setText("FEN copied to clipboard!");
            }
        });
        resignBtn.addActionListener(e -> resignGame());

        actionCard.add(hintBtn);
        actionCard.add(undoBtn);
        actionCard.add(flipBtn);
        actionCard.add(analyzeBtn);
        actionCard.add(fenBtn);
        actionCard.add(resignBtn);

        side.add(setupCard, BorderLayout.NORTH);
        side.add(midCard, BorderLayout.CENTER);
        side.add(actionCard, BorderLayout.SOUTH);
        return side;
    }

    private JPanel makeFieldRow(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(labelText);
        l.setFont(Theme.FONT_SMALL_BOLD);
        l.setForeground(Theme.TEXT_SECONDARY);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void styleMoveTable() {
        moveTable.setBackground(Theme.BG_SURFACE);
        moveTable.setForeground(Theme.TEXT_PRIMARY);
        moveTable.setFont(Theme.FONT_MONO);
        moveTable.setRowHeight(26);
        moveTable.setGridColor(Theme.BG_CARD);
        moveTable.setSelectionBackground(Theme.BG_ELEVATED);
        moveTable.setSelectionForeground(Theme.TEXT_PRIMARY);
        moveTable.getTableHeader().setBackground(Theme.BG_CARD);
        moveTable.getTableHeader().setForeground(Theme.TEXT_SECONDARY);
        moveTable.getTableHeader().setFont(Theme.FONT_SMALL_BOLD);
        moveTable.getColumnModel().getColumn(0).setMaxWidth(42);
        moveTable.setCellSelectionEnabled(true);

        DefaultTableCellRenderer centerNum = new DefaultTableCellRenderer();
        centerNum.setHorizontalAlignment(SwingConstants.CENTER);
        centerNum.setBackground(Theme.BG_CARD);
        centerNum.setForeground(Theme.TEXT_MUTED);
        moveTable.getColumnModel().getColumn(0).setCellRenderer(centerNum);

        moveTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = moveTable.rowAtPoint(e.getPoint());
                int col = moveTable.columnAtPoint(e.getPoint());
                if (row >= 0 && (col == 1 || col == 2)) {
                    int ply = row * 2 + col;
                    if (ply < historyStates.size()) jumpToPly(ply);
                }
            }
        });
    }

    public void startNewGame(String startFen) {
        gameGeneration++;
        engineThinking = false;
        clockTimer.stop();
        engine.newGame();

        String sideStr = (String) sideCombo.getSelectedItem();
        if ("Black".equals(sideStr)) humanColor = Color.BLACK;
        else if ("Random".equals(sideStr)) humanColor = new Random().nextBoolean() ? Color.WHITE : Color.BLACK;
        else humanColor = Color.WHITE;

        TimeControl tc = (TimeControl) timeCombo.getSelectedItem();
        timedGame = tc != null && tc.initialSeconds > 0;
        whiteMs = timedGame ? tc.initialSeconds * 1000L : 0;
        blackMs = timedGame ? tc.initialSeconds * 1000L : 0;
        incrementMs = timedGame ? tc.incrementSeconds * 1000 : 0;
        lastTickMs = System.currentTimeMillis();

        historyStates.clear();
        historyMoves.clear();
        historySan.clear();
        BoardState initial = BoardState.fromFen(startFen);
        historyStates.add(initial);
        viewPly = 0;
        status = initial.getGameStatus();

        boolean flip = (humanColor == Color.BLACK);
        boardPanel.setFlipped(flip);
        evalBar.setFlipped(flip);
        evalBar.reset();

        rebuildMoveTable();
        updateBoardAndLabels(false);

        if (timedGame) clockTimer.start();

        if (!status.isGameOver() && initial.getSideToMove() != humanColor) {
            triggerEngineMove();
        } else {
            refreshEvalAsync(initial, gameGeneration);
        }
    }

    private void handleHumanMove(Move move) {
        if (status.isGameOver() || engineThinking) return;
        if (viewPly != historyStates.size() - 1) {
            jumpToPly(historyStates.size() - 1);
            return;
        }
        BoardState current = historyStates.get(historyStates.size() - 1);
        if (current.getSideToMove() != humanColor) return;

        commitMove(move, false);

        if (!status.isGameOver()) {
            triggerEngineMove();
        }
    }

    private void commitMove(Move move, boolean animate) {
        BoardState prev = historyStates.get(historyStates.size() - 1);
        String san = prev.moveToSan(move);
        BoardState next = prev.applyMove(move);

        if (timedGame) {
            if (prev.getSideToMove() == Color.WHITE) whiteMs += incrementMs;
            else blackMs += incrementMs;
            lastTickMs = System.currentTimeMillis();
        }

        historyMoves.add(move);
        historySan.add(san);
        historyStates.add(next);
        viewPly = historyStates.size() - 1;
        status = next.getGameStatus();

        // Sound effects
        if (next.isInCheck()) sound.playCheck();
        else if (move.isCastleKingside || move.isCastleQueenside) sound.playCastle();
        else if (move.isCapture) sound.playCapture();
        else sound.playMove();

        rebuildMoveTable();
        updateBoardAndLabels(animate);

        if (status.isGameOver()) {
            clockTimer.stop();
            profile.gamesPlayed++;
            if ((status == GameStatus.WHITE_WINS_CHECKMATE && humanColor == Color.WHITE)
                    || (status == GameStatus.BLACK_WINS_CHECKMATE && humanColor == Color.BLACK)) {
                profile.gamesWon++;
                sound.playSuccess();
            }
            profile.save();
        }

        refreshEvalAsync(next, gameGeneration);
    }

    private void triggerEngineMove() {
        final int gen = gameGeneration;
        final BoardState pos = historyStates.get(historyStates.size() - 1);
        final int elo = eloSlider.getValue();
        final boolean full = maxStrengthCheck.isSelected();

        engineThinking = true;
        boardPanel.setInteractive(false);
        statusBanner.setForeground(Theme.ACCENT_AMBER);
        statusBanner.setText(engine.getEngineName() + " is thinking...");

        worker.submit(() -> {
            String uci = engine.computeMove(pos, elo, full);
            SwingUtilities.invokeLater(() -> {
                if (gen != gameGeneration || status.isGameOver()) {
                    engineThinking = false;
                    return;
                }
                engineThinking = false;
                if (uci != null) {
                    Move m = pos.parseUci(uci);
                    if (m != null) {
                        commitMove(m, true);
                    }
                }
                updateBoardAndLabels(false);
            });
        });
    }

    private void refreshEvalAsync(BoardState pos, int gen) {
        if (!showEvalCheck.isSelected()) return;
        worker.submit(() -> {
            StockfishEngine.EvalResult res = engine.analyzePosition(pos, 1, 220);
            SwingUtilities.invokeLater(() -> {
                if (gen == gameGeneration) {
                    evalBar.setEvaluation(res.centipawnsWhite, res.mateInWhite);
                }
            });
        });
    }

    private void requestHint() {
        if (status.isGameOver() || engineThinking) return;
        BoardState pos = historyStates.get(viewPly);
        int gen = gameGeneration;
        statusBanner.setText("Calculating Stockfish hint...");
        worker.submit(() -> {
            StockfishEngine.EvalResult res = engine.analyzePosition(pos, 1, 450);
            SwingUtilities.invokeLater(() -> {
                if (gen != gameGeneration || res.bestMoveUci == null) return;
                Move m = pos.parseUci(res.bestMoveUci);
                if (m != null) {
                    boardPanel.setHintArrow(m.from, m.to, new java.awt.Color(0x4C, 0xAF, 0x50, 215));
                    statusBanner.setForeground(Theme.ACCENT_BLUE);
                    statusBanner.setText("Stockfish recommends: " + pos.moveToSan(m) + " (" + res.formattedScore() + ")");
                }
            });
        });
    }

    private void performTakeback() {
        if (engineThinking || historyStates.size() <= 1) return;
        int removePlies = (historyStates.size() >= 3 && historyStates.get(historyStates.size() - 1).getSideToMove() == humanColor) ? 2 : 1;
        for (int i = 0; i < removePlies && historyStates.size() > 1; i++) {
            historyStates.remove(historyStates.size() - 1);
            historyMoves.remove(historyMoves.size() - 1);
            historySan.remove(historySan.size() - 1);
        }
        viewPly = historyStates.size() - 1;
        status = historyStates.get(viewPly).getGameStatus();
        rebuildMoveTable();
        updateBoardAndLabels(false);
        refreshEvalAsync(historyStates.get(viewPly), gameGeneration);
    }

    private void resignGame() {
        if (status.isGameOver()) return;
        status = humanColor == Color.WHITE ? GameStatus.BLACK_WINS_RESIGNATION : GameStatus.WHITE_WINS_RESIGNATION;
        clockTimer.stop();
        profile.gamesPlayed++;
        profile.save();
        updateBoardAndLabels(false);
    }

    public void flipBoard() {
        boolean f = !boardPanel.isFlipped();
        boardPanel.setFlipped(f);
        evalBar.setFlipped(f);
        updatePlayerBars();
    }

    private void jumpToPly(int ply) {
        if (ply < 0 || ply >= historyStates.size()) return;
        viewPly = ply;
        updateBoardAndLabels(false);
    }

    private void updateBoardAndLabels(boolean animate) {
        BoardState st = historyStates.get(viewPly);
        int lFrom = viewPly > 0 ? historyMoves.get(viewPly - 1).from : -1;
        int lTo = viewPly > 0 ? historyMoves.get(viewPly - 1).to : -1;

        boardPanel.setBoardState(st, lFrom, lTo, animate);
        boolean canPlay = !status.isGameOver() && !engineThinking && viewPly == historyStates.size() - 1
                && st.getSideToMove() == humanColor;
        boardPanel.setInteractive(canPlay);
        boardPanel.setAllowedMoveColor(humanColor);

        if (status.isGameOver()) {
            statusBanner.setForeground(Theme.ACCENT_AMBER);
            statusBanner.setText(status.description());
        } else if (viewPly != historyStates.size() - 1) {
            statusBanner.setForeground(Theme.ACCENT_BLUE);
            statusBanner.setText("Reviewing move " + viewPly + " / " + (historyStates.size() - 1));
        } else if (engineThinking) {
            statusBanner.setForeground(Theme.ACCENT_AMBER);
            statusBanner.setText(engine.getEngineName() + " thinking...");
        } else {
            statusBanner.setForeground(Theme.ACCENT_GREEN);
            statusBanner.setText("Your move (" + humanColor.displayName() + ")"
                    + (st.isInCheck() ? " — CHECK!" : ""));
        }

        updatePlayerBars();
    }

    private void updatePlayerBars() {
        BoardState st = historyStates.get(viewPly);
        Color bottomColor = boardPanel.isFlipped() ? Color.BLACK : Color.WHITE;
        Color topColor = bottomColor.opposite();

        String sfTag = maxStrengthCheck.isSelected() ? "Stockfish 19 (MAX)" : "Stockfish (" + eloSlider.getValue() + " Elo)";
        bottomNameLabel.setText(bottomColor == humanColor ? "You (" + bottomColor.displayName() + ")" : sfTag);
        topNameLabel.setText(topColor == humanColor ? "You (" + topColor.displayName() + ")" : sfTag);

        int matWhite = st.materialBalanceWhite();
        int topAdv = topColor == Color.WHITE ? matWhite : -matWhite;
        int botAdv = bottomColor == Color.WHITE ? matWhite : -matWhite;

        topCapLabel.setText(formatCaptured(st.getCapturedPiecesOf(topColor.opposite()), topAdv));
        bottomCapLabel.setText(formatCaptured(st.getCapturedPiecesOf(bottomColor.opposite()), botAdv));

        topClockLabel.setText(timedGame ? formatClock(topColor == Color.WHITE ? whiteMs : blackMs) : "∞");
        bottomClockLabel.setText(timedGame ? formatClock(bottomColor == Color.WHITE ? whiteMs : blackMs) : "∞");

        Color active = st.getSideToMove();
        topClockLabel.setForeground(!status.isGameOver() && active == topColor ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED);
        bottomClockLabel.setForeground(!status.isGameOver() && active == bottomColor ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED);
    }

    private static String formatCaptured(List<PieceType> caps, int adv) {
        StringBuilder sb = new StringBuilder();
        for (PieceType pt : caps) {
            sb.append(switch (pt) {
                case QUEEN -> "♛";
                case ROOK -> "♜";
                case BISHOP -> "♝";
                case KNIGHT -> "♞";
                case PAWN -> "♟";
                case KING -> "";
            });
        }
        if (adv > 0) sb.append(" +").append(adv);
        return sb.toString();
    }

    private void tickClock() {
        if (!timedGame || status.isGameOver()) return;
        long now = System.currentTimeMillis();
        long dt = now - lastTickMs;
        lastTickMs = now;
        BoardState latest = historyStates.get(historyStates.size() - 1);
        if (latest.getSideToMove() == Color.WHITE) {
            whiteMs = Math.max(0, whiteMs - dt);
            if (whiteMs == 0) {
                status = GameStatus.BLACK_WINS_TIMEOUT;
                clockTimer.stop();
                updateBoardAndLabels(false);
                return;
            }
        } else {
            blackMs = Math.max(0, blackMs - dt);
            if (blackMs == 0) {
                status = GameStatus.WHITE_WINS_TIMEOUT;
                clockTimer.stop();
                updateBoardAndLabels(false);
                return;
            }
        }
        updatePlayerBars();
    }

    private static String formatClock(long ms) {
        long totalSec = (ms + 999) / 1000;
        long m = totalSec / 60;
        long s = totalSec % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void rebuildMoveTable() {
        moveTableModel.setRowCount(0);
        for (int i = 0; i < historySan.size(); i += 2) {
            String w = historySan.get(i);
            String b = (i + 1 < historySan.size()) ? historySan.get(i + 1) : "";
            moveTableModel.addRow(new Object[]{(i / 2 + 1) + ".", w, b});
        }
        if (moveTable.getRowCount() > 0) {
            moveTable.scrollRectToVisible(moveTable.getCellRect(moveTable.getRowCount() - 1, 0, true));
        }
    }

    public void shutdown() {
        clockTimer.stop();
        worker.shutdownNow();
    }
}
