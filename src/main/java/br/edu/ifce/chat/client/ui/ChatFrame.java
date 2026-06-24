package br.edu.ifce.chat.client.ui;

import br.edu.ifce.chat.client.ChatClient;
import br.edu.ifce.chat.common.Message;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Janela principal do chat. À esquerda, a lista de amigos (sempre visível, com status e
 * inclusão/exclusão); no topo, o controle de estado online/offline; ao centro, a conversa
 * com o amigo selecionado e o campo de envio.
 */
public class ChatFrame extends JFrame implements ChatClient.Listener {

    private static final Color VERDE = new Color(0x4CAF50);
    private static final Color CINZA = new Color(0x9E9E9E);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ChatClient client;

    private final DefaultListModel<String> modeloAmigos = new DefaultListModel<>();
    private final JList<String> listaAmigos = new JList<>(modeloAmigos);
    private final JTextField campoNovoAmigo = new JTextField();

    private final JLabel rotuloEstado = new JLabel();
    private final JButton botaoEstado = new JButton();

    private final JLabel cabecalhoConversa = new JLabel("Selecione um amigo", SwingConstants.LEFT);
    private final JTextArea areaConversa = new JTextArea();
    private final JTextField campoMensagem = new JTextField();
    private final JButton botaoEnviar = new JButton("Enviar");

    public ChatFrame(ChatClient client) {
        super("Mensageiro PPD — " + client.getContato());
        this.client = client;
        client.setListener(this);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(construirTopo(), BorderLayout.NORTH);
        add(construirPainelAmigos(), BorderLayout.WEST);
        add(construirPainelConversa(), BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                client.desconectar();
                dispose();
                System.exit(0);
            }
        });

        atualizarAmigos();
        onEstado(client.isOnline());
        setVisible(true);
    }

    // --- Construção da UI ------------------------------------------------------------------

    private JComponent construirTopo() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JLabel eu = new JLabel("Você: " + client.getContato());
        eu.setFont(eu.getFont().deriveFont(Font.BOLD));

        JPanel direita = new JPanel();
        direita.add(rotuloEstado);
        botaoEstado.addActionListener(e -> alternarEstado());
        direita.add(botaoEstado);

        topo.add(eu, BorderLayout.WEST);
        topo.add(direita, BorderLayout.EAST);
        return topo;
    }

    private JComponent construirPainelAmigos() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 6));
        painel.setPreferredSize(new Dimension(230, 0));

        JLabel titulo = new JLabel("Amigos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD));

        listaAmigos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaAmigos.setCellRenderer(new RenderizadorAmigo());
        listaAmigos.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderizarConversa();
            }
        });

        // Inclusão de amigo (requisito 8): basta digitar o nome e adicionar.
        JPanel adicionar = new JPanel(new BorderLayout(4, 0));
        campoNovoAmigo.addActionListener(e -> adicionarAmigo());
        JButton botaoAdd = new JButton("+");
        botaoAdd.setToolTipText("Adicionar amigo");
        botaoAdd.addActionListener(e -> adicionarAmigo());
        adicionar.add(campoNovoAmigo, BorderLayout.CENTER);
        adicionar.add(botaoAdd, BorderLayout.EAST);

        JButton botaoRemover = new JButton("Remover amigo");
        botaoRemover.addActionListener(e -> removerAmigo());

        JPanel sul = new JPanel(new BorderLayout(0, 6));
        sul.add(adicionar, BorderLayout.NORTH);
        sul.add(botaoRemover, BorderLayout.SOUTH);

        painel.add(titulo, BorderLayout.NORTH);
        painel.add(new JScrollPane(listaAmigos), BorderLayout.CENTER);
        painel.add(sul, BorderLayout.SOUTH);
        return painel;
    }

    private JComponent construirPainelConversa() {
        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createEmptyBorder(0, 6, 10, 10));

        cabecalhoConversa.setFont(cabecalhoConversa.getFont().deriveFont(Font.BOLD));

        areaConversa.setEditable(false);
        areaConversa.setLineWrap(true);
        areaConversa.setWrapStyleWord(true);

        JPanel envio = new JPanel(new BorderLayout(6, 0));
        campoMensagem.addActionListener(e -> enviar());
        botaoEnviar.addActionListener(e -> enviar());
        envio.add(campoMensagem, BorderLayout.CENTER);
        envio.add(botaoEnviar, BorderLayout.EAST);

        painel.add(cabecalhoConversa, BorderLayout.NORTH);
        painel.add(new JScrollPane(areaConversa), BorderLayout.CENTER);
        painel.add(envio, BorderLayout.SOUTH);
        return painel;
    }

    // --- Ações da UI -----------------------------------------------------------------------

    private void alternarEstado() {
        if (client.isOnline()) {
            client.desconectar();
        } else {
            try {
                client.conectar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Falha ao reconectar: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void adicionarAmigo() {
        String nome = campoNovoAmigo.getText().trim();
        if (!nome.isEmpty()) {
            client.adicionarAmigo(nome);
            campoNovoAmigo.setText("");
        }
    }

    private void removerAmigo() {
        String amigo = listaAmigos.getSelectedValue();
        if (amigo != null) {
            client.removerAmigo(amigo);
        }
    }

    private void enviar() {
        String amigo = listaAmigos.getSelectedValue();
        if (amigo == null) {
            JOptionPane.showMessageDialog(this, "Selecione um amigo para conversar.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String texto = campoMensagem.getText().trim();
        if (texto.isEmpty()) {
            return;
        }
        try {
            client.enviar(amigo, texto);
            campoMensagem.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Falha ao enviar: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- Renderização ----------------------------------------------------------------------

    private void atualizarAmigos() {
        String selecionado = listaAmigos.getSelectedValue();
        modeloAmigos.clear();
        for (String amigo : client.getAmigos()) {
            modeloAmigos.addElement(amigo);
        }
        if (selecionado != null) {
            listaAmigos.setSelectedValue(selecionado, false);
        }
        listaAmigos.repaint();
    }

    private void renderizarConversa() {
        String amigo = listaAmigos.getSelectedValue();
        if (amigo == null) {
            cabecalhoConversa.setText("Selecione um amigo");
            areaConversa.setText("");
            return;
        }
        cabecalhoConversa.setText("Conversa com " + amigo
                + (client.isAmigoOnline(amigo) ? "  (online)" : "  (offline)"));
        StringBuilder sb = new StringBuilder();
        List<Message> historico = client.getHistorico(amigo);
        for (Message m : historico) {
            String quem = m.from().equals(client.getContato()) ? "você" : m.from();
            sb.append('[').append(m.momento().format(HORA)).append("] ")
                    .append(quem).append(": ").append(m.text()).append('\n');
        }
        areaConversa.setText(sb.toString());
        areaConversa.setCaretPosition(areaConversa.getDocument().getLength());
    }

    // --- ChatClient.Listener (eventos vindos da rede; despachados para a EDT) ---------------

    @Override
    public void onMensagem(Message msg) {
        SwingUtilities.invokeLater(() -> {
            String outro = msg.from().equals(client.getContato()) ? msg.to() : msg.from();
            if (outro.equals(listaAmigos.getSelectedValue())) {
                renderizarConversa();
            }
        });
    }

    @Override
    public void onPresenca(String contato, boolean online) {
        SwingUtilities.invokeLater(() -> {
            listaAmigos.repaint();
            if (contato.equals(listaAmigos.getSelectedValue())) {
                renderizarConversa();
            }
        });
    }

    @Override
    public void onEstado(boolean online) {
        SwingUtilities.invokeLater(() -> {
            int pendentes = client.getPendentes();
            if (online) {
                rotuloEstado.setText("● ONLINE   ");
                rotuloEstado.setForeground(VERDE);
                botaoEstado.setText("Ficar Offline");
            } else {
                String extra = pendentes > 0 ? "  (" + pendentes + " na fila local)" : "";
                rotuloEstado.setText("● OFFLINE" + extra + "   ");
                rotuloEstado.setForeground(CINZA);
                botaoEstado.setText("Ficar Online");
            }
        });
    }

    @Override
    public void onAmigosAlterados() {
        SwingUtilities.invokeLater(this::atualizarAmigos);
    }

    /** Renderiza cada amigo com um marcador colorido conforme o status de presença. */
    private final class RenderizadorAmigo extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel rotulo = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            String amigo = String.valueOf(value);
            boolean amigoOnline = client.isAmigoOnline(amigo);
            rotulo.setText((amigoOnline ? "● " : "○ ") + amigo);
            if (!isSelected) {
                rotulo.setForeground(amigoOnline ? VERDE : CINZA);
            }
            rotulo.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            return rotulo;
        }
    }
}
