from flask import Flask, request, jsonify
from flask_cors import CORS
import chess
from ai import get_best_move, evaluate_board

app = Flask(__name__)
CORS(app)

@app.route('/ai-move', methods=['POST'])
def ai_move():
    try:
        data = request.json
        if not data or 'fen' not in data:
            return jsonify({'error': 'Missing fen parameter'}), 400

        fen = data['fen']
        board = chess.Board(fen)

        if board.is_game_over():
            result = board.result()
            return jsonify({'game_over': True, 'result': result})

        san_move = get_best_move(board)
        if san_move is None:
            return jsonify({'error': 'No legal moves available'}), 400

        board.push_san(san_move)
        in_check = board.is_check()
        in_checkmate = board.is_checkmate()

        return jsonify({
            'move': san_move,
            'in_check': in_check,
            'in_checkmate': in_checkmate,
            'fen': board.fen()
        })

    except ValueError as e:
        return jsonify({'error': f'Invalid board position: {str(e)}'}), 400
    except Exception as e:
        return jsonify({'error': f'Server error: {str(e)}'}), 500

@app.route('/status', methods=['GET'])
def status():
    return jsonify({'status': 'running'})

if __name__ == '__main__':
    app.run(debug=True)
