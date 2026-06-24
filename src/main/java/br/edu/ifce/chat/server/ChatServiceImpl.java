package br.edu.ifce.chat.server;

import br.edu.ifce.chat.common.ChatService;
import br.edu.ifce.chat.common.ClientCallback;
import br.edu.ifce.chat.common.Message;

import javax.jms.JMSException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação do serviço remoto. Mantém o mapa de presença (quem está online) e
 * decide o roteamento de cada mensagem:
 * <ul>
 *   <li>destinatário online  → entrega instantânea via {@link ClientCallback} (requisito 3);</li>
 *   <li>destinatário offline → enfileira na fila do destinatário no broker (requisitos 5 e 6).</li>
 * </ul>
 */
public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {

    /** contato -> callback do cliente atualmente online. */
    private final Map<String, ClientCallback> online = new ConcurrentHashMap<>();
    private final OfflineQueueManager filas;

    public ChatServiceImpl(OfflineQueueManager filas) throws RemoteException {
        super();
        this.filas = filas;
    }

    @Override
    public void createQueue(String contato) throws RemoteException {
        try {
            filas.createQueue(contato);
            log(contato + " solicitou criação da sua fila de mensagens.");
        } catch (JMSException e) {
            throw new RemoteException("Falha ao criar fila de " + contato, e);
        }
    }

    @Override
    public Set<String> register(String contato, ClientCallback cb) throws RemoteException {
        online.put(contato, cb);
        log(contato + " ficou ONLINE.");
        notificarPresenca(contato, true);

        // Drena a fila offline e entrega o que chegou enquanto estava desconectado.
        // A entrega é feita em outra thread para que register() retorne imediatamente:
        // o cliente segura seu próprio monitor durante o conectar(), e o callback (que
        // chega por uma thread RMI) precisa desse monitor — empurrar de forma síncrona
        // aqui causaria deadlock de reentrância.
        try {
            List<Message> pendentes = filas.drain(contato);
            if (!pendentes.isEmpty()) {
                entregarAssincrono(contato, cb, pendentes);
            }
        } catch (JMSException e) {
            throw new RemoteException("Falha ao drenar fila de " + contato, e);
        }

        // Snapshot dos demais contatos online (para a UI inicializar os status dos amigos).
        return Set.copyOf(online.keySet());
    }

    @Override
    public void unregister(String contato) throws RemoteException {
        online.remove(contato);
        log(contato + " ficou OFFLINE.");
        notificarPresenca(contato, false);
    }

    @Override
    public boolean isOnline(String contato) {
        return online.containsKey(contato);
    }

    @Override
    public void sendMessage(Message msg) throws RemoteException {
        ClientCallback destino = online.get(msg.to());
        if (destino != null) {
            // Caminho online: entrega direta via RMI.
            try {
                destino.receiveMessage(msg);
                log("ONLINE  " + msg.from() + " -> " + msg.to());
                return;
            } catch (RemoteException e) {
                // Destinatário caiu sem avisar: trata como offline e enfileira.
                online.remove(msg.to());
                log(msg.to() + " parecia online mas falhou; enfileirando.");
            }
        }
        // Caminho offline: aciona o broker (MOM).
        try {
            filas.enqueue(msg);
            log("OFFLINE " + msg.from() + " -> fila." + msg.to());
        } catch (JMSException e) {
            throw new RemoteException("Falha ao enfileirar mensagem para " + msg.to(), e);
        }
    }

    /** Entrega mensagens drenadas da fila numa thread separada, evitando reentrância no callback. */
    private void entregarAssincrono(String contato, ClientCallback cb, List<Message> pendentes) {
        Thread t = new Thread(() -> {
            for (int i = 0; i < pendentes.size(); i++) {
                try {
                    cb.receiveMessage(pendentes.get(i));
                } catch (RemoteException ex) {
                    // Cliente caiu durante a entrega: reenfileira o restante (em ordem) e encerra.
                    online.remove(contato);
                    for (int j = i; j < pendentes.size(); j++) {
                        try {
                            filas.enqueue(pendentes.get(j));
                        } catch (Exception ignored) {
                        }
                    }
                    return;
                }
            }
            log("Entregues " + pendentes.size() + " mensagem(ns) offline para " + contato + ".");
        }, "drain-" + contato);
        t.setDaemon(true);
        t.start();
    }

    /** Avisa todos os clientes online que um contato mudou de estado (a UI filtra pelos amigos). */
    private void notificarPresenca(String contato, boolean ativo) {
        for (Map.Entry<String, ClientCallback> e : online.entrySet()) {
            if (e.getKey().equals(contato)) {
                continue;
            }
            try {
                e.getValue().presenceChanged(contato, ativo);
            } catch (RemoteException ex) {
                online.remove(e.getKey());
            }
        }
    }

    private static void log(String texto) {
        System.out.println("[ChatService] " + texto);
    }
}
