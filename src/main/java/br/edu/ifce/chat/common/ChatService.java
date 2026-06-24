package br.edu.ifce.chat.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/**
 * Serviço remoto (RMI) que centraliza a troca de mensagens e o controle de presença.
 *
 * <p>O cliente nunca acessa o broker diretamente: todo o acesso à fila de mensagens
 * offline é mediado por este servidor remoto (requisito 4 — "servidor de mensagens
 * offline acessado através de um servidor remoto via RMI/RPC").
 */
public interface ChatService extends Remote {

    /**
     * Solicita ao servidor a criação da fila de mensagens do cliente.
     * Chamado ao entrar no sistema (requisito 7).
     */
    void createQueue(String contato) throws RemoteException;

    /**
     * Marca o contato como online, registrando seu callback para entrega instantânea.
     * Como efeito, o servidor drena a fila offline do contato e empurra as mensagens
     * pendentes. Retorna o conjunto de contatos atualmente online (para inicializar a UI).
     */
    Set<String> register(String contato, ClientCallback cb) throws RemoteException;

    /** Marca o contato como offline (deixa de receber mensagens instantâneas). */
    void unregister(String contato) throws RemoteException;

    /** Indica se um contato está online no momento. */
    boolean isOnline(String contato) throws RemoteException;

    /**
     * Roteia uma mensagem: se o destinatário está online, entrega na hora via callback
     * (requisito 3); caso contrário, enfileira na fila do destinatário no broker
     * (requisitos 5 e 6).
     */
    void sendMessage(Message msg) throws RemoteException;
}
