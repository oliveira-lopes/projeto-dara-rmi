package br.edu.ifce.server.network;

import br.edu.ifce.server.core.DaraEngine;
import br.edu.ifce.shared.model.CellState;
import br.edu.ifce.shared.model.DaraServerInterface;
import br.edu.ifce.shared.model.DaraClientInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class DaraServerRMI extends UnicastRemoteObject implements DaraServerInterface {
    
    private final List<DaraClientInterface> clientes = new ArrayList<>();
    private final DaraEngine gameEngine = new DaraEngine();

    public DaraServerRMI() throws RemoteException {
        super();
    }

    @Override
    public synchronized String registrarJogador(DaraClientInterface clienteRef) throws RemoteException {
        if (clientes.size() >= 2) {
            return "CHEIO";
        }

        clientes.add(clienteRef);
        String papel = (clientes.size() == 1) ? "PLAYER_1" : "PLAYER_2";
        System.out.println("[INFO] Jogador registrado como: " + papel);

        // Se o segundo jogador conectou, inicia a partida de fato
        if (clientes.size() == 2) {
            broadcastChat("Sistema", "Ambos os jogadores conectados! Iniciando a partida...");
            sendStateUpdate();
        }

        return papel;
    }

    @Override
    public synchronized void enviarMensagemChat(String jogador, String texto) throws RemoteException {
        broadcastChat("[" + jogador + "]", texto);
    }

    @Override
    public synchronized void processarColocacao(String jogador, int linha, int coluna) throws RemoteException {
        CellState cor = CellState.valueOf(jogador);
        if (gameEngine.placePiece(linha, coluna, cor)) {
            sendStateUpdate();
        } else {
            // Envia um aviso apenas para o cliente que tentou a jogada inválida
            obterClientePorPapel(jogador).receberMensagemChat("Sistema", "Posicionamento invalido ou proibido nesta fase!");
            sendStateUpdate();
        }
    }

    @Override
    public synchronized void processarMovimento(String jogador, int deLinha, int deColuna, int paraLinha, int paraColuna) throws RemoteException {
        CellState cor = CellState.valueOf(jogador);
        if (gameEngine.movePiece(deLinha, deColuna, paraLinha, paraColuna, cor)) {
            sendStateUpdate();
        } else {
            obterClientePorPapel(jogador).receberMensagemChat("Sistema", "Movimento de peca invalido!");
            sendStateUpdate();
        }
    }

    @Override
    public synchronized void processarCaptura(String jogador, int linha, int coluna) throws RemoteException {
        CellState cor = CellState.valueOf(jogador);
        if (gameEngine.capturePiece(linha, coluna, cor)) {
            sendStateUpdate();
            CellState winner = gameEngine.checkWinner();
            if (winner != null) {
                broadcastVencedor(winner.name());
            }
        } else {
            obterClientePorPapel(jogador).receberMensagemChat("Sistema", "Captura invalida!");
            sendStateUpdate();
        }
    }

    @Override
    public synchronized void processarDesistencia(String jogador) throws RemoteException {
        broadcastChat("Sistema", "O jogador " + jogador + " abdicou da partida.");
        String vencedor = jogador.equals("PLAYER_1") ? "PLAYER_2" : "PLAYER_1";
        broadcastVencedor(vencedor);
    }

    // Métodos auxiliares de Broadcast herdados da lógica antiga do seu DaraServer:
    private void broadcastChat(String remetente, String texto) {
        for (DaraClientInterface cliente : clientes) {
            try {
                cliente.receberMensagemChat(remetente, texto);
            } catch (RemoteException e) {
                System.err.println("[WARN] Falha ao enviar chat para um cliente.");
            }
        }
    }

    private void sendStateUpdate() {
        String fase = gameEngine.isPlacementPhase() ? "PLACE" : (gameEngine.isAwaitCapturePhase() ? "CAPTURE" : "MOVE");
        String board = gameEngine.getBoardSerialized();
        String turn = gameEngine.getCurrentTurn().name();

        for (DaraClientInterface cliente : clientes) {
            try {
                cliente.receberAtualizacaoEstado(board, turn, fase);
            } catch (RemoteException e) {
                System.err.println("[WARN] Falha ao atualizar estado de um cliente.");
            }
        }
    }

    private void broadcastVencedor(String vencedor) {
        for (DaraClientInterface cliente : clientes) {
            try {
                cliente.notificarVencedor(vencedor);
            } catch (RemoteException e) {
                System.err.println("[WARN] Falha ao notificar vencedor.");
            }
        }
    }

    private DaraClientInterface obterClientePorPapel(String papel) {
        return papel.equals("PLAYER_1") ? clientes.get(0) : clientes.get(1);
    }

    public static void main(String[] args) {
        try {
            System.out.println("[INFO] Inicializando o Registro RMI na porta 1099...");
            Registry registry = LocateRegistry.createRegistry(1099);

            DaraServerRMI servidor = new DaraServerRMI();
            registry.rebind("DaraServerService", servidor);

            System.out.println("[INFO] Servidor RMI do Jogo Dara pronto e aguardando conexões.");
        } catch (Exception e) {
            System.err.println("[ERROR] Falha critica no ciclo RMI: " + e.getMessage());
        }
    }
}