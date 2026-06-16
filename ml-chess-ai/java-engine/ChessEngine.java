import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessEngine {

    private static final Map<Character, Integer> PIECE_VALUES = new HashMap<>();
    static {
        PIECE_VALUES.put('P', 100);  PIECE_VALUES.put('p', -100);
        PIECE_VALUES.put('N', 320);  PIECE_VALUES.put('n', -320);
        PIECE_VALUES.put('B', 330);  PIECE_VALUES.put('b', -330);
        PIECE_VALUES.put('R', 500);  PIECE_VALUES.put('r', -500);
        PIECE_VALUES.put('Q', 900);  PIECE_VALUES.put('q', -900);
        PIECE_VALUES.put('K', 0);    PIECE_VALUES.put('k', 0);
    }

    private static final int[] PAWN_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_TABLE = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_TABLE = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10, 10,  5, 10, 10,  5, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_TABLE = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_TABLE = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    private char[][] board = new char[8][8];
    private boolean whiteToMove = true;
    private int nodesSearched = 0;

    public ChessEngine() {
        loadFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public void loadFEN(String fen) {
        String[] parts = fen.split(" ");
        String[] rows = parts[0].split("/");
        for (int r = 0; r < 8; r++) {
            int col = 0;
            for (char c : rows[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    for (int i = 0; i < Character.getNumericValue(c); i++) {
                        board[r][col++] = '.';
                    }
                } else {
                    board[r][col++] = c;
                }
            }
        }
        whiteToMove = parts[1].equals("w");
    }

    public String toFEN() {
        StringBuilder fen = new StringBuilder();
        for (int r = 0; r < 8; r++) {
            int empty = 0;
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == '.') {
                    empty++;
                } else {
                    if (empty > 0) { fen.append(empty); empty = 0; }
                    fen.append(board[r][c]);
                }
            }
            if (empty > 0) fen.append(empty);
            if (r < 7) fen.append('/');
        }
        fen.append(whiteToMove ? " w" : " b");
        fen.append(" - - 0 1");
        return fen.toString();
    }

    private int getPSTValue(char piece, int row, int col) {
        int square = row * 8 + col;
        int tableIdx = (7 - row) * 8 + col;
        switch (Character.toLowerCase(piece)) {
            case 'p': return PAWN_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            case 'n': return KNIGHT_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            case 'b': return BISHOP_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            case 'r': return ROOK_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            case 'q': return QUEEN_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            case 'k': return KING_TABLE[tableIdx] * (Character.isUpperCase(piece) ? 1 : -1);
            default: return 0;
        }
    }

    public int evaluate() {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = board[r][c];
                if (piece != '.') {
                    Integer val = PIECE_VALUES.get(piece);
                    if (val != null) score += val;
                    score += getPSTValue(piece, r, c);
                }
            }
        }
        return whiteToMove ? score : -score;
    }

    public List<int[]> getLegalMoves() {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char piece = board[r][c];
                if (piece == '.') continue;
                boolean isWhite = Character.isUpperCase(piece);
                if (isWhite != whiteToMove) continue;

                switch (Character.toLowerCase(piece)) {
                    case 'p': addPawnMoves(moves, r, c, isWhite); break;
                    case 'n': addKnightMoves(moves, r, c, isWhite); break;
                    case 'b': addSlidingMoves(moves, r, c, isWhite, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}); break;
                    case 'r': addSlidingMoves(moves, r, c, isWhite, new int[][]{{-1,0},{1,0},{0,-1},{0,1}}); break;
                    case 'q': addSlidingMoves(moves, r, c, isWhite, new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}); break;
                    case 'k': addKingMoves(moves, r, c, isWhite); break;
                }
            }
        }
        return moves;
    }

    private void addPawnMoves(List<int[]> moves, int r, int c, boolean isWhite) {
        int dir = isWhite ? -1 : 1;
        int nr = r + dir;
        if (nr >= 0 && nr < 8 && board[nr][c] == '.') {
            moves.add(new int[]{r, c, nr, c});
            int startRow = isWhite ? 6 : 1;
            int nr2 = r + 2 * dir;
            if (r == startRow && board[nr2][c] == '.') {
                moves.add(new int[]{r, c, nr2, c});
            }
        }
        for (int dc = -1; dc <= 1; dc += 2) {
            int nc = c + dc;
            if (nc >= 0 && nc < 8 && nr >= 0 && nr < 8) {
                char target = board[nr][nc];
                if (target != '.' && Character.isUpperCase(target) != isWhite) {
                    moves.add(new int[]{r, c, nr, nc});
                }
            }
        }
    }

    private void addKnightMoves(List<int[]> moves, int r, int c, boolean isWhite) {
        int[][] offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] off : offsets) {
            int nr = r + off[0], nc = c + off[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char target = board[nr][nc];
                if (target == '.' || Character.isUpperCase(target) != isWhite) {
                    moves.add(new int[]{r, c, nr, nc});
                }
            }
        }
    }

    private void addKingMoves(List<int[]> moves, int r, int c, boolean isWhite) {
        int[][] offsets = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] off : offsets) {
            int nr = r + off[0], nc = c + off[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char target = board[nr][nc];
                if (target == '.' || Character.isUpperCase(target) != isWhite) {
                    moves.add(new int[]{r, c, nr, nc});
                }
            }
        }
    }

    private void addSlidingMoves(List<int[]> moves, int r, int c, boolean isWhite, int[][] directions) {
        for (int[] dir : directions) {
            int nr = r + dir[0], nc = c + dir[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char target = board[nr][nc];
                if (target == '.') {
                    moves.add(new int[]{r, c, nr, nc});
                } else {
                    if (Character.isUpperCase(target) != isWhite) {
                        moves.add(new int[]{r, c, nr, nc});
                    }
                    break;
                }
                nr += dir[0];
                nc += dir[1];
            }
        }
    }

    private List<int[]> orderMoves(List<int[]> moves) {
        List<int[]> scored = new ArrayList<>(moves);
        scored.sort((a, b) -> {
            int scoreA = getMoveScore(a);
            int scoreB = getMoveScore(b);
            return Integer.compare(scoreB, scoreA);
        });
        return scored;
    }

    private int getMoveScore(int[] move) {
        int score = 0;
        char target = board[move[2]][move[3]];
        if (target != '.') {
            score += 10 * Math.abs(PIECE_VALUES.getOrDefault(target, 0)) - Math.abs(PIECE_VALUES.getOrDefault(board[move[0]][move[1]], 0));
        }
        if (Character.toLowerCase(board[move[0]][move[1]]) == 'p' && (move[2] == 0 || move[2] == 7)) {
            score += 900;
        }
        return score;
    }

    public void makeMove(int[] move) {
        board[move[2]][move[3]] = board[move[0]][move[1]];
        board[move[0]][move[1]] = '.';
        whiteToMove = !whiteToMove;
    }

    public String getBestMove(int depth) {
        nodesSearched = 0;
        long startTime = System.currentTimeMillis();
        List<int[]> moves = orderMoves(getLegalMoves());
        int bestScore = whiteToMove ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int[] bestMove = null;

        for (int[] move : moves) {
            char[][] saved = copyBoard();
            boolean savedTurn = whiteToMove;
            makeMove(move);
            int score = minimax(depth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, !whiteToMove);
            board = saved;
            whiteToMove = savedTurn;

            if (whiteToMove ? score > bestScore : score < bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("Searched " + nodesSearched + " nodes in " + elapsed + "ms");

        if (bestMove == null) return null;
        return "" + (char)('a' + bestMove[1]) + (8 - bestMove[0]) +
               (char)('a' + bestMove[3]) + (8 - bestMove[2]);
    }

    private int minimax(int depth, int alpha, int beta, boolean maximizing) {
        nodesSearched++;
        if (depth == 0) return evaluate();
        List<int[]> moves = orderMoves(getLegalMoves());
        if (moves.isEmpty()) {
            boolean inCheck = isKingAttacked(maximizing);
            return inCheck ? (maximizing ? -100000 + (SEARCH_DEPTH - depth) : 100000 - (SEARCH_DEPTH - depth)) : 0;
        }

        if (maximizing) {
            int maxEval = Integer.MIN_VALUE + 1;
            for (int[] move : moves) {
                char[][] saved = copyBoard();
                boolean savedTurn = whiteToMove;
                makeMove(move);
                int eval = minimax(depth - 1, alpha, beta, false);
                board = saved;
                whiteToMove = savedTurn;
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE - 1;
            for (int[] move : moves) {
                char[][] saved = copyBoard();
                boolean savedTurn = whiteToMove;
                makeMove(move);
                int eval = minimax(depth - 1, alpha, beta, true);
                board = saved;
                whiteToMove = savedTurn;
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private boolean isKingAttacked(boolean byWhite) {
        int[] kingPos = findKing(byWhite);
        if (kingPos == null) return true;
        return isSquareAttacked(kingPos[0], kingPos[1], !byWhite);
    }

    private int[] findKing(boolean white) {
        char king = white ? 'K' : 'k';
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == king) return new int[]{r, c};
            }
        }
        return null;
    }

    private boolean isSquareAttacked(int row, int col, boolean byWhite) {
        int[][] knightOffsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] off : knightOffsets) {
            int nr = row + off[0], nc = col + off[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char p = board[nr][nc];
                if (p == (byWhite ? 'N' : 'n')) return true;
            }
        }

        int pawnDir = byWhite ? -1 : 1;
        for (int dc = -1; dc <= 1; dc += 2) {
            int pr = row + pawnDir, pc = col + dc;
            if (pr >= 0 && pr < 8 && pc >= 0 && pc < 8) {
                char p = board[pr][pc];
                if (p == (byWhite ? 'P' : 'p')) return true;
            }
        }

        int[][] kingOffsets = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        for (int[] off : kingOffsets) {
            int nr = row + off[0], nc = col + off[1];
            if (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char p = board[nr][nc];
                if (p == (byWhite ? 'K' : 'k')) return true;
            }
        }

        int[][] slidingDirs = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
        char bishop = byWhite ? 'B' : 'b';
        char rook = byWhite ? 'R' : 'r';
        char queen = byWhite ? 'Q' : 'q';
        int[][] bishopDirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        int[][] rookDirs = {{-1,0},{1,0},{0,-1},{0,1}};

        for (int[] dir : bishopDirs) {
            int nr = row + dir[0], nc = col + dir[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char p = board[nr][nc];
                if (p != '.') {
                    if (p == bishop || p == queen) return true;
                    break;
                }
                nr += dir[0]; nc += dir[1];
            }
        }

        for (int[] dir : rookDirs) {
            int nr = row + dir[0], nc = col + dir[1];
            while (nr >= 0 && nr < 8 && nc >= 0 && nc < 8) {
                char p = board[nr][nc];
                if (p != '.') {
                    if (p == rook || p == queen) return true;
                    break;
                }
                nr += dir[0]; nc += dir[1];
            }
        }

        return false;
    }

    private char[][] copyBoard() {
        char[][] copy = new char[8][8];
        for (int r = 0; r < 8; r++) {
            copy[r] = board[r].clone();
        }
        return copy;
    }

    public void printBoard() {
        System.out.println("  a b c d e f g h");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + " ");
            for (int c = 0; c < 8; c++) {
                System.out.print(board[r][c] + " ");
            }
            System.out.println(8 - r);
        }
        System.out.println("  a b c d e f g h");
    }

    private static final int SEARCH_DEPTH = 4;

    public static void main(String[] args) {
        ChessEngine engine = new ChessEngine();
        System.out.println("=== Chess AI Engine ===");
        System.out.println("Starting position:");
        engine.printBoard();
        System.out.println();

        System.out.println("Best move (depth " + SEARCH_DEPTH + "):");
        String bestMove = engine.getBestMove(SEARCH_DEPTH);
        System.out.println("=> " + bestMove);
    }
}
