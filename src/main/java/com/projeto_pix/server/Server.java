// Em: src/main/java/com/projeto_pix/server/Server.java
package com.projeto_pix.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger; // Importar para o ID da transação
import com.projeto_pix.common.model.Usuario;
import com.projeto_pix.common.model.Transacao; // Importar o modelo de Transacao

public class Server {
    // Usamos ConcurrentHashMap para segurança em ambiente com múltiplas threads
    // static para que sejam compartilhados entre todas as instâncias de ClientHandler
    public static final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    public static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>();

    // Lista segura para threads para armazenar todas as transações (Extrato)
    public static final List<Transacao> transacoes = Collections.synchronizedList(new ArrayList<>());
    // Contador atômico para garantir IDs de transação únicos
    public static final AtomicInteger transacaoIdCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        final int PORT = 24000;
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            try {
                // Obtém o endereço de IP da máquina local
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