package com.chessmaster.ui;

import com.chessmaster.model.*;
import com.chessmaster.model.Color;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full-featured Lichess / Chess.com interactive chess board panel:
 *  - Click-to-move AND smooth drag-and-drop
 *  - Smooth 135ms ease-out slide animations for engine/opponent moves
 *  - Legal move dots & capture corner rings
 *  - Right-click drag arrows (with Shift/Ctrl/Alt color variants) and square highlight circles
 *  - Hint arrows, check radial glow, last-move highlights, wrong-move flash
 *  - Visual pawn promotion selector popup
 *  - Crisp inside-edge file/rank coordinates
 */
public final class ChessBoardPanel extends JPanel {

    public static final class Arrow {
        public final int from;
        public final int to;
        public final java.awt.Color color;

        public Arrow(int from, int to, java.awt.Color color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }
    }

    private BoardState state = new BoardState();
    private Theme.BoardPalette palette = Theme.getBoardTheme("Lichess Wood");
    private boolean flipped = false;
    private boolean interactive = true;
    private Color allowedMoveColor = null; // null = both sides allowed

    private Consumer<Move> moveListener;

    // Selection & legal moves
    private int selectedSq = -1;
    private List<Move> selectedMoves = List.of();
    private int lastFrom = -1;
    private int lastTo = -1;

    // Drag-and-drop state
    private int dragFromSq = -1;
    private Piece dragPiece = null;
    private Point dragPoint = null;
    private boolean isDragging = false;

    // Right-click annotation state
    private int rightDragFrom = -1;
    private final List<Arrow> userArrows = new ArrayList<>();
    private final Map<Integer, java.awt.Color> userCircles = new HashMap<>();

    // Hint / coaching / error overlays
    private final List<Arrow> hintArrows = new ArrayList<>();
    private int hintSquare = -1;
    private int flashErrorSq = -1;
    private long flashErrorUntil = 0;

    // Smooth piece move animation
    private Piece animPiece = null;
    private int animFrom = -1;
    private int animTo = -1;
    private long animStartNs = 0;
    private static final long ANIM_DURATION_NS = 135_000_000L;
    private final javax.swing.Timer animTimer;

    public ChessBoardPanel() {
        setBackground(Theme.BG_MAIN);
        setOpaque(true);
        setPreferredSize(new Dimension(600, 600));
        setMinimumSize(new Dimension(360, 360));

        animTimer = new javax.swing.Timer(12, e -> {
            if (System.nanoTime() - animStartNs >= ANIM_DURATION_NS) {
                animPiece = null;
                animFrom = -1;
                animTo = -1;
                ((javax.swing.Timer) e.getSource()).stop();
            }
            repaint();
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int sq = pointToSquare(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e)) {
                    rightDragFrom = sq;
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                // Left click clears right-click annotations
                if (!userArrows.isEmpty() || !userCircles.isEmpty()) {
                    userArrows.clear();
                    userCircles.clear();
                    repaint();
                }

                if (!interactive || sq < 0) return;

                // If clicking a legal target for an already-selected piece, execute move
                if (selectedSq != -1) {
                    Move matched = findMatchingSelectedMove(sq);
                    if (matched != null) {
                        handleMoveChoice(selectedSq, sq);
                        clearSelection();
                        return;
                    }
                }

                // Otherwise select/drag piece of side-to-move
                Piece p = state.getPiece(sq);
                if (p != null && p.color() == state.getSideToMove()
                        && (allowedMoveColor == null || p.color() == allowedMoveColor)) {
                    selectedSq = sq;
                    selectedMoves = state.generateLegalMovesFrom(sq);
                    dragFromSq = sq;
                    dragPiece = p;
                    dragPoint = e.getPoint();
                    isDragging = false;
                    repaint();
                } else {
                    clearSelection();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && dragFromSq != -1 && dragPiece != null) {
                    isDragging = true;
                    dragPoint = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int sq = pointToSquare(e.getPoint());
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (rightDragFrom != -1 && sq != -1) {
                        java.awt.Color col = arrowColorFromModifiers(e);
                        if (rightDragFrom == sq) {
                            if (userCircles.containsKey(sq)) userCircles.remove(sq);
                            else userCircles.put(sq, col);
                        } else {
                            toggleUserArrow(rightDragFrom, sq, col);
                        }
                        repaint();
                    }
                    rightDragFrom = -1;
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e) && dragFromSq != -1) {
                    if (isDragging && sq != -1 && sq != dragFromSq) {
                        Move matched = findMatchingSelectedMove(sq);
                        if (matched != null) {
                            handleMoveChoice(dragFromSq, sq);
                            clearSelection();
                        }
                    }
                    dragFromSq = -1;
                    dragPiece = null;
                    dragPoint = null;
                    isDragging = false;
                    repaint();
                }
            }
        };

        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setMoveListener(Consumer<Move> listener) {
        this.moveListener = listener;
    }

    public void setPalette(Theme.BoardPalette palette) {
        this.palette = palette;
        repaint();
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        repaint();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) clearSelection();
    }

    public void setAllowedMoveColor(Color color) {
        this.allowedMoveColor = color;
    }

    public void setBoardState(BoardState newState, int lastFrom, int lastTo, boolean animate) {
        if (animate && lastFrom >= 0 && lastTo >= 0) {
            Piece moved = newState.getPiece(lastTo);
            if (moved != null) {
                this.animPiece = moved;
                this.animFrom = lastFrom;
                this.animTo = lastTo;
                this.animStartNs = System.nanoTime();
                if (!animTimer.isRunning()) animTimer.start();
            }
        } else {
            this.animPiece = null;
            this.animFrom = -1;
            this.animTo = -1;
        }
        this.state = newState;
        this.lastFrom = lastFrom;
        this.lastTo = lastTo;
        clearSelection();
        clearHints();
        repaint();
    }

    public void setHintArrow(int from, int to, java.awt.Color color) {
        hintArrows.clear();
        hintSquare = -1;
        if (from >= 0 && to >= 0) {
            hintArrows.add(new Arrow(from, to, color));
        }
        repaint();
    }

    public void setHintSquare(int sq) {
        hintArrows.clear();
        hintSquare = sq;
        repaint();
    }

    public void clearHints() {
        hintArrows.clear();
        hintSquare = -1;
        repaint();
    }

    public void flashErrorSquare(int sq) {
        this.flashErrorSq = sq;
        this.flashErrorUntil = System.currentTimeMillis() + 550;
        javax.swing.Timer t = new javax.swing.Timer(570, e -> {
            flashErrorSq = -1;
            repaint();
        });
        t.setRepeats(false);
        t.start();
        repaint();
    }

    private void clearSelection() {
        selectedSq = -1;
        selectedMoves = List.of();
        dragFromSq = -1;
        dragPiece = null;
        isDragging = false;
        repaint();
    }

    private Move findMatchingSelectedMove(int toSq) {
        for (Move m : selectedMoves) {
            if (m.to == toSq) return m;
        }
        return null;
    }

    private void handleMoveChoice(int from, int to) {
        List<Move> matches = new ArrayList<>();
        for (Move m : selectedMoves) {
            if (m.from == from && m.to == to) matches.add(m);
        }
        if (matches.isEmpty()) return;
        Move chosen;
        if (matches.size() == 1) {
            chosen = matches.get(0);
        } else {
            PieceType promo = showPromotionPicker(state.getSideToMove());
            if (promo == null) return;
            chosen = matches.stream().filter(m -> m.promotion == promo).findFirst().orElse(matches.get(0));
        }
        if (moveListener != null) {
            moveListener.accept(chosen);
        }
    }

    private PieceType showPromotionPicker(Color side) {
        PieceType[] opts = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
        JPanel panel = new JPanel(new GridLayout(1, 4, 8, 8));
        panel.setBackground(Theme.BG_SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        final PieceType[] result = {PieceType.QUEEN};
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Choose Promotion", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);

        for (PieceType pt : opts) {
            BufferedImage icon = PieceRenderer.getPieceImage(new Piece(side, pt), 64);
            JButton b = new JButton(new ImageIcon(icon));
            b.setBackground(Theme.BG_CARD);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                result[0] = pt;
                dlg.dispose();
            });
            panel.add(b);
        }

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_GREEN, 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        dlg.setContentPane(panel);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        return result[0];
    }

    private java.awt.Color arrowColorFromModifiers(MouseEvent e) {
        if (e.isShiftDown()) return new java.awt.Color(0xE0, 0x45, 0x45, 195);
        if (e.isAltDown()) return new java.awt.Color(0x38, 0x93, 0xE8, 195);
        if (e.isControlDown()) return new java.awt.Color(0xE5, 0xA8, 0x2E, 195);
        return new java.awt.Color(0x63, 0xB8, 0x46, 195);
    }

    private void toggleUserArrow(int from, int to, java.awt.Color col) {
        for (int i = 0; i < userArrows.size(); i++) {
            Arrow a = userArrows.get(i);
            if (a.from == from && a.to == to) {
                userArrows.remove(i);
                return;
            }
        }
        userArrows.add(new Arrow(from, to, col));
    }

    private Rectangle getBoardBounds() {
        int size = Math.min(getWidth(), getHeight());
        int sqSize = Math.max(32, size / 8);
        int boardPx = sqSize * 8;
        int ox = (getWidth() - boardPx) / 2;
        int oy = (getHeight() - boardPx) / 2;
        return new Rectangle(ox, oy, boardPx, boardPx);
    }

    private int pointToSquare(Point pt) {
        Rectangle b = getBoardBounds();
        if (!b.contains(pt)) return -1;
        int sqSize = b.width / 8;
        int visCol = (pt.x - b.x) / sqSize;
        int visRow = (pt.y - b.y) / sqSize;
        if (visCol < 0 || visCol > 7 || visRow < 0 || visRow > 7) return -1;
        int row = flipped ? 7 - visRow : visRow;
        int col = flipped ? 7 - visCol : visCol;
        return row * 8 + col;
    }

    private Point squareTopLeft(int sq, Rectangle b) {
        int sqSize = b.width / 8;
        int row = BoardState.rowOf(sq);
        int col = BoardState.colOf(sq);
        int visRow = flipped ? 7 - row : row;
        int visCol = flipped ? 7 - col : col;
        return new Point(b.x + visCol * sqSize, b.y + visRow * sqSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Rectangle b = getBoardBounds();
        int sqSize = b.width / 8;

        // Subtle outer board frame shadow
        g2.setColor(new java.awt.Color(0, 0, 0, 110));
        g2.fillRoundRect(b.x - 3, b.y - 3, b.width + 6, b.height + 6, 8, 8);

        int checkKingSq = state.isInCheck() ? state.findKingSquare(state.getSideToMove()) : -1;

        // 1. Draw squares & highlights
        for (int sq = 0; sq < 64; sq++) {
            int row = BoardState.rowOf(sq);
            int col = BoardState.colOf(sq);
            boolean isLight = ((row + col) & 1) == 0;
            Point tl = squareTopLeft(sq, b);

            java.awt.Color sqColor = isLight ? palette.lightSquare : palette.darkSquare;
            if (sq == lastFrom || sq == lastTo) {
                sqColor = isLight ? palette.lastMoveLight : palette.lastMoveDark;
            }
            if (sq == selectedSq) {
                sqColor = palette.selectedSquare;
            }

            g2.setColor(sqColor);
            g2.fillRect(tl.x, tl.y, sqSize, sqSize);

            if (sq == hintSquare) {
                g2.setColor(new java.awt.Color(0x38, 0x93, 0xE8, 125));
                g2.fillRect(tl.x, tl.y, sqSize, sqSize);
            }

            if (sq == flashErrorSq && System.currentTimeMillis() < flashErrorUntil) {
                g2.setColor(new java.awt.Color(0xE0, 0x3C, 0x3C, 150));
                g2.fillRect(tl.x, tl.y, sqSize, sqSize);
            }

            // Radial red check halo on king
            if (sq == checkKingSq) {
                float cx = tl.x + sqSize / 2f;
                float cy = tl.y + sqSize / 2f;
                RadialGradientPaint rgp = new RadialGradientPaint(
                        new Point2D.Float(cx, cy), sqSize * 0.55f,
                        new float[]{0f, 0.55f, 1f},
                        new java.awt.Color[]{
                                new java.awt.Color(255, 25, 25, 230),
                                new java.awt.Color(220, 0, 0, 145),
                                new java.awt.Color(180, 0, 0, 0)
                        });
                g2.setPaint(rgp);
                g2.fillRect(tl.x, tl.y, sqSize, sqSize);
            }

            // User right-click highlight circle
            java.awt.Color circleCol = userCircles.get(sq);
            if (circleCol != null) {
                g2.setColor(circleCol);
                g2.setStroke(new BasicStroke(Math.max(3f, sqSize * 0.065f)));
                int pad = Math.max(4, sqSize / 14);
                g2.drawOval(tl.x + pad, tl.y + pad, sqSize - pad * 2, sqSize - pad * 2);
            }
        }

        // 2. Coordinates inside edge squares
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(10, sqSize / 6)));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < 8; i++) {
            // Left edge ranks
            int row = flipped ? 7 - i : i;
            int col0 = flipped ? 7 : 0;
            boolean isLight = ((row + col0) & 1) == 0;
            g2.setColor(isLight ? palette.darkSquare : palette.lightSquare);
            String rankStr = String.valueOf((char) ('8' - row));
            g2.drawString(rankStr, b.x + 4, b.y + i * sqSize + fm.getAscent() + 2);

            // Bottom edge files
            int row7 = flipped ? 0 : 7;
            int col = flipped ? 7 - i : i;
            boolean isLightB = ((row7 + col) & 1) == 0;
            g2.setColor(isLightB ? palette.darkSquare : palette.lightSquare);
            String fileStr = String.valueOf((char) ('a' + col));
            int fw = fm.stringWidth(fileStr);
            g2.drawString(fileStr, b.x + (i + 1) * sqSize - fw - 4, b.y + b.height - 4);
        }

        // 3. Legal move indicators (dots on empty squares, corner rings on captures)
        Set<Integer> drawnTargets = new HashSet<>();
        for (Move m : selectedMoves) {
            if (!drawnTargets.add(m.to)) continue;
            Point tl = squareTopLeft(m.to, b);
            boolean hasPiece = state.getPiece(m.to) != null || m.isEnPassant;
            g2.setColor(new java.awt.Color(20, 85, 30, 125));
            if (hasPiece) {
                g2.setStroke(new BasicStroke(Math.max(4f, sqSize * 0.085f)));
                int pad = Math.max(4, sqSize / 12);
                g2.drawOval(tl.x + pad, tl.y + pad, sqSize - pad * 2, sqSize - pad * 2);
            } else {
                int r = Math.max(7, sqSize / 7);
                g2.fillOval(tl.x + sqSize / 2 - r, tl.y + sqSize / 2 - r, r * 2, r * 2);
            }
        }

        // 4. Draw pieces
        for (int sq = 0; sq < 64; sq++) {
            if (isDragging && sq == dragFromSq) continue;
            if (animPiece != null && sq == animTo) continue;
            Piece p = state.getPiece(sq);
            if (p == null) continue;
            Point tl = squareTopLeft(sq, b);
            BufferedImage img = PieceRenderer.getPieceImage(p, sqSize);
            if (img != null) {
                g2.drawImage(img, tl.x, tl.y, null);
            }
        }

        // 5. Animated sliding piece
        if (animPiece != null && animFrom >= 0 && animTo >= 0) {
            double t = Math.min(1.0, (System.nanoTime() - animStartNs) / (double) ANIM_DURATION_NS);
            double ease = 1.0 - Math.pow(1.0 - t, 3);
            Point p1 = squareTopLeft(animFrom, b);
            Point p2 = squareTopLeft(animTo, b);
            int ax = (int) Math.round(p1.x + (p2.x - p1.x) * ease);
            int ay = (int) Math.round(p1.y + (p2.y - p1.y) * ease);
            BufferedImage img = PieceRenderer.getPieceImage(animPiece, sqSize);
            if (img != null) g2.drawImage(img, ax, ay, null);
        }

        // 6. Arrows (hint arrows + user arrows)
        for (Arrow a : hintArrows) drawArrow(g2, a, b, sqSize);
        for (Arrow a : userArrows) drawArrow(g2, a, b, sqSize);

        // 7. Dragged piece floating on top of cursor
        if (isDragging && dragPiece != null && dragPoint != null) {
            int dragSize = (int) (sqSize * 1.12);
            BufferedImage img = PieceRenderer.getPieceImage(dragPiece, dragSize);
            if (img != null) {
                g2.drawImage(img, dragPoint.x - dragSize / 2, dragPoint.y - dragSize / 2, null);
            }
        }

        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, Arrow arrow, Rectangle b, int sqSize) {
        Point p1 = squareTopLeft(arrow.from, b);
        Point p2 = squareTopLeft(arrow.to, b);
        double x1 = p1.x + sqSize / 2.0;
        double y1 = p1.y + sqSize / 2.0;
        double x2 = p2.x + sqSize / 2.0;
        double y2 = p2.y + sqSize / 2.0;

        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;

        double ux = dx / len;
        double uy = dy / len;
        double headLen = sqSize * 0.42;
        double headWidth = sqSize * 0.34;
        double shaftWidth = sqSize * 0.16;

        double sx = x1 + ux * (sqSize * 0.18);
        double sy = y1 + uy * (sqSize * 0.18);
        double ex = x2 - ux * (headLen * 0.72);
        double ey = y2 - uy * (headLen * 0.72);

        g2.setColor(arrow.color);
        g2.setStroke(new BasicStroke((float) shaftWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(sx, sy, ex, ey));

        // Arrowhead triangle
        double px = -uy;
        double py = ux;
        Path2D head = new Path2D.Double();
        head.moveTo(x2, y2);
        head.lineTo(x2 - ux * headLen + px * headWidth * 0.5, y2 - uy * headLen + py * headWidth * 0.5);
        head.lineTo(x2 - ux * headLen - px * headWidth * 0.5, y2 - uy * headLen - py * headWidth * 0.5);
        head.closePath();
        g2.fill(head);
    }
}
