const game = new Chess();
let playerTurn = true;
let moveHistory = [];
let capturedByWhite = [];
let capturedByBlack = [];

const PIECE_UNICODE = {
  p: '\u265F', n: '\u265E', b: '\u265D', r: '\u265C', q: '\u265B', k: '\u265A',
  P: '\u2659', N: '\u2658', B: '\u2657', R: '\u2656', Q: '\u2655', K: '\u2654'
};

const PIECE_VALUES = { p: 1, n: 3, b: 3, r: 5, q: 9, k: 0 };

const board = Chessboard('board', {
  draggable: true,
  position: 'start',
  onDrop: onDrop,
  pieceTheme: 'https://unpkg.com/@chrisoakman/chessboardjs@1.0.0/dist/img/chesspieces/wikipedia/{piece}.png',
  showNotation: true
});

function updateStatus() {
  const statusEl = document.getElementById('status');
  if (!statusEl) return;

  if (game.in_checkmate()) {
    const winner = game.turn() === 'w' ? 'Black' : 'White';
    statusEl.textContent = `Checkmate! ${winner} wins.`;
    statusEl.className = 'status game-over';
  } else if (game.in_stalemate()) {
    statusEl.textContent = 'Stalemate! Draw.';
    statusEl.className = 'status game-over';
  } else if (game.in_draw()) {
    statusEl.textContent = 'Draw!';
    statusEl.className = 'status game-over';
  } else if (game.in_check()) {
    const turn = game.turn() === 'w' ? 'White' : 'Black';
    statusEl.textContent = `${turn} is in check!`;
    statusEl.className = 'status check';
  } else {
    const turn = game.turn() === 'w' ? 'White' : 'Black';
    statusEl.textContent = `${turn}'s turn`;
    statusEl.className = 'status';
  }

  document.getElementById('undo-btn').disabled = moveHistory.length === 0;
}

function updateCapturedPieces() {
  const fen = game.fen();
  const boardParts = fen.split(' ')[0];
  const pieceCounts = {};

  for (const c of boardParts) {
    if (c === '/') continue;
    if (c >= '1' && c <= '8') continue;
    pieceCounts[c] = (pieceCounts[c] || 0) + 1;
  }

  const startPieces = { P: 8, N: 2, B: 2, R: 2, Q: 1, p: 8, n: 2, b: 2, r: 2, q: 1 };
  const capturedWhite = [];
  const capturedBlack = [];

  for (const [piece, startCount] of Object.entries(startPieces)) {
    const current = pieceCounts[piece] || 0;
    const diff = startCount - current;
    if (diff > 0) {
      for (let i = 0; i < diff; i++) {
        if (piece === piece.toUpperCase()) {
          capturedBlack.push(piece);
        } else {
          capturedWhite.push(piece);
        }
      }
    }
  }

  const sortPieces = (a, b) => PIECE_VALUES[b.toLowerCase()] - PIECE_VALUES[a.toLowerCase()];
  capturedWhite.sort(sortPieces);
  capturedBlack.sort(sortPieces);

  document.getElementById('captured-white').innerHTML =
    capturedWhite.map(p => `<span class="captured-piece">${PIECE_UNICODE[p]}</span>`).join('');
  document.getElementById('captured-black').innerHTML =
    capturedBlack.map(p => `<span class="captured-piece">${PIECE_UNICODE[p]}</span>`).join('');

  const whiteMaterial = capturedWhite.reduce((sum, p) => sum + PIECE_VALUES[p.toLowerCase()], 0);
  const blackMaterial = capturedBlack.reduce((sum, p) => sum + PIECE_VALUES[p.toLowerCase()], 0);
  const diff = whiteMaterial - blackMaterial;

  document.getElementById('advantage-white').textContent = diff > 0 ? `+${diff}` : '';
  document.getElementById('advantage-black').textContent = diff < 0 ? `+${-diff}` : '';
}

function updateMoveHistory() {
  const historyEl = document.getElementById('move-history');
  const moves = game.history();
  let html = '';
  for (let i = 0; i < moves.length; i += 2) {
    const moveNum = Math.floor(i / 2) + 1;
    const whiteMove = moves[i];
    const blackMove = moves[i + 1] || '';
    html += `<div class="move-row">
      <span class="move-num">${moveNum}.</span>
      <span class="move-white">${whiteMove}</span>
      <span class="move-black">${blackMove}</span>
    </div>`;
  }
  historyEl.innerHTML = html;
  historyEl.scrollTop = historyEl.scrollHeight;
}

async function onDrop(source, target) {
  if (!playerTurn) return 'snapback';

  const move = game.move({
    from: source,
    to: target,
    promotion: 'q'
  });

  if (move === null) return 'snapback';

  moveHistory.push({ fen: game.fen(), move: move });
  board.position(game.fen());
  updateStatus();
  updateCapturedPieces();
  updateMoveHistory();

  if (game.game_over()) return;

  playerTurn = false;

  const statusEl = document.getElementById('status');
  if (statusEl) {
    statusEl.textContent = 'AI is thinking...';
    statusEl.className = 'status thinking';
  }

  try {
    const response = await fetch('http://127.0.0.1:5000/ai-move', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fen: game.fen() })
    });

    const data = await response.json();

    if (data.error) {
      console.error('AI error:', data.error);
      playerTurn = true;
      updateStatus();
      return;
    }

    if (data.game_over) {
      playerTurn = true;
      updateStatus();
      return;
    }

    const aiMove = game.move(data.move, { sloppy: true });
    if (!aiMove) {
      const uciMove = data.move;
      const from = uciMove.substring(0, 2);
      const to = uciMove.substring(2, 4);
      const promotion = uciMove.length > 4 ? uciMove[4] : undefined;
      game.move({ from, to, promotion: promotion || 'q' });
    }

    moveHistory.push({ fen: game.fen(), move: data.move });
    board.position(game.fen());
    updateStatus();
    updateCapturedPieces();
    updateMoveHistory();

  } catch (err) {
    console.error('Failed to get AI move:', err);
    if (statusEl) {
      statusEl.textContent = 'Error: Could not reach AI server.';
      statusEl.className = 'status error';
    }
  }

  playerTurn = true;
  updateStatus();
}

function undoMove() {
  if (moveHistory.length < 2) return;
  game.undo();
  game.undo();
  moveHistory.pop();
  moveHistory.pop();
  board.position(game.fen());
  updateStatus();
  updateCapturedPieces();
  updateMoveHistory();
  playerTurn = true;
}

function resetGame() {
  game.reset();
  board.start();
  playerTurn = true;
  moveHistory = [];
  capturedByWhite = [];
  capturedByBlack = [];
  updateStatus();
  updateCapturedPieces();
  updateMoveHistory();
}

$(window).resize(() => board.resize());
updateStatus();
updateCapturedPieces();
