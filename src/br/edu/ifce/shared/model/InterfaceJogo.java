package br.edu.ifce.shared.model;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceJogo extends Remote {
    
    // Chamado pelo oponente para colocar uma peça na fase de colocação
    void colocarPeca(int linha, int coluna) throws RemoteException;
    
    // Chamado pelo oponente para mover uma peça na fase de movimentação
    void moverPeca(int linhaOrigem, int colunaOrigem, int linhaDestino, int colunaDestino) throws RemoteException;
    
    // Chamado pelo oponente para capturar uma peça sua
    void capturarPeca(int linha, int coluna) throws RemoteException;
    
    // Envio de mensagens do Chat do jogo
    void receberMensagemChat(String remetente, String mensagem) throws RemoteException;
    
    // Notificar que o oponente desistiu da partida
    void notificarDesistencia() throws RemoteException;
}