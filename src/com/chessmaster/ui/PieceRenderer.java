package com.chessmaster.ui;

import com.chessmaster.model.Color;
import com.chessmaster.model.Piece;
import com.chessmaster.model.PieceType;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-DPI custom vector Staunton chess piece renderer inspired by Lichess Cburnett / Chess.com Neo pieces.
 * Caches rendered sprites per piece and size for smooth 60 FPS board dragging and animations.
 */
public final class PieceRenderer {

    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    public static BufferedImage getPieceImage(Piece piece, int size) {
        if (piece == null || size <= 4) return null;
        String key = piece.fenChar() + "_" + size;
        return CACHE.computeIfAbsent(key, k -> renderPiece(piece, size));
    }

    private static BufferedImage renderPiece(Piece piece, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Normalize coordinate space to 0..100
        double scale = size / 100.0;
        g2.scale(scale, scale);

        boolean isWhite = piece.color() == Color.WHITE;
        Paint fillPaint = isWhite
                ? new GradientPaint(22, 14, new java.awt.Color(0xFF, 0xFF, 0xFA),
                80, 90, new java.awt.Color(0xDC, 0xD7, 0xCA))
                : new GradientPaint(24, 14, new java.awt.Color(0x4E, 0x4C, 0x48),
                82, 92, new java.awt.Color(0x18, 0x17, 0x15));
        java.awt.Color strokeColor = isWhite
                ? new java.awt.Color(0x22, 0x20, 0x1D)
                : new java.awt.Color(0x0F, 0x0E, 0x0D);
        java.awt.Color detailColor = isWhite
                ? new java.awt.Color(0x2A, 0x28, 0x24)
                : new java.awt.Color(0xDC, 0xD8, 0xCE);

        // Subtle drop shadow underneath base
        g2.setColor(new java.awt.Color(0, 0, 0, 55));
        g2.fill(new Ellipse2D.Double(17, 84, 66, 9));

        switch (piece.type()) {
            case PAWN -> drawPawn(g2, fillPaint, strokeColor, detailColor);
            case ROOK -> drawRook(g2, fillPaint, strokeColor, detailColor);
            case KNIGHT -> drawKnight(g2, fillPaint, strokeColor, detailColor, isWhite);
            case BISHOP -> drawBishop(g2, fillPaint, strokeColor, detailColor);
            case QUEEN -> drawQueen(g2, fillPaint, strokeColor, detailColor);
            case KING -> drawKing(g2, fillPaint, strokeColor, detailColor);
        }

        g2.dispose();
        return img;
    }

    private static void fillAndStroke(Graphics2D g2, Shape s, Paint fill, java.awt.Color stroke, float width) {
        g2.setPaint(fill);
        g2.fill(s);
        g2.setColor(stroke);
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(s);
    }

    private static void drawBase(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        RoundRectangle2D base = new RoundRectangle2D.Double(20, 77, 60, 10, 6, 6);
        fillAndStroke(g2, base, fill, stroke, 3.0f);
        g2.setColor(detail);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(26, 82, 74, 82));
    }

    private static void drawPawn(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        Path2D body = new Path2D.Double();
        body.moveTo(25, 84);
        body.curveTo(28, 66, 39, 57, 41, 44);
        body.lineTo(59, 44);
        body.curveTo(61, 57, 72, 66, 75, 84);
        body.closePath();
        fillAndStroke(g2, body, fill, stroke, 3.2f);

        // Collar ring
        RoundRectangle2D collar = new RoundRectangle2D.Double(34, 41, 32, 7, 5, 5);
        fillAndStroke(g2, collar, fill, stroke, 2.8f);

        // Head sphere
        Ellipse2D head = new Ellipse2D.Double(36, 18, 28, 26);
        fillAndStroke(g2, head, fill, stroke, 3.2f);

        drawBase(g2, fill, stroke, detail);
    }

    private static void drawRook(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        // Tower shaft
        Path2D shaft = new Path2D.Double();
        shaft.moveTo(27, 78);
        shaft.lineTo(32, 37);
        shaft.lineTo(68, 37);
        shaft.lineTo(73, 78);
        shaft.closePath();
        fillAndStroke(g2, shaft, fill, stroke, 3.2f);

        // Battlement crown with 3 crenellations
        Path2D crown = new Path2D.Double();
        crown.moveTo(24, 38);
        crown.lineTo(24, 18);
        crown.lineTo(35, 18);
        crown.lineTo(35, 25);
        crown.lineTo(44, 25);
        crown.lineTo(44, 18);
        crown.lineTo(56, 18);
        crown.lineTo(56, 25);
        crown.lineTo(65, 25);
        crown.lineTo(65, 18);
        crown.lineTo(76, 18);
        crown.lineTo(76, 38);
        crown.closePath();
        fillAndStroke(g2, crown, fill, stroke, 3.2f);

        // Detail lines
        g2.setColor(detail);
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(32, 43, 68, 43));
        g2.draw(new Line2D.Double(30, 71, 70, 71));

        drawBase(g2, fill, stroke, detail);
    }

    private static void drawKnight(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail, boolean isWhite) {
        Path2D horse = new Path2D.Double();
        horse.moveTo(26, 78);
        horse.curveTo(26, 62, 34, 52, 42, 45);
        horse.curveTo(35, 47, 27, 50, 21, 47); // chin/jaw
        horse.curveTo(16, 44, 17, 38, 22, 33); // snout
        horse.curveTo(27, 27, 35, 21, 42, 18); // forehead
        horse.lineTo(45, 11);                 // ear tip
        horse.lineTo(53, 17);                 // behind ear
        horse.curveTo(66, 20, 76, 34, 77, 52); // arched mane
        horse.curveTo(78, 63, 75, 72, 74, 78);
        horse.closePath();
        fillAndStroke(g2, horse, fill, stroke, 3.2f);

        // Eye & nostril & mane highlight
        g2.setColor(detail);
        g2.fill(new Ellipse2D.Double(34, 27, 4.5, 4.5));
        g2.fill(new Ellipse2D.Double(22, 40, 2.8, 2.8));
        g2.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D mane = new Path2D.Double();
        mane.moveTo(54, 23);
        mane.curveTo(65, 29, 70, 44, 68, 65);
        g2.draw(mane);

        if (!isWhite) {
            g2.draw(new Line2D.Double(36, 46, 46, 42));
        }

        drawBase(g2, fill, stroke, detail);
    }

    private static void drawBishop(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        // Tapered robe body
        Path2D body = new Path2D.Double();
        body.moveTo(27, 78);
        body.curveTo(32, 62, 36, 56, 37, 52);
        body.lineTo(63, 52);
        body.curveTo(64, 56, 68, 62, 73, 78);
        body.closePath();
        fillAndStroke(g2, body, fill, stroke, 3.2f);

        // Mitre head
        Path2D mitre = new Path2D.Double();
        mitre.moveTo(50, 17);
        mitre.curveTo(67, 27, 68, 44, 60, 53);
        mitre.lineTo(40, 53);
        mitre.curveTo(32, 44, 33, 27, 50, 17);
        mitre.closePath();
        fillAndStroke(g2, mitre, fill, stroke, 3.2f);

        // Top finial knob
        Ellipse2D knob = new Ellipse2D.Double(45, 9, 10, 10);
        fillAndStroke(g2, knob, fill, stroke, 2.8f);

        // Collar & diagonal mitre slit / cross
        g2.setColor(detail);
        g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(45, 34, 55, 34));
        g2.draw(new Line2D.Double(50, 28, 50, 40));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(37, 57, 63, 57));

        drawBase(g2, fill, stroke, detail);
    }

    private static void drawQueen(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        // Crown silhouette with 5 spikes
        Path2D crown = new Path2D.Double();
        crown.moveTo(25, 78);
        crown.lineTo(15, 29);
        crown.lineTo(32, 49);
        crown.lineTo(35, 22);
        crown.lineTo(45, 46);
        crown.lineTo(50, 18);
        crown.lineTo(55, 46);
        crown.lineTo(65, 22);
        crown.lineTo(68, 49);
        crown.lineTo(85, 29);
        crown.lineTo(75, 78);
        crown.closePath();
        fillAndStroke(g2, crown, fill, stroke, 3.0f);

        // Five crown jewels
        double[][] pearls = {{15, 26}, {35, 19}, {50, 15}, {65, 19}, {85, 26}};
        for (double[] pt : pearls) {
            Ellipse2D pearl = new Ellipse2D.Double(pt[0] - 4.2, pt[1] - 4.2, 8.4, 8.4);
            fillAndStroke(g2, pearl, fill, stroke, 2.5f);
        }

        // Waist accents
        g2.setColor(detail);
        g2.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(27, 66, 73, 66));
        g2.draw(new Line2D.Double(26, 72, 74, 72));

        drawBase(g2, fill, stroke, detail);
    }

    private static void drawKing(Graphics2D g2, Paint fill, java.awt.Color stroke, java.awt.Color detail) {
        // Cross atop the crown
        g2.setColor(stroke);
        g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(50, 8, 50, 26));
        g2.draw(new Line2D.Double(42, 14, 58, 14));

        // Central crest
        Path2D crest = new Path2D.Double();
        crest.moveTo(43, 35);
        crest.curveTo(43, 24, 57, 24, 57, 35);
        crest.lineTo(55, 58);
        crest.lineTo(45, 58);
        crest.closePath();
        fillAndStroke(g2, crest, fill, stroke, 2.8f);

        // Imperial twin lobes
        Path2D crown = new Path2D.Double();
        crown.moveTo(25, 78);
        crown.curveTo(14, 54, 15, 31, 33, 31);
        crown.curveTo(44, 31, 48, 41, 50, 49);
        crown.curveTo(52, 41, 56, 31, 67, 31);
        crown.curveTo(85, 31, 86, 54, 75, 78);
        crown.closePath();
        fillAndStroke(g2, crown, fill, stroke, 3.2f);

        // Crown band lines
        g2.setColor(detail);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(26, 65, 74, 65));
        g2.draw(new Line2D.Double(25, 72, 75, 72));
        g2.draw(new Line2D.Double(50, 49, 50, 64));

        drawBase(g2, fill, stroke, detail);
    }
}
