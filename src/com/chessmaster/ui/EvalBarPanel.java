package com.chessmaster.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Smooth animated vertical Lichess / Chess.com style Evaluation Bar.
 */
public final class EvalBarPanel extends JPanel {
    private double targetWhiteShare = 0.5;
    private double currentWhiteShare = 0.5;
    private String evalLabel = "0.0";
    private boolean whiteAdvantage = true;
    private boolean flipped = false;
    private final Timer animTimer;

    public EvalBarPanel() {
        setPreferredSize(new Dimension(30, 560));
        setMinimumSize(new Dimension(28, 300));
        setMaximumSize(new Dimension(32, 1200));
        setOpaque(false);

        animTimer = new Timer(16, e -> {
            double diff = targetWhiteShare - currentWhiteShare;
            if (Math.abs(diff) < 0.004) {
                currentWhiteShare = targetWhiteShare;
                ((Timer) e.getSource()).stop();
            } else {
                currentWhiteShare += diff * 0.22;
            }
            repaint();
        });
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public void setEvaluation(int centipawnsWhite, Integer mateInWhite) {
        if (mateInWhite != null) {
            whiteAdvantage = mateInWhite >= 0;
            targetWhiteShare = whiteAdvantage ? 0.97 : 0.03;
            evalLabel = "M" + Math.abs(mateInWhite);
        } else {
            whiteAdvantage = centipawnsWhite >= 0;
            // Sigmoid mapping (smoothly maps +/-1000 cp to 0.04..0.96)
            double share = 1.0 / (1.0 + Math.exp(-0.0038 * centipawnsWhite));
            targetWhiteShare = Math.max(0.04, Math.min(0.96, share));
            double pawns = Math.abs(centipawnsWhite) / 100.0;
            evalLabel = pawns >= 10.0 ? String.format("%.0f", pawns) : String.format("%.1f", pawns);
        }
        if (!animTimer.isRunning()) animTimer.start();
        repaint();
    }

    public void reset() {
        setEvaluation(0, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int barX = 2;
        int barW = w - 4;

        // Clip rounded bar
        Shape clip = new java.awt.geom.RoundRectangle2D.Double(barX, 0, barW, h, 6, 6);
        g2.setClip(clip);

        Color whiteFill = new Color(0xEC, 0xEC, 0xE8);
        Color blackFill = new Color(0x36, 0x34, 0x31);

        int whiteH = (int) Math.round(h * currentWhiteShare);
        if (!flipped) {
            // Black on top, White on bottom
            g2.setColor(blackFill);
            g2.fillRect(barX, 0, barW, h - whiteH);
            g2.setColor(whiteFill);
            g2.fillRect(barX, h - whiteH, barW, whiteH);
        } else {
            // White on top, Black on bottom
            g2.setColor(whiteFill);
            g2.fillRect(barX, 0, barW, whiteH);
            g2.setColor(blackFill);
            g2.fillRect(barX, whiteH, barW, h - whiteH);
        }

        // Center 0.0 tick mark
        g2.setColor(new Color(0xD9, 0x53, 0x4F, 140));
        g2.fillRect(barX, h / 2 - 1, barW, 2);

        // Score text anchored to the side with the advantage
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(evalLabel);
        int tx = barX + (barW - tw) / 2;

        boolean drawAtBottom = (!flipped && whiteAdvantage) || (flipped && !whiteAdvantage);
        g2.setColor(whiteAdvantage ? new Color(0x22, 0x20, 0x1D) : new Color(0xEE, 0xEE, 0xEA));
        int ty = drawAtBottom ? h - 7 : fm.getAscent() + 5;
        g2.drawString(evalLabel, tx, ty);

        g2.setClip(null);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(barX, 0, barW - 1, h - 1, 6, 6);
        g2.dispose();
    }
}
