package com.chessmaster.data;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Optional live client for Lichess Masters Opening Explorer API to enrich the Opening Studio
 * with real master-level move frequencies and Win/Draw/Loss bars.
 */
public final class OpeningExplorerClient {

    public static final class ExplorerMove {
        public final String san;
        public final String uci;
        public final int whiteWins;
        public final int draws;
        public final int blackWins;

        public ExplorerMove(String san, String uci, int whiteWins, int draws, int blackWins) {
            this.san = san;
            this.uci = uci;
            this.whiteWins = whiteWins;
            this.draws = draws;
            this.blackWins = blackWins;
        }

        public int totalGames() {
            return whiteWins + draws + blackWins;
        }
    }

    public static final class ExplorerResult {
        public final String openingName;
        public final String eco;
        public final List<ExplorerMove> moves;

        public ExplorerResult(String openingName, String eco, List<ExplorerMove> moves) {
            this.openingName = openingName;
            this.eco = eco;
            this.moves = moves;
        }
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ExplorerResult queryMasters(String fen) {
        try {
            String encoded = URLEncoder.encode(fen, StandardCharsets.UTF_8);
            String url = "https://explorer.lichess.ovh/masters?fen=" + encoded + "&moves=8";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json")
                    .header("User-Agent", "ChessMasterJava/1.0")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            Map<String, Object> root = JsonParser.parseObject(resp.body());
            String opName = "";
            String eco = "";
            Map<String, Object> opObj = JsonParser.asObject(root.get("opening"));
            if (opObj != null) {
                opName = JsonParser.asString(opObj.get("name"), "");
                eco = JsonParser.asString(opObj.get("eco"), "");
            }

            List<ExplorerMove> list = new ArrayList<>();
            List<Object> movesArr = JsonParser.asArray(root.get("moves"));
            if (movesArr != null) {
                for (Object item : movesArr) {
                    Map<String, Object> m = JsonParser.asObject(item);
                    if (m == null) continue;
                    String san = JsonParser.asString(m.get("san"), "");
                    String uci = JsonParser.asString(m.get("uci"), "");
                    int w = JsonParser.asInt(m.get("white"), 0);
                    int d = JsonParser.asInt(m.get("draws"), 0);
                    int b = JsonParser.asInt(m.get("black"), 0);
                    if (!san.isEmpty()) list.add(new ExplorerMove(san, uci, w, d, b));
                }
            }
            return new ExplorerResult(opName, eco, list);
        } catch (Exception e) {
            return null;
        }
    }
}
