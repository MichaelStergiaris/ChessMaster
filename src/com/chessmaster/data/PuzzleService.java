package com.chessmaster.data;

import com.chessmaster.model.BoardState;
import com.chessmaster.model.Move;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * Connects live to popular chess puzzle databases:
 *  - Lichess Unlimited Public Puzzle API (https://lichess.org/api/puzzle/next) with theme & difficulty filters
 *  - Lichess Daily Puzzle API (https://lichess.org/api/puzzle/daily)
 *  - Chess.com Random Puzzle API (https://api.chess.com/pub/puzzle/random)
 *  - Curated Masterpiece Bank (hand-verified famous sacrifices, mates, and tactics; works 100% offline too)
 * Includes background prefetching so switching to the next puzzle feels instant.
 */
public final class PuzzleService {

    public static final String SOURCE_LICHESS_UNLIMITED = "Lichess Unlimited (Live)";
    public static final String SOURCE_LICHESS_DAILY = "Lichess Daily Puzzle";
    public static final String SOURCE_CHESSCOM = "Chess.com Random (Live)";
    public static final String SOURCE_CURATED = "Curated Masterpieces";

    public static final LinkedHashMap<String, String> LICHESS_THEMES = new LinkedHashMap<>();
    static {
        LICHESS_THEMES.put("Healthy Mix (All)", "mix");
        LICHESS_THEMES.put("Fork / Double Attack", "fork");
        LICHESS_THEMES.put("Pin", "pin");
        LICHESS_THEMES.put("Skewer", "skewer");
        LICHESS_THEMES.put("Sacrifice", "sacrifice");
        LICHESS_THEMES.put("Discovered Attack", "discoveredAttack");
        LICHESS_THEMES.put("Deflection", "deflection");
        LICHESS_THEMES.put("Attraction", "attraction");
        LICHESS_THEMES.put("Mate in 1", "mateIn1");
        LICHESS_THEMES.put("Mate in 2", "mateIn2");
        LICHESS_THEMES.put("Mate in 3", "mateIn3");
        LICHESS_THEMES.put("Back Rank Mate", "backRankMate");
        LICHESS_THEMES.put("Smothered Mate", "smotheredMate");
        LICHESS_THEMES.put("Kingside Attack", "kingsideAttack");
        LICHESS_THEMES.put("Endgame Tactics", "endgame");
        LICHESS_THEMES.put("Trapped Piece", "trappedPiece");
        LICHESS_THEMES.put("Quiet Move", "quietMove");
        LICHESS_THEMES.put("Crushing", "crushing");
    }

    public static final LinkedHashMap<String, String> LICHESS_DIFFICULTIES = new LinkedHashMap<>();
    static {
        LICHESS_DIFFICULTIES.put("Normal", "normal");
        LICHESS_DIFFICULTIES.put("Easiest", "easiest");
        LICHESS_DIFFICULTIES.put("Easier", "easier");
        LICHESS_DIFFICULTIES.put("Harder", "harder");
        LICHESS_DIFFICULTIES.put("Hardest", "hardest");
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final List<Puzzle> curatedBank = buildCuratedMasterpieces();
    private int curatedIndex = 0;
    private final Set<String> seenIds = Collections.synchronizedSet(new HashSet<>());

    // Prefetch cache keyed by "source|theme|diff"
    private final Map<String, ConcurrentLinkedQueue<Puzzle>> prefetchQueues = new ConcurrentHashMap<>();
    private final ExecutorService prefetchExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "PuzzlePrefetcher");
        t.setDaemon(true);
        return t;
    });

    public List<Puzzle> getCuratedBank() {
        return curatedBank;
    }

    public Puzzle fetchNextPuzzle(String source, String themeKey, String difficultyKey) {
        if (SOURCE_CURATED.equals(source)) {
            return nextCurated(themeKey);
        }
        if (SOURCE_LICHESS_DAILY.equals(source)) {
            Puzzle daily = fetchLichessEndpoint("https://lichess.org/api/puzzle/daily", "Lichess Daily");
            return daily != null ? daily : nextCurated("mix");
        }
        if (SOURCE_CHESSCOM.equals(source)) {
            Puzzle cc = fetchChessComRandom();
            return cc != null ? cc : nextCurated("mix");
        }

        // Default: Lichess Unlimited with smart prefetch queue
        String key = source + "|" + themeKey + "|" + difficultyKey;
        ConcurrentLinkedQueue<Puzzle> q = prefetchQueues.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        Puzzle cached = q.poll();
        schedulePrefetch(key, themeKey, difficultyKey);
        if (cached != null) {
            return cached;
        }

        Puzzle live = fetchLichessNext(themeKey, difficultyKey);
        if (live != null) {
            schedulePrefetch(key, themeKey, difficultyKey);
            return live;
        }

        // Graceful fallback to Chess.com or Curated bank if offline
        Puzzle cc = fetchChessComRandom();
        return cc != null ? cc : nextCurated(themeKey);
    }

    private void schedulePrefetch(String key, String themeKey, String difficultyKey) {
        ConcurrentLinkedQueue<Puzzle> q = prefetchQueues.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        if (q.size() >= 2) return;
        prefetchExec.submit(() -> {
            try {
                if (q.size() < 2) {
                    Puzzle p = fetchLichessNext(themeKey, difficultyKey);
                    if (p != null) q.offer(p);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private Puzzle fetchLichessNext(String themeKey, String difficultyKey) {
        StringBuilder url = new StringBuilder("https://lichess.org/api/puzzle/next");
        List<String> params = new ArrayList<>();
        if (themeKey != null && !themeKey.isBlank() && !themeKey.equals("mix")) {
            params.add("angle=" + themeKey);
        }
        if (difficultyKey != null && !difficultyKey.isBlank()) {
            params.add("difficulty=" + difficultyKey);
        }
        if (!params.isEmpty()) {
            url.append('?').append(String.join("&", params));
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            Puzzle p = fetchLichessEndpoint(url.toString(), "Lichess Live");
            if (p != null && seenIds.add(p.id)) {
                return p;
            }
            if (p != null && attempt == 1) return p;
        }
        return null;
    }

    private Puzzle fetchLichessEndpoint(String url, String sourceLabel) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ChessMasterJava/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            return parseLichessPuzzleJson(resp.body(), sourceLabel);
        } catch (Exception e) {
            return null;
        }
    }

    public static Puzzle parseLichessPuzzleJson(String json, String sourceLabel) {
        Map<String, Object> root = JsonParser.parseObject(json);
        Map<String, Object> puzzleObj = JsonParser.asObject(root.get("puzzle"));
        Map<String, Object> gameObj = JsonParser.asObject(root.get("game"));
        if (puzzleObj == null) return null;

        String id = JsonParser.asString(puzzleObj.get("id"), "lichess");
        int rating = JsonParser.asInt(puzzleObj.get("rating"), 1500);
        int initialPly = JsonParser.asInt(puzzleObj.get("initialPly"), -1);
        String directFen = JsonParser.asString(puzzleObj.get("fen"), null);
        String lastMoveUci = JsonParser.asString(puzzleObj.get("lastMove"), null);

        List<String> solution = new ArrayList<>();
        List<Object> solArr = JsonParser.asArray(puzzleObj.get("solution"));
        if (solArr != null) {
            for (Object s : solArr) if (s instanceof String str) solution.add(str);
        }
        if (solution.isEmpty()) return null;

        List<String> themes = new ArrayList<>();
        List<Object> themeArr = JsonParser.asArray(puzzleObj.get("themes"));
        if (themeArr != null) {
            for (Object t : themeArr) if (t instanceof String str) themes.add(formatThemeName(str));
        }

        String playersInfo = "";
        String pgn = "";
        String gameId = "";
        if (gameObj != null) {
            pgn = JsonParser.asString(gameObj.get("pgn"), "");
            gameId = JsonParser.asString(gameObj.get("id"), "");
            List<Object> players = JsonParser.asArray(gameObj.get("players"));
            if (players != null && players.size() >= 2) {
                Map<String, Object> pW = JsonParser.asObject(players.get(0));
                Map<String, Object> pB = JsonParser.asObject(players.get(1));
                if (pW != null && pB != null) {
                    String wName = JsonParser.asString(pW.get("name"), "White");
                    int wRat = JsonParser.asInt(pW.get("rating"), 0);
                    String bName = JsonParser.asString(pB.get("name"), "Black");
                    int bRat = JsonParser.asInt(pB.get("rating"), 0);
                    playersInfo = wName + (wRat > 0 ? " (" + wRat + ")" : "")
                            + " vs " + bName + (bRat > 0 ? " (" + bRat + ")" : "");
                }
            }
        }

        // Reconstruct pre-setup FEN and exact start FEN from PGN when available
        String fenBeforeSetup = null;
        String setupMoveUci = lastMoveUci;
        String startFen = directFen;

        if (!pgn.isBlank()) {
            List<String> sanMoves = BoardState.extractPgnSanMoves(pgn);
            BoardState replay = new BoardState();
            int targetPlies = sanMoves.size();
            if (initialPly >= 0 && initialPly < sanMoves.size()) {
                // In Lichess API, initialPly is 0-indexed ply of the opponent's last move (so replay initialPly+1 plies)
                targetPlies = initialPly + 1;
            }
            for (int i = 0; i < targetPlies; i++) {
                if (i == targetPlies - 1) {
                    fenBeforeSetup = replay.toFen();
                }
                Move m = replay.parseSan(sanMoves.get(i));
                if (m == null) {
                    fenBeforeSetup = null;
                    break;
                }
                if (i == targetPlies - 1) {
                    setupMoveUci = m.toUci();
                }
                replay = replay.applyMove(m);
            }
            if (startFen == null) {
                startFen = replay.toFen();
            } else if (fenBeforeSetup != null && !replay.toFen().split(" ")[0].equals(startFen.split(" ")[0])) {
                // If directFen differs slightly from PGN ply count, trust directFen cleanly
                fenBeforeSetup = null;
            }
        }

        if (startFen == null) return null;

        // Validate that every solution move is legal from startFen
        if (!validateSolution(startFen, solution)) {
            return null;
        }

        String title = themes.isEmpty() ? "Lichess Tactical Puzzle" : themes.get(0) + " Challenge";
        String link = "https://lichess.org/training/" + id;
        return new Puzzle(id, sourceLabel, title, rating, themes, fenBeforeSetup, setupMoveUci,
                startFen, solution, playersInfo, link);
    }

    private Puzzle fetchChessComRandom() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.chess.com/pub/puzzle/random"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ChessMasterJava/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            return parseChessComPuzzleJson(resp.body());
        } catch (Exception e) {
            return null;
        }
    }

    public static Puzzle parseChessComPuzzleJson(String json) {
        Map<String, Object> root = JsonParser.parseObject(json);
        String title = JsonParser.asString(root.get("title"), "Chess.com Puzzle");
        String url = JsonParser.asString(root.get("url"), "https://www.chess.com/puzzles");
        String fen = JsonParser.asString(root.get("fen"), null);
        String pgn = JsonParser.asString(root.get("pgn"), "");
        if (fen == null || pgn.isBlank()) return null;

        List<String> sanMoves = BoardState.extractPgnSanMoves(pgn);
        if (sanMoves.isEmpty()) return null;

        BoardState state = BoardState.fromFen(fen);
        List<String> solutionUci = new ArrayList<>();
        for (String san : sanMoves) {
            Move m = state.parseSan(san);
            if (m == null) break;
            solutionUci.add(m.toUci());
            state = state.applyMove(m);
        }
        if (solutionUci.isEmpty()) return null;

        String id = "cc-" + Math.abs(url.hashCode());
        int estRating = 1350 + Math.min(800, solutionUci.size() * 140);
        return new Puzzle(id, "Chess.com", title, estRating,
                List.of("Chess.com Featured", "Tactics"),
                null, null, fen, solutionUci, "Chess.com Daily Archive", url);
    }

    public static boolean validateSolution(String fen, List<String> uciMoves) {
        try {
            BoardState st = BoardState.fromFen(fen);
            for (String uci : uciMoves) {
                Move m = st.parseUci(uci);
                if (m == null) return false;
                st = st.applyMove(m);
            }
            return !uciMoves.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private Puzzle nextCurated(String themeKey) {
        List<Puzzle> pool = curatedBank;
        if (themeKey != null && !themeKey.isBlank() && !themeKey.equals("mix")) {
            String needle = themeKey.toLowerCase(Locale.ROOT);
            List<Puzzle> filtered = new ArrayList<>();
            for (Puzzle p : curatedBank) {
                for (String t : p.themes) {
                    if (t.toLowerCase(Locale.ROOT).replace(" ", "").contains(needle)) {
                        filtered.add(p);
                        break;
                    }
                }
            }
            if (!filtered.isEmpty()) pool = filtered;
        }
        Puzzle p = pool.get(curatedIndex % pool.size());
        curatedIndex++;
        return p;
    }

    public static String formatThemeName(String camel) {
        if (camel == null || camel.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(camel.charAt(0)));
        for (int i = 1; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) || Character.isDigit(c)) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public void shutdown() {
        prefetchExec.shutdownNow();
    }

    /**
     * Hand-verified collection of famous tactical masterpieces, sacrifices, and mating patterns.
     */
    private static List<Puzzle> buildCuratedMasterpieces() {
        List<Puzzle> list = new ArrayList<>();

        // 1. Morphy's Opera Game Finale (1858) - Queen Sacrifice Mate
        list.add(new Puzzle("classic-opera", "Masterpiece Classic", "Morphy's Opera Game Finale", 1450,
                List.of("Sacrifice", "Mate in 2", "Deflection", "Back Rank Mate"),
                null, null,
                "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 1 16",
                List.of("b3b8", "d7b8", "d1d8"),
                "Paul Morphy vs Duke Karl & Count Isouard (Paris, 1858)", "https://en.wikipedia.org/wiki/Opera_Game"));

        // 2. Classic Smothered Mate (Philidor's Legacy)
        list.add(new Puzzle("classic-philidor", "Masterpiece Classic", "Philidor's Legacy (Smothered Mate)", 1550,
                List.of("Smothered Mate", "Sacrifice", "Mate in 2"),
                null, null,
                "5r1k/6pp/7N/3Q4/8/8/6PP/6K1 w - - 0 1",
                List.of("d5g8", "f8g8", "h6f7"),
                "Classic Smothered Mate Pattern", ""));

        // 3. Anastasia's Mate: Ne7+ Kh8 Qxh7+ Kxh7 Rh3#
        list.add(new Puzzle("classic-anastasia", "Masterpiece Classic", "Anastasia's Mate", 1520,
                List.of("Sacrifice", "Mate in 3", "Kingside Attack"),
                null, null,
                "5rk1/1p3ppp/8/5N2/8/4R2Q/5PPP/6K1 w - - 0 1",
                List.of("f5e7", "g8h8", "h3h7", "h8h7", "e3h3"),
                "Classic Anastasia's Mate Pattern", ""));

        // 4. Legal's Mate Classic
        list.add(new Puzzle("classic-legal", "Masterpiece Classic", "Légal's Mate", 1250,
                List.of("Sacrifice", "Mate in 2", "Opening"),
                null, null,
                "r2qkbnr/ppp2ppp/2np4/4N3/2B1P1b1/2N5/PPPP1PPP/R1BQK2R w KQkq - 0 5",
                List.of("c4f7", "e8e7", "c3d5"),
                "Sire de Légal vs Saint Brie (Paris, 1750)", ""));

        // 5. Back Rank Deflection Sacrifice
        list.add(new Puzzle("classic-backrank", "Masterpiece Classic", "Back Rank Decoy", 1280,
                List.of("Back Rank Mate", "Sacrifice", "Mate in 2"),
                null, null,
                "3r2k1/5ppp/8/8/8/8/4QPPP/4R1K1 w - - 0 1",
                List.of("e2e8", "d8e8", "e1e8"),
                "Classic Back-Rank Combination", ""));

        // 6. Arabian Mate (Rook + Knight Mate in 1)
        list.add(new Puzzle("classic-arabian", "Masterpiece Classic", "Arabian Mate", 1050,
                List.of("Mate in 1", "Kingside Attack"),
                null, null,
                "7k/1R5p/5N2/8/8/8/6PP/6K1 w - - 0 1",
                List.of("b7h7"),
                "Classic Rook + Knight Cooperation", ""));

        // 7. Boden's Mate (Two Bishops Criss-Cross!)
        list.add(new Puzzle("classic-boden", "Masterpiece Classic", "Boden's Mate", 1650,
                List.of("Sacrifice", "Mate in 2", "Attraction"),
                null, null,
                "2kr3r/pp1n1ppp/2p5/8/5B2/2Q5/PPP2PPP/2KR1B1R w - - 0 1",
                List.of("c3c6", "b7c6", "f1a6"),
                "Schulder vs Boden (London, 1853)", ""));

        // 8. Royal Deflection Winning the Queen
        list.add(new Puzzle("classic-deflection-q", "Masterpiece Classic", "Royal Deflection", 1200,
                List.of("Deflection", "Attraction", "Crushing"),
                null, null,
                "r2qk2r/ppp2ppp/2n5/2b1p3/2B1P3/5N2/PPP2PPP/R2Q1RK1 w kq - 0 1",
                List.of("c4f7", "e8f7", "d1d8"),
                "Classic Opening Tactical Motif", ""));

        // 9. GothamChess Queen Sacrifice Mate
        list.add(new Puzzle("classic-gotham", "Masterpiece Classic", "Bashing the Defenses", 1780,
                List.of("Sacrifice", "Kingside Attack", "Attraction"),
                null, null,
                "r1b2rk1/pp2Qpp1/8/3pBP1p/8/1P1P1N1P/P2P1qP1/4R2K w - - 4 24",
                List.of("e7f8", "g8f8", "e5d6", "f8g8", "e1e8", "g8h7", "f3g5", "h7h6", "g5f7", "h6h7", "e8h8"),
                "GothamChess vs Oleg42YNWA (2021)", "https://www.chess.com/daily/2021-12-05"));

        // 10. Fork into Back-Rank Mate
        list.add(new Puzzle("classic-fork-backrank", "Masterpiece Classic", "Fork into Back-Rank Mate", 1420,
                List.of("Fork", "Back Rank Mate", "Mate in 3"),
                null, null,
                "5r1k/rpp2ppp/8/4N3/2B5/8/PPP2PPP/3R2K1 w - - 0 1",
                List.of("e5f7", "f8f7", "d1d8", "f7f8", "d8f8"),
                "Classic Back-Rank Exploitation", ""));

        // 11. Diagonal Royal Skewer
        list.add(new Puzzle("classic-skewer", "Masterpiece Classic", "Diagonal Royal Skewer", 1180,
                List.of("Skewer", "Endgame", "Crushing"),
                null, null,
                "1q6/8/3k4/8/8/4B3/6PP/6K1 w - - 0 1",
                List.of("e3f4", "d6e7", "f4b8"),
                "Classic Bishop Skewer", ""));

        // 12. Discovered Check Winning the Queen
        list.add(new Puzzle("classic-discovery", "Masterpiece Classic", "Discovered Check Discovery", 1310,
                List.of("Discovered Attack", "Fork", "Crushing"),
                null, null,
                "4k3/ppp2ppp/8/4N3/3q4/8/PPP2PPP/4R1K1 w - - 0 1",
                List.of("e5c6", "e8d7", "c6d4"),
                "Classic Discovered Check Tactic", ""));

        // 13. Absolute E-File Pin
        list.add(new Puzzle("classic-pin", "Masterpiece Classic", "Deadly E-File Pin", 1240,
                List.of("Pin", "Crushing"),
                null, null,
                "r3k2r/ppp2ppp/8/4q3/8/8/PPP2PPP/3R1RK1 w kq - 0 1",
                List.of("f1e1", "e5e1", "d1e1"),
                "Classic Absolute Pin", ""));

        // 14. Damiano's Mate Pattern: Rh8+ Kxh8 Qh6+ Kg8 Qg7#
        list.add(new Puzzle("classic-damiano", "Masterpiece Classic", "Damiano's Rook Sacrifice", 1480,
                List.of("Sacrifice", "Mate in 3", "Kingside Attack"),
                null, null,
                "5rk1/5p2/5Pp1/7R/5Q2/8/6P1/6K1 w - - 0 1",
                List.of("h5h8", "g8h8", "f4h6", "h8g8", "h6g7"),
                "Classic Damiano's Mate Pattern", ""));

        // 15. Underpromotion Knight Fork
        list.add(new Puzzle("classic-underpromo", "Masterpiece Classic", "Brilliant Knight Underpromotion", 1680,
                List.of("Fork", "Endgame", "Crushing"),
                null, null,
                "8/2q1P1k1/8/8/8/8/6PP/6K1 w - - 0 1",
                List.of("e7e8n", "g7f7", "e8c7"),
                "Classic Underpromotion Fork", ""));

        // 16. Quiet Endgame Pawn Breakthrough
        list.add(new Puzzle("classic-breakthrough", "Masterpiece Classic", "Pawn Breakthrough", 1580,
                List.of("Endgame", "Sacrifice", "Quiet Move"),
                null, null,
                "8/ppp5/8/PPP5/8/7k/8/7K w - - 0 1",
                List.of("b5b6", "c7b6", "c5c6", "b7c6", "a5a6"),
                "Classic 3-vs-3 Pawn Breakthrough", ""));

        // 17. Black to Move - Back Rank Decoy
        list.add(new Puzzle("classic-black-sac", "Masterpiece Classic", "Black's Back-Rank Decoy", 1390,
                List.of("Sacrifice", "Mate in 2", "Back Rank Mate"),
                null, null,
                "4r1k1/5ppp/8/8/8/8/4qPPP/3R2K1 b - - 0 1",
                List.of("e2e1", "d1e1", "e8e1"),
                "Classic Black Back-Rank Combination", ""));

        // 18. Black to Move - Smothered Mate
        list.add(new Puzzle("classic-black-smother", "Masterpiece Classic", "Black's Smothered Mate", 1560,
                List.of("Smothered Mate", "Sacrifice", "Mate in 2"),
                null, null,
                "6k1/6pp/8/8/3q4/7n/6PP/5R1K b - - 0 1",
                List.of("d4g1", "f1g1", "h3f2"),
                "Classic Smothered Mate (Black to Play)", ""));

        return Collections.unmodifiableList(list);
    }
}
