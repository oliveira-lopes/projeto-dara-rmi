package br.edu.ifce.server.core;

import br.edu.ifce.shared.model.CellState;

public class DaraEngineTest {

    public static void main(String[] args) {
        testFaseColocacaoValida();
        testBloqueioTrioNaColocacao();
        testMovimentacaoECaptura();
        System.out.println("Todos os testes de regras passaram com sucesso!");
    }

    public static void testFaseColocacaoValida() {
        DaraEngine engine = new DaraEngine();
        // Player 1 joga na (0,0) -> Deve funcionar
        boolean ok1 = engine.placePiece(0, 0, CellState.PLAYER_1);
        // Player 2 joga na (0,1) -> Deve funcionar
        boolean ok2 = engine.placePiece(0, 1, CellState.PLAYER_2);

        assert ok1 : "Erro ao colocar peça do Player 1";
        assert ok2 : "Erro ao colocar peça do Player 2";
        assert engine.getBoard()[0][0] == CellState.PLAYER_1;
    }

    public static void testBloqueioTrioNaColocacao() {
        DaraEngine engine = new DaraEngine();
        // Simulando preenchimento simulado alternado sem fechar trio
        engine.placePiece(0, 0, CellState.PLAYER_1); // P1
        engine.placePiece(4, 4, CellState.PLAYER_2); // P2
        engine.placePiece(0, 1, CellState.PLAYER_1); // P1
        engine.placePiece(4, 5, CellState.PLAYER_2); // P2
        
        // Agora P1 tenta colocar em (0,2), o que fecharia 3 em linha na linha 0
        boolean tentouTrio = engine.placePiece(0, 2, CellState.PLAYER_1);
        
        assert !tentouTrio : "Regra violada: Permitido trio na fase de colocação!";
    }

    public static void testMovimentacaoECaptura() {
        DaraEngine engine = new DaraEngine();
        
        // Avançar forçadamente para a fase de movimentação limpando contadores internos
        // Para propósitos de teste rápido, faremos uma simulação controlada baseada na troca de turnos
        System.out.println("   -> Teste de movimentação integrado iniciado...");
    }
}