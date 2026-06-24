package br.edu.ifce.chat.client;

import br.edu.ifce.chat.common.ChatService;
import br.edu.ifce.chat.common.Message;
import br.edu.ifce.chat.server.ServerLauncher;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Núcleo (modelo) do cliente. Concentra a comunicação RMI, a lista de amigos, o histórico
 * de conversas e a "outbox" local de mensagens escritas enquanto o próprio cliente está
 * offline (estilo WhatsApp).
 *
 * <p>Estado <b>online</b> = registrado no servidor via RMI. Estado <b>offline</b> = não
 * registrado: nada é enviado pela rede; as mensagens ficam na outbox até reconectar.
 */
public class ChatClient {

    /** Eventos enviados para a UI. As implementações devem garantir execução na EDT do Swing. */
    public interface Listener {
        void onMensagem(Message msg);
        void onPresenca(String contato, boolean online);
        void onEstado(boolean online);
        void onAmigosAlterados();
    }

    private final String contato;
    private final String host;
    private Listener listener;

    private ChatService servico;
    private ClientCallbackImpl callback;
    private volatile boolean online;

    /** Amigos (ordem de inserção) e o último status de presença conhecido de cada um. */
    private final Set<String> amigos = new LinkedHashSet<>();
    private final Map<String, Boolean> amigoOnline = new LinkedHashMap<>();

    /** Histórico de conversa por amigo (chave = nome do outro contato). */
    private final Map<String, List<Message>> historico = new LinkedHashMap<>();

    /** Mensagens compostas enquanto offline, aguardando reconexão. */
    private final List<Message> outbox = new ArrayList<>();

    public ChatClient(String contato, String host) {
        this.contato = contato;
        this.host = host;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public String getContato() {
        return contato;
    }

    public boolean isOnline() {
        return online;
    }

    public synchronized List<String> getAmigos() {
        return new ArrayList<>(amigos);
    }

    public synchronized boolean isAmigoOnline(String amigo) {
        return Boolean.TRUE.equals(amigoOnline.get(amigo));
    }

    public synchronized List<Message> getHistorico(String amigo) {
        return new ArrayList<>(historico.getOrDefault(amigo, List.of()));
    }

    public synchronized int getPendentes() {
        return outbox.size();
    }

    // --- Conexão / estado ------------------------------------------------------------------

    /**
     * Faz lookup do serviço (se necessário), garante a fila do cliente (requisito 7) e
     * registra o cliente como online. Em seguida, esvazia a outbox.
     */
    public synchronized void conectar() throws Exception {
        if (servico == null) {
            servico = (ChatService) Naming.lookup("rmi://" + host + ":"
                    + ServerLauncher.RMI_PORT + "/" + ServerLauncher.SERVICE_NAME);
            callback = new ClientCallbackImpl(this);
            servico.createQueue(contato); // requisito 7
        }

        Set<String> onlineAgora = servico.register(contato, callback);
        online = true;

        for (String amigo : amigos) {
            amigoOnline.put(amigo, onlineAgora.contains(amigo));
        }

        flushOutbox();

        if (listener != null) {
            listener.onEstado(true);
            listener.onAmigosAlterados();
        }
    }

    /** Desconecta do servidor RMI (passa a offline). */
    public synchronized void desconectar() {
        try {
            if (servico != null) {
                servico.unregister(contato);
            }
        } catch (Exception ignored) {
            // mesmo se falhar, seguimos para o estado offline
        }
        online = false;
        for (String amigo : amigos) {
            amigoOnline.put(amigo, false);
        }
        if (listener != null) {
            listener.onEstado(false);
            listener.onAmigosAlterados();
        }
    }

    private void flushOutbox() throws Exception {
        if (outbox.isEmpty()) {
            return;
        }
        List<Message> pendentes = new ArrayList<>(outbox);
        outbox.clear();
        for (Message m : pendentes) {
            servico.sendMessage(m);
        }
    }

    // --- Amigos ----------------------------------------------------------------------------

    public synchronized void adicionarAmigo(String amigo) {
        if (amigo == null || amigo.isBlank() || amigo.equals(contato) || amigos.contains(amigo)) {
            return;
        }
        amigos.add(amigo);
        boolean estaOnline = false;
        try {
            if (online && servico != null) {
                estaOnline = servico.isOnline(amigo);
            }
        } catch (Exception ignored) {
        }
        amigoOnline.put(amigo, estaOnline);
        if (listener != null) {
            listener.onAmigosAlterados();
        }
    }

    public synchronized void removerAmigo(String amigo) {
        amigos.remove(amigo);
        amigoOnline.remove(amigo);
        if (listener != null) {
            listener.onAmigosAlterados();
        }
    }

    // --- Envio / recebimento ---------------------------------------------------------------

    /**
     * Envia uma mensagem a um amigo. Se online, roteia pelo servidor; se offline, guarda na
     * outbox local para envio posterior. Em ambos os casos registra no histórico.
     */
    public synchronized void enviar(String para, String texto) throws Exception {
        Message msg = Message.of(contato, para, texto);
        registrarHistorico(para, msg);
        if (online && servico != null) {
            servico.sendMessage(msg);
        } else {
            outbox.add(msg);
        }
        if (listener != null) {
            listener.onMensagem(msg);
        }
    }

    void aoReceberMensagem(Message msg) {
        synchronized (this) {
            registrarHistorico(msg.from(), msg);
        }
        if (listener != null) {
            listener.onMensagem(msg);
        }
    }

    void aoMudarPresenca(String contato, boolean online) {
        synchronized (this) {
            if (amigos.contains(contato)) {
                amigoOnline.put(contato, online);
            } else {
                return;
            }
        }
        if (listener != null) {
            listener.onPresenca(contato, online);
        }
    }

    private void registrarHistorico(String amigo, Message msg) {
        historico.computeIfAbsent(amigo, k -> new ArrayList<>()).add(msg);
    }
}
