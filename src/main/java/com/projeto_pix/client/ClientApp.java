package com.projeto_pix.client;

import com.projeto_pix.client.view.ClientGUI;
import javax.swing.SwingUtilities;

public class ClientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientGUI().setVisible(true);
        });
    }
}