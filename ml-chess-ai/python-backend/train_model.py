import numpy as np
import chess
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, BatchNormalization
from tensorflow.keras.callbacks import EarlyStopping
from pathlib import Path
import random

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

PST = {
    chess.PAWN: PAWN_TABLE,
    chess.KNIGHT: KNIGHT_TABLE,
    chess.BISHOP: BISHOP_TABLE,
    chess.ROOK: ROOK_TABLE,
    chess.QUEEN: QUEEN_TABLE,
    chess.KING: KING_MIDDLE_TABLE
}


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


def evaluate_material(board):
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
    return score


def generate_positions(num_positions=10000):
    X, y = [], []
    for _ in range(num_positions):
        board = chess.Board()
        num_moves = random.randint(5, 80)
        for _ in range(num_moves):
            moves = list(board.legal_moves)
            if not moves:
                break
            weighted_moves = []
            for m in moves:
                if board.is_capture(m):
                    weighted_moves.extend([m] * 3)
                elif board.gives_check(m):
                    weighted_moves.extend([m] * 2)
                else:
                    weighted_moves.append(m)
            board.push(random.choice(weighted_moves))
            if board.is_game_over():
                break

        if board.is_game_over():
            result = board.result()
            if result == "1-0":
                label = 1.0
            elif result == "0-1":
                label = -1.0
            else:
                label = 0.0
        else:
            raw = evaluate_material(board)
            label = max(-1.0, min(1.0, raw / 1500.0))

        X.append(board_to_tensor(board))
        y.append(label)

        if len(X) % 1000 == 0:
            print(f"  Generated {len(X)}/{num_positions} positions...")

    return np.array(X), np.array(y)


print("Generating training positions...")
X, y = generate_positions(10000)
print(f"Dataset: {len(X)} positions")

model = Sequential([
    Dense(512, activation='relu', input_shape=(768,)),
    BatchNormalization(),
    Dropout(0.3),
    Dense(256, activation='relu'),
    BatchNormalization(),
    Dropout(0.3),
    Dense(128, activation='relu'),
    BatchNormalization(),
    Dropout(0.2),
    Dense(64, activation='relu'),
    Dense(1, activation='tanh')
])

model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
              loss='mse', metrics=['mae'])

print(f"Training on {len(X)} positions...")
early_stop = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)
model.fit(X, y, epochs=50, batch_size=64, validation_split=0.2,
          callbacks=[early_stop], verbose=1)

Path('model').mkdir(exist_ok=True)
model.save('model/chess_model.h5')
print('Model saved to model/chess_model.h5')
