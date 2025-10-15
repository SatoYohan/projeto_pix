package com.projeto_pix.server;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        final int PORT = 12345; // Escolha a porta que desejar
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado na porta " + PORT);

            while (true) {
                // O método accept() bloqueia a execução até que um cliente se conecte
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                // Para cada cliente, crie uma nova Thread para lidar com ele.
                // Isso permite que o servidor atenda vários clientes ao mesmo tempo.
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}