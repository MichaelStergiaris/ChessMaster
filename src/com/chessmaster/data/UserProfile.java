package com.chessmaster.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Persists user puzzle rating, streaks, mastered openings, and preferences locally.
 */
public final class UserProfile {
    private static final Path PROFILE_PATH = Paths.get(System.getProperty("user.home"), ".chessmaster", "profile.json");

    public int puzzleRating = 1400;
    public int currentStreak = 0;
    public int bestStreak = 0;
    public int puzzlesSolved = 0;
    public int puzzlesAttempted = 0;
    public int gamesPlayed = 0;
    public int gamesWon = 0;
    public String boardTheme = "Lichess Wood";
    public boolean soundEnabled = true;
    public final Set<String> masteredOpenings = new LinkedHashSet<>();

    public static UserProfile load() {
        UserProfile p = new UserProfile();
        if (!Files.exists(PROFILE_PATH)) return p;
        try {
            String raw = Files.readString(PROFILE_PATH, StandardCharsets.UTF_8);
            Map<String, Object> m = JsonParser.parseObject(raw);
            p.puzzleRating = JsonParser.asInt(m.get("puzzleRating"), 1400);
            p.currentStreak = JsonParser.asInt(m.get("currentStreak"), 0);
            p.bestStreak = JsonParser.asInt(m.get("bestStreak"), 0);
            p.puzzlesSolved = JsonParser.asInt(m.get("puzzlesSolved"), 0);
            p.puzzlesAttempted = JsonParser.asInt(m.get("puzzlesAttempted"), 0);
            p.gamesPlayed = JsonParser.asInt(m.get("gamesPlayed"), 0);
            p.gamesWon = JsonParser.asInt(m.get("gamesWon"), 0);
            p.boardTheme = JsonParser.asString(m.get("boardTheme"), "Lichess Wood");
            Object snd = m.get("soundEnabled");
            if (snd instanceof Boolean b) p.soundEnabled = b;
            List<Object> mo = JsonParser.asArray(m.get("masteredOpenings"));
            if (mo != null) {
                for (Object item : mo) {
                    if (item instanceof String s) p.masteredOpenings.add(s);
                }
            }
        } catch (Exception ignored) {
        }
        return p;
    }

    public void save() {
        try {
            Files.createDirectories(PROFILE_PATH.getParent());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("puzzleRating", puzzleRating);
            map.put("currentStreak", currentStreak);
            map.put("bestStreak", bestStreak);
            map.put("puzzlesSolved", puzzlesSolved);
            map.put("puzzlesAttempted", puzzlesAttempted);
            map.put("gamesPlayed", gamesPlayed);
            map.put("gamesWon", gamesWon);
            map.put("boardTheme", boardTheme);
            map.put("soundEnabled", soundEnabled);
            map.put("masteredOpenings", new ArrayList<>(masteredOpenings));
            Files.writeString(PROFILE_PATH, JsonParser.stringify(map), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /**
     * Elo-style rating update after a puzzle attempt. Returns the rating delta.
     */
    public int recordPuzzleResult(int puzzleElo, boolean solvedCleanly) {
        puzzlesAttempted++;
        double expected = 1.0 / (1.0 + Math.pow(10.0, (puzzleElo - puzzleRating) / 400.0));
        int k = puzzlesAttempted < 20 ? 32 : 20;
        double score = solvedCleanly ? 1.0 : 0.0;
        int delta = (int) Math.round(k * (score - expected));
        if (solvedCleanly) {
            delta = Math.max(4, delta);
            puzzlesSolved++;
            currentStreak++;
            if (currentStreak > bestStreak) bestStreak = currentStreak;
        } else {
            delta = Math.min(-4, delta);
            currentStreak = 0;
        }
        puzzleRating = Math.max(400, Math.min(3200, puzzleRating + delta));
        save();
        return delta;
    }

    public void markOpeningMastered(String openingId) {
        if (masteredOpenings.add(openingId)) {
            save();
        }
    }
}
