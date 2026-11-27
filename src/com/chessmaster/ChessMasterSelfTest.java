package com.chessmaster;

import com.chessmaster.data.*;
import com.chessmaster.engine.StockfishEngine;
import com.chessmaster.model.*;
import com.chessmaster.ui.MainFrame;
import com.chessmaster.ui.PieceRenderer;
import com.chessmaster.ui.Theme;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Comprehensive verification suite covering FIDE Perft accuracy, all openings,
 * all curated puzzles, live Lichess & Chess.com puzzle APIs, Stockfish 19 UCI engine,
 * vector piece rendering, and full GUI instantiation.
 */
public final class ChessMasterSelfTest {

    private static long perft(BoardState st, int depth) {
        if (depth == 0) return 1;
        List<Move> moves = st.generateLegalMoves();
        if (depth == 1) return moves.size();
        long nodes = 0;
        for (Move m : moves) {
            nodes += perft(st.applyMove(m), depth - 1);
        }
        return nodes;
    }

    private static void assertEq(String name, long actual, long expected) {
        if (actual != expected) {
            throw new AssertionError(name + " failed: expected " + expected + ", got " + actual);
        }
        System.out.println("[PASS] " + name + " = " + actual);
    }

    private static void assertTrue(String name, boolean cond) {
        if (!cond) throw new AssertionError(name + " failed!");
        System.out.println("[PASS] " + name);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. FIDE Perft Move Generation Verification ===");
        BoardState start = new BoardState();
        assertEq("Startpos Perft(1)", perft(start, 1), 20);
        assertEq("Startpos Perft(2)", perft(start, 2), 400);
        assertEq("Startpos Perft(3)", perft(start, 3), 8902);
        assertEq("Startpos Perft(4)", perft(start, 4), 197281);

        BoardState kiwi = BoardState.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEq("Kiwipete Perft(1)", perft(kiwi, 1), 48);
        assertEq("Kiwipete Perft(2)", perft(kiwi, 2), 2039);
        assertEq("Kiwipete Perft(3)", perft(kiwi, 3), 97862);

        System.out.println("\n=== 2. Opening Database Verification ===");
        List<Opening> openings = OpeningDatabase.getAll();
        assertTrue("Opening count >= 40 (" + openings.size() + ")", openings.size() >= 40);
        for (Opening op : openings) {
            BoardState st = new BoardState();
            for (int i = 0; i < op.movesSan.size(); i++) {
                String san = op.movesSan.get(i);
                Move m = st.parseSan(san);
                if (m == null) {
                    throw new AssertionError("Illegal SAN '" + san + "' at ply " + i + " in opening " + op.id);
                }
                String back = st.moveToSan(m);
                if (st.parseSan(back) == null) {
                    throw new AssertionError("Round-trip SAN failed: " + back);
                }
                st = st.applyMove(m);
            }
            if (op.explanations.size() != op.movesSan.size()) {
                throw new AssertionError("Explanation count mismatch in opening " + op.id
                        + ": moves=" + op.movesSan.size() + " notes=" + op.explanations.size());
            }
        }
        System.out.println("[PASS] All " + openings.size() + " openings verified with 100% legal SAN and matching coaching notes!");

        System.out.println("\n=== 3. Curated Masterpiece Puzzles Verification ===");
        PuzzleService ps = new PuzzleService();
        for (Puzzle p : ps.getCuratedBank()) {
            BoardState st = BoardState.fromFen(p.startFen);
            for (String uci : p.solutionUci) {
                Move m = st.parseUci(uci);
                if (m == null) {
                    throw new AssertionError("Illegal solution move " + uci + " in curated puzzle " + p.id);
                }
                st = st.applyMove(m);
            }
            boolean claimsMate = p.themes.stream().anyMatch(t -> t.contains("Mate"));
            if (claimsMate) {
                GameStatus gs = st.getGameStatus();
                if (gs != GameStatus.WHITE_WINS_CHECKMATE && gs != GameStatus.BLACK_WINS_CHECKMATE) {
                    throw new AssertionError("Mate puzzle " + p.id + " did not end in checkmate! Status=" + gs);
                }
            }
        }
        System.out.println("[PASS] All " + ps.getCuratedBank().size() + " curated masterpiece puzzles verified!");

        System.out.println("\n=== 4. Live Puzzle & Masters Explorer API Verification ===");
        Puzzle liveLichess = ps.fetchNextPuzzle(PuzzleService.SOURCE_LICHESS_UNLIMITED, "fork", "normal");
        assertTrue("Live Lichess Puzzle fetched & validated (" + liveLichess.id + ", rating " + liveLichess.rating + ")",
                liveLichess != null && PuzzleService.validateSolution(liveLichess.startFen, liveLichess.solutionUci));

        Puzzle dailyLichess = ps.fetchNextPuzzle(PuzzleService.SOURCE_LICHESS_DAILY, "mix", "normal");
        assertTrue("Live Lichess Daily Puzzle fetched & validated (" + dailyLichess.id + ")",
                dailyLichess != null && PuzzleService.validateSolution(dailyLichess.startFen, dailyLichess.solutionUci));

        Puzzle chessCom = ps.fetchNextPuzzle(PuzzleService.SOURCE_CHESSCOM, "mix", "normal");
        assertTrue("Live Chess.com Puzzle fetched & validated (" + chessCom.title + ")",
                chessCom != null && PuzzleService.validateSolution(chessCom.startFen, chessCom.solutionUci));

        OpeningExplorerClient exp = new OpeningExplorerClient();
        OpeningExplorerClient.ExplorerResult er = exp.queryMasters(BoardState.START_FEN);
        assertTrue("Lichess Masters Explorer API responded", er != null && !er.moves.isEmpty());
        ps.shutdown();

        System.out.println("\n=== 5. Stockfish 19 UCI Engine Verification ===");
        StockfishEngine sf = new StockfishEngine();
        assertTrue("Stockfish 19 binary active (" + sf.getEngineName() + ")", sf.isStockfishReady());
        String m600 = sf.computeMove(start, 600, false);
        assertTrue("Stockfish 600 Elo move valid (" + m600 + ")", start.parseUci(m600) != null);
        String m1600 = sf.computeMove(start, 1600, false);
        assertTrue("Stockfish 1600 Elo move valid (" + m1600 + ")", start.parseUci(m1600) != null);
        String mMax = sf.computeMove(start, 3500, true);
        assertTrue("Stockfish MAX move valid (" + mMax + ")", start.parseUci(mMax) != null);
        StockfishEngine.EvalResult eval = sf.analyzePosition(start, 3, 400);
        assertTrue("Stockfish MultiPV=3 analysis valid (lines=" + eval.lines.size() + ", score=" + eval.formattedScore() + ")",
                eval.lines.size() == 3 && eval.bestMoveUci != null);
        sf.shutdown();

        System.out.println("\n=== 6. Vector Piece Renderer & GUI Construction Smoke Test ===");
        for (Color c : Color.values()) {
            for (PieceType pt : PieceType.values()) {
                BufferedImage img = PieceRenderer.getPieceImage(new Piece(c, pt), 72);
                if (img == null || img.getWidth() != 72 || img.getHeight() != 72) {
                    throw new AssertionError("Piece rendering failed for " + c + " " + pt);
                }
            }
        }
        System.out.println("[PASS] All 12 high-DPI vector Staunton pieces rendered cleanly!");

        SwingUtilities.invokeAndWait(() -> {
            Theme.applyGlobalLookAndFeel();
            MainFrame frame = new MainFrame();
            frame.pack();
            frame.dispose();
        });
        System.out.println("[PASS] MainFrame & all 4 tabs constructed and disposed cleanly with 0 errors!");

        System.out.println("\nALL TESTS PASSED SUCCESSFULLY!");
        System.exit(0);
    }
}
