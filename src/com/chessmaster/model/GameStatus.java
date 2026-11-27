package com.chessmaster.model;

public enum GameStatus {
    IN_PROGRESS("In Progress", false),
    WHITE_WINS_CHECKMATE("White wins by checkmate", true),
    BLACK_WINS_CHECKMATE("Black wins by checkmate", true),
    DRAW_STALEMATE("Draw by stalemate", true),
    DRAW_INSUFFICIENT_MATERIAL("Draw by insufficient material", true),
    DRAW_FIFTY_MOVE("Draw by 50-move rule", true),
    DRAW_THREEFOLD_REPETITION("Draw by threefold repetition", true),
    WHITE_WINS_TIMEOUT("White wins on time", true),
    BLACK_WINS_TIMEOUT("Black wins on time", true),
    WHITE_WINS_RESIGNATION("White wins by resignation", true),
    BLACK_WINS_RESIGNATION("Black wins by resignation", true),
    DRAW_AGREEMENT("Draw by agreement", true);

    private final String description;
    private final boolean gameOver;

    GameStatus(String description, boolean gameOver) {
        this.description = description;
        this.gameOver = gameOver;
    }

    public String description() {
        return description;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}
