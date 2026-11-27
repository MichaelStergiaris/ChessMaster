package com.chessmaster.model;

public enum PieceType {
    PAWN('p', "", 1),
    KNIGHT('n', "N", 3),
    BISHOP('b', "B", 3),
    ROOK('r', "R", 5),
    QUEEN('q', "Q", 9),
    KING('k', "K", 0);

    private final char fenChar;
    private final String sanLetter;
    private final int materialValue;

    PieceType(char fenChar, String sanLetter, int materialValue) {
        this.fenChar = fenChar;
        this.sanLetter = sanLetter;
        this.materialValue = materialValue;
    }

    public char fenChar(Color color) {
        return color == Color.WHITE ? Character.toUpperCase(fenChar) : fenChar;
    }

    public char uciPromoChar() {
        return fenChar;
    }

    public String sanLetter() {
        return sanLetter;
    }

    public int materialValue() {
        return materialValue;
    }

    public static PieceType fromChar(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'p' -> PAWN;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'r' -> ROOK;
            case 'q' -> QUEEN;
            case 'k' -> KING;
            default -> throw new IllegalArgumentException("Unknown piece char: " + c);
        };
    }
}
