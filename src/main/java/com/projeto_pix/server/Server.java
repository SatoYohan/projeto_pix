// Em: src/main/java/com/projeto_pix/server/Server.java
package com.projeto_pix.server;

import java.net.InetAddress; // 1. Importe a classe InetAddress
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.projeto_pix.common.model.Usuario;

public class Server {
    // Usamos ConcurrentHashMap para segurança em ambiente com múltiplas threads
    // static para que sejam compartilhados entre todas as instâncias de ClientHandler
    public static final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    public static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>(); // Mapeia token -> cpf

    public static void main(String[] args) {
        final int PORT = 12345;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            try {
                // 2. Obtém o endereço de IP da máquina local
                InetAddress ip = InetAddress.getLocalHost();
                System.out.println("=================================================");
                System.out.println("Servidor iniciado!");
                System.out.println("IP para conexão: " + ip.getHostAddress());
                System.out.println("Porta: " + PORT);
                System.out.println("=================================================");
            } catch (UnknownHostException e) {
                System.err.println("Não foi possível obter o IP local: " + e.getMessage());
            }

            System.out.println("\nAguardando conexões de clientes...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}