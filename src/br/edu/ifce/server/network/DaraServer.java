package br.edu.ifce.server.network;

import br.edu.ifce.server.core.DaraEngine;
import br.edu.ifce.shared.model.CellState;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DaraServer {
    private static final int PORT = 12345;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final DaraEngine gameEngine = new DaraEngine();

    public static void main(String[] args) {
        System.out.println("[INFO] Inicializando o servidor do Jogo Dara...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[INFO] Servidor escutando na porta " + PORT + ". Aguardando conexoes...");

            while (true) {
                Socket socket = serverSocket.accept();
                
                synchronized (clients) {
                    if (clients.size() < 2) {
                        CellState playerColor = (clients.isEmpty()) ? CellState.PLAYER_1 : CellState.PLAYER_2;
                        System.out.println("[INFO] Jogador " + (clients.size() + 1) + " conectado a partir de " + socket.getInetAddress());
                        
                        ClientHandler handler = new ClientHandler(socket, playerColor, gameEngine);
                        clients.add(handler);
                        new Thread(handler).start();
                    } else {
                        System.out.println("[WARN] Conexao recusada de " + socket.getInetAddress() + ". Limite de jogadores atingido.");
                        PrintWriter rejectedOut = new PrintWriter(socket.getOutputStream(), true);
                        rejectedOut.println("CHAT;Sistema;A partida ja esta cheia!");
                        socket.close();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Falha critica no ciclo de execucao do servidor: " + e.getMessage());
        }
    }

    public static synchronized void checkAndStartGame() {
        if (clients.size() == 2) {
            System.out.println("[INFO] Ambos os jogadores estao prontos. Disparando inicializacao da partida...");
            
            clients.get(0).send("START;PLAYER_1");
            clients.get(1).send("START;PLAYER_2");
            
            broadcast("CHAT;Sistema;Ambos os jogadores conectados! Iniciando a partida...");
            sendStateUpdate();
        }
    }

    public static synchronized void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    public static synchronized void sendStateUpdate() {
        String phase = gameEngine.isPlacementPhase() ? "PLACE" : (gameEngine.isAwaitCapturePhase() ? "CAPTURE" : "MOVE");
        broadcast("UPDATE;" + gameEngine.getBoardSerialized() + ";" + gameEngine.getCurrentTurn().name() + ";" + phase);
    }

    public static synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}