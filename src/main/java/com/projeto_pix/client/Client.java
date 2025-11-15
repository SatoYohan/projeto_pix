package com.projeto_pix.client;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto_pix.common.Validator;
import java.time.Instant;

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

            ObjectNode connectRequest = mapper.createObjectNode();
            connectRequest.put("operacao", "conectar");

            // 1. Tenta conectar e usa o novo método auxiliar que valida a resposta
            String serverResponse = sendAndGetResponse(connectRequest, "conectar");
            if (serverResponse == null) {
                throw new IOException("Falha na validação da resposta do servidor.");
            }

            JsonNode responseNode = mapper.readTree(serverResponse);

            // 2. Verifica se a conexão foi bem-sucedida
            if (responseNode.get("status").asBoolean()) {
                System.out.println("Conexão estabelecida com sucesso!");
                mainMenu(); // Só chama o menu se a conexão for OK
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
                System.out.println("7. Depositar");
                System.out.println("8. Ver Extrato");
                System.out.println("9. Fazer PIX (Transferência)");
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
                // --- ATUALIZAÇÃO: NOVOS CASES EP-2 ---
                case "7":
                    if (userToken != null) depositar();
                    else System.out.println("Opção inválida.");
                    break;
                case "8":
                    if (userToken != null) lerExtrato();
                    else System.out.println("Opção inválida.");
                    break;
                case "9":
                    if (userToken != null) fazerPix();
                    else System.out.println("Opção inválida.");
                    break;
                // --- FIM DA ATUALIZAÇÃO ---
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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        sendAndPrintResponse(request, "usuario_criar");
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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        String responseStr = sendAndGetResponse(request, "usuario_login");
        if (responseStr == null) return; // Validação falhou, não continua

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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        sendAndPrintResponse(request, "usuario_ler");
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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        sendAndPrintResponse(request, "usuario_atualizar");
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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        String responseStr = sendAndGetResponse(request, "usuario_deletar");
        if (responseStr == null) return; // Validação falhou

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

        // ATUALIZAÇÃO: Chamada ao novo método auxiliar
        sendAndPrintResponse(request, "usuario_logout");
        userToken = null; // Limpa o token localmente após o pedido de logout
    }

    // --- NOVOS MÉTODOS ADICIONADOS PARA EP-2 ---

    /**
     * (Item a) Cliente pede para depositar dinheiro.
     */
    private static void depositar() throws IOException {
        System.out.print("Digite o valor a ser depositado (ex: 123.45): ");
        try {
            double valor = Double.parseDouble(scanner.nextLine());

            ObjectNode request = mapper.createObjectNode();
            request.put("operacao", "depositar");
            request.put("token", userToken);
            request.put("valor_enviado", valor);

            sendAndPrintResponse(request, "depositar");

        } catch (NumberFormatException e) {
            System.err.println("Valor inválido. Use ponto como separador decimal.");
        }
    }

    /**
     * (Itens b, c) Cliente pede e mostra o extrato.
     */
    private static void lerExtrato() throws IOException {
        System.out.print("Digite a data inicial (ex: 01/10/2025 08:00:00): ");
        String dataInicialStr = scanner.nextLine();
        System.out.print("Digite a data final (ex: 31/10/2025 23:59:59): ");
        String dataFinalStr = scanner.nextLine();

        try {
            // Formata a data para o padrão ISO 8601 UTC exigido pelo protocolo
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String dataInicialISO = LocalDateTime.parse(dataInicialStr, formatter).toInstant(ZoneOffset.UTC).toString();
            String dataFinalISO = LocalDateTime.parse(dataFinalStr, formatter).toInstant(ZoneOffset.UTC).toString();

            ObjectNode request = mapper.createObjectNode();
            request.put("operacao", "transacao_ler");
            request.put("token", userToken);
            request.put("data_inicial", dataInicialISO);
            request.put("data_final", dataFinalISO);

            String responseStr = sendAndGetResponse(request, "transacao_ler");
            if (responseStr == null) return; // Erro já foi reportado

            JsonNode responseNode = mapper.readTree(responseStr);
            System.out.println("Resposta do Servidor: " + responseNode.toString()); // Mostra o JSON cru

            // (Item c) Mostra o extrato formatado
            if (responseNode.get("status").asBoolean()) {
                System.out.println("\n--- EXTRATO DA CONTA ---");
                JsonNode transacoes = responseNode.get("transacoes");
                if (transacoes.isEmpty()) {
                    System.out.println("Nenhuma transação encontrada no período.");
                } else {
                    for (JsonNode tx : transacoes) {
                        // Esta linha agora compila graças ao 'import java.time.Instant;'
                        String data = Instant.parse(tx.get("criado_em").asText()).toString();
                        String enviador = tx.get("usuario_enviador").get("nome").asText();
                        String recebedor = tx.get("usuario_recebedor").get("nome").asText();
                        double valor = tx.get("valor_enviado").asDouble();

                        System.out.printf("[%s] R$ %.2f de '%s' para '%s'\n", data, valor, enviador, recebedor);
                    }
                }
                System.out.println("--------------------------");
            }

        } catch (Exception e) {
            System.err.println("Formato de data inválido. Use dd/MM/yyyy HH:mm:ss");
            // e.printStackTrace(); // Descomente para ver o erro completo
        }
    }

    /**
     * (Protocolo 4.7) Envia um PIX (transacao_criar).
     */
    private static void fazerPix() throws IOException {
        System.out.print("Digite o CPF de destino (formato 000.000.000-00): ");
        String cpfDestino = scanner.nextLine();

        System.out.print("Digite o valor a ser transferido (ex: 50.25): ");
        try {
            double valor = Double.parseDouble(scanner.nextLine());

            ObjectNode request = mapper.createObjectNode();
            request.put("operacao", "transacao_criar");
            request.put("token", userToken);
            request.put("valor", valor);
            request.put("cpf_destino", cpfDestino);

            sendAndPrintResponse(request, "transacao_criar");

        } catch (NumberFormatException e) {
            System.err.println("Valor inválido. Use ponto como separador decimal.");
        }
    }

    // --- MÉTODOS AUXILIARES DE COMUNICAÇÃO (ATUALIZADOS) ---
    // Substituímos os métodos antigos por estes, que validam a resposta
    // e reportam erros conforme os protocolos 4.11 e 5.2

    /**
     * Envia uma requisição, valida a resposta e apenas a imprime.
     * @param request O ObjectNode da requisição.
     * @param operacao O nome da operação (para reportar erros).
     */
    private static void sendAndPrintResponse(ObjectNode request, String operacao) throws IOException {
        String response = sendAndGetResponse(request, operacao);
        if (response != null) {
            System.out.println("Resposta do Servidor: " + response);
        }
    }

    /**
     * Envia uma requisição, valida a resposta e a retorna como String.
     * @param request O ObjectNode da requisição.
     * @param operacao O nome da operação (para reportar erros).
     * @return A String da resposta se for válida, ou null se a validação falhar.
     */
    private static String sendAndGetResponse(ObjectNode request, String operacao) throws IOException {
        String jsonRequest = request.toString();
        System.out.println("Enviando para o Servidor: " + jsonRequest);
        out.println(jsonRequest);

        String serverResponse = in.readLine();
        if (serverResponse == null) {
            System.err.println("Servidor não enviou resposta.");
            return null;
        }

        try {
            // (Protocolo 5.2) Cliente valida a resposta do servidor
            Validator.validateServer(serverResponse);
            return serverResponse; // Retorna a resposta válida
        } catch (Exception e) {
            // (Protocolo 4.11) Se a validação falhar, reporta o erro!
            System.err.println("!!! ERRO NA RESPOSTA DO SERVIDOR: " + e.getMessage() + " !!!");
            System.err.println("Resposta recebida: " + serverResponse);
            reportErrorToServer(operacao, e.getMessage());
            return null; // Retorna nulo para sinalizar que a resposta foi inválida
        }
    }

    /**
     * (Protocolo 4.11) Envia um relatório de erro para o servidor.
     */
    private static void reportErrorToServer(String operacaoEnviada, String infoErro) {
        ObjectNode errorReport = mapper.createObjectNode();
        errorReport.put("operacao", "erro_servidor");
        errorReport.put("operacao_enviada", operacaoEnviada);
        errorReport.put("info", "Erro do cliente: " + infoErro);

        System.out.println("Enviando relatório de erro para o Servidor: " + errorReport.toString());
        out.println(errorReport.toString());
        // Não esperamos uma resposta para um relatório de erro
    }
}