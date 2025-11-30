package com.projeto_pix.server.view;

import com.projeto_pix.server.Server;

import javax.swing.*;
import java.awt.*;

public class ServerGUI extends JFrame {
    private JTextArea txtLog;
    private DefaultListModel<String> listModel;
    private JList<String> listUsers;
    private JButton btnStart;
    private JTextField txtPort;
    private Server serverInstance;

    public ServerGUI() {
        setTitle("Servidor PIX");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Topo: Config
        JPanel topPanel = new JPanel();
        txtPort = new JTextField("24000", 5);
        btnStart = new JButton("Iniciar Servidor");
        btnStart.addActionListener(e -> startServer());
        topPanel.add(new JLabel("Porta:"));
        topPanel.add(txtPort);
        topPanel.add(btnStart);

        // Centro: Logs e Lista
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        listModel = new DefaultListModel<>();
        listUsers = new JList<>(listModel);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(listUsers), new JScrollPane(txtLog));
        split.setDividerLocation(150);

        add(topPanel, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(txtPort.getText());
            serverInstance = new Server(this); // Passa a GUI para o servidor
            new Thread(() -> serverInstance.start(port)).start();
            btnStart.setEnabled(false);
            log("Servidor iniciado na porta " + port);
        } catch (Exception e) {
            log("Erro ao iniciar: " + e.getMessage());
        }
    }

    // Métodos públicos para o Server chamar
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> txtLog.append(msg + "\n"));
    }

    public void addUser(String userInfo) {
        SwingUtilities.invokeLater(() -> {
            if (!listModel.contains(userInfo)) listModel.addElement(userInfo);
        });
    }

    public void removeUser(String userInfo) {
        SwingUtilities.invokeLater(() -> listModel.removeElement(userInfo));
    }
}