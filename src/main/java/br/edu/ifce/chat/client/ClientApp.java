package br.edu.ifce.chat.client;

import br.edu.ifce.chat.client.ui.LoginFrame;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.SwingUtilities;

/** Ponto de entrada da aplicação cliente. Aplica o tema dark e abre a tela de login. */
public final class ClientApp {

    private ClientApp() {
    }

    public static void main(String[] args) {
        // Garante que o objeto de callback exportado seja alcançável na demo local.
        System.setProperty("java.rmi.server.hostname", "localhost");
        FlatDarculaLaf.setup();
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
