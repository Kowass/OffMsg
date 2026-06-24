package br.edu.ifce.chat.server;

import br.edu.ifce.chat.common.JmsSupport;
import org.apache.activemq.broker.BrokerService;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * Processo servidor (único e combinado): sobe o broker ActiveMQ embarcado (o "servidor
 * de mensagens offline") e o registro/serviço RMI no mesmo JVM.
 *
 * <p>Auto-contido: não é preciso instalar ActiveMQ nem rmiregistry separadamente.
 */
public final class ServerLauncher {

    public static final int RMI_PORT = 1099;
    public static final String SERVICE_NAME = "ChatService";
    public static final String SERVICE_URL = "rmi://localhost:" + RMI_PORT + "/" + SERVICE_NAME;

    private ServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        // Stubs RMI alcançáveis na demo local (servidor e clientes na mesma máquina).
        System.setProperty("java.rmi.server.hostname", "localhost");

        // 1) Broker ActiveMQ (MOM) embarcado, só em memória.
        BrokerService broker = new BrokerService();
        broker.setBrokerName("chat-broker");
        broker.addConnector(JmsSupport.BROKER_URL);
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.start();

        // 2) Gerenciador das filas offline + serviço RMI.
        OfflineQueueManager filas = new OfflineQueueManager();
        ChatServiceImpl service = new ChatServiceImpl(filas);

        LocateRegistry.createRegistry(RMI_PORT);
        Naming.rebind(SERVICE_URL, service);

        System.out.println("==================================================");
        System.out.println(" Servidor de Mensagens (Projeto Final PPD)");
        System.out.println(" Broker MOM  : " + JmsSupport.BROKER_URL);
        System.out.println(" Serviço RMI : " + SERVICE_URL);
        System.out.println(" Pressione Ctrl+C para encerrar.");
        System.out.println("==================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                filas.close();
                broker.stop();
                System.out.println("Servidor encerrado.");
            } catch (Exception ignored) {
            }
        }));

        broker.waitUntilStopped();
    }
}
