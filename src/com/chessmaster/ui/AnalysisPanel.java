package com.chessmaster.ui;

import com.chessmaster.engine.StockfishEngine;
import com.chessmaster.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Full-strength Stockfish 19 MultiPV Analysis Studio with top-3 engine lines,
 * live Evaluation Bar, FEN loader, right-click arrows, and seamless transition to Play vs Stockfish.
 */
public final class AnalysisPanel extends JPanel {

    private final StockfishEngine engine;
    private final SoundManager sound;
    private final Consumer<String> onPlayFromFen;

    private final ChessBoardPanel boardPanel = new ChessBoardPanel();
    private final EvalBarPanel evalBar = new EvalBarPanel();

    private final List<BoardState> states = new ArrayList<>();
    private final List<Move> moves = new ArrayList<>();
    private final List<String> sans = new ArrayList<>();
    private int currentPly = 0;
    private int analyzeSeq = 0;

    private final JCheckBox engineOnCheck = new JCheckBox("Stockfish 19 Live Analysis (Top 3 Lines)", true);
    private final JLabel evalHeaderLabel = new JLabel("Eval: 0.00");
    private final JPanel linesPanel = new JPanel();
    private final JTextField fenField = new JTextField();
    private final JTextArea movesArea = new JTextArea(4, 24);

    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AnalysisWorker");
        t.setDaemon(true);
        return t;
    });

    public AnalysisPanel(StockfishEngine engine, SoundManager sound, Consumer<String> onPlayFromFen) {
        this.engine = engine;
        this.sound = sound;
        this.onPlayFromFen = onPlayFromFen;

        setLayout(new BorderLayout(16, 0));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(evalBar, BorderLayout.WEST);
        center.add(boardPanel, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        boardPanel.setMoveListener(this::handleMove);
        loadFen(BoardState.START_FEN);
    }

    public ChessBoardPanel getBoardPanel() {
        return boardPanel;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(390, 650));

        // 1. Engine Lines Card
        JPanel engCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        engCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        engineOnCheck.setOpaque(false);
        engineOnCheck.setForeground(Theme.TEXT_PRIMARY);
        engineOnCheck.setFont(Theme.FONT_BODY_BOLD);
        engineOnCheck.setFocusable(false);
        engineOnCheck.addActionListener(e -> triggerAnalysis());

        evalHeaderLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        evalHeaderLabel.setForeground(Theme.ACCENT_GREEN);
        top.add(engineOnCheck, BorderLayout.NORTH);
        top.add(evalHeaderLabel, BorderLayout.SOUTH);

        linesPanel.setLayout(new BoxLayout(linesPanel, BoxLayout.Y_AXIS));
        linesPanel.setBackground(Theme.BG_INPUT);
        linesPanel.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane linesScroll = new JScrollPane(linesPanel);
        Theme.styleScrollPane(linesScroll);
        linesScroll.setPreferredSize(new Dimension(350, 145));

        engCard.add(top, BorderLayout.NORTH);
        engCard.add(linesScroll, BorderLayout.CENTER);

        // 2. Moves & FEN Card
        JPanel midCard = Theme.makeCardPanel(new BorderLayout(0, 8));
        midCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        movesArea.setEditable(false);
        movesArea.setLineWrap(true);
        movesArea.setWrapStyleWord(true);
        movesArea.setBackground(Theme.BG_INPUT);
        movesArea.setForeground(Theme.TEXT_PRIMARY);
        movesArea.setFont(Theme.FONT_MONO);
        movesArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane mvScroll = new JScrollPane(movesArea);
        Theme.styleScrollPane(mvScroll);

        JPanel nav = new JPanel(new GridLayout(1, 4, 6, 0));
        nav.setOpaque(false);
        JButton b0 = Theme.makeSecondaryButton("|<");
        JButton bP = Theme.makeSecondaryButton("<");
        JButton bN = Theme.makeSecondaryButton(">");
        JButton bE = Theme.makeSecondaryButton(">|");
        b0.addActionListener(e -> goToPly(0));
        bP.addActionListener(e -> goToPly(currentPly - 1));
        bN.addActionListener(e -> goToPly(currentPly + 1));
        bE.addActionListener(e -> goToPly(states.size() - 1));
        nav.add(b0); nav.add(bP); nav.add(bN); nav.add(bE);

        JPanel fenBox = new JPanel(new BorderLayout(6, 4));
        fenBox.setOpaque(false);
        JLabel fenLbl = new JLabel("FEN Position");
        fenLbl.setFont(Theme.FONT_SMALL_BOLD);
        fenLbl.setForeground(Theme.TEXT_SECONDARY);
        fenField.setBackground(Theme.BG_INPUT);
        fenField.setForeground(Theme.TEXT_PRIMARY);
        fenField.setCaretColor(Theme.TEXT_PRIMARY);
        fenField.setFont(new Font("Consolas", Font.PLAIN, 11));
        fenField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(5, 6, 5, 6)));

        JPanel fenBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        fenBtns.setOpaque(false);
        JButton loadBtn = Theme.makeBlueButton("Load FEN");
        JButton copyBtn = Theme.makeSecondaryButton("Copy FEN");
        loadBtn.addActionListener(e -> {
            try {
                loadFen(fenField.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid FEN string.", "FEN Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fenField.getText()), null);
        });
        fenBtns.add(loadBtn);
        fenBtns.add(copyBtn);

        fenBox.add(fenLbl, BorderLayout.NORTH);
        fenBox.add(fenField, BorderLayout.CENTER);
        fenBox.add(fenBtns, BorderLayout.SOUTH);

        midCard.add(mvScroll, BorderLayout.CENTER);
        midCard.add(nav, BorderLayout.NORTH);
        midCard.add(fenBox, BorderLayout.SOUTH);

        // 3. Footer Actions
        JPanel footer = Theme.makeCardPanel(new GridLayout(1, 3, 6, 0));
        footer.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton resetBtn = Theme.makeSecondaryButton("Start Pos");
        JButton flipBtn = Theme.makeSecondaryButton("Flip");
        JButton playBtn = Theme.makePrimaryButton("Play Here");

        resetBtn.addActionListener(e -> loadFen(BoardState.START_FEN));
        flipBtn.addActionListener(e -> {
            boolean f = !boardPanel.isFlipped();
            boardPanel.setFlipped(f);
            evalBar.setFlipped(f);
        });
        playBtn.addActionListener(e -> {
            if (onPlayFromFen != null && !states.isEmpty()) {
                onPlayFromFen.accept(states.get(currentPly).toFen());
            }
        });

        footer.add(resetBtn);
        footer.add(flipBtn);
        footer.add(playBtn);

        side.add(engCard, BorderLayout.NORTH);
        side.add(midCard, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    public void loadFen(String fen) {
        BoardState st = BoardState.fromFen(fen);
        states.clear();
        moves.clear();
        sans.clear();
        states.add(st);
        currentPly = 0;
        refreshView();
    }

    private void handleMove(Move m) {
        // Truncate future branch if moving from earlier ply
        while (states.size() > currentPly + 1) {
            states.remove(states.size() - 1);
            moves.remove(moves.size() - 1);
            sans.remove(sans.size() - 1);
        }
        BoardState cur = states.get(currentPly);
        String san = cur.moveToSan(m);
        BoardState next = cur.applyMove(m);

        moves.add(m);
        sans.add(san);
        states.add(next);
        currentPly = states.size() - 1;

        if (next.isInCheck()) sound.playCheck();
        else if (m.isCapture) sound.playCapture();
        else sound.playMove();

        refreshView();
    }

    private void goToPly(int ply) {
        if (ply < 0 || ply >= states.size()) return;
        currentPly = ply;
        refreshView();
    }

    private void refreshView() {
        BoardState st = states.get(currentPly);
        int lf = currentPly > 0 ? moves.get(currentPly - 1).from : -1;
        int lt = currentPly > 0 ? moves.get(currentPly - 1).to : -1;
        boardPanel.setBoardState(st, lf, lt, false);
        boardPanel.setInteractive(true);
        boardPanel.setAllowedMoveColor(null);
        fenField.setText(st.toFen());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sans.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1)).append(". ");
            if (i == currentPly - 1) sb.append("[").append(sans.get(i)).append("] ");
            else sb.append(sans.get(i)).append(" ");
        }
        movesArea.setText(sb.toString().trim());
        triggerAnalysis();
    }

    private void triggerAnalysis() {
        final int seq = ++analyzeSeq;
        if (!engineOnCheck.isSelected() || states.isEmpty()) {
            linesPanel.removeAll();
            linesPanel.revalidate();
            linesPanel.repaint();
            return;
        }
        BoardState pos = states.get(currentPly);
        evalHeaderLabel.setText("Analyzing...");

        exec.submit(() -> {
            StockfishEngine.EvalResult res = engine.analyzePosition(pos, 3, 550);
            SwingUtilities.invokeLater(() -> {
                if (seq != analyzeSeq) return;
                evalBar.setEvaluation(res.centipawnsWhite, res.mateInWhite);
                evalHeaderLabel.setText("Eval: " + res.formattedScore()
                        + (res.lines.isEmpty() ? "" : "  (depth " + res.lines.get(0).depth + ")"));

                if (res.bestMoveUci != null) {
                    Move best = pos.parseUci(res.bestMoveUci);
                    if (best != null) {
                        boardPanel.setHintArrow(best.from, best.to, new java.awt.Color(0x38, 0x93, 0xE8, 175));
                    }
                }

                linesPanel.removeAll();
                for (StockfishEngine.PvLine pv : res.lines) {
                    linesPanel.add(buildPvRow(pos, pv));
                    linesPanel.add(Box.createVerticalStrut(4));
                }
                linesPanel.revalidate();
                linesPanel.repaint();
            });
        });
    }

    private JPanel buildPvRow(BoardState root, StockfishEngine.PvLine pv) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Theme.BG_CARD);
        row.setBorder(new EmptyBorder(6, 8, 6, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel sc = new JLabel(pv.formattedScore());
        sc.setFont(Theme.FONT_MONO);
        sc.setForeground(pv.centipawnsWhite >= 0 ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        sc.setPreferredSize(new Dimension(56, 20));

        // Convert UCI PV moves to readable SAN
        StringBuilder sb = new StringBuilder();
        BoardState sim = root;
        int limit = Math.min(7, pv.uciMoves.size());
        for (int i = 0; i < limit; i++) {
            Move m = sim.parseUci(pv.uciMoves.get(i));
            if (m == null) break;
            if (i > 0) sb.append(' ');
            sb.append(sim.moveToSan(m));
            sim = sim.applyMove(m);
        }
        JLabel lineLbl = new JLabel(sb.toString());
        lineLbl.setFont(Theme.FONT_SMALL);
        lineLbl.setForeground(Theme.TEXT_PRIMARY);

        row.add(sc, BorderLayout.WEST);
        row.add(lineLbl, BorderLayout.CENTER);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!pv.uciMoves.isEmpty()) {
                    Move first = root.parseUci(pv.uciMoves.get(0));
                    if (first != null) handleMove(first);
                }
            }
        });
        return row;
    }

    public void shutdown() {
        exec.shutdownNow();
    }
}
