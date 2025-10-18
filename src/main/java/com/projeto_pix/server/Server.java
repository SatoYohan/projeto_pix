// Em: src/main/java/com/projeto_pix/server/Server.java
package com.projeto_pix.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.projeto_pix.common.model.Usuario;

public class Server {
    public static final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    public static final Map<String, String> sessoesAtivas = new ConcurrentHashMap<>();

    private static final int DEFAULT_PORT = 24000;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Lógica para ler a porta a partir dos argumentos da linha de comando
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Argumento de porta inválido. Usando a porta padrão: " + DEFAULT_PORT);
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            // --- INÍCIO DA ALTERAÇÃO: LÓGICA ROBUSTA PARA ENCONTRAR O IP ---
            String ipAddress = findCorrectIPAddress();
            System.out.println("=================================================");
            System.out.println("Servidor iniciado!");
            System.out.println("IP para conexão: " + ipAddress);
            System.out.println("Porta: " + port);
            System.out.println("=================================================");
            // --- FIM DA ALTERAÇÃO ---

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

    /**
     * Itera por todas as interfaces de rede para encontrar o endereço IPv4 local correto.
     * @return O endereço de IP como String, ou "IP não encontrado" em caso de falha.
     */
    private static String findCorrectIPAddress() {
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if (!netint.isLoopback() && netint.isUp()) { // Ignora interfaces de loopback e inativas
                    Enumeration<InetAddress> addresses = netint.getInetAddresses();
                    for (InetAddress addr : Collections.list(addresses)) {
                        // Verifica se é um endereço IPv4 e se é um IP privado (da rede local)
                        if (addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Erro ao buscar interfaces de rede: " + e.getMessage());
        }
        return "IP não encontrado";
    }
}