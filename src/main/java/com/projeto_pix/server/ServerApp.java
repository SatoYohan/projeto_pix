package com.projeto_pix.server;

import com.projeto_pix.server.view.ServerGUI;

import javax.swing.SwingUtilities;

public class ServerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServerGUI().setVisible(true);
        });
    }
}