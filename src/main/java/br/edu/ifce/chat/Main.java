package br.edu.ifce.chat;

import br.edu.ifce.chat.client.ClientApp;
import br.edu.ifce.chat.server.ServerLauncher;

/**
 * Ponto de entrada único do fat-jar. O primeiro argumento escolhe o papel:
 * <pre>
 *   java -jar projeto-final.jar server   # servidor (broker MOM + RMI)
 *   java -jar projeto-final.jar client   # cliente  (GUI)
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        String papel = args.length > 0 ? args[0].toLowerCase() : "client";
        switch (papel) {
            case "server", "servidor" -> ServerLauncher.main(stripFirst(args));
            case "client", "cliente" -> ClientApp.main(stripFirst(args));
            default -> {
                System.out.println("Uso: java -jar projeto-final.jar [server|client]");
                System.exit(1);
            }
        }
    }

    private static String[] stripFirst(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] resto = new String[args.length - 1];
        System.arraycopy(args, 1, resto, 0, resto.length);
        return resto;
    }
}
