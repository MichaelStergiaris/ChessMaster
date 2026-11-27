package com.chessmaster.ui;

import com.chessmaster.data.Opening;
import com.chessmaster.data.OpeningDatabase;
import com.chessmaster.data.OpeningExplorerClient;
import com.chessmaster.data.UserProfile;
import com.chessmaster.model.*;
import com.chessmaster.model.Color;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Interactive Opening Studio featuring 40+ of the most popular chess openings:
 *  - Searchable & filterable ECO library (Open, Semi-Open, Closed/QG, Indian, Flank)
 *  - Guided Study Mode with move-by-move coaching commentary & live Lichess Masters Win/Draw/Loss bars
 *  - Interactive Drill Mode testing your memory move-by-move with mastery badges
 *  - One-click launch into Play vs. Stockfish or Analysis Board from any opening position!
 */
public final class OpeningsPanel extends JPanel {

    private final SoundManager sound;
    private final UserProfile profile;
    private final Consumer<String> onPlayFromFen;
    private final Consumer<String> onAnalyzeFen;
    private final OpeningExplorerClient explorerClient = new OpeningExplorerClient();

    private final ChessBoardPanel boardPanel = new ChessBoardPanel();

    // Library list
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> categoryCombo;
    private final JComboBox<String> sideFilterCombo = new JComboBox<>(new String[]{"All Sides", "White Openings", "Black Defenses"});
    private final DefaultListModel<Opening> openingListModel = new DefaultListModel<>();
    private final JList<Opening> openingList = new JList<>(openingListModel);

    // Selected opening & state
    private Opening currentOpening;
    private final List<BoardState> lineStates = new ArrayList<>();
    private final List<Move> lineMoves = new ArrayList<>();
    private int currentPly = 0;

    // Mode: Study vs Drill
    private boolean drillMode = false;
    private int drillMistakes = 0;
    private boolean drillCompleted = false;
    private javax.swing.Timer autoPlayTimer;
    private int explorerSeq = 0;

    // Right panel widgets
    private final JButton studyTabBtn = Theme.makeBlueButton("Study & Explore");
    private final JButton drillTabBtn = Theme.makeSecondaryButton("Practice Drill");
    private final JLabel titleLabel = new JLabel("");
    private final JLabel metaLabel = new JLabel("");
    private final JTextArea summaryArea = new JTextArea(3, 24);
    private final JLabel coachHeaderLabel = new JLabel("Coach Commentary");
    private final JTextArea coachArea = new JTextArea(4, 24);
    private final JTextArea lineMovesArea = new JTextArea(3, 24);
    private final JPanel explorerContainer = new JPanel();
    private final JLabel drillStatusLabel = new JLabel(" ");

    private final ExecutorService explorerExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "OpeningExplorerExec");
        t.setDaemon(true);
        return t;
    });

    public OpeningsPanel(SoundManager sound, UserProfile profile,
                         Consumer<String> onPlayFromFen, Consumer<String> onAnalyzeFen) {
        this.sound = sound;
        this.profile = profile;
        this.onPlayFromFen = onPlayFromFen;
        this.onAnalyzeFen = onAnalyzeFen;

        setLayout(new BorderLayout(14, 0));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(14, 16, 14, 16));

        categoryCombo = new JComboBox<>(OpeningDatabase.getCategories().toArray(new String[0]));
        Theme.styleComboBox(categoryCombo);
        Theme.styleComboBox(sideFilterCombo);

        add(buildLibrarySidebar(), BorderLayout.WEST);
        add(buildCenterBoardArea(), BorderLayout.CENTER);
        add(buildCoachSidebar(), BorderLayout.EAST);

        boardPanel.setMoveListener(this::handleBoardMove);
        populateOpeningList();
        if (!openingListModel.isEmpty()) {
            openingList.setSelectedIndex(0);
        }
    }

    public ChessBoardPanel getBoardPanel() {
        return boardPanel;
    }

    private JPanel buildLibrarySidebar() {
        JPanel card = Theme.makeCardPanel(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(285, 650));
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.insets = new Insets(2, 0, 4, 0);

        JLabel hdr = new JLabel("Opening Library (" + OpeningDatabase.getAll().size() + ")");
        hdr.setFont(Theme.FONT_HEADER);
        gc.gridy = 0;
        top.add(hdr, gc);

        searchField.setBackground(Theme.BG_INPUT);
        searchField.setForeground(Theme.TEXT_PRIMARY);
        searchField.setCaretColor(Theme.TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)));
        searchField.setToolTipText("Search by name, variation, or ECO code...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { populateOpeningList(); }
            @Override public void removeUpdate(DocumentEvent e) { populateOpeningList(); }
            @Override public void changedUpdate(DocumentEvent e) { populateOpeningList(); }
        });
        gc.gridy = 1;
        top.add(searchField, gc);

        categoryCombo.addActionListener(e -> populateOpeningList());
        sideFilterCombo.addActionListener(e -> populateOpeningList());
        gc.gridy = 2;
        top.add(categoryCombo, gc);
        gc.gridy = 3;
        top.add(sideFilterCombo, gc);

        openingList.setBackground(Theme.BG_SURFACE);
        openingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        openingList.setCellRenderer(new OpeningCellRenderer());
        openingList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Opening sel = openingList.getSelectedValue();
                if (sel != null) selectOpening(sel);
            }
        });

        JScrollPane sp = new JScrollPane(openingList);
        Theme.styleScrollPane(sp);

        card.add(top, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCenterBoardArea() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel topStrip = Theme.makeCardPanel(new BorderLayout(10, 0));
        topStrip.setBorder(new EmptyBorder(9, 14, 9, 14));
        drillStatusLabel.setFont(Theme.FONT_HEADER);
        drillStatusLabel.setForeground(Theme.TEXT_PRIMARY);
        topStrip.add(drillStatusLabel, BorderLayout.WEST);

        JPanel modeSwitch = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        modeSwitch.setOpaque(false);
        studyTabBtn.addActionListener(e -> setDrillMode(false));
        drillTabBtn.addActionListener(e -> setDrillMode(true));
        modeSwitch.add(studyTabBtn);
        modeSwitch.add(drillTabBtn);
        topStrip.add(modeSwitch, BorderLayout.EAST);

        JPanel bottomBar = Theme.makeCardPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        JButton bStart = Theme.makeSecondaryButton("|<");
        JButton bPrev = Theme.makeSecondaryButton("< Prev");
        JButton bNext = Theme.makePrimaryButton("Next >");
        JButton bEnd = Theme.makeSecondaryButton(">|");
        JButton bAuto = Theme.makeBlueButton("Auto-Play");
        JButton bHint = Theme.makeSecondaryButton("Hint");
        JButton bFlip = Theme.makeSecondaryButton("Flip");

        bStart.addActionListener(e -> { stopAutoPlay(); if (!drillMode) goToPly(0, false); else restartDrill(); });
        bPrev.addActionListener(e -> { stopAutoPlay(); if (!drillMode) goToPly(currentPly - 1, false); });
        bNext.addActionListener(e -> { stopAutoPlay(); if (!drillMode) goToPly(currentPly + 1, true); });
        bEnd.addActionListener(e -> { stopAutoPlay(); if (!drillMode) goToPly(lineStates.size() - 1, false); });
        bAuto.addActionListener(e -> toggleAutoPlay());
        bHint.addActionListener(e -> showOpeningHint());
        bFlip.addActionListener(e -> boardPanel.setFlipped(!boardPanel.isFlipped()));

        bottomBar.add(bStart);
        bottomBar.add(bPrev);
        bottomBar.add(bNext);
        bottomBar.add(bEnd);
        bottomBar.add(bAuto);
        bottomBar.add(bHint);
        bottomBar.add(bFlip);

        p.add(topStrip, BorderLayout.NORTH);
        p.add(boardPanel, BorderLayout.CENTER);
        p.add(bottomBar, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCoachSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(355, 650));

        // 1. Opening Overview + Coach Card
        JPanel topCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        topCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel hdrBox = new JPanel();
        hdrBox.setLayout(new BoxLayout(hdrBox, BoxLayout.Y_AXIS));
        hdrBox.setOpaque(false);
        titleLabel.setFont(Theme.FONT_HEADER);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        metaLabel.setFont(Theme.FONT_SMALL_BOLD);
        metaLabel.setForeground(Theme.ACCENT_AMBER);
        hdrBox.add(titleLabel);
        hdrBox.add(Box.createVerticalStrut(3));
        hdrBox.add(metaLabel);

        styleReadOnlyArea(summaryArea, Theme.FONT_SMALL, Theme.TEXT_SECONDARY, Theme.BG_SURFACE);
        styleReadOnlyArea(coachArea, Theme.FONT_BODY, Theme.TEXT_PRIMARY, Theme.BG_CARD);
        coachArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_BLUE, 1),
                new EmptyBorder(8, 10, 8, 10)));

        JPanel coachWrap = new JPanel(new BorderLayout(0, 4));
        coachWrap.setOpaque(false);
        coachHeaderLabel.setFont(Theme.FONT_SMALL_BOLD);
        coachHeaderLabel.setForeground(Theme.ACCENT_BLUE);
        coachWrap.add(coachHeaderLabel, BorderLayout.NORTH);
        coachWrap.add(coachArea, BorderLayout.CENTER);

        topCard.add(hdrBox, BorderLayout.NORTH);
        topCard.add(summaryArea, BorderLayout.CENTER);
        topCard.add(coachWrap, BorderLayout.SOUTH);

        // 2. Move Sequence + Master Explorer Card
        JPanel midCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        midCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        styleReadOnlyArea(lineMovesArea, Theme.FONT_MONO, Theme.TEXT_PRIMARY, Theme.BG_INPUT);
        lineMovesArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane moveScroll = new JScrollPane(lineMovesArea);
        Theme.styleScrollPane(moveScroll);
        moveScroll.setPreferredSize(new Dimension(320, 85));

        JPanel explorerWrap = new JPanel(new BorderLayout(0, 4));
        explorerWrap.setOpaque(false);
        JLabel expTitle = new JLabel("Master Database Continuations (Lichess Masters)");
        expTitle.setFont(Theme.FONT_SMALL_BOLD);
        expTitle.setForeground(Theme.TEXT_SECONDARY);
        explorerContainer.setLayout(new BoxLayout(explorerContainer, BoxLayout.Y_AXIS));
        explorerContainer.setBackground(Theme.BG_SURFACE);
        JScrollPane expScroll = new JScrollPane(explorerContainer);
        Theme.styleScrollPane(expScroll);
        explorerWrap.add(expTitle, BorderLayout.NORTH);
        explorerWrap.add(expScroll, BorderLayout.CENTER);

        midCard.add(moveScroll, BorderLayout.NORTH);
        midCard.add(explorerWrap, BorderLayout.CENTER);

        // 3. Action Footer
        JPanel footer = Theme.makeCardPanel(new GridLayout(1, 2, 8, 0));
        footer.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton playHereBtn = Theme.makePrimaryButton("Play vs Stockfish");
        JButton analyzeBtn = Theme.makeSecondaryButton("Analyze Position");

        playHereBtn.addActionListener(e -> {
            if (onPlayFromFen != null && !lineStates.isEmpty()) {
                onPlayFromFen.accept(lineStates.get(currentPly).toFen());
            }
        });
        analyzeBtn.addActionListener(e -> {
            if (onAnalyzeFen != null && !lineStates.isEmpty()) {
                onAnalyzeFen.accept(lineStates.get(currentPly).toFen());
            }
        });
        footer.add(playHereBtn);
        footer.add(analyzeBtn);

        side.add(topCard, BorderLayout.NORTH);
        side.add(midCard, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private void styleReadOnlyArea(JTextArea area, Font font, java.awt.Color fg, java.awt.Color bg) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(font);
        area.setForeground(fg);
        area.setBackground(bg);
    }

    private void populateOpeningList() {
        Opening prev = openingList.getSelectedValue();
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String cat = (String) categoryCombo.getSelectedItem();
        String sideFilter = (String) sideFilterCombo.getSelectedItem();

        openingListModel.clear();
        for (Opening op : OpeningDatabase.getAll()) {
            if (cat != null && !cat.equals(OpeningDatabase.CAT_ALL) && !op.category.equals(cat)) continue;
            if ("White Openings".equals(sideFilter) && op.trainSide != Color.WHITE) continue;
            if ("Black Defenses".equals(sideFilter) && op.trainSide != Color.BLACK) continue;
            if (!query.isEmpty()) {
                String hay = (op.eco + " " + op.name + " " + op.variation + " " + op.summary).toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }
            openingListModel.addElement(op);
        }
        if (prev != null && openingListModel.contains(prev)) {
            openingList.setSelectedValue(prev, true);
        } else if (!openingListModel.isEmpty()) {
            openingList.setSelectedIndex(0);
        }
    }

    private void selectOpening(Opening op) {
        stopAutoPlay();
        this.currentOpening = op;
        lineStates.clear();
        lineMoves.clear();

        BoardState st = new BoardState();
        lineStates.add(st);
        for (String san : op.movesSan) {
            Move m = st.parseSan(san);
            if (m == null) break;
            lineMoves.add(m);
            st = st.applyMove(m);
            lineStates.add(st);
        }

        titleLabel.setText(op.eco + " — " + op.fullTitle());
        boolean mastered = profile.masteredOpenings.contains(op.id);
        metaLabel.setText("Train as: " + op.trainSide.displayName()
                + "  •  " + op.difficulty
                + "  •  Popularity: " + op.popularity
                + (mastered ? "  •  ✓ MASTERED" : ""));
        summaryArea.setText(op.summary);

        boardPanel.setFlipped(op.trainSide == Color.BLACK);

        if (drillMode) {
            restartDrill();
        } else {
            goToPly(Math.min(4, lineStates.size() - 1), false);
        }
    }

    private void setDrillMode(boolean drill) {
        stopAutoPlay();
        this.drillMode = drill;
        studyTabBtn.setForeground(drill ? Theme.TEXT_SECONDARY : java.awt.Color.WHITE);
        drillTabBtn.setForeground(drill ? java.awt.Color.WHITE : Theme.TEXT_SECONDARY);
        if (currentOpening == null) return;
        if (drill) {
            restartDrill();
        } else {
            goToPly(currentPly, false);
        }
    }

    private void restartDrill() {
        if (currentOpening == null) return;
        drillMistakes = 0;
        drillCompleted = false;
        currentPly = 0;
        boardPanel.setFlipped(currentOpening.trainSide == Color.BLACK);
        boardPanel.setBoardState(lineStates.get(0), -1, -1, false);
        boardPanel.setAllowedMoveColor(currentOpening.trainSide);
        updateLineMovesDisplay();

        coachHeaderLabel.setText("Interactive Drill Mode");
        coachArea.setText("Play the book moves for " + currentOpening.trainSide.displayName()
                + " in the " + currentOpening.fullTitle() + ". Use Hint if you get stuck!");

        advanceDrillOpponentIfNeeded();
    }

    private void advanceDrillOpponentIfNeeded() {
        if (!drillMode || currentOpening == null) return;
        if (currentPly >= lineMoves.size()) {
            drillCompleted = true;
            boardPanel.setInteractive(false);
            sound.playSuccess();
            if (drillMistakes == 0) {
                profile.markOpeningMastered(currentOpening.id);
                openingList.repaint();
                metaLabel.setText(metaLabel.getText().contains("MASTERED")
                        ? metaLabel.getText() : metaLabel.getText() + "  •  ✓ MASTERED");
            }
            drillStatusLabel.setForeground(Theme.ACCENT_GREEN);
            drillStatusLabel.setText(drillMistakes == 0
                    ? "Perfect! Line Mastered with 0 mistakes!"
                    : "Drill Complete (" + drillMistakes + " mistake" + (drillMistakes == 1 ? "" : "s") + ")");
            coachArea.setText("Excellent work! You completed the full " + currentOpening.fullTitle()
                    + " repertoire line. Click 'Play vs Stockfish' to practice the resulting middlegame!");
            return;
        }

        BoardState st = lineStates.get(currentPly);
        if (st.getSideToMove() != currentOpening.trainSide) {
            boardPanel.setInteractive(false);
            drillStatusLabel.setForeground(Theme.TEXT_SECONDARY);
            drillStatusLabel.setText("Opponent playing book reply...");
            int plySnap = currentPly;
            javax.swing.Timer t = new javax.swing.Timer(420, e -> {
                if (!drillMode || currentPly != plySnap || currentPly >= lineMoves.size()) return;
                Move m = lineMoves.get(currentPly);
                currentPly++;
                boardPanel.setBoardState(lineStates.get(currentPly), m.from, m.to, true);
                sound.playMove();
                updateLineMovesDisplay();
                advanceDrillOpponentIfNeeded();
            });
            t.setRepeats(false);
            t.start();
        } else {
            boardPanel.setInteractive(true);
            drillStatusLabel.setForeground(Theme.ACCENT_BLUE);
            drillStatusLabel.setText("Drill: Play " + currentOpening.trainSide.displayName()
                    + "'s book move (" + (currentPly + 1) + "/" + lineMoves.size() + ")");
        }
    }

    private void handleBoardMove(Move move) {
        if (currentOpening == null) return;
        if (drillMode) {
            if (drillCompleted || currentPly >= lineMoves.size()) return;
            Move expected = lineMoves.get(currentPly);
            if (move.equals(expected)) {
                String note = currentOpening.getExplanation(currentPly);
                String san = currentOpening.movesSan.get(currentPly);
                currentPly++;
                boardPanel.setBoardState(lineStates.get(currentPly), move.from, move.to, false);
                sound.playMove();
                coachArea.setText("Correct (" + san + ")! " + note);
                updateLineMovesDisplay();
                advanceDrillOpponentIfNeeded();
            } else {
                drillMistakes++;
                sound.playError();
                boardPanel.flashErrorSquare(move.to);
                drillStatusLabel.setForeground(Theme.ACCENT_RED);
                drillStatusLabel.setText("Not the book move! Try again or click Hint.");
            }
        } else {
            // Study mode: if user plays the next book move, step forward cleanly!
            if (currentPly < lineMoves.size() && move.equals(lineMoves.get(currentPly))) {
                goToPly(currentPly + 1, false);
            } else {
                // Allow exploring any move freely and querying Masters explorer
                BoardState next = lineStates.get(currentPly).applyMove(move);
                boardPanel.setBoardState(next, move.from, move.to, false);
                sound.playMove();
                coachArea.setText("Exploring sideline: " + lineStates.get(currentPly).moveToSan(move)
                        + ". Press '< Prev' or 'Next >' to return to the main repertoire line.");
                fetchMastersExplorer(next.toFen());
            }
        }
    }

    private void goToPly(int ply, boolean animate) {
        if (lineStates.isEmpty()) return;
        currentPly = Math.max(0, Math.min(lineStates.size() - 1, ply));
        BoardState st = lineStates.get(currentPly);
        int lf = currentPly > 0 ? lineMoves.get(currentPly - 1).from : -1;
        int lt = currentPly > 0 ? lineMoves.get(currentPly - 1).to : -1;

        boardPanel.setInteractive(true);
        boardPanel.setAllowedMoveColor(null);
        boardPanel.setBoardState(st, lf, lt, animate);
        if (animate && currentPly > 0) sound.playMove();

        // Draw subtle green arrow for the NEXT recommended book move in Study mode
        if (currentPly < lineMoves.size()) {
            Move nextBook = lineMoves.get(currentPly);
            boardPanel.setHintArrow(nextBook.from, nextBook.to, new java.awt.Color(0x4C, 0xAF, 0x50, 155));
        }

        drillStatusLabel.setForeground(Theme.TEXT_PRIMARY);
        drillStatusLabel.setText("Study: " + currentOpening.fullTitle()
                + "  (" + currentPly + " / " + lineMoves.size() + ")");

        coachHeaderLabel.setText(currentPly == 0 ? "Starting Position"
                : "Move " + ((currentPly + 1) / 2) + (currentPly % 2 == 1 ? ". " : "... ")
                + currentOpening.movesSan.get(currentPly - 1));
        coachArea.setText(currentPly == 0
                ? currentOpening.summary
                : currentOpening.getExplanation(currentPly - 1));

        updateLineMovesDisplay();
        fetchMastersExplorer(st.toFen());
    }

    private void showOpeningHint() {
        if (currentOpening == null || currentPly >= lineMoves.size()) return;
        Move m = lineMoves.get(currentPly);
        boardPanel.setHintArrow(m.from, m.to, new java.awt.Color(0x38, 0x93, 0xE8, 220));
        coachArea.setText("Book Hint: Play " + currentOpening.movesSan.get(currentPly) + " — "
                + currentOpening.getExplanation(currentPly));
    }

    private void toggleAutoPlay() {
        if (autoPlayTimer != null && autoPlayTimer.isRunning()) {
            stopAutoPlay();
            return;
        }
        if (drillMode) setDrillMode(false);
        if (currentPly >= lineStates.size() - 1) goToPly(0, false);
        autoPlayTimer = new javax.swing.Timer(1250, e -> {
            if (currentPly < lineStates.size() - 1) {
                goToPly(currentPly + 1, true);
            } else {
                stopAutoPlay();
            }
        });
        autoPlayTimer.start();
    }

    private void stopAutoPlay() {
        if (autoPlayTimer != null) autoPlayTimer.stop();
    }

    private void updateLineMovesDisplay() {
        if (currentOpening == null) return;
        StringBuilder sb = new StringBuilder();
        int limit = drillMode ? currentPly : currentOpening.movesSan.size();
        for (int i = 0; i < limit; i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1)).append(". ");
            if (!drillMode && i == currentPly - 1) sb.append("[").append(currentOpening.movesSan.get(i)).append("] ");
            else sb.append(currentOpening.movesSan.get(i)).append(" ");
        }
        if (drillMode && limit == 0) sb.append("Play the first move to begin the drill...");
        lineMovesArea.setText(sb.toString().trim());
    }

    private void fetchMastersExplorer(String fen) {
        final int seq = ++explorerSeq;
        explorerContainer.removeAll();
        JLabel loading = new JLabel("  Querying Lichess Masters database...");
        loading.setFont(Theme.FONT_SMALL);
        loading.setForeground(Theme.TEXT_MUTED);
        explorerContainer.add(loading);
        explorerContainer.revalidate();
        explorerContainer.repaint();

        explorerExec.submit(() -> {
            OpeningExplorerClient.ExplorerResult res = explorerClient.queryMasters(fen);
            SwingUtilities.invokeLater(() -> {
                if (seq != explorerSeq) return;
                explorerContainer.removeAll();
                if (res == null || res.moves.isEmpty()) {
                    JLabel empty = new JLabel("  No further master games recorded for this position.");
                    empty.setFont(Theme.FONT_SMALL);
                    empty.setForeground(Theme.TEXT_MUTED);
                    explorerContainer.add(empty);
                } else {
                    for (OpeningExplorerClient.ExplorerMove em : res.moves) {
                        explorerContainer.add(buildExplorerRow(em));
                    }
                }
                explorerContainer.revalidate();
                explorerContainer.repaint();
            });
        });
    }

    private JPanel buildExplorerRow(OpeningExplorerClient.ExplorerMove em) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.BG_SURFACE);
        row.setBorder(new EmptyBorder(4, 6, 4, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel moveLbl = new JLabel(em.san);
        moveLbl.setFont(Theme.FONT_MONO);
        moveLbl.setForeground(Theme.TEXT_PRIMARY);
        moveLbl.setPreferredSize(new Dimension(56, 20));

        JLabel countLbl = new JLabel(String.format("%,d", em.totalGames()));
        countLbl.setFont(Theme.FONT_SMALL);
        countLbl.setForeground(Theme.TEXT_MUTED);
        countLbl.setPreferredSize(new Dimension(58, 20));

        // Proportional Win / Draw / Loss bar
        JPanel wdlBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int total = Math.max(1, em.totalGames());
                int w = getWidth(), h = getHeight();
                int wPx = (int) Math.round(w * (em.whiteWins / (double) total));
                int dPx = (int) Math.round(w * (em.draws / (double) total));
                int bPx = Math.max(0, w - wPx - dPx);
                g.setColor(new java.awt.Color(0xE8, 0xE6, 0xDF));
                g.fillRect(0, 2, wPx, h - 4);
                g.setColor(new java.awt.Color(0x88, 0x85, 0x7E));
                g.fillRect(wPx, 2, dPx, h - 4);
                g.setColor(new java.awt.Color(0x36, 0x34, 0x31));
                g.fillRect(wPx + dPx, 2, bPx, h - 4);
            }
        };
        wdlBar.setOpaque(false);
        wdlBar.setPreferredSize(new Dimension(130, 18));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(moveLbl);
        left.add(countLbl);

        row.add(left, BorderLayout.WEST);
        row.add(wdlBar, BorderLayout.CENTER);
        return row;
    }

    private final class OpeningCellRenderer extends JPanel implements ListCellRenderer<Opening> {
        private final JLabel ecoLbl = new JLabel();
        private final JLabel nameLbl = new JLabel();
        private final JLabel subLbl = new JLabel();
        private final JLabel badgeLbl = new JLabel();

        OpeningCellRenderer() {
            setLayout(new BorderLayout(8, 2));
            setBorder(new EmptyBorder(7, 8, 7, 8));
            ecoLbl.setFont(Theme.FONT_MONO);
            ecoLbl.setForeground(Theme.ACCENT_AMBER);
            ecoLbl.setPreferredSize(new Dimension(34, 20));
            nameLbl.setFont(Theme.FONT_BODY_BOLD);
            nameLbl.setForeground(Theme.TEXT_PRIMARY);
            subLbl.setFont(Theme.FONT_SMALL);
            subLbl.setForeground(Theme.TEXT_MUTED);
            badgeLbl.setFont(Theme.FONT_SMALL_BOLD);

            JPanel center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.add(nameLbl, BorderLayout.NORTH);
            center.add(subLbl, BorderLayout.SOUTH);

            add(ecoLbl, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(badgeLbl, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Opening> list, Opening val,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setBackground(isSelected ? Theme.BG_ELEVATED : (index % 2 == 0 ? Theme.BG_SURFACE : Theme.BG_CARD));
            ecoLbl.setText(val.eco);
            nameLbl.setText(val.name);
            subLbl.setText(val.variation + " • " + val.trainSide.displayName());
            if (profile.masteredOpenings.contains(val.id)) {
                badgeLbl.setText("✓");
                badgeLbl.setForeground(Theme.ACCENT_GREEN);
            } else {
                badgeLbl.setText("");
            }
            return this;
        }
    }

    public void shutdown() {
        stopAutoPlay();
        explorerExec.shutdownNow();
    }
}
