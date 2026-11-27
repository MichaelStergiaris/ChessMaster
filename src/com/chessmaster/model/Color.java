package com.chessmaster.model;

public enum Color {
    WHITE, BLACK;

    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    public String displayName() {
        return this == WHITE ? "White" : "Black";
    }
}
