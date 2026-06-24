package br.edu.ifce.chat.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mensagem trocada entre dois contatos.
 *
 * <p>É {@link Serializable} porque trafega tanto via RMI (entre cliente e servidor)
 * quanto dentro de mensagens JMS guardadas na fila offline do ActiveMQ.
 *
 * @param from     nome do contato remetente
 * @param to       nome do contato destinatário
 * @param text     conteúdo da mensagem
 * @param momento  instante em que foi criada
 */
public record Message(String from, String to, String text, LocalDateTime momento)
        implements Serializable {

    /** Cria uma mensagem carimbando o instante atual. */
    public static Message of(String from, String to, String text) {
        return new Message(from, to, text, LocalDateTime.now());
    }
}
