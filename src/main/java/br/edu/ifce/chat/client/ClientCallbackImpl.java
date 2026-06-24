package br.edu.ifce.chat.client;

import br.edu.ifce.chat.common.ClientCallback;
import br.edu.ifce.chat.common.Message;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Objeto remoto do cliente, exportado para que o servidor possa empurrar (push) mensagens
 * e mudanças de presença. Apenas repassa os eventos para o {@link ChatClient}.
 */
public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private final ChatClient client;

    public ClientCallbackImpl(ChatClient client) throws RemoteException {
        super();
        this.client = client;
    }

    @Override
    public void receiveMessage(Message msg) throws RemoteException {
        client.aoReceberMensagem(msg);
    }

    @Override
    public void presenceChanged(String contato, boolean online) throws RemoteException {
        client.aoMudarPresenca(contato, online);
    }
}
