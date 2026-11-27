package com.chessmaster.model;

import java.util.Objects;

public final class Move {
    public final int from;
    public final int to;
    public final PieceType promotion;
    public final boolean isCapture;
    public final boolean isEnPassant;
    public final boolean isCastleKingside;
    public final boolean isCastleQueenside;
    public final boolean isDoublePawnPush;

    public Move(int from, int to, PieceType promotion, boolean isCapture,
                boolean isEnPassant, boolean isCastleKingside,
                boolean isCastleQueenside, boolean isDoublePawnPush) {
        this.from = from;
        this.to = to;
        this.promotion = promotion;
        this.isCapture = isCapture;
        this.isEnPassant = isEnPassant;
        this.isCastleKingside = isCastleKingside;
        this.isCastleQueenside = isCastleQueenside;
        this.isDoublePawnPush = isDoublePawnPush;
    }

    public String toUci() {
        String base = BoardState.squareToAlgebraic(from) + BoardState.squareToAlgebraic(to);
        if (promotion != null) {
            return base + promotion.uciPromoChar();
        }
        return base;
    }

    public boolean matchesUci(String uci) {
        return toUci().equalsIgnoreCase(uci.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move other)) return false;
        return from == other.from && to == other.to && promotion == other.promotion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, promotion);
    }

    @Override
    public String toString() {
        return toUci();
    }
}
