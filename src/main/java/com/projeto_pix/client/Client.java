package com.projeto_pix.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.projeto_pix.common.Validator; // Importe o seu validador

public class Client {
    private static String userToken = null; // Armazena o token após o login

    public static void main(String[] args) {
        final String SERVER_ADDRESS = "localhost";
        final int SERVER_PORT = 12345;

        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println("Conectado ao servidor.");

            // Loop principal do menu
            while (true) {
                // Mostre o menu de opções para o usuário
                System.out.println("\nEscolha uma opção:");
                System.out.println("1. Cadastrar Usuário");
                System.out.println("2. Fazer Login");
                // ... adicione outras opções (Ler dados, Atualizar, etc.)

                String choice = scanner.nextLine();

                if ("1".equals(choice)) {
                    // Lógica para pedir nome, cpf, senha e montar o JSON
                    String jsonRequest = "{\"operacao\": \"usuario_criar\", \"nome\": \"...\", \"cpf\": \"...\", \"senha\": \"...\"}";
                    out.println(jsonRequest); // Envia para o servidor

                    String serverResponse = in.readLine(); // Recebe a resposta
                    System.out.println("Resposta do Servidor: " + serverResponse);
                }
                // ... implemente os outros casos (login, etc.)
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}