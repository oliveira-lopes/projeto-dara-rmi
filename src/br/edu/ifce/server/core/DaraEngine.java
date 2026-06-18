package br.edu.ifce.server.core;

import br.edu.ifce.shared.model.CellState;

public class DaraEngine {
    public static final int ROWS = 5;
    public static final int COLS = 6;
    public static final int MAX_PIECES = 12;

    private CellState[][] board;
    private CellState currentTurn;
    private boolean isPlacementPhase;
    
    private int p1PiecesToPlace = MAX_PIECES;
    private int p2PiecesToPlace = MAX_PIECES;
    
    private int p1RemainingPieces = MAX_PIECES;
    private int p2RemainingPieces = MAX_PIECES;
    
    private boolean awaitCapturePhase = false;

    public DaraEngine() {
        board = new CellState[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = CellState.EMPTY;
            }
        }
        currentTurn = CellState.PLAYER_1; // Player 1 inicia
        isPlacementPhase = true;
    }

    // --- GETTERS E SETTERS ---
    public CellState[][] getBoard() { return board; }
    public CellState getCurrentTurn() { return currentTurn; }
    public boolean isPlacementPhase() { return isPlacementPhase; }
    public boolean isAwaitCapturePhase() { return awaitCapturePhase; }
    public int getP1RemainingPieces() { return p1RemainingPieces; }
    public int getP2RemainingPieces() { return p2RemainingPieces; }

    /**
     * Tenta posicionar uma peça na fase de colocação.
     */
    public boolean placePiece(int row, int col, CellState player) {
        if (!isPlacementPhase || awaitCapturePhase || currentTurn != player) return false;
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS || board[row][col] != CellState.EMPTY) return false;

        // Regra do Dara: Não pode formar linha de 3 na fase de colocação
        board[row][col] = player;
        if (formsThreeInARow(row, col, player)) {
            board[row][col] = CellState.EMPTY; // Desfaz
            return false; 
        }

        // Decrementa contadores de posicionamento
        if (player == CellState.PLAYER_1) p1PiecesToPlace--;
        else p2PiecesToPlace--;

        // Verifica se a fase de colocação acabou
        if (p1PiecesToPlace == 0 && p2PiecesToPlace == 0) {
            isPlacementPhase = false;
        }

        switchTurn();
        return true;
    }

    /**
     * Tenta mover uma peça na fase de movimentação.
     */
    public boolean movePiece(int fromR, int fromC, int toR, int toC, CellState player) {
        if (isPlacementPhase || awaitCapturePhase || currentTurn != player) return false;
        if (board[fromR][fromC] != player || board[toR][toC] != CellState.EMPTY) return false;

        // Verifica adjacência (apenas H ou V, distância de 1 casa)
        int diffR = Math.abs(fromR - toR);
        int diffC = Math.abs(fromC - toC);
        if (!((diffR == 1 && diffC == 0) || (diffR == 0 && diffC == 1))) return false;

        // Executa o movimento
        board[fromR][fromC] = CellState.EMPTY;
        board[toR][toC] = player;

        // Se formou um trio, entra em modo de captura e NÃO passa o turno ainda
        if (formsThreeInARow(toR, toC, player)) {
            awaitCapturePhase = true;
        } else {
            switchTurn();
        }
        return true;
    }

    /**
     * Tenta capturar uma peça do oponente.
     */
    public boolean capturePiece(int row, int col, CellState player) {
        if (!awaitCapturePhase || currentTurn != player) return false;
        
        CellState opponent = (player == CellState.PLAYER_1) ? CellState.PLAYER_2 : CellState.PLAYER_1;
        if (board[row][col] != opponent) return false;

        // Remove a peça do oponente
        board[row][col] = CellState.EMPTY;
        if (opponent == CellState.PLAYER_1) p1RemainingPieces--;
        else p2RemainingPieces--;

        awaitCapturePhase = false;
        switchTurn();
        return true;
    }

    /**
     * Verifica se existe um alinhamento exato de 3 peças na horizontal ou vertical.
     */
    private boolean formsThreeInARow(int row, int col, CellState player) {
    // Checagem Horizontal (Procura por 3 peças consecutivas na mesma linha)
    int consecutiveH = 0;
    for (int c = 0; c < COLS; c++) {
        if (board[row][c] == player) {
            consecutiveH++;
            if (consecutiveH == 3) return true; // Encontrou exatamente um trio
        } else {
            consecutiveH = 0;
        }
    }

    // Checagem Vertical (Procura por 3 peças consecutivas na mesma coluna)
    int consecutiveV = 0;
    for (int r = 0; r < ROWS; r++) {
        if (board[r][col] == player) {
            consecutiveV++;
            if (consecutiveV == 3) return true; // Encontrou exatamente um trio
        } else {
            consecutiveV = 0;
        }
    }
    return false;
}

    private void switchTurn() {
        currentTurn = (currentTurn == CellState.PLAYER_1) ? CellState.PLAYER_2 : CellState.PLAYER_1;
    }

    /**
     * Verifica se temos um vencedor (quando um oponente cai para menos de 3 peças).
     */
    public CellState checkWinner() {
        if (isPlacementPhase) return null;
        if (p1RemainingPieces < 3) return CellState.PLAYER_2;
        if (p2RemainingPieces < 3) return CellState.PLAYER_1;
        return null;
    }

    /**
     * Serializa o tabuleiro em String para enviar via rede de forma simples.
     */
    public String getBoardSerialized() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                sb.append(board[i][j].ordinal()).append(",");
            }
        }
        return sb.toString();
    }
}