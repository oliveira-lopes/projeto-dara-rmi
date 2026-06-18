package br.edu.ifce.server.network;

import br.edu.ifce.server.core.DaraEngine;
import br.edu.ifce.shared.model.CellState;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DaraServer {
    private static final int PORT = 12345; // Porta padrão do projeto
    private static List<ClientHandler> clients = new ArrayList<>();
    private static DaraEngine gameEngine = new DaraEngine();

    public static void main(String[] args) {
        System.out.println("🚀 [SERVIDOR] Iniciando servidor do Jogo Dara...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("📍 [SERVIDOR] Escutando na porta " + PORT + ". Aguardando jogadores...");

            // Aguarda exatamente 2 jogadores se conectarem para iniciar a partida
            while (clients.size() < 2) {
                Socket socket = serverSocket.accept();
                CellState playerColor = (clients.isEmpty()) ? CellState.PLAYER_1 : CellState.PLAYER_2;
                
                System.out.println("👤 [SERVIDOR] Jogador " + (clients.size() + 1) + " conectado de " + socket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(socket, playerColor);
                clients.add(handler);
                new Thread(handler).start();
            }

            // Notifica os clientes e inicia o jogo 
            broadcast("CHAT;⚙️ Sistema;Ambos os jogadores conectados! Iniciando a partida...");
            clients.get(0).send("START;PLAYER_1"); // Jogador 1 (Brancas/Começa) 
            clients.get(1).send("START;PLAYER_2"); // Jogador 2 (Pretas)
            
            // Envia o estado inicial do tabuleiro vazio
            broadcast("UPDATE;" + gameEngine.getBoardSerialized() + ";" + gameEngine.getCurrentTurn().name() + ";PLACE");

        } catch (IOException e) {
            System.err.println("❌ [SERVIDOR] Erro crítico no servidor: " + e.getMessage());
        }
    }

    /**
     * Envia uma mensagem de rede para todos os jogadores conectados.
     */
    public static synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    /**
     * Classe interna (Thread) que manipula a conexão individual de cada jogador.
     */
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private CellState myColor;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket, CellState color) {
            this.socket = socket;
            this.myColor = color;
        }

        public void send(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processCommand(inputLine);
                }
            } catch (IOException e) {
                System.out.println("⚠️ [SERVIDOR] Conexão perdida com o jogador " + myColor);
            } finally {
                cleanup();
            }
        }

        /**
         * Interpretador do protocolo de rede definido no Passo 1.3.
         */
        private void processCommand(String commandLine) {
            String[] tokens = commandLine.split(";");
            String command = tokens[0];

            switch (command) {
                case "CHAT": // 
                    // Formato: CHAT;Mensagem
                    broadcast("CHAT;[" + myColor.name() + "]: " + tokens[1]);
                    break;

                case "PUT": // Fase de Colocação [cite: 13, 22]
                    int pRow = Integer.parseInt(tokens[1]);
                    int pCol = Integer.parseInt(tokens[2]);
                    if (gameEngine.placePiece(pRow, pCol, myColor)) {
                        sendStateUpdate();
                    } else {
                        send("CHAT;⚙️ Sistema;Jogada inválida ou fora do seu turno!");
                    }
                    break;

                case "MOVE": // Fase de Movimentação [cite: 15, 22]
                    int fromR = Integer.parseInt(tokens[1]);
                    int fromC = Integer.parseInt(tokens[2]);
                    int toR = Integer.parseInt(tokens[3]);
                    int toC = Integer.parseInt(tokens[4]);
                    if (gameEngine.movePiece(fromR, fromC, toR, toC, myColor)) {
                        sendStateUpdate();
                    } else {
                        send("CHAT;⚙️ Sistema;Movimento inválido!");
                    }
                    break;

                case "CAPTURE": // Captura de peça do oponente [cite: 16]
                    int capR = Integer.parseInt(tokens[1]);
                    int capC = Integer.parseInt(tokens[2]);
                    if (gameEngine.capturePiece(capR, capC, myColor)) {
                        sendStateUpdate();
                        checkEndGame();
                    } else {
                        send("CHAT;⚙️ Sistema;Captura inválida!");
                    }
                    break;

                case "DESISTENCIA": // [cite: 23]
                    broadcast("CHAT;⚙️ Sistema;O jogador " + myColor.name() + " desistiu da partida!");
                    CellState winner = (myColor == CellState.PLAYER_1) ? CellState.PLAYER_2 : CellState.PLAYER_1;
                    broadcast("WINNER;" + winner.name()); // 
                    break;
            }
        }

        private void sendStateUpdate() {
            String phase = gameEngine.isPlacementPhase() ? "PLACE" : (gameEngine.isAwaitCapturePhase() ? "CAPTURE" : "MOVE");
            broadcast("UPDATE;" + gameEngine.getBoardSerialized() + ";" + gameEngine.getCurrentTurn().name() + ";" + phase);
        }

        private void checkEndGame() {
            CellState winner = gameEngine.checkWinner();
            if (winner != null) {
                broadcast("WINNER;" + winner.name()); // 
            }
        }

        private void cleanup() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.remove(this);
        }
    }
}