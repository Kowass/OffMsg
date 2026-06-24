package br.edu.ifce.chat.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Objeto remoto exportado por cada cliente. O servidor o utiliza para "empurrar"
 * (push) eventos para o cliente enquanto este estiver online, sem necessidade de polling.
 */
public interface ClientCallback extends Remote {

    /** Entrega uma mensagem ao cliente (instantânea, se online; ou drenada da fila ao reconectar). */
    void receiveMessage(Message msg) throws RemoteException;

    /** Notifica que um contato mudou de estado (online/offline), para atualizar a lista de amigos. */
    void presenceChanged(String contato, boolean online) throws RemoteException;
}
