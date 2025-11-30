// Em: src/main/java/com/projeto_pix/server/Server.java
package com.projeto_pix.server;

import com.projeto_pix.common.model.Transacao;
import com.projeto_pix.common.model.Usuario;
import com.projeto_pix.server.view.ServerGUI;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    public static final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    public static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>();
    public static final List<Transacao> transacoes = Collections.synchronizedList(new ArrayList<>());
    public static final AtomicInteger transacaoIdCounter = new AtomicInteger(1);

    private ServerGUI gui; // Referência para a interface

    public Server(ServerGUI gui) {
        this.gui = gui;
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                gui.log("Novo cliente: " + clientSocket.getInetAddress());
                // Passamos a instância do Server para o Handler
                new Thread(new ClientHandler(clientSocket, this)).start();
            }
        } catch (Exception e) {
            gui.log("Erro fatal: " + e.getMessage());
        }
    }

    public ServerGUI getGui() {
        return gui;
    }
}