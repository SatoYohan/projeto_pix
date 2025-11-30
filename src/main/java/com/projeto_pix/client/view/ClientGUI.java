package com.projeto_pix.client.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto_pix.client.service.PixClientService;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class ClientGUI extends JFrame {
    private PixClientService service;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Componentes globais
    private JLabel lblSaldo;
    private JTextArea txtExtratoArea;

    public ClientGUI() {
        service = new PixClientService();
        service.setListener(this::handleServerResponse);

        setTitle("Sistema Bancário PIX");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createConnectionPanel(), "CONEXAO");
        mainPanel.add(createAuthPanel(), "AUTH");
        mainPanel.add(createDashboardPanel(), "DASHBOARD");

        add(mainPanel);
    }

    // --- TELA 1: CONEXÃO ---
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField txtIp = new JTextField("localhost", 15);
        JTextField txtPort = new JTextField("24000", 5);
        JButton btnConnect = new JButton("Conectar ao Servidor");

        btnConnect.addActionListener(e -> {
            try {
                String ip = txtIp.getText();
                int port = Integer.parseInt(txtPort.getText());
                service.connect(ip, port);
                // Aguarda resposta do "conectar" para mudar de tela
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage());
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("IP:"), gbc);
        gbc.gridx = 1; panel.add(txtIp, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Porta:"), gbc);
        gbc.gridx = 1; panel.add(txtPort, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; panel.add(btnConnect, gbc);

        return panel;
    }

    // --- TELA 2: LOGIN / CADASTRO ---
    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();

        // Aba Login
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField txtCpfLogin = new JTextField();
        JPasswordField txtSenhaLogin = new JPasswordField();
        JButton btnLogin = new JButton("Entrar");

        btnLogin.addActionListener(e -> {
            ObjectNode json = service.getMapper().createObjectNode();
            json.put("operacao", "usuario_login");
            json.put("cpf", txtCpfLogin.getText());
            json.put("senha", new String(txtSenhaLogin.getPassword()));
            service.sendJson(json);
        });

        loginPanel.add(new JLabel("CPF:")); loginPanel.add(txtCpfLogin);
        loginPanel.add(new JLabel("Senha:")); loginPanel.add(txtSenhaLogin);
        loginPanel.add(new JLabel("")); loginPanel.add(btnLogin);

        // Aba Cadastro
        JPanel cadPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField txtNomeCad = new JTextField();
        JTextField txtCpfCad = new JTextField();
        JPasswordField txtSenhaCad = new JPasswordField();
        JButton btnCad = new JButton("Cadastrar");

        btnCad.addActionListener(e -> {
            ObjectNode json = service.getMapper().createObjectNode();
            json.put("operacao", "usuario_criar");
            json.put("nome", txtNomeCad.getText());
            json.put("cpf", txtCpfCad.getText());
            json.put("senha", new String(txtSenhaCad.getPassword()));
            service.sendJson(json);
        });

        cadPanel.add(new JLabel("Nome:")); cadPanel.add(txtNomeCad);
        cadPanel.add(new JLabel("CPF:")); cadPanel.add(txtCpfCad);
        cadPanel.add(new JLabel("Senha:")); cadPanel.add(txtSenhaCad);
        cadPanel.add(new JLabel("")); cadPanel.add(btnCad);

        tabs.addTab("Login", loginPanel);
        tabs.addTab("Criar Conta", cadPanel);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    // --- TELA 3: DASHBOARD ---
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Topo
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblSaldo = new JLabel("Saldo: R$ --");
        lblSaldo.setFont(new Font("Arial", Font.BOLD, 16));
        JButton btnRefresh = new JButton("Atualizar Dados");
        JButton btnLogout = new JButton("Sair");

        btnRefresh.addActionListener(e -> requestUserData());
        btnLogout.addActionListener(e -> {
            ObjectNode json = service.getMapper().createObjectNode();
            json.put("operacao", "usuario_logout");
            json.put("token", service.getToken());
            service.sendJson(json);
        });

        topPanel.add(lblSaldo);
        topPanel.add(btnRefresh);
        topPanel.add(btnLogout);
        panel.add(topPanel, BorderLayout.NORTH);

        // Centro (Abas de Operações)
        JTabbedPane tabs = new JTabbedPane();

        // 1. Depósito
        JPanel pnlDeposito = new JPanel(new FlowLayout());
        JTextField txtValorDep = new JTextField(10);
        JButton btnDepositar = new JButton("Realizar Depósito");
        btnDepositar.addActionListener(e -> {
            try {
                double val = Double.parseDouble(txtValorDep.getText());
                ObjectNode json = service.getMapper().createObjectNode();
                json.put("operacao", "depositar");
                json.put("token", service.getToken());
                json.put("valor_enviado", val);
                service.sendJson(json);
                txtValorDep.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Valor inválido!");
            }
        });
        pnlDeposito.add(new JLabel("Valor R$:"));
        pnlDeposito.add(txtValorDep);
        pnlDeposito.add(btnDepositar);

        // 2. PIX
        JPanel pnlPix = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField txtCpfPix = new JTextField();
        JTextField txtValorPix = new JTextField();
        JButton btnPix = new JButton("Enviar PIX");
        btnPix.addActionListener(e -> {
            try {
                double val = Double.parseDouble(txtValorPix.getText());
                ObjectNode json = service.getMapper().createObjectNode();
                json.put("operacao", "transacao_criar");
                json.put("token", service.getToken());
                json.put("valor", val);
                json.put("cpf_destino", txtCpfPix.getText());
                service.sendJson(json);
                txtValorPix.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Dados inválidos!");
            }
        });
        pnlPix.add(new JLabel("CPF Destino:")); pnlPix.add(txtCpfPix);
        pnlPix.add(new JLabel("Valor R$:")); pnlPix.add(txtValorPix);
        pnlPix.add(new JLabel("")); pnlPix.add(btnPix);

        // 3. Extrato
        JPanel pnlExtrato = new JPanel(new BorderLayout());
        JPanel pnlExtratoForm = new JPanel(new FlowLayout());
        JTextField txtDataIni = new JTextField("01/01/2025 00:00:00", 12);
        JTextField txtDataFim = new JTextField("31/12/2025 23:59:59", 12);
        JButton btnExtrato = new JButton("Consultar");
        txtExtratoArea = new JTextArea();
        txtExtratoArea.setEditable(false);

        btnExtrato.addActionListener(e -> {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                String iniIso = LocalDateTime.parse(txtDataIni.getText(), fmt).toInstant(ZoneOffset.UTC).toString();
                String fimIso = LocalDateTime.parse(txtDataFim.getText(), fmt).toInstant(ZoneOffset.UTC).toString();

                ObjectNode json = service.getMapper().createObjectNode();
                json.put("operacao", "transacao_ler");
                json.put("token", service.getToken());
                json.put("data_inicial", iniIso);
                json.put("data_final", fimIso);
                service.sendJson(json);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Data inválida! Use dd/MM/yyyy HH:mm:ss");
            }
        });

        pnlExtratoForm.add(new JLabel("Início:")); pnlExtratoForm.add(txtDataIni);
        pnlExtratoForm.add(new JLabel("Fim:")); pnlExtratoForm.add(txtDataFim);
        pnlExtratoForm.add(btnExtrato);
        pnlExtrato.add(pnlExtratoForm, BorderLayout.NORTH);
        pnlExtrato.add(new JScrollPane(txtExtratoArea), BorderLayout.CENTER);

        // 4. Meus Dados (Atualizar/Deletar)
        JPanel pnlDados = new JPanel(new FlowLayout());
        JTextField txtNovoNome = new JTextField(10);
        JButton btnAtualizar = new JButton("Atualizar Nome");
        JButton btnDeletar = new JButton("Excluir Conta");

        btnAtualizar.addActionListener(e -> {
            ObjectNode json = service.getMapper().createObjectNode();
            json.put("operacao", "usuario_atualizar");
            json.put("token", service.getToken());
            ObjectNode userNode = service.getMapper().createObjectNode();
            userNode.put("nome", txtNovoNome.getText());
            json.set("usuario", userNode);
            service.sendJson(json);
        });

        btnDeletar.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "Tem certeza?", "Excluir", JOptionPane.YES_NO_OPTION);
            if(opt == JOptionPane.YES_OPTION) {
                ObjectNode json = service.getMapper().createObjectNode();
                json.put("operacao", "usuario_deletar");
                json.put("token", service.getToken());
                service.sendJson(json);
            }
        });

        pnlDados.add(new JLabel("Novo Nome:")); pnlDados.add(txtNovoNome);
        pnlDados.add(btnAtualizar);
        pnlDados.add(btnDeletar);

        tabs.addTab("Depósito", pnlDeposito);
        tabs.addTab("PIX", pnlPix);
        tabs.addTab("Extrato", pnlExtrato);
        tabs.addTab("Meus Dados", pnlDados);

        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private void requestUserData() {
        ObjectNode json = service.getMapper().createObjectNode();
        json.put("operacao", "usuario_ler");
        json.put("token", service.getToken());
        service.sendJson(json);
    }

    // --- CALLBACK: O que fazer quando chega mensagem do servidor ---
    private void handleServerResponse(JsonNode msg) {
        SwingUtilities.invokeLater(() -> {
            String operacao = msg.get("operacao").asText();
            boolean status = msg.get("status").asBoolean();
            String info = msg.get("info").asText();

            if (!status) {
                JOptionPane.showMessageDialog(this, info, "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Sucesso
            switch (operacao) {
                case "conectar":
                    cardLayout.show(mainPanel, "AUTH");
                    break;
                case "usuario_criar":
                    JOptionPane.showMessageDialog(this, "Conta criada! Faça login.");
                    break;
                case "usuario_login":
                    cardLayout.show(mainPanel, "DASHBOARD");
                    requestUserData(); // Pede saldo
                    break;
                case "usuario_ler":
                    if (msg.has("usuario")) {
                        double saldo = msg.get("usuario").get("saldo").asDouble();
                        String nome = msg.get("usuario").get("nome").asText();
                        lblSaldo.setText("Olá " + nome + " | Saldo: R$ " + String.format("%.2f", saldo));
                    }
                    break;
                case "depositar":
                case "transacao_criar": // PIX
                    JOptionPane.showMessageDialog(this, info);
                    requestUserData(); // Atualiza saldo
                    break;
                case "transacao_ler": // Extrato
                    txtExtratoArea.setText("");
                    if (msg.has("transacoes")) {
                        ArrayNode arr = (ArrayNode) msg.get("transacoes");
                        if (arr.isEmpty()) txtExtratoArea.append("Nenhuma transação.\n");
                        for (JsonNode tx : arr) {
                            String data = Instant.parse(tx.get("criado_em").asText()).toString();
                            double valor = tx.get("valor_enviado").asDouble();
                            String de = tx.get("usuario_enviador").get("nome").asText();
                            String para = tx.get("usuario_recebedor").get("nome").asText();
                            txtExtratoArea.append(String.format("[%s] R$ %.2f | %s -> %s\n", data, valor, de, para));
                        }
                    }
                    break;
                case "usuario_logout":
                case "usuario_deletar":
                    cardLayout.show(mainPanel, "AUTH");
                    JOptionPane.showMessageDialog(this, info);
                    break;
            }
        });
    }
}