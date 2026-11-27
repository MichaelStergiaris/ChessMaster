package com.chessmaster.engine;

import com.chessmaster.model.BoardState;
import com.chessmaster.model.Color;
import com.chessmaster.model.Move;
import com.chessmaster.model.Piece;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Full UCI interface to Stockfish 19 with adjustable difficulty (400 Elo Beginner to 3200+ Stockfish Max),
 * live evaluation bar scoring, best-move hints, MultiPV analysis lines, and a built-in pure-Java fallback engine.
 */
public final class StockfishEngine {

    public static final class DifficultyPreset {
        public final String label;
        public final int elo;
        public final int skillLevel;
        public final int depthCap;
        public final int moveTimeMs;
        public final boolean fullStrength;

        public DifficultyPreset(String label, int elo, int skillLevel, int depthCap, int moveTimeMs, boolean fullStrength) {
            this.label = label;
            this.elo = elo;
            this.skillLevel = skillLevel;
            this.depthCap = depthCap;
            this.moveTimeMs = moveTimeMs;
            this.fullStrength = fullStrength;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final List<DifficultyPreset> PRESETS = List.of(
        new DifficultyPreset("Level 1 — Beginner (400)", 400, 0, 1, 120, false),
        new DifficultyPreset("Level 2 — Novice (750)", 750, 1, 2, 160, false),
        new DifficultyPreset("Level 3 — Casual (1050)", 1050, 3, 4, 220, false),
        new DifficultyPreset("Level 4 — Club (1350)", 1350, 6, 8, 320, false),
        new DifficultyPreset("Level 5 — Intermediate (1650)", 1650, 10, 11, 450, false),
        new DifficultyPreset("Level 6 — Advanced (2000)", 2000, 14, 14, 650, false),
        new DifficultyPreset("Level 7 — Master (2400)", 2400, 17, 16, 850, false),
        new DifficultyPreset("Level 8 — Grandmaster (2800)", 2800, 19, 18, 1100, false),
        new DifficultyPreset("MAX — Stockfish 19 Full Strength", 3500, 20, 20, 1400, true)
    );

    public static final class EvalResult {
        public final int centipawnsWhite; // Positive = White advantage
        public final Integer mateInWhite; // Non-null if mate detected (+N white mates, -N black mates)
        public final String bestMoveUci;
        public final List<PvLine> lines;

        public EvalResult(int centipawnsWhite, Integer mateInWhite, String bestMoveUci, List<PvLine> lines) {
            this.centipawnsWhite = centipawnsWhite;
            this.mateInWhite = mateInWhite;
            this.bestMoveUci = bestMoveUci;
            this.lines = lines;
        }

        public String formattedScore() {
            if (mateInWhite != null) {
                return (mateInWhite > 0 ? "+M" : "-M") + Math.abs(mateInWhite);
            }
            double pawns = centipawnsWhite / 100.0;
            return String.format(Locale.US, "%+.2f", pawns);
        }
    }

    public static final class PvLine {
        public final int multiPv;
        public final int depth;
        public final int centipawnsWhite;
        public final Integer mateInWhite;
        public final List<String> uciMoves;

        public PvLine(int multiPv, int depth, int centipawnsWhite, Integer mateInWhite, List<String> uciMoves) {
            this.multiPv = multiPv;
            this.depth = depth;
            this.centipawnsWhite = centipawnsWhite;
            this.mateInWhite = mateInWhite;
            this.uciMoves = uciMoves;
        }

        public String formattedScore() {
            if (mateInWhite != null) {
                return (mateInWhite > 0 ? "+M" : "-M") + Math.abs(mateInWhite);
            }
            return String.format(Locale.US, "%+.2f", centipawnsWhite / 100.0);
        }
    }

    private Process process;
    private BufferedWriter writer;
    private BufferedReader reader;
    private boolean uciAvailable = false;
    private String engineName = "Built-in Java Engine (Fallback)";
    private final Object lock = new Object();
    private final Random rng = new Random();

    private static final Pattern SCORE_CP = Pattern.compile("\\bscore cp (-?\\d+)");
    private static final Pattern SCORE_MATE = Pattern.compile("\\bscore mate (-?\\d+)");
    private static final Pattern DEPTH_PAT = Pattern.compile("\\bdepth (\\d+)");
    private static final Pattern MULTIPV_PAT = Pattern.compile("\\bmultipv (\\d+)");

    public StockfishEngine() {
        startEngine();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "StockfishShutdown"));
    }

    public boolean isStockfishReady() {
        return uciAvailable;
    }

    public String getEngineName() {
        return engineName;
    }

    private Path resolveStockfishBinary() {
        List<Path> candidates = List.of(
            Paths.get("engine", "stockfish.exe"),
            Paths.get("C:\\AIproject\\ChessMaster\\engine\\stockfish.exe"),
            Paths.get("stockfish.exe")
        );
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) return p.toAbsolutePath();
        }
        return null;
    }

    private void startEngine() {
        synchronized (lock) {
            Path exe = resolveStockfishBinary();
            if (exe == null) {
                uciAvailable = false;
                return;
            }
            try {
                ProcessBuilder pb = new ProcessBuilder(exe.toString());
                pb.redirectErrorStream(true);
                process = pb.start();
                writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                send("uci");
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("id name ")) {
                        engineName = line.substring(8).trim();
                    }
                    if (line.equals("uciok")) {
                        uciAvailable = true;
                        break;
                    }
                }
                if (uciAvailable) {
                    send("setoption name Threads value 2");
                    send("setoption name Hash value 64");
                    send("isready");
                    waitFor("readyok");
                }
            } catch (Exception e) {
                uciAvailable = false;
                shutdown();
            }
        }
    }

    public void newGame() {
        synchronized (lock) {
            if (!ensureAlive()) return;
            try {
                send("ucinewgame");
                send("isready");
                waitFor("readyok");
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Computes Stockfish's move at the requested Elo (400..3500) or full strength.
     */
    public String computeMove(BoardState state, int targetElo, boolean fullStrength) {
        List<Move> legal = state.generateLegalMoves();
        if (legal.isEmpty()) return null;
        if (legal.size() == 1) return legal.get(0).toUci();

        synchronized (lock) {
            if (!ensureAlive()) {
                return fallbackBestMove(state, targetElo);
            }
            try {
                // For very low Elo (< 1320, below Stockfish's native UCI_Elo minimum of 1320),
                // use MultiPV=4 at shallow depth and pick human-like imperfect moves based on Elo.
                if (!fullStrength && targetElo < 1320) {
                    return computeBeginnerMove(state, targetElo, legal);
                }

                send("setoption name MultiPV value 1");
                if (fullStrength || targetElo >= 3200) {
                    send("setoption name UCI_LimitStrength value false");
                    send("setoption name Skill Level value 20");
                } else {
                    int clampedElo = Math.max(1320, Math.min(3190, targetElo));
                    int skill = (int) Math.round(((clampedElo - 1320.0) / (3190.0 - 1320.0)) * 20.0);
                    send("setoption name UCI_LimitStrength value true");
                    send("setoption name UCI_Elo value " + clampedElo);
                    send("setoption name Skill Level value " + Math.max(0, Math.min(20, skill)));
                }
                send("isready");
                waitFor("readyok");

                send("position fen " + state.toFen());
                int moveTime = fullStrength ? 1200 : Math.max(200, Math.min(1000, (targetElo - 1000) / 2));
                send("go movetime " + moveTime);

                String best = null;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("bestmove ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) best = parts[1];
                        break;
                    }
                }
                if (best != null && state.parseUci(best) != null) {
                    return best;
                }
            } catch (Exception ignored) {
            }
        }
        return fallbackBestMove(state, targetElo);
    }

    private String computeBeginnerMove(BoardState state, int targetElo, List<Move> legal) throws IOException {
        int multiPv = Math.min(5, legal.size());
        send("setoption name UCI_LimitStrength value false");
        send("setoption name Skill Level value 0");
        send("setoption name MultiPV value " + multiPv);
        send("isready");
        waitFor("readyok");

        send("position fen " + state.toFen());
        int depth = targetElo < 650 ? 2 : (targetElo < 1000 ? 3 : 5);
        send("go depth " + depth);

        Map<Integer, String> pvMoves = new TreeMap<>();
        String fallbackBest = legal.get(0).toUci();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info ") && line.contains(" pv ")) {
                int pvIdx = 1;
                Matcher mpv = MULTIPV_PAT.matcher(line);
                if (mpv.find()) pvIdx = Integer.parseInt(mpv.group(1));
                String pvPart = line.substring(line.indexOf(" pv ") + 4).trim();
                String firstMove = pvPart.split("\\s+")[0];
                pvMoves.put(pvIdx, firstMove);
            } else if (line.startsWith("bestmove ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) fallbackBest = parts[1];
                break;
            }
        }

        // Reset MultiPV back to 1
        send("setoption name MultiPV value 1");

        // Beginner blunder/inaccuracy probability scaled smoothly by Elo (400 -> 1319)
        double strength = Math.max(0.0, Math.min(1.0, (targetElo - 400.0) / 920.0));
        double roll = rng.nextDouble();

        // Very low Elo occasionally plays a random legal move
        if (targetElo <= 600 && roll < 0.28) {
            return legal.get(rng.nextInt(legal.size())).toUci();
        }
        List<String> candidates = new ArrayList<>(pvMoves.values());
        if (candidates.isEmpty()) return fallbackBest;

        // Higher strength prefers index 0 (best move), lower strength picks from 2nd-5th choices more often
        if (roll < 0.30 + 0.55 * strength) {
            return candidates.get(0);
        }
        int maxIdx = Math.min(candidates.size() - 1, targetElo < 800 ? 4 : (targetElo < 1100 ? 3 : 2));
        return candidates.get(Math.max(0, rng.nextInt(maxIdx + 1)));
    }

    /**
     * Evaluates a position at full Stockfish strength with MultiPV lines (for Eval Bar, Hints, Analysis Board).
     */
    public EvalResult analyzePosition(BoardState state, int multiPvCount, int moveTimeMs) {
        List<Move> legal = state.generateLegalMoves();
        if (legal.isEmpty()) {
            if (state.isInCheck()) {
                int mate = state.getSideToMove() == Color.WHITE ? -0 : 0;
                return new EvalResult(state.getSideToMove() == Color.WHITE ? -10000 : 10000, mate, null, List.of());
            }
            return new EvalResult(0, null, null, List.of());
        }

        synchronized (lock) {
            if (!ensureAlive()) {
                int cp = state.materialBalanceWhite() * 100;
                return new EvalResult(cp, null, fallbackBestMove(state, 1500), List.of());
            }
            try {
                int mpv = Math.max(1, Math.min(multiPvCount, legal.size()));
                send("setoption name UCI_LimitStrength value false");
                send("setoption name Skill Level value 20");
                send("setoption name MultiPV value " + mpv);
                send("isready");
                waitFor("readyok");

                send("position fen " + state.toFen());
                send("go movetime " + Math.max(150, moveTimeMs));

                boolean whiteToMove = state.getSideToMove() == Color.WHITE;
                Map<Integer, PvLine> latestLines = new TreeMap<>();
                String bestMove = null;
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("info ") && line.contains(" pv ")) {
                        int pvIdx = 1;
                        Matcher mPv = MULTIPV_PAT.matcher(line);
                        if (mPv.find()) pvIdx = Integer.parseInt(mPv.group(1));

                        int depth = 1;
                        Matcher mDepth = DEPTH_PAT.matcher(line);
                        if (mDepth.find()) depth = Integer.parseInt(mDepth.group(1));

                        int cpWhite = 0;
                        Integer mateWhite = null;
                        Matcher mMate = SCORE_MATE.matcher(line);
                        Matcher mCp = SCORE_CP.matcher(line);
                        if (mMate.find()) {
                            int rawMate = Integer.parseInt(mMate.group(1));
                            mateWhite = whiteToMove ? rawMate : -rawMate;
                            cpWhite = mateWhite > 0 ? 10000 : -10000;
                        } else if (mCp.find()) {
                            int rawCp = Integer.parseInt(mCp.group(1));
                            cpWhite = whiteToMove ? rawCp : -rawCp;
                        }

                        String pvStr = line.substring(line.indexOf(" pv ") + 4).trim();
                        List<String> moves = Arrays.asList(pvStr.split("\\s+"));
                        latestLines.put(pvIdx, new PvLine(pvIdx, depth, cpWhite, mateWhite, moves));
                    } else if (line.startsWith("bestmove ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) bestMove = parts[1];
                        break;
                    }
                }

                send("setoption name MultiPV value 1");
                List<PvLine> lines = new ArrayList<>(latestLines.values());
                PvLine top = lines.isEmpty() ? null : lines.get(0);
                int cp = top != null ? top.centipawnsWhite : state.materialBalanceWhite() * 100;
                Integer mate = top != null ? top.mateInWhite : null;
                return new EvalResult(cp, mate, bestMove, lines);
            } catch (Exception ignored) {
            }
        }
        return new EvalResult(state.materialBalanceWhite() * 100, null, fallbackBestMove(state, 1500), List.of());
    }

    private boolean ensureAlive() {
        if (uciAvailable && process != null && process.isAlive()) return true;
        startEngine();
        return uciAvailable && process != null && process.isAlive();
    }

    private void send(String cmd) throws IOException {
        writer.write(cmd);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String expected) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(expected)) return;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            uciAvailable = false;
            try {
                if (writer != null) {
                    writer.write("quit\n");
                    writer.flush();
                }
            } catch (Exception ignored) {
            }
            if (process != null) {
                process.destroyForcibly();
                process = null;
            }
        }
    }

    /**
     * Built-in pure-Java alpha-beta fallback (used only if stockfish.exe is removed/unavailable).
     */
    private String fallbackBestMove(BoardState state, int targetElo) {
        List<Move> legal = state.generateLegalMoves();
        if (legal.isEmpty()) return null;
        int depth = targetElo < 1000 ? 1 : 2;
        boolean maximizing = state.getSideToMove() == Color.WHITE;
        Move best = legal.get(0);
        int bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move m : legal) {
            int sc = minimax(state.applyMove(m), depth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, !maximizing);
            if (maximizing ? sc > bestScore : sc < bestScore) {
                bestScore = sc;
                best = m;
            }
        }
        return best.toUci();
    }

    private int minimax(BoardState st, int depth, int alpha, int beta, boolean max) {
        List<Move> moves = st.generateLegalMoves();
        if (moves.isEmpty()) {
            if (st.isInCheck()) return max ? -99999 - depth : 99999 + depth;
            return 0;
        }
        if (depth == 0) {
            int score = st.materialBalanceWhite() * 100;
            for (int sq = 0; sq < 64; sq++) {
                Piece p = st.getPiece(sq);
                if (p == null) continue;
                int r = sq >> 3, c = sq & 7;
                int centerBonus = (3 - Math.abs(r - 3)) + (3 - Math.abs(c - 3));
                score += (p.color() == Color.WHITE ? centerBonus * 4 : -centerBonus * 4);
            }
            return score;
        }
        if (max) {
            int best = Integer.MIN_VALUE;
            for (Move m : moves) {
                best = Math.max(best, minimax(st.applyMove(m), depth - 1, alpha, beta, false));
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (Move m : moves) {
                best = Math.min(best, minimax(st.applyMove(m), depth - 1, alpha, beta, true));
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }
}
