// Em: src/main/java/com/projeto_pix/client/Client.java
package com.projeto_pix.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Client {
    private static String userToken = null; // Armazena o token após o login
    private static final ObjectMapper mapper = new ObjectMapper();
    private static PrintWriter out;
    private static BufferedReader in;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.print("Digite o endereço IP do servidor: ");
        String serverAddress = scanner.nextLine();
        System.out.print("Digite a porta do servidor: ");
        int serverPort = Integer.parseInt(scanner.nextLine());

        try (Socket socket = new Socket(serverAddress, serverPort)) {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Conectando ao servidor para validar o protocolo...");

            // 1. Enviar a operação 'conectar' obrigatória
            ObjectNode connectRequest = mapper.createObjectNode();
            connectRequest.put("operacao", "conectar");
            out.println(connectRequest.toString());

            // 2. Esperar e validar a resposta
            String serverResponse = in.readLine();
            if (serverResponse == null) {
                throw new IOException("Servidor não respondeu à tentativa de conexão.");
            }

            // Usamos o seu validador para garantir que a resposta está correta
            com.projeto_pix.common.Validator.validateServer(serverResponse);
            JsonNode responseNode = mapper.readTree(serverResponse);

            // 3. Verificar se o status da conexão é 'true'
            if (responseNode.get("status").asBoolean()) {
                System.out.println("Conexão estabelecida com sucesso!");
                mainMenu(); // Só chama o menu principal se a conexão for bem-sucedida
            } else {
                System.err.println("Falha ao conectar: " + responseNode.get("info").asText());
            }

        } catch (Exception e) {
            System.err.println("Erro ao conectar ou comunicar com o servidor: " + e.getMessage());
        } finally {
            scanner.close();
            System.out.println("Conexão encerrada.");
        }
    }

    private static void mainMenu() throws IOException {
        while (true) {
            System.out.println("\n--- MENU PRINCIPAL ---");
            if (userToken == null) {
                System.out.println("1. Cadastrar Usuário");
                System.out.println("2. Fazer Login");
                System.out.println("0. Sair");
            } else {
                System.out.println("3. Ver Meus Dados");
                System.out.println("4. Atualizar Cadastro");
                System.out.println("5. Apagar Minha Conta");
                System.out.println("6. Fazer Logout");
                System.out.println("0. Sair");
            }

            System.out.print("Escolha uma opção: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    if (userToken == null) cadastrarUsuario();
                    else System.out.println("Opção inválida.");
                    break;
                case "2":
                    if (userToken == null) fazerLogin();
                    else System.out.println("Opção inválida.");
                    break;
                case "3":
                    if (userToken != null) lerUsuario();
                    else System.out.println("Opção inválida.");
                    break;
                case "4":
                    if (userToken != null) atualizarUsuario();
                    else System.out.println("Opção inválida.");
                    break;
                case "5":
                    if (userToken != null) apagarUsuario();
                    else System.out.println("Opção inválida.");
                    break;
                case "6":
                    if (userToken != null) fazerLogout();
                    else System.out.println("Opção inválida.");
                    break;
                case "0":
                    return;
                default:
                    System.out.println("Opção inválida. Tente novamente.");
            }
        }
    }

    // --- MÉTODOS PARA CADA OPERAÇÃO ---

    private static void cadastrarUsuario() throws IOException {
        System.out.print("Digite seu nome completo: ");
        String nome = scanner.nextLine();
        System.out.print("Digite seu CPF (formato 000.000.000-00): ");
        String cpf = scanner.nextLine();
        System.out.print("Digite sua senha: ");
        String senha = scanner.nextLine();

        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_criar");
        request.put("nome", nome);
        request.put("cpf", cpf);
        request.put("senha", senha);

        sendAndPrintResponse(request.toString());
    }

    private static void fazerLogin() throws IOException {
        System.out.print("Digite seu CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Digite sua senha: ");
        String senha = scanner.nextLine();

        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_login");
        request.put("cpf", cpf);
        request.put("senha", senha);

        String responseStr = sendAndGetResponse(request.toString());
        System.out.println("Resposta do Servidor: " + responseStr);

        JsonNode responseNode = mapper.readTree(responseStr);
        if (responseNode.get("status").asBoolean()) {
            userToken = responseNode.get("token").asText(); // Armazena o token
            System.out.println("Login realizado com sucesso!");
        }
    }

    private static void lerUsuario() throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_ler");
        request.put("token", userToken);
        sendAndPrintResponse(request.toString());
    }

    private static void atualizarUsuario() throws IOException {
        System.out.print("Digite o novo nome (ou deixe em branco para não alterar): ");
        String nome = scanner.nextLine();
        System.out.print("Digite a nova senha (ou deixe em branco para não alterar): ");
        String senha = scanner.nextLine();

        if (nome.isEmpty() && senha.isEmpty()) {
            System.out.println("Nenhum dado para alterar.");
            return;
        }

        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_atualizar");
        request.put("token", userToken);

        ObjectNode usuarioNode = mapper.createObjectNode();
        if (!nome.isEmpty()) {
            usuarioNode.put("nome", nome);
        }
        if (!senha.isEmpty()) {
            usuarioNode.put("senha", senha);
        }
        request.set("usuario", usuarioNode);

        sendAndPrintResponse(request.toString());
    }

    private static void apagarUsuario() throws IOException {
        System.out.print("Você tem certeza que deseja apagar sua conta? (s/n): ");
        String confirmacao = scanner.nextLine();
        if(!confirmacao.equalsIgnoreCase("s")) {
            System.out.println("Operação cancelada.");
            return;
        }

        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_deletar");
        request.put("token", userToken);

        String responseStr = sendAndGetResponse(request.toString());
        System.out.println("Resposta do Servidor: " + responseStr);

        JsonNode responseNode = mapper.readTree(responseStr);
        if (responseNode.get("status").asBoolean()) {
            userToken = null; // Limpa o token localmente
        }
    }

    private static void fazerLogout() throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("operacao", "usuario_logout");
        request.put("token", userToken);

        sendAndPrintResponse(request.toString());
        userToken = null; // Limpa o token localmente após o pedido de logout
    }

    // --- MÉTODOS AUXILIARES DE COMUNICAÇÃO ---

    private static void sendAndPrintResponse(String json) throws IOException {
        System.out.println("Enviando para o Servidor: " + json);
        out.println(json);
        System.out.println("Resposta do Servidor: " + in.readLine());
    }

    private static String sendAndGetResponse(String json) throws IOException {
        System.out.println("Enviando para o Servidor: " + json);
        out.println(json);
        return in.readLine();
    }
}