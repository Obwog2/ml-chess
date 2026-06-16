import chess
import numpy as np
import os

model = None
model_path = os.path.join(os.path.dirname(__file__), 'model', 'chess_model.h5')

MATE_SCORE = 100000
SEARCH_DEPTH = 3

PIECE_VALUES = {
    chess.PAWN: 100, chess.KNIGHT: 320, chess.BISHOP: 330,
    chess.ROOK: 500, chess.QUEEN: 900, chess.KING: 0
}

PAWN_TABLE = [
     0,  0,  0,  0,  0,  0,  0,  0,
    50, 50, 50, 50, 50, 50, 50, 50,
    10, 10, 20, 30, 30, 20, 10, 10,
     5,  5, 10, 25, 25, 10,  5,  5,
     0,  0,  0, 20, 20,  0,  0,  0,
     5, -5,-10,  0,  0,-10, -5,  5,
     5, 10, 10,-20,-20, 10, 10,  5,
     0,  0,  0,  0,  0,  0,  0,  0
]

KNIGHT_TABLE = [
    -50,-40,-30,-30,-30,-30,-40,-50,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -30,  5, 15, 20, 20, 15,  5,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  5, 10, 15, 15, 10,  5,-30,
    -40,-20,  0,  5,  5,  0,-20,-40,
    -50,-40,-30,-30,-30,-30,-40,-50
]

BISHOP_TABLE = [
    -20,-10,-10,-10,-10,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0, 10, 10, 10, 10,  0,-10,
    -10,  5,  5, 10, 10,  5,  5,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10, 10,  5, 10, 10,  5, 10,-10,
    -10,  5,  0,  0,  0,  0,  5,-10,
    -20,-10,-10,-10,-10,-10,-10,-20
]

ROOK_TABLE = [
     0,  0,  0,  0,  0,  0,  0,  0,
     5, 10, 10, 10, 10, 10, 10,  5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
    -5,  0,  0,  0,  0,  0,  0, -5,
     0,  0,  0,  5,  5,  0,  0,  0
]

QUEEN_TABLE = [
    -20,-10,-10, -5, -5,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
     -5,  0,  5,  5,  5,  5,  0, -5,
      0,  0,  5,  5,  5,  5,  0, -5,
    -10,  5,  5,  5,  5,  5,  0,-10,
    -10,  0,  5,  0,  0,  0,  0,-10,
    -20,-10,-10, -5, -5,-10,-10,-20
]

KING_MIDDLE_TABLE = [
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -20,-30,-30,-40,-40,-30,-30,-20,
    -10,-20,-20,-20,-20,-20,-20,-10,
     20, 20,  0,  0,  0,  0, 20, 20,
     20, 30, 10,  0,  0, 10, 30, 20
]

KING_END_TABLE = [
    -50,-40,-30,-20,-20,-30,-40,-50,
    -30,-20,-10,  0,  0,-10,-20,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-30,  0,  0,  0,  0,-30,-30,
    -50,-30,-30,-30,-30,-30,-30,-50
]

PST = {
    chess.PAWN: PAWN_TABLE,
    chess.KNIGHT: KNIGHT_TABLE,
    chess.BISHOP: BISHOP_TABLE,
    chess.ROOK: ROOK_TABLE,
    chess.QUEEN: QUEEN_TABLE,
    chess.KING: KING_MIDDLE_TABLE
}


def load_model():
    global model
    try:
        import tensorflow as tf
        model = tf.keras.models.load_model(model_path)
        print("Model loaded successfully")
    except Exception as e:
        print(f"Could not load model: {e}. Using fallback evaluation.")
        model = None


def board_to_tensor(board):
    tensor = np.zeros(768)
    for sq in chess.SQUARES:
        piece = board.piece_at(sq)
        if piece:
            idx = (piece.piece_type - 1) * 128 + sq
            if piece.color == chess.BLACK:
                idx += 64
            tensor[idx] = 1.0
    return tensor


def is_endgame(board):
    queens = 0
    minors_and_rooks = 0
    for sq in chess.SQUARES:
        piece = board.piece_at(sq)
        if piece:
            if piece.piece_type == chess.QUEEN:
                queens += 1
            elif piece.piece_type in (chess.ROOK, chess.BISHOP, chess.KNIGHT):
                minors_and_rooks += 1
    return queens == 0 or (queens == 2 and minors_and_rooks <= 2)


def evaluate_board(board):
    if board.is_checkmate():
        if board.turn == chess.WHITE:
            return -MATE_SCORE
        else:
            return MATE_SCORE

    if board.is_stalemate() or board.is_insufficient_material() or board.can_claim_draw():
        return 0

    endgame = is_endgame(board)
    score = 0

    for sq in chess.SQUARES:
        piece = board.piece_at(sq)
        if piece:
            val = PIECE_VALUES[piece.piece_type]
            table = PST[piece.piece_type]
            if piece.color == chess.WHITE:
                table_sq = (7 - sq // 8) * 8 + sq % 8
                score += val + table[table_sq]
            else:
                table_sq = sq
                score -= val + table[table_sq]

    mobility = len(list(board.legal_moves))
    board.push(chess.Move.null())
    opp_mobility = len(list(board.legal_moves))
    board.pop()

    if board.turn == chess.WHITE:
        score += (mobility - opp_mobility) * 5
    else:
        score -= (mobility - opp_mobility) * 5

    return score


def order_moves(board):
    scored = []
    for move in board.legal_moves:
        score = 0
        if board.is_capture(move):
            victim = board.piece_at(move.to_square)
            attacker = board.piece_at(move.from_square)
            if victim and attacker:
                score += 10 * PIECE_VALUES.get(victim.piece_type, 0) - PIECE_VALUES.get(attacker.piece_type, 0)
            else:
                score += 1000
        if board.gives_check(move):
            score += 800
        if move.promotion:
            score += PIECE_VALUES.get(move.promotion, 0)
        scored.append((score, move))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [m for _, m in scored]


def minimax(board, depth, alpha, beta, maximizing):
    if depth == 0 or board.is_game_over():
        return evaluate_board(board), None

    best_move = None
    ordered_moves = order_moves(board)

    if maximizing:
        max_eval = -float('inf')
        for move in ordered_moves:
            board.push(move)
            eval_score, _ = minimax(board, depth - 1, alpha, beta, False)
            board.pop()
            if eval_score > max_eval:
                max_eval = eval_score
                best_move = move
            alpha = max(alpha, eval_score)
            if beta <= alpha:
                break
        return max_eval, best_move
    else:
        min_eval = float('inf')
        for move in ordered_moves:
            board.push(move)
            eval_score, _ = minimax(board, depth - 1, alpha, beta, True)
            board.pop()
            if eval_score < min_eval:
                min_eval = eval_score
                best_move = move
            beta = min(beta, eval_score)
            if beta <= alpha:
                break
        return min_eval, best_move


def get_best_move(board):
    legal_moves = list(board.legal_moves)
    if not legal_moves:
        return None

    is_white = board.turn == chess.WHITE

    if model is not None:
        scores = []
        for move in legal_moves:
            board.push(move)
            tensor = board_to_tensor(board)
            prediction = model.predict(tensor.reshape(1, -1), verbose=0)[0][0]
            scores.append((move, prediction))
            board.pop()

        if is_white:
            best = max(scores, key=lambda x: x[1])
        else:
            best = min(scores, key=lambda x: x[1])
        return best[0].san()

    _, best_move = minimax(board, SEARCH_DEPTH, -float('inf'), float('inf'), is_white)

    if best_move is None:
        return legal_moves[0].san()

    return best_move.san()


load_model()
