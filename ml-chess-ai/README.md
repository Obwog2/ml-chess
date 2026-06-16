# Machine Learning Chess AI

A web-based chess game where you play against a neural network AI. The backend uses TensorFlow to evaluate board positions and selects moves via minimax search with alpha-beta pruning.

## Technologies

- **Frontend:** HTML, CSS, JavaScript, Chessboard.js, Chess.js
- **Backend:** Python, Flask, python-chess, TensorFlow
- **Engine:** Java (standalone minimax engine with PST evaluation)

## Setup

### 1. Python Backend

```bash
cd python-backend
pip install -r requirements.txt
python train_model.py
python app.py
```

### 2. Frontend

Open `frontend/index.html` in your browser.

### 3. Java Engine (optional)

```bash
cd java-engine
javac ChessEngine.java
java ChessEngine
```

## How It Works

1. The player (White) makes a move on the board
2. The frontend sends the FEN position to the Flask backend
3. The AI evaluates positions using minimax search (depth 3) with alpha-beta pruning
4. Move ordering prioritizes captures, checks, and promotions for faster search
5. Piece-square tables provide positional evaluation beyond material
6. The AI returns its best move in standard algebraic notation
7. If the trained model is available, it supplements the evaluation

## AI Features

- **Minimax with alpha-beta pruning** (depth 3)
- **Piece-square tables** for positional play (pawns, knights, bishops, rooks, queens, king)
- **Move ordering** (captures, checks, promotions searched first)
- **Endgame detection** with king safety evaluation
- **Checkmate/stalemate detection** in evaluation function
- **Optional neural network** trained on self-play positions

## Architecture

```
frontend/        -> Browser UI (chessboard.js)
    |
    | HTTP POST /ai-move {fen}
    v
python-backend/  -> Flask server
    |              - Loads trained model (optional)
    |              - Minimax search with alpha-beta pruning
    |              - Piece-square table evaluation
    |              - Returns SAN notation
    v
model/           -> Saved TensorFlow model (.h5)
```
