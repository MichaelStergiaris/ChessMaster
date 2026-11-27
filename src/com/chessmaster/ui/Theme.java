package com.chessmaster.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lichess / Chess.com inspired dark studio palette, typography, board themes, and sleek UI helpers.
 */
public final class Theme {

    public static final Color BG_MAIN = new Color(0x16, 0x15, 0x12);
    public static final Color BG_SURFACE = new Color(0x26, 0x24, 0x21);
    public static final Color BG_CARD = new Color(0x30, 0x2E, 0x2B);
    public static final Color BG_ELEVATED = new Color(0x3D, 0x3A, 0x36);
    public static final Color BG_INPUT = new Color(0x21, 0x1F, 0x1C);
    public static final Color BORDER = new Color(0x45, 0x42, 0x3D);

    public static final Color TEXT_PRIMARY = new Color(0xED, 0xED, 0xEA);
    public static final Color TEXT_SECONDARY = new Color(0xB8, 0xB5, 0xAE);
    public static final Color TEXT_MUTED = new Color(0x8A, 0x87, 0x80);

    public static final Color ACCENT_GREEN = new Color(0x81, 0xB6, 0x4C);
    public static final Color ACCENT_GREEN_HOVER = new Color(0x95, 0xC9, 0x5E);
    public static final Color ACCENT_BLUE = new Color(0x38, 0x93, 0xE8);
    public static final Color ACCENT_BLUE_HOVER = new Color(0x52, 0xA5, 0xF5);
    public static final Color ACCENT_AMBER = new Color(0xE5, 0x9F, 0x3A);
    public static final Color ACCENT_RED = new Color(0xD9, 0x53, 0x4F);
    public static final Color ACCENT_RED_HOVER = new Color(0xE7, 0x69, 0x65);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.BOLD, 14);
    public static final Font FONT_CLOCK = new Font("Consolas", Font.BOLD, 22);

    public static final class BoardPalette {
        public final String name;
        public final Color lightSquare;
        public final Color darkSquare;
        public final Color lastMoveLight;
        public final Color lastMoveDark;
        public final Color selectedSquare;

        public BoardPalette(String name, Color light, Color dark, Color lastLight, Color lastDark, Color selected) {
            this.name = name;
            this.lightSquare = light;
            this.darkSquare = dark;
            this.lastMoveLight = lastLight;
            this.lastMoveDark = lastDark;
            this.selectedSquare = selected;
        }
    }

    public static final Map<String, BoardPalette> BOARD_THEMES = new LinkedHashMap<>();
    static {
        BOARD_THEMES.put("Lichess Wood", new BoardPalette(
                "Lichess Wood",
                new Color(0xF0, 0xD9, 0xB5), new Color(0xB5, 0x88, 0x63),
                new Color(0xCD, 0xD2, 0x6A), new Color(0xAA, 0xA2, 0x3A),
                new Color(0x82, 0x97, 0x69)));
        BOARD_THEMES.put("Chess.com Green", new BoardPalette(
                "Chess.com Green",
                new Color(0xEE, 0xEE, 0xD2), new Color(0x76, 0x96, 0x56),
                new Color(0xF6, 0xF6, 0x69), new Color(0xBA, 0xCA, 0x2B),
                new Color(0xBB, 0xCB, 0x2B)));
        BOARD_THEMES.put("Midnight Slate", new BoardPalette(
                "Midnight Slate",
                new Color(0xDE, 0xE3, 0xE6), new Color(0x8C, 0xA2, 0xAD),
                new Color(0xB9, 0xDA, 0xE6), new Color(0x78, 0xB0, 0xC9),
                new Color(0x69, 0xA3, 0xC2)));
        BOARD_THEMES.put("Tournament Blue", new BoardPalette(
                "Tournament Blue",
                new Color(0xEA, 0xEF, 0xF2), new Color(0x4B, 0x73, 0x99),
                new Color(0xC5, 0xE1, 0xA5), new Color(0x73, 0xA9, 0x73),
                new Color(0x5E, 0x97, 0xC4)));
        BOARD_THEMES.put("Walnut Dark", new BoardPalette(
                "Walnut Dark",
                new Color(0xD9, 0xC3, 0xA3), new Color(0x8B, 0x5A, 0x2B),
                new Color(0xD8, 0xD0, 0x66), new Color(0xA6, 0x8E, 0x34),
                new Color(0x9E, 0x8C, 0x40)));
    }

    public static BoardPalette getBoardTheme(String name) {
        return BOARD_THEMES.getOrDefault(name, BOARD_THEMES.get("Lichess Wood"));
    }

    public static void applyGlobalLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("Panel.background", BG_MAIN);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_BLUE);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("List.background", BG_SURFACE);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", BG_ELEVATED);
        UIManager.put("List.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", BG_SURFACE);
        UIManager.put("Slider.background", BG_SURFACE);
        UIManager.put("Slider.foreground", ACCENT_GREEN);
        UIManager.put("OptionPane.background", BG_SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("CheckBox.background", BG_SURFACE);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
    }

    public static JButton makeButton(String text, Color bg, Color hoverBg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setForeground(fg);
                setFont(FONT_BODY_BOLD);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setBorder(new EmptyBorder(8, 14, 8, 14));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = !isEnabled() ? BG_CARD : (hovered ? hoverBg : bg);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    public static JButton makePrimaryButton(String text) {
        return makeButton(text, ACCENT_GREEN, ACCENT_GREEN_HOVER, Color.WHITE);
    }

    public static JButton makeBlueButton(String text) {
        return makeButton(text, ACCENT_BLUE, ACCENT_BLUE_HOVER, Color.WHITE);
    }

    public static JButton makeSecondaryButton(String text) {
        return makeButton(text, BG_ELEVATED, new Color(0x4E, 0x4A, 0x45), TEXT_PRIMARY);
    }

    public static JButton makeDangerButton(String text) {
        return makeButton(text, ACCENT_RED, ACCENT_RED_HOVER, Color.WHITE);
    }

    public static JPanel makeCardPanel(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBackground(BG_SURFACE);
        return p;
    }

    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        combo.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        combo.setFocusable(false);
    }

    public static void styleScrollPane(JScrollPane sp) {
        sp.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        sp.getViewport().setBackground(BG_SURFACE);
        sp.setBackground(BG_SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }
}
