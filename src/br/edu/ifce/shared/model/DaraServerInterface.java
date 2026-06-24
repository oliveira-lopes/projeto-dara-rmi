package br.edu.ifce.shared.model;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaraServerInterface extends Remote {
    
    // Conecta um jogador e retorna se ele é PLAYER_1 ou PLAYER_2
    String registrarJogador(DaraClientInterface clienteRef) throws RemoteException;
    
    // Métodos que substituem os comandos enviados por String do ClientHandler:
    void enviarMensagemChat(String jogador, String texto) throws RemoteException;
    
    void processarColocacao(String jogador, int linha, int coluna) throws RemoteException;
    
    void processarMovimento(String jogador, int deLinha, int deColuna, int paraLinha, int paraColuna) throws RemoteException;
    
    void processarCaptura(String jogador, int linha, int coluna) throws RemoteException;
    
    void processarDesistencia(String jogador) throws RemoteException;
}