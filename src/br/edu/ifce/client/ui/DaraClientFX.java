package br.edu.ifce.client.ui;

import br.edu.ifce.shared.model.CellState;
import br.edu.ifce.shared.model.DaraClientInterface;
import br.edu.ifce.shared.model.DaraServerInterface;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class DaraClientFX extends Application implements DaraClientInterface {
    
    // Configurações do Registro RMI
    private static final String RMI_URL = "rmi://127.0.0.1:1099/DaraServerService";

    // Interfaces de Comunicação RMI (Substituem os Sockets)
    private DaraServerInterface servidorRemoto;

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
        primaryStage.setTitle("Dara Strategy Game Pro (RMI Edition)");

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

        statusLabel = new Label("LOCALIZANDO SERVIÇO RMI...");
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

        // Conecta ao servidor RMI de forma assíncrona para não congelar o carregamento da janela
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
            // 1. Exporta esta instância JavaFX no subsistema RMI de forma dinâmica
            UnicastRemoteObject.exportObject(this, 0);

            // 2. Localiza o serviço remoto do Dara Server pelo contrato de nomes
            servidorRemoto = (DaraServerInterface) Naming.lookup(RMI_URL);
            
            // 3. Cadastra o cliente e descobre dinamicamente a cor/papel associado (PLAYER_1 ou PLAYER_2)
            String papel = servidorRemoto.registrarJogador(this);

            if (papel.equals("CHEIO")) {
                Platform.runLater(() -> {
                    showErrorAlert("Partida Cheia", "A partida solicitada já conta com o limite de 2 jogadores ativos.");
                    System.exit(0);
                });
                return;
            }

            myColor = CellState.valueOf(papel);
            
            // Configura o título da janela de acordo com a cor recebida do RMI
            Platform.runLater(() -> {
                Stage stage = (Stage) chatArea.getScene().getWindow();
                stage.setTitle("Dara Game RMI - " + (myColor == CellState.PLAYER_1 ? "Brancas (Jogador 1)" : "Pretas (Jogador 2)"));
                chatArea.appendText(" Conectado ao Servidor RMI como " + myColor.name() + ".\n");
                chatArea.appendText(" Aguardando segundo oponente iniciar...\n");
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                showErrorAlert("Erro de Comunicação RMI", "Não foi possível se associar ao serviço do DaraServerRMI.\nVerifique se o servidor está ativo no Registro (porta 1099).");
                System.exit(0);
            });
        }
    }

    private void sendChatMessage() {
        String text = chatInput.getText().trim();
        if (!text.isEmpty() && servidorRemoto != null) {
            try {
                // Invocação remota direta substituindo o fluxo manual por strings concatenadas
                servidorRemoto.enviarMensagemChat(myColor.name(), text);
                chatInput.setText("");
            } catch (RemoteException e) {
                chatArea.appendText("❌ Falha de rede ao tentar enviar mensagem.\n");
            }
        }
    }

    private void handleBoardClick(int row, int col) {
        if (myColor == null || currentTurn != myColor || servidorRemoto == null) return;

        try {
            if (currentPhase.equals("PLACE")) {
                servidorRemoto.processarColocacao(myColor.name(), row, col);
            } else if (currentPhase.equals("MOVE")) {
                if (selectedRow == -1) {
                    selectedRow = row;
                    selectedCol = col;
                    boardButtons[row][col].setStyle(boardButtons[row][col].getStyle() + " -fx-border-color: #ffcc00; -fx-border-radius: 12; -fx-border-width: 3;");
                } else {
                    servidorRemoto.processarMovimento(myColor.name(), selectedRow, selectedCol, row, col);
                    selectedRow = -1;
                    selectedCol = -1;
                }
            } else if (currentPhase.equals("CAPTURE")) {
                servidorRemoto.processarCaptura(myColor.name(), row, col);
            }
        } catch (RemoteException e) {
            chatArea.appendText("❌ Erro ao sincronizar ação no tabuleiro remoto.\n");
        }
    }

    private void handleDesistencia() {
        if (servidorRemoto != null && myColor != null) {
            try {
                servidorRemoto.processarDesistencia(myColor.name());
            } catch (RemoteException e) {
                chatArea.appendText("❌ Erro de conexão ao notificar desistência.\n");
            }
        }
    }

    // =========================================================================
    // IMPLEMENTAÇÃO DAS OPERAÇÕES DO CONTRATO RMI (CALLBACKS DO SERVIDOR)
    // =========================================================================

    @Override
    public void receberAtualizacaoEstado(String tabuleiroSerializado, String turnoAtual, String faseAtual) throws RemoteException {
        this.currentTurn = CellState.valueOf(turnoAtual);
        this.currentPhase = faseAtual;
        
        // Garante que a atualização gráfica rode sincronizada com o JavaFX Application Thread
        Platform.runLater(() -> updateBoardUI(tabuleiroSerializado));
    }

    @Override
    public void receberMensagemChat(String remetente, String texto) throws RemoteException {
        Platform.runLater(() -> chatArea.appendText(" " + remetente + ": " + texto + "\n"));
    }

    @Override
    public void notificarVencedor(String vencedor) throws RemoteException {
        Platform.runLater(() -> {
            statusLabel.setText("PARTIDA CONCLUÍDA");
            statusPanel.setStyle("-fx-background-color: #4b6eaf; -fx-background-radius: 10;");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Fim de Jogo");
            alert.setHeaderText(null);
            alert.setContentText("🎉 VITÓRIA DO JOGADOR: " + vencedor);
            alert.showAndWait();
            System.exit(0);
        });
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE RENDERIZAÇÃO
    // =========================================================================

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
                    btn.setStyle("-fx-background-color: #e1e1e6; -fx-background-radius: 12; -fx-text-fill: #121214; -fx-font-size: 36px;");
                } else if (state == CellState.PLAYER_2) {
                    btn.setText("⚫");
                    btn.setStyle("-fx-background-color: #121214; -fx-background-radius: 12; -fx-text-fill: #e1e1e6; -fx-border-color: #2d2d35; -fx-border-radius: 12; -fx-font-size: 36px;");
                } else {
                    btn.setText("");
                    btn.setStyle("-fx-background-color: #2a2c32; -fx-background-radius: 12; -fx-font-size: 36px;");
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

    private void showErrorAlert(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}