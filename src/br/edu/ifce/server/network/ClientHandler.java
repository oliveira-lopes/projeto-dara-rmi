package br.edu.ifce.server.network;

import br.edu.ifce.server.core.DaraEngine;
import br.edu.ifce.shared.model.CellState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CellState myColor;
    private final DaraEngine gameEngine;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket socket, CellState color, DaraEngine gameEngine) {
        this.socket = socket;
        this.myColor = color;
        this.gameEngine = gameEngine;
    }

    public void send(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            DaraServer.checkAndStartGame();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processCommand(inputLine);
            }
        } catch (IOException e) {
            System.out.println("[WARN] Conexao encerrada abruptamente com o jogador " + myColor.name());
        } finally {
            cleanup();
        }
    }

    private void processCommand(String commandLine) {
        String[] tokens = commandLine.split(";");
        if (tokens.length == 0) return;
        String command = tokens[0];

        switch (command) {
            case "CHAT":
                if (tokens.length > 1) {
                    DaraServer.broadcast("CHAT;[" + myColor.name() + "]: " + tokens[1]);
                }
                break;

            case "PUT":
                int pRow = Integer.parseInt(tokens[1]);
                int pCol = Integer.parseInt(tokens[2]);
                if (gameEngine.placePiece(pRow, pCol, myColor)) {
                    DaraServer.sendStateUpdate();
                } else {
                    send("CHAT;Sistema;Posicionamento invalido ou proibido nesta fase!");
                    DaraServer.sendStateUpdate();
                }
                break;

            case "MOVE":
                int fromR = Integer.parseInt(tokens[1]);
                int fromC = Integer.parseInt(tokens[2]);
                int toR = Integer.parseInt(tokens[3]);
                int toC = Integer.parseInt(tokens[4]);
                if (gameEngine.movePiece(fromR, fromC, toR, toC, myColor)) {
                    DaraServer.sendStateUpdate();
                } else {
                    send("CHAT;Sistema;Movimento de peca invalido!");
                    DaraServer.sendStateUpdate();
                }
                break;

            case "CAPTURE":
                int capR = Integer.parseInt(tokens[1]);
                int capC = Integer.parseInt(tokens[2]);
                if (gameEngine.capturePiece(capR, capC, myColor)) {
                    DaraServer.sendStateUpdate();
                    CellState winner = gameEngine.checkWinner();
                    if (winner != null) {
                        DaraServer.broadcast("WINNER;" + winner.name());
                    }
                } else {
                    send("CHAT;Sistema;Captura invalida!");
                    DaraServer.sendStateUpdate();
                }
                break;

            case "DESISTENCIA":
                DaraServer.broadcast("CHAT;Sistema;O jogador " + myColor.name() + " abdicou da partida.");
                CellState winner = (myColor == CellState.PLAYER_1) ? CellState.PLAYER_2 : CellState.PLAYER_1;
                DaraServer.broadcast("WINNER;" + winner.name());
                break;
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Erro ao fechar socket de cliente: " + e.getMessage());
        }
        DaraServer.removeClient(this);
    }
}