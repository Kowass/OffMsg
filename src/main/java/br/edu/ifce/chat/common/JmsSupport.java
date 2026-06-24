package br.edu.ifce.chat.common;

import org.apache.activemq.ActiveMQConnectionFactory;

/** Configurações JMS compartilhadas e nomes das filas por cliente. */
public final class JmsSupport {

    /** Endereço TCP do broker ActiveMQ embarcado no processo servidor. */
    public static final String BROKER_URL = "tcp://localhost:61616";

    private JmsSupport() {
    }

    /** Cria a fábrica de conexões apontando para o broker. */
    public static ActiveMQConnectionFactory connectionFactory() {
        return new ActiveMQConnectionFactory(BROKER_URL);
    }

    /** Nome da fila offline (MOM) de um contato: {@code fila.<contato>}. */
    public static String filaDe(String contato) {
        return "fila." + contato;
    }
}
