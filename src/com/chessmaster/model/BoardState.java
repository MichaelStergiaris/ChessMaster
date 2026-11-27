package com.chessmaster.model;

import java.util.*;

/**
 * Full FIDE-compliant chess position representation and move generator.
 * Square indexing: 0 = a8, 7 = h8, 56 = a1, 63 = h1 (row 0..7 top-to-bottom, col 0..7 a..h).
 */
public final class BoardState {
    public static final String START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public static final int CASTLE_WK = 1;
    public static final int CASTLE_WQ = 2;
    public static final int CASTLE_BK = 4;
    public static final int CASTLE_BQ = 8;

    private final Piece[] squares = new Piece[64];
    private Color sideToMove = Color.WHITE;
    private int castlingRights = CASTLE_WK | CASTLE_WQ | CASTLE_BK | CASTLE_BQ;
    private int enPassantSquare = -1;
    private int halfMoveClock = 0;
    private int fullMoveNumber = 1;
    private final Map<String, Integer> repetitionCounts = new HashMap<>();

    private static final int[][] KNIGHT_DELTAS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
        {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };
    private static final int[][] KING_DELTAS = {
        {-1, -1}, {-1, 0}, {-1, 1}, {0, -1},
        {0, 1}, {1, -1}, {1, 0}, {1, 1}
    };
    private static final int[][] BISHOP_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private static final int[][] ROOK_DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public BoardState() {
        loadFen(START_FEN);
    }

    public static BoardState fromFen(String fen) {
        BoardState state = new BoardState();
        state.loadFen(fen);
        return state;
    }

    public BoardState copy() {
        BoardState c = new BoardState();
        System.arraycopy(this.squares, 0, c.squares, 0, 64);
        c.sideToMove = this.sideToMove;
        c.castlingRights = this.castlingRights;
        c.enPassantSquare = this.enPassantSquare;
        c.halfMoveClock = this.halfMoveClock;
        c.fullMoveNumber = this.fullMoveNumber;
        c.repetitionCounts.clear();
        c.repetitionCounts.putAll(this.repetitionCounts);
        return c;
    }

    public void loadFen(String fen) {
        Arrays.fill(squares, null);
        repetitionCounts.clear();
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid FEN: " + fen);
        }

        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("Invalid FEN ranks: " + fen);
        }
        for (int r = 0; r < 8; r++) {
            int c = 0;
            for (int i = 0; i < ranks[r].length(); i++) {
                char ch = ranks[r].charAt(i);
                if (ch >= '1' && ch <= '8') {
                    c += (ch - '0');
                } else {
                    if (c >= 8) throw new IllegalArgumentException("FEN rank overflow: " + fen);
                    squares[r * 8 + c] = Piece.fromFenChar(ch);
                    c++;
                }
            }
            if (c != 8) {
                throw new IllegalArgumentException("FEN rank width mismatch: " + fen);
            }
        }

        sideToMove = parts[1].equals("b") ? Color.BLACK : Color.WHITE;

        castlingRights = 0;
        if (!parts[2].equals("-")) {
            for (int i = 0; i < parts[2].length(); i++) {
                switch (parts[2].charAt(i)) {
                    case 'K' -> castlingRights |= CASTLE_WK;
                    case 'Q' -> castlingRights |= CASTLE_WQ;
                    case 'k' -> castlingRights |= CASTLE_BK;
                    case 'q' -> castlingRights |= CASTLE_BQ;
                }
            }
        }

        enPassantSquare = parts[3].equals("-") ? -1 : algebraicToSquare(parts[3]);
        halfMoveClock = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
        fullMoveNumber = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

        recordRepetitionKey();
    }

    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                Piece p = squares[r * 8 + c];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    sb.append(p.fenChar());
                }
            }
            if (empty > 0) sb.append(empty);
            if (r < 7) sb.append('/');
        }
        sb.append(' ').append(sideToMove == Color.WHITE ? 'w' : 'b').append(' ');
        if (castlingRights == 0) {
            sb.append('-');
        } else {
            if ((castlingRights & CASTLE_WK) != 0) sb.append('K');
            if ((castlingRights & CASTLE_WQ) != 0) sb.append('Q');
            if ((castlingRights & CASTLE_BK) != 0) sb.append('k');
            if ((castlingRights & CASTLE_BQ) != 0) sb.append('q');
        }
        sb.append(' ').append(enPassantSquare == -1 ? "-" : squareToAlgebraic(enPassantSquare));
        sb.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber);
        return sb.toString();
    }

    private String positionKey() {
        StringBuilder sb = new StringBuilder(80);
        for (int i = 0; i < 64; i++) {
            Piece p = squares[i];
            sb.append(p == null ? '.' : p.fenChar());
        }
        sb.append(sideToMove == Color.WHITE ? 'w' : 'b');
        sb.append(castlingRights);
        sb.append(':').append(enPassantSquare);
        return sb.toString();
    }

    private void recordRepetitionKey() {
        String key = positionKey();
        repetitionCounts.merge(key, 1, Integer::sum);
    }

    public Piece getPiece(int sq) {
        return (sq >= 0 && sq < 64) ? squares[sq] : null;
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row > 7 || col < 0 || col > 7) return null;
        return squares[row * 8 + col];
    }

    public Color getSideToMove() {
        return sideToMove;
    }

    public int getCastlingRights() {
        return castlingRights;
    }

    public int getEnPassantSquare() {
        return enPassantSquare;
    }

    public int getHalfMoveClock() {
        return halfMoveClock;
    }

    public int getFullMoveNumber() {
        return fullMoveNumber;
    }

    public static int rowOf(int sq) {
        return sq >> 3;
    }

    public static int colOf(int sq) {
        return sq & 7;
    }

    public static int squareOf(int row, int col) {
        return (row << 3) | col;
    }

    public static String squareToAlgebraic(int sq) {
        if (sq < 0 || sq >= 64) return "-";
        char file = (char) ('a' + (sq & 7));
        char rank = (char) ('8' - (sq >> 3));
        return "" + file + rank;
    }

    public static int algebraicToSquare(String alg) {
        if (alg == null || alg.length() < 2) return -1;
        int col = alg.charAt(0) - 'a';
        int row = '8' - alg.charAt(1);
        if (col < 0 || col > 7 || row < 0 || row > 7) return -1;
        return row * 8 + col;
    }

    public int findKingSquare(Color color) {
        for (int i = 0; i < 64; i++) {
            Piece p = squares[i];
            if (p != null && p.color() == color && p.type() == PieceType.KING) {
                return i;
            }
        }
        return -1;
    }

    public boolean isInCheck(Color color) {
        int kingSq = findKingSquare(color);
        return kingSq != -1 && isSquareAttacked(kingSq, color.opposite());
    }

    public boolean isInCheck() {
        return isInCheck(sideToMove);
    }

    public boolean isSquareAttacked(int sq, Color byColor) {
        int r = sq >> 3;
        int c = sq & 7;

        // Pawn attacks: a white pawn attacks from row+1, black pawn from row-1
        int pawnRow = byColor == Color.WHITE ? r + 1 : r - 1;
        if (pawnRow >= 0 && pawnRow <= 7) {
            if (c - 1 >= 0) {
                Piece p = squares[pawnRow * 8 + (c - 1)];
                if (p != null && p.color() == byColor && p.type() == PieceType.PAWN) return true;
            }
            if (c + 1 <= 7) {
                Piece p = squares[pawnRow * 8 + (c + 1)];
                if (p != null && p.color() == byColor && p.type() == PieceType.PAWN) return true;
            }
        }

        // Knight attacks
        for (int[] d : KNIGHT_DELTAS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                Piece p = squares[nr * 8 + nc];
                if (p != null && p.color() == byColor && p.type() == PieceType.KNIGHT) return true;
            }
        }

        // King attacks
        for (int[] d : KING_DELTAS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                Piece p = squares[nr * 8 + nc];
                if (p != null && p.color() == byColor && p.type() == PieceType.KING) return true;
            }
        }

        // Bishop / Queen diagonal attacks
        for (int[] d : BISHOP_DIRS) {
            int nr = r + d[0], nc = c + d[1];
            while (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                Piece p = squares[nr * 8 + nc];
                if (p != null) {
                    if (p.color() == byColor && (p.type() == PieceType.BISHOP || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }

        // Rook / Queen orthogonal attacks
        for (int[] d : ROOK_DIRS) {
            int nr = r + d[0], nc = c + d[1];
            while (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                Piece p = squares[nr * 8 + nc];
                if (p != null) {
                    if (p.color() == byColor && (p.type() == PieceType.ROOK || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }

        return false;
    }

    public List<Move> generateLegalMoves() {
        List<Move> pseudo = generatePseudoLegalMoves();
        List<Move> legal = new ArrayList<>(pseudo.size());
        Color us = sideToMove;
        for (Move m : pseudo) {
            BoardState next = this.copyWithoutRepetition();
            next.applyMoveUnchecked(m, false);
            if (!next.isInCheck(us)) {
                legal.add(m);
            }
        }
        return legal;
    }

    public List<Move> generateLegalMovesFrom(int fromSq) {
        List<Move> result = new ArrayList<>();
        for (Move m : generateLegalMoves()) {
            if (m.from == fromSq) result.add(m);
        }
        return result;
    }

    private BoardState copyWithoutRepetition() {
        BoardState c = new BoardState();
        System.arraycopy(this.squares, 0, c.squares, 0, 64);
        c.sideToMove = this.sideToMove;
        c.castlingRights = this.castlingRights;
        c.enPassantSquare = this.enPassantSquare;
        c.halfMoveClock = this.halfMoveClock;
        c.fullMoveNumber = this.fullMoveNumber;
        return c;
    }

    private List<Move> generatePseudoLegalMoves() {
        List<Move> moves = new ArrayList<>(48);
        Color us = sideToMove;
        Color them = us.opposite();

        for (int sq = 0; sq < 64; sq++) {
            Piece p = squares[sq];
            if (p == null || p.color() != us) continue;
            int r = sq >> 3;
            int c = sq & 7;

            switch (p.type()) {
                case PAWN -> generatePawnMoves(sq, r, c, us, them, moves);
                case KNIGHT -> {
                    for (int[] d : KNIGHT_DELTAS) {
                        int nr = r + d[0], nc = c + d[1];
                        if (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                            int target = nr * 8 + nc;
                            Piece tp = squares[target];
                            if (tp == null) {
                                moves.add(new Move(sq, target, null, false, false, false, false, false));
                            } else if (tp.color() == them) {
                                moves.add(new Move(sq, target, null, true, false, false, false, false));
                            }
                        }
                    }
                }
                case BISHOP -> generateSliderMoves(sq, r, c, them, BISHOP_DIRS, moves);
                case ROOK -> generateSliderMoves(sq, r, c, them, ROOK_DIRS, moves);
                case QUEEN -> {
                    generateSliderMoves(sq, r, c, them, BISHOP_DIRS, moves);
                    generateSliderMoves(sq, r, c, them, ROOK_DIRS, moves);
                }
                case KING -> generateKingMoves(sq, r, c, us, them, moves);
            }
        }
        return moves;
    }

    private void generatePawnMoves(int sq, int r, int c, Color us, Color them, List<Move> moves) {
        int dir = us == Color.WHITE ? -1 : 1;
        int startRow = us == Color.WHITE ? 6 : 1;
        int promoRow = us == Color.WHITE ? 0 : 7;

        int oneRow = r + dir;
        if (oneRow >= 0 && oneRow <= 7) {
            int oneSq = oneRow * 8 + c;
            if (squares[oneSq] == null) {
                if (oneRow == promoRow) {
                    addPromotions(sq, oneSq, false, moves);
                } else {
                    moves.add(new Move(sq, oneSq, null, false, false, false, false, false));
                    if (r == startRow) {
                        int twoSq = (r + 2 * dir) * 8 + c;
                        if (squares[twoSq] == null) {
                            moves.add(new Move(sq, twoSq, null, false, false, false, false, true));
                        }
                    }
                }
            }

            // Captures (including en passant)
            for (int dc : new int[]{-1, 1}) {
                int nc = c + dc;
                if (nc < 0 || nc > 7) continue;
                int target = oneRow * 8 + nc;
                Piece tp = squares[target];
                if (tp != null && tp.color() == them) {
                    if (oneRow == promoRow) {
                        addPromotions(sq, target, true, moves);
                    } else {
                        moves.add(new Move(sq, target, null, true, false, false, false, false));
                    }
                } else if (target == enPassantSquare) {
                    moves.add(new Move(sq, target, null, true, true, false, false, false));
                }
            }
        }
    }

    private void addPromotions(int from, int to, boolean capture, List<Move> moves) {
        moves.add(new Move(from, to, PieceType.QUEEN, capture, false, false, false, false));
        moves.add(new Move(from, to, PieceType.ROOK, capture, false, false, false, false));
        moves.add(new Move(from, to, PieceType.BISHOP, capture, false, false, false, false));
        moves.add(new Move(from, to, PieceType.KNIGHT, capture, false, false, false, false));
    }

    private void generateSliderMoves(int sq, int r, int c, Color them, int[][] dirs, List<Move> moves) {
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            while (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                int target = nr * 8 + nc;
                Piece tp = squares[target];
                if (tp == null) {
                    moves.add(new Move(sq, target, null, false, false, false, false, false));
                } else {
                    if (tp.color() == them) {
                        moves.add(new Move(sq, target, null, true, false, false, false, false));
                    }
                    break;
                }
                nr += d[0];
                nc += d[1];
            }
        }
    }

    private void generateKingMoves(int sq, int r, int c, Color us, Color them, List<Move> moves) {
        for (int[] d : KING_DELTAS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr <= 7 && nc >= 0 && nc <= 7) {
                int target = nr * 8 + nc;
                Piece tp = squares[target];
                if (tp == null) {
                    moves.add(new Move(sq, target, null, false, false, false, false, false));
                } else if (tp.color() == them) {
                    moves.add(new Move(sq, target, null, true, false, false, false, false));
                }
            }
        }

        // Castling
        if (us == Color.WHITE && sq == 60) {
            if ((castlingRights & CASTLE_WK) != 0
                    && squares[61] == null && squares[62] == null
                    && isRookOf(63, Color.WHITE)
                    && !isSquareAttacked(60, them)
                    && !isSquareAttacked(61, them)
                    && !isSquareAttacked(62, them)) {
                moves.add(new Move(60, 62, null, false, false, true, false, false));
            }
            if ((castlingRights & CASTLE_WQ) != 0
                    && squares[59] == null && squares[58] == null && squares[57] == null
                    && isRookOf(56, Color.WHITE)
                    && !isSquareAttacked(60, them)
                    && !isSquareAttacked(59, them)
                    && !isSquareAttacked(58, them)) {
                moves.add(new Move(60, 58, null, false, false, false, true, false));
            }
        } else if (us == Color.BLACK && sq == 4) {
            if ((castlingRights & CASTLE_BK) != 0
                    && squares[5] == null && squares[6] == null
                    && isRookOf(7, Color.BLACK)
                    && !isSquareAttacked(4, them)
                    && !isSquareAttacked(5, them)
                    && !isSquareAttacked(6, them)) {
                moves.add(new Move(4, 6, null, false, false, true, false, false));
            }
            if ((castlingRights & CASTLE_BQ) != 0
                    && squares[3] == null && squares[2] == null && squares[1] == null
                    && isRookOf(0, Color.BLACK)
                    && !isSquareAttacked(4, them)
                    && !isSquareAttacked(3, them)
                    && !isSquareAttacked(2, them)) {
                moves.add(new Move(4, 2, null, false, false, false, true, false));
            }
        }
    }

    private boolean isRookOf(int sq, Color color) {
        Piece p = squares[sq];
        return p != null && p.color() == color && p.type() == PieceType.ROOK;
    }

    public BoardState applyMove(Move move) {
        BoardState next = this.copy();
        next.applyMoveUnchecked(move, true);
        return next;
    }

    private void applyMoveUnchecked(Move m, boolean trackRepetition) {
        Piece moving = squares[m.from];
        Piece captured = squares[m.to];
        squares[m.from] = null;

        if (m.isEnPassant) {
            int capRow = m.from >> 3;
            int capCol = m.to & 7;
            squares[capRow * 8 + capCol] = null;
        }

        if (m.promotion != null) {
            squares[m.to] = new Piece(moving.color(), m.promotion);
        } else {
            squares[m.to] = moving;
        }

        if (m.isCastleKingside) {
            if (moving.color() == Color.WHITE) {
                squares[61] = squares[63];
                squares[63] = null;
            } else {
                squares[5] = squares[7];
                squares[7] = null;
            }
        } else if (m.isCastleQueenside) {
            if (moving.color() == Color.WHITE) {
                squares[59] = squares[56];
                squares[56] = null;
            } else {
                squares[3] = squares[0];
                squares[0] = null;
            }
        }

        // Update castling rights
        if (m.from == 60 || m.to == 60) castlingRights &= ~(CASTLE_WK | CASTLE_WQ);
        if (m.from == 4 || m.to == 4) castlingRights &= ~(CASTLE_BK | CASTLE_BQ);
        if (m.from == 63 || m.to == 63) castlingRights &= ~CASTLE_WK;
        if (m.from == 56 || m.to == 56) castlingRights &= ~CASTLE_WQ;
        if (m.from == 7 || m.to == 7) castlingRights &= ~CASTLE_BK;
        if (m.from == 0 || m.to == 0) castlingRights &= ~CASTLE_BQ;

        // Update en passant square
        if (m.isDoublePawnPush) {
            enPassantSquare = (m.from + m.to) / 2;
        } else {
            enPassantSquare = -1;
        }

        // Update clocks
        if (moving.type() == PieceType.PAWN || m.isCapture || captured != null) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }
        if (sideToMove == Color.BLACK) {
            fullMoveNumber++;
        }

        sideToMove = sideToMove.opposite();
        if (trackRepetition) {
            recordRepetitionKey();
        }
    }

    public Move findLegalMove(int from, int to, PieceType promotion) {
        for (Move m : generateLegalMoves()) {
            if (m.from == from && m.to == to) {
                if (m.promotion == promotion || (m.promotion != null && promotion == null && m.promotion == PieceType.QUEEN)) {
                    return m;
                }
            }
        }
        return null;
    }

    public Move parseUci(String uci) {
        if (uci == null || uci.length() < 4) return null;
        String clean = uci.trim().toLowerCase(Locale.ROOT);
        int from = algebraicToSquare(clean.substring(0, 2));
        int to = algebraicToSquare(clean.substring(2, 4));
        PieceType promo = clean.length() >= 5 ? PieceType.fromChar(clean.charAt(4)) : null;
        for (Move m : generateLegalMoves()) {
            if (m.from == from && m.to == to && m.promotion == promo) {
                return m;
            }
        }
        return null;
    }

    public String moveToSan(Move move) {
        if (move.isCastleKingside) {
            return appendCheckSuffix(move, "O-O");
        }
        if (move.isCastleQueenside) {
            return appendCheckSuffix(move, "O-O-O");
        }

        Piece moving = squares[move.from];
        StringBuilder sb = new StringBuilder();
        if (moving.type() == PieceType.PAWN) {
            if (move.isCapture) {
                sb.append((char) ('a' + colOf(move.from))).append('x');
            }
            sb.append(squareToAlgebraic(move.to));
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanLetter());
            }
        } else {
            sb.append(moving.type().sanLetter());
            // Disambiguation
            List<Move> ambiguous = new ArrayList<>();
            for (Move other : generateLegalMoves()) {
                if (other.from != move.from && other.to == move.to) {
                    Piece op = squares[other.from];
                    if (op != null && op.type() == moving.type()) {
                        ambiguous.add(other);
                    }
                }
            }
            if (!ambiguous.isEmpty()) {
                boolean sameFile = false;
                boolean sameRank = false;
                for (Move other : ambiguous) {
                    if (colOf(other.from) == colOf(move.from)) sameFile = true;
                    if (rowOf(other.from) == rowOf(move.from)) sameRank = true;
                }
                if (!sameFile) {
                    sb.append((char) ('a' + colOf(move.from)));
                } else if (!sameRank) {
                    sb.append((char) ('8' - rowOf(move.from)));
                } else {
                    sb.append(squareToAlgebraic(move.from));
                }
            }
            if (move.isCapture) {
                sb.append('x');
            }
            sb.append(squareToAlgebraic(move.to));
        }

        return appendCheckSuffix(move, sb.toString());
    }

    private String appendCheckSuffix(Move move, String base) {
        BoardState next = applyMove(move);
        if (next.isInCheck()) {
            return next.generateLegalMoves().isEmpty() ? base + "#" : base + "+";
        }
        return base;
    }

    public Move parseSan(String rawSan) {
        if (rawSan == null || rawSan.isBlank()) return null;
        String s = rawSan.trim()
                .replace("+", "")
                .replace("#", "")
                .replace("!", "")
                .replace("?", "");

        List<Move> legal = generateLegalMoves();

        if (s.equals("O-O") || s.equals("0-0")) {
            for (Move m : legal) if (m.isCastleKingside) return m;
            return null;
        }
        if (s.equals("O-O-O") || s.equals("0-0-0")) {
            for (Move m : legal) if (m.isCastleQueenside) return m;
            return null;
        }

        PieceType promo = null;
        int eqIdx = s.indexOf('=');
        if (eqIdx >= 0 && eqIdx + 1 < s.length()) {
            promo = PieceType.fromChar(s.charAt(eqIdx + 1));
            s = s.substring(0, eqIdx);
        } else if (s.length() >= 3 && Character.isUpperCase(s.charAt(s.length() - 1))
                && "QRBN".indexOf(s.charAt(s.length() - 1)) >= 0
                && Character.isDigit(s.charAt(s.length() - 2))) {
            promo = PieceType.fromChar(s.charAt(s.length() - 1));
            s = s.substring(0, s.length() - 1);
        }

        PieceType pieceType = PieceType.PAWN;
        if (!s.isEmpty() && "KQRBN".indexOf(s.charAt(0)) >= 0) {
            pieceType = PieceType.fromChar(s.charAt(0));
            s = s.substring(1);
        }

        s = s.replace("x", "");
        if (s.length() < 2) return null;

        String destAlg = s.substring(s.length() - 2);
        int destSq = algebraicToSquare(destAlg);
        if (destSq < 0) return null;

        String disambig = s.substring(0, s.length() - 2);
        int reqFile = -1;
        int reqRow = -1;
        for (int i = 0; i < disambig.length(); i++) {
            char ch = disambig.charAt(i);
            if (ch >= 'a' && ch <= 'h') reqFile = ch - 'a';
            else if (ch >= '1' && ch <= '8') reqRow = '8' - ch;
        }

        for (Move m : legal) {
            Piece p = squares[m.from];
            if (p == null || p.type() != pieceType) continue;
            if (m.to != destSq) continue;
            if (m.promotion != promo) continue;
            if (reqFile != -1 && colOf(m.from) != reqFile) continue;
            if (reqRow != -1 && rowOf(m.from) != reqRow) continue;
            return m;
        }
        return null;
    }

    /**
     * Parses a PGN move text string (ignoring headers, comments, variations, NAGs, move numbers, results)
     * and returns the clean list of SAN move tokens.
     */
    public static List<String> extractPgnSanMoves(String pgn) {
        List<String> tokens = new ArrayList<>();
        if (pgn == null || pgn.isBlank()) return tokens;

        // Strip PGN header tags [Key "Value"]
        StringBuilder body = new StringBuilder();
        boolean inHeader = false;
        boolean inBraceComment = false;
        boolean inLineComment = false;
        int parenDepth = 0;

        for (int i = 0; i < pgn.length(); i++) {
            char ch = pgn.charAt(i);
            if (inLineComment) {
                if (ch == '\n' || ch == '\r') inLineComment = false;
                continue;
            }
            if (inBraceComment) {
                if (ch == '}') inBraceComment = false;
                continue;
            }
            if (inHeader) {
                if (ch == ']') inHeader = false;
                continue;
            }
            if (ch == '[') { inHeader = true; continue; }
            if (ch == '{') { inBraceComment = true; continue; }
            if (ch == ';') { inLineComment = true; continue; }
            if (ch == '(') { parenDepth++; continue; }
            if (ch == ')') { if (parenDepth > 0) parenDepth--; continue; }
            if (parenDepth > 0) continue;

            body.append(ch);
        }

        String[] rawTokens = body.toString().trim().split("\\s+");
        for (String tok : rawTokens) {
            if (tok.isEmpty()) continue;
            if (tok.startsWith("$")) continue; // NAG
            if (tok.equals("1-0") || tok.equals("0-1") || tok.equals("1/2-1/2") || tok.equals("*")) continue;
            // Remove leading move numbers like "1." or "12..." or "1.e4"
            int lastDot = tok.lastIndexOf('.');
            String cleaned = lastDot >= 0 ? tok.substring(lastDot + 1) : tok;
            if (cleaned.isEmpty()) continue;
            if (Character.isDigit(cleaned.charAt(0)) && !cleaned.startsWith("0-0")) continue;
            tokens.add(cleaned);
        }
        return tokens;
    }

    public GameStatus getGameStatus() {
        List<Move> legal = generateLegalMoves();
        if (legal.isEmpty()) {
            if (isInCheck()) {
                return sideToMove == Color.WHITE
                        ? GameStatus.BLACK_WINS_CHECKMATE
                        : GameStatus.WHITE_WINS_CHECKMATE;
            }
            return GameStatus.DRAW_STALEMATE;
        }
        if (isInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE;
        }
        if (repetitionCounts.getOrDefault(positionKey(), 0) >= 3) {
            return GameStatus.DRAW_THREEFOLD_REPETITION;
        }
        return GameStatus.IN_PROGRESS;
    }

    public boolean isInsufficientMaterial() {
        int whiteBishops = 0, blackBishops = 0;
        int whiteKnights = 0, blackKnights = 0;
        int whiteBishopColor = -1, blackBishopColor = -1;

        for (int sq = 0; sq < 64; sq++) {
            Piece p = squares[sq];
            if (p == null || p.type() == PieceType.KING) continue;
            if (p.type() == PieceType.PAWN || p.type() == PieceType.ROOK || p.type() == PieceType.QUEEN) {
                return false;
            }
            int sqColor = (rowOf(sq) + colOf(sq)) & 1;
            if (p.color() == Color.WHITE) {
                if (p.type() == PieceType.BISHOP) { whiteBishops++; whiteBishopColor = sqColor; }
                if (p.type() == PieceType.KNIGHT) whiteKnights++;
            } else {
                if (p.type() == PieceType.BISHOP) { blackBishops++; blackBishopColor = sqColor; }
                if (p.type() == PieceType.KNIGHT) blackKnights++;
            }
        }
        int totalMinor = whiteBishops + blackBishops + whiteKnights + blackKnights;
        if (totalMinor <= 1) return true; // K vs K, K+B vs K, K+N vs K
        if (totalMinor == 2 && whiteBishops == 1 && blackBishops == 1 && whiteBishopColor == blackBishopColor) {
            return true; // K+B vs K+B same color
        }
        return false;
    }

    /**
     * Computes material score from White's perspective (positive = White ahead).
     */
    public int materialBalanceWhite() {
        int score = 0;
        for (Piece p : squares) {
            if (p != null) {
                int v = p.type().materialValue();
                score += (p.color() == Color.WHITE ? v : -v);
            }
        }
        return score;
    }

    /**
     * Returns captured pieces relative to standard starting army for display.
     */
    public List<PieceType> getCapturedPiecesOf(Color lostByColor) {
        int[] expected = {8, 2, 2, 2, 1}; // P, N, B, R, Q
        PieceType[] types = {PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN};
        int[] present = new int[5];
        for (Piece p : squares) {
            if (p != null && p.color() == lostByColor) {
                for (int i = 0; i < types.length; i++) {
                    if (p.type() == types[i]) present[i]++;
                }
            }
        }
        List<PieceType> lost = new ArrayList<>();
        // Order highest value first (Q, R, B, N, P)
        for (int i = types.length - 1; i >= 0; i--) {
            int diff = Math.max(0, expected[i] - present[i]);
            for (int k = 0; k < diff; k++) lost.add(types[i]);
        }
        return lost;
    }
}
