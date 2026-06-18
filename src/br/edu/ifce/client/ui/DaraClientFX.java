package br.edu.ifce.client.ui;

import br.edu.ifce.shared.model.CellState;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DaraClientFX extends Application {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private CellState myColor = null;
    private CellState currentTurn = null;
    private String currentPhase = "AGUARDANDO";

    private Button[][] boardButtons = new Button[5][6];
    private TextArea chatArea;
    private TextField chatInput;
    private Label statusLabel;
    private Button btnDesistir;
    private HBox statusPanel;

    private int selectedRow = -1;
    private int selectedCol = -1;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dara Strategy Game Pro");

        // --- PAINEL DO TABULEIRO ---
        GridPane boardGrid = new GridPane();
        boardGrid.setHgap(10);
        boardGrid.setVgap(10);
        boardGrid.setPadding(new Insets(20));
        boardGrid.setStyle("-fx-background-color: #1e1e24; -fx-background-radius: 15;");
        initializeBoard(boardGrid);

        StackPane boardContainer = new StackPane(boardGrid);
        boardContainer.setPadding(new Insets(20));
        boardContainer.setStyle("-fx-background-color: #121214;");

        // --- PAINEL LATERAL ---
        VBox sidePanel = new VBox(15);
        sidePanel.setPrefWidth(320);
        sidePanel.setPadding(new Insets(20));
        sidePanel.setStyle("-fx-background-color: #1a1a1e;");

        statusLabel = new Label("A CONECTAR AO SERVIDOR...");
        statusLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
        
        statusPanel = new HBox(statusLabel);
        statusPanel.setAlignment(Pos.CENTER);
        statusPanel.setPadding(new Insets(15));
        statusPanel.setStyle("-fx-background-color: #2d2d35; -fx-background-radius: 10;");

        // Chat
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefHeight(300);
        chatArea.setStyle("-fx-control-inner-background: #121214; -fx-text-fill: #e1e1e6; -fx-font-family: 'Consolas'; -fx-background-radius: 8; -fx-border-color: #2d2d35; -fx-border-radius: 8;");

        chatInput = new TextField();
        chatInput.setPromptText("Escreve uma mensagem...");
        chatInput.setStyle("-fx-control-inner-background: #121214; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8; -fx-border-color: #2d2d35; -fx-border-radius: 8;");
        chatInput.setOnAction(e -> sendChatMessage());

        Button btnEnviar = new Button("Enviar");
        btnEnviar.setStyle("-fx-background-color: #4b6eaf; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnEnviar.setOnAction(e -> sendChatMessage());

        HBox chatInputBox = new HBox(8, chatInput, btnEnviar);
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        VBox chatBox = new VBox(10, new Label("CHAT DA PARTIDA"), chatArea, chatInputBox);
        ((Label)chatBox.getChildren().get(0)).setStyle("-fx-text-fill: #a0a0a5; -fx-font-weight: bold; -fx-font-size: 11px;");

        // Desistência
        btnDesistir = new Button("Desistir da Partida");
        btnDesistir.setMaxWidth(Double.MAX_VALUE);
        btnDesistir.setStyle("-fx-background-color: #b43232; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
        btnDesistir.setOnAction(e -> handleDesistencia());

        sidePanel.getChildren().addAll(statusPanel, chatBox, btnDesistir);

        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(boardContainer);
        mainLayout.setRight(sidePanel);

        Scene scene = new Scene(mainLayout, 980, 620);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // EXECUÇÃO EM SEGUNDO PLANO: Evita travar a abertura da janela
        new Thread(this::connectToServer).start();
    }

    private void initializeBoard(GridPane grid) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                final int r = i;
                final int c = j;
                Button btn = new Button();
                btn.setPrefSize(85, 85);
                btn.setStyle("-fx-background-color: #2a2c32; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 36px;");
                
                btn.setOnMouseEntered(e -> {
                    if (currentTurn == myColor && btn.getText().isEmpty()) {
                        btn.setStyle("-fx-background-color: #3a3d45; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 36px;");
                    }
                });
                btn.setOnMouseExited(e -> {
                    if (btn.getStyle().contains("#3a3d45")) {
                        btn.setStyle("-fx-background-color: #2a2c32; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 36px;");
                    }
                });

                btn.setOnAction(e -> handleBoardClick(r, c));
                boardButtons[i][j] = btn;
                grid.add(btn, j, i);
            }
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            Platform.runLater(() -> chatArea.appendText(" AGUARDANDO JOGADOR 2...\n"));
            
            // Inicia a escuta de mensagens do servidor
            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            // Se falhar, exibe o alerta de erro de forma segura na Thread da UI
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro de Conexão");
                alert.setHeaderText("Não foi possível conectar ao servidor");
                alert.setContentText("Certifique-se de que o DaraServer está rodando no Terminal 1 (porta 12345).");
                alert.showAndWait();
                System.exit(0);
            });
        }
    }

    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty() && out != null) {
            out.println("CHAT;" + text);
            chatInput.setText("");
        }
    }

    private void handleBoardClick(int row, int col) {
        if (myColor == null || currentTurn != myColor) return;

        if (currentPhase.equals("PLACE")) {
            out.println("PUT;" + row + ";" + col);
        } else if (currentPhase.equals("MOVE")) {
            if (selectedRow == -1) {
                selectedRow = row;
                selectedCol = col;
                boardButtons[row][col].setStyle(boardButtons[row][col].getStyle() + " -fx-border-color: #ffcc00; -fx-border-radius: 12; -fx-border-width: 3;");
            } else {
                out.println("MOVE;" + selectedRow + ";" + selectedCol + ";" + row + ";" + col);
                selectedRow = -1;
                selectedCol = -1;
            }
        } else if (currentPhase.equals("CAPTURE")) {
            out.println("CAPTURE;" + row + ";" + col);
        }
    }

    private void handleDesistencia() {
        if (out != null) out.println("DESISTENCIA");
    }

    private class ServerListener implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split(";");
                    String command = tokens[0];

                    switch (command) {
                        case "START":
                            myColor = CellState.valueOf(tokens[1]);
                            Platform.runLater(() -> {
                                Stage stage = (Stage) chatArea.getScene().getWindow();
                                stage.setTitle("Dara Game - " + (myColor == CellState.PLAYER_1 ? "Brancas (Jogador 1)" : "Pretas (Jogador 2)"));
                            });
                            break;

                        case "CHAT":
                            String msg = tokens[1];
                            Platform.runLater(() -> chatArea.appendText(" " + msg + "\n"));
                            break;

                        case "UPDATE":
                            String boardData = tokens[1];
                            currentTurn = CellState.valueOf(tokens[2]);
                            currentPhase = tokens[3];
                            Platform.runLater(() -> updateBoardUI(boardData));
                            break;

                        case "WINNER":
                            String winner = tokens[1];
                            Platform.runLater(() -> {
                                statusLabel.setText("PARTIDA CONCLUÍDA");
                                statusPanel.setStyle("-fx-background-color: #4b6eaf; -fx-background-radius: 10;");
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Fim de Jogo");
                                alert.setHeaderText(null);
                                alert.setContentText("🎉 VITÓRIA DO JOGADOR: " + winner);
                                alert.showAndWait();
                                System.exit(0);
                            });
                            break;
                    }
                }
            } catch (IOException e) {
                Platform.runLater(() -> chatArea.appendText("❌ Conexão perdida.\n"));
            }
        }
    }

    private void updateBoardUI(String boardData) {
        String[] cells = boardData.split(",");
        int index = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 6; j++) {
                int stateOrdinal = Integer.parseInt(cells[index++]);
                CellState state = CellState.values()[stateOrdinal];

                Button btn = boardButtons[i][j];

                if (state == CellState.PLAYER_1) {
                    btn.setText("⚪");
                    btn.setStyle("-fx-background-color: #e1e1e6; -fx-background-radius: 12; -fx-text-fill: #121214;");
                } else if (state == CellState.PLAYER_2) {
                    btn.setText("⚫");
                    btn.setStyle("-fx-background-color: #121214; -fx-background-radius: 12; -fx-text-fill: #e1e1e6; -fx-border-color: #2d2d35; -fx-border-radius: 12;");
                } else {
                    btn.setText("");
                    btn.setStyle("-fx-background-color: #2a2c32; -fx-background-radius: 12;");
                }
            }
        }

        if (currentTurn == myColor) {
            statusLabel.setText("SUA VEZ • FASE: " + currentPhase);
            statusPanel.setStyle("-fx-background-color: #2e8b57; -fx-background-radius: 10;");
        } else {
            statusLabel.setText("A GUARDAR OPONENTE...");
            statusPanel.setStyle("-fx-background-color: #8c5a28; -fx-background-radius: 10;");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}