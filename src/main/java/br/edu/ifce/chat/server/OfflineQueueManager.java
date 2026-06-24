package br.edu.ifce.chat.server;

import br.edu.ifce.chat.common.JmsSupport;
import br.edu.ifce.chat.common.Message;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Middleware Orientado a Mensagens (MOM): gerencia, no broker ActiveMQ, uma fila de
 * mensagens por cliente ({@code fila.<contato>}).
 *
 * <p>Quando o destinatário está offline, a mensagem é enfileirada aqui (requisitos 5 e 6).
 * Ao reconectar, a fila do cliente é drenada e suas mensagens são entregues.
 *
 * <p>Cada operação usa uma {@link Session} própria pois sessões JMS não são thread-safe
 * e o servidor RMI pode atender chamadas concorrentes. A {@link Connection} é compartilhada
 * (criar sessões a partir dela é seguro).
 */
public final class OfflineQueueManager implements AutoCloseable {

    private final Connection connection;

    public OfflineQueueManager() throws JMSException {
        this.connection = JmsSupport.connectionFactory().createConnection();
        this.connection.start();
    }

    /** Garante a existência da fila do contato no broker (requisito 7). */
    public void createQueue(String contato) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue fila = session.createQueue(JmsSupport.filaDe(contato));
            // Criar (e fechar) um consumidor materializa a fila no broker.
            session.createConsumer(fila).close();
        }
    }

    /** Enfileira uma mensagem na fila do destinatário. */
    public void enqueue(Message msg) throws JMSException {
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue fila = session.createQueue(JmsSupport.filaDe(msg.to()));
            MessageProducer producer = session.createProducer(fila);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(encode(session, msg));
        }
    }

    /** Drena e remove todas as mensagens pendentes da fila do contato, em ordem. */
    public List<Message> drain(String contato) throws JMSException {
        List<Message> mensagens = new ArrayList<>();
        try (Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            Queue fila = session.createQueue(JmsSupport.filaDe(contato));
            try (MessageConsumer consumer = session.createConsumer(fila)) {
                javax.jms.Message jms;
                // receive(timeout) curto: retorna null quando a fila esvazia.
                while ((jms = consumer.receive(300)) != null) {
                    if (jms instanceof TextMessage tm) {
                        mensagens.add(decode(tm));
                    }
                }
            }
        }
        return mensagens;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (JMSException ignored) {
            // encerrando
        }
    }

    // --- Codec: Message <-> TextMessage (propriedades de string, sem serialização frágil) ---

    private static TextMessage encode(Session session, Message msg) throws JMSException {
        TextMessage tm = session.createTextMessage(msg.text());
        tm.setStringProperty("from", msg.from());
        tm.setStringProperty("to", msg.to());
        tm.setStringProperty("momento", msg.momento().toString());
        return tm;
    }

    private static Message decode(TextMessage tm) throws JMSException {
        return new Message(
                tm.getStringProperty("from"),
                tm.getStringProperty("to"),
                tm.getText(),
                LocalDateTime.parse(tm.getStringProperty("momento")));
    }
}
