package br.edu.ifce.chat.client.ui;

import br.edu.ifce.chat.client.ChatClient;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** Tela inicial: pede o nome de contato e o host do servidor, e abre a janela de chat. */
public class LoginFrame extends JFrame {

    private final JTextField campoNome = new JTextField();
    private final JTextField campoHost = new JTextField("localhost");

    public LoginFrame() {
        super("Mensageiro PPD — Entrar");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(360, 200);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        form.add(new JLabel("Nome de contato:"));
        form.add(campoNome);
        form.add(new JLabel("Servidor:"));
        form.add(campoHost);

        JButton entrar = new JButton("Entrar");
        entrar.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(entrar);
        campoNome.addActionListener(e -> entrar());

        JPanel sul = new JPanel();
        sul.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        sul.add(entrar);

        add(form, BorderLayout.CENTER);
        add(sul, BorderLayout.SOUTH);
        setVisible(true);
        campoNome.requestFocusInWindow();
    }

    private void entrar() {
        String nome = campoNome.getText().trim();
        String host = campoHost.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe um nome de contato.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (host.isEmpty()) {
            host = "localhost";
        }

        ChatClient client = new ChatClient(nome, host);
        try {
            client.conectar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível conectar ao servidor (" + host + ").\n"
                            + "Verifique se o servidor está em execução.\n\nDetalhe: " + ex.getMessage(),
                    "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new ChatFrame(client);
        dispose();
    }
}
