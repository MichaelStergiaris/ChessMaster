package com.chessmaster.model;

import java.util.Objects;

public final class Piece {
    private final Color color;
    private final PieceType type;

    public Piece(Color color, PieceType type) {
        this.color = Objects.requireNonNull(color);
        this.type = Objects.requireNonNull(type);
    }

    public Color color() {
        return color;
    }

    public PieceType type() {
        return type;
    }

    public char fenChar() {
        return type.fenChar(color);
    }

    public static Piece fromFenChar(char c) {
        Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
        return new Piece(color, PieceType.fromChar(c));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece other)) return false;
        return color == other.color && type == other.type;
    }

    @Override
    public int hashCode() {
        return 31 * color.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(fenChar());
    }
}
