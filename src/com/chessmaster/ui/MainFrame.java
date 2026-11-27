package com.chessmaster.ui;

import com.chessmaster.data.PuzzleService;
import com.chessmaster.data.UserProfile;
import com.chessmaster.engine.StockfishEngine;
import com.chessmaster.model.Color;
import com.chessmaster.model.Piece;
import com.chessmaster.model.PieceType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main Lichess / Chess.com inspired application frame with top navigation bar,
 * global board theme selector, sound toggle, and seamless mode switching.
 */
public final class MainFrame extends JFrame {

    private static final String CARD_PLAY = "PLAY";
    private static final String CARD_PUZZLES = "PUZZLES";
    private static final String CARD_OPENINGS = "OPENINGS";
    private static final String CARD_ANALYSIS = "ANALYSIS";

    private final UserProfile profile = UserProfile.load();
    private final SoundManager sound = new SoundManager();
    private final StockfishEngine stockfish = new StockfishEngine();
    private final PuzzleService puzzleService = new PuzzleService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();

    private final PlayStockfishPanel playPanel;
    private final PuzzlesPanel puzzlesPanel;
    private final OpeningsPanel openingsPanel;
    private final AnalysisPanel analysisPanel;

    public MainFrame() {
        super("ChessMaster Studio — Puzzles, Stockfish 19 & Opening Trainer");
        sound.setEnabled(profile.soundEnabled);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1340, 840);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_MAIN);
        setLayout(new BorderLayout());

        // Set crisp app icon using our vector knight
        setIconImage(PieceRenderer.getPieceImage(new Piece(Color.WHITE, PieceType.KNIGHT), 64));

        playPanel = new PlayStockfishPanel(stockfish, sound, profile, this::openInAnalysis);
        puzzlesPanel = new PuzzlesPanel(puzzleService, sound, profile, this::openInAnalysis);
        openingsPanel = new OpeningsPanel(sound, profile, this::playFromFen, this::openInAnalysis);
        analysisPanel = new AnalysisPanel(stockfish, sound, this::playFromFen);

        contentPanel.setBackground(Theme.BG_MAIN);
        contentPanel.add(playPanel, CARD_PLAY);
        contentPanel.add(puzzlesPanel, CARD_PUZZLES);
        contentPanel.add(openingsPanel, CARD_OPENINGS);
        contentPanel.add(analysisPanel, CARD_ANALYSIS);

        add(buildTopNavBar(), BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        applyBoardTheme(profile.boardTheme);
        switchMode(CARD_PLAY);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                playPanel.shutdown();
                puzzlesPanel.shutdown();
                openingsPanel.shutdown();
                analysisPanel.shutdown();
                puzzleService.shutdown();
                stockfish.shutdown();
                profile.save();
            }
        });
    }

    private JPanel buildTopNavBar() {
        JPanel nav = new JPanel(new BorderLayout(16, 0));
        nav.setBackground(Theme.BG_SURFACE);
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(8, 18, 8, 18)));

        // Left: Brand + Mode Tabs
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel brandIcon = new JLabel(new ImageIcon(PieceRenderer.getPieceImage(new Piece(Color.WHITE, PieceType.KNIGHT), 32)));
        JLabel brandTitle = new JLabel("ChessMaster");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brandTitle.setForeground(Theme.TEXT_PRIMARY);
        brandTitle.setBorder(new EmptyBorder(0, 0, 0, 16));

        left.add(brandIcon);
        left.add(brandTitle);

        addNavButton(left, CARD_PLAY, "Play Stockfish");
        addNavButton(left, CARD_PUZZLES, "Unlimited Puzzles");
        addNavButton(left, CARD_OPENINGS, "Opening Studio");
        addNavButton(left, CARD_ANALYSIS, "Analysis Board");

        // Right: Board Theme selector + Sound toggle + Stockfish status pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel themeLbl = new JLabel("Board:");
        themeLbl.setFont(Theme.FONT_SMALL_BOLD);
        themeLbl.setForeground(Theme.TEXT_MUTED);

        JComboBox<String> themeCombo = new JComboBox<>(Theme.BOARD_THEMES.keySet().toArray(new String[0]));
        themeCombo.setSelectedItem(profile.boardTheme);
        Theme.styleComboBox(themeCombo);
        themeCombo.addActionListener(e -> {
            String sel = (String) themeCombo.getSelectedItem();
            if (sel != null) {
                profile.boardTheme = sel;
                profile.save();
                applyBoardTheme(sel);
            }
        });

        JButton soundBtn = Theme.makeSecondaryButton(sound.isEnabled() ? "Sound: ON" : "Sound: OFF");
        soundBtn.addActionListener(e -> {
            sound.setEnabled(!sound.isEnabled());
            profile.soundEnabled = sound.isEnabled();
            profile.save();
            soundBtn.setText(sound.isEnabled() ? "Sound: ON" : "Sound: OFF");
        });

        JLabel sfBadge = new JLabel(stockfish.isStockfishReady() ? "● Stockfish 19" : "● Fallback AI");
        sfBadge.setFont(Theme.FONT_SMALL_BOLD);
        sfBadge.setForeground(stockfish.isStockfishReady() ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
        sfBadge.setBorder(new EmptyBorder(0, 6, 0, 0));

        right.add(themeLbl);
        right.add(themeCombo);
        right.add(soundBtn);
        right.add(sfBadge);

        nav.add(left, BorderLayout.WEST);
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private void addNavButton(JPanel container, String cardKey, String label) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = Boolean.TRUE.equals(getClientProperty("active"));
                if (active) {
                    g2.setColor(Theme.ACCENT_GREEN);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(Theme.BG_ELEVATED);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setFont(Theme.FONT_BODY_BOLD);
        btn.setForeground(Theme.TEXT_SECONDARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.addActionListener(e -> switchMode(cardKey));
        navButtons.put(cardKey, btn);
        container.add(btn);
    }

    private void switchMode(String cardKey) {
        cardLayout.show(contentPanel, cardKey);
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(cardKey);
            e.getValue().putClientProperty("active", active);
            e.getValue().setForeground(active ? java.awt.Color.WHITE : Theme.TEXT_SECONDARY);
            e.getValue().repaint();
        }
    }

    private void applyBoardTheme(String name) {
        Theme.BoardPalette pal = Theme.getBoardTheme(name);
        playPanel.getBoardPanel().setPalette(pal);
        puzzlesPanel.getBoardPanel().setPalette(pal);
        openingsPanel.getBoardPanel().setPalette(pal);
        analysisPanel.getBoardPanel().setPalette(pal);
    }

    private void openInAnalysis(String fen) {
        analysisPanel.loadFen(fen);
        switchMode(CARD_ANALYSIS);
    }

    private void playFromFen(String fen) {
        playPanel.startNewGame(fen);
        switchMode(CARD_PLAY);
    }
}
