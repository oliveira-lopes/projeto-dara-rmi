package br.edu.ifce.shared.model;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DaraClientInterface extends Remote {
    
    // Método para o servidor atualizar a tela do JavaFX com o estado atualizado do tabuleiro
    void receberAtualizacaoEstado(String tabuleiroSerializado, String turnoAtual, String faseAtual) throws RemoteException;
    
    // Método para o servidor empurrar mensagens de chat na tela do cliente
    void receberMensagemChat(String remetente, String texto) throws RemoteException;
    
    // Método para notificar vitória
    void notificarVencedor(String vencedor) throws RemoteException;
}