package com.chessmaster.data;

import java.util.List;

/**
 * Represents a tactical chess puzzle from Lichess, Chess.com, or the Curated Masterpiece bank.
 */
public final class Puzzle {
    public final String id;
    public final String source;        // "Lichess Live", "Lichess Daily", "Chess.com", "Masterpiece Classic"
    public final String title;
    public final int rating;
    public final List<String> themes;
    public final String fenBeforeSetup; // Optional: position before opponent's blunder move (for animation)
    public final String setupMoveUci;   // Optional: opponent's last move UCI (e.g. "b3b4")
    public final String startFen;       // Position where the PLAYER makes solutionUci.get(0)
    public final List<String> solutionUci; // Player move 0, opponent reply 1, player move 2, ...
    public final String playersInfo;
    public final String sourceUrl;

    public Puzzle(String id, String source, String title, int rating, List<String> themes,
                  String fenBeforeSetup, String setupMoveUci, String startFen,
                  List<String> solutionUci, String playersInfo, String sourceUrl) {
        this.id = id;
        this.source = source;
        this.title = title;
        this.rating = rating;
        this.themes = List.copyOf(themes);
        this.fenBeforeSetup = fenBeforeSetup;
        this.setupMoveUci = setupMoveUci;
        this.startFen = startFen;
        this.solutionUci = List.copyOf(solutionUci);
        this.playersInfo = playersInfo;
        this.sourceUrl = sourceUrl;
    }
}
