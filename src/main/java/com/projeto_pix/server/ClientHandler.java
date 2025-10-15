// Em: src/main/java/com/projeto_pix/server/ClientHandler.java
package com.projeto_pix.server;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto_pix.common.Validator;
import com.projeto_pix.common.model.Usuario;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private static final ObjectMapper mapper = new ObjectMapper(); // Reutilizamos o ObjectMapper

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("Recebido do cliente: " + jsonRequest);
                String jsonResponse = "";
                try {
                    Validator.validateClient(jsonRequest);
                    jsonResponse = processOperation(jsonRequest);
                } catch (Exception e) {
                    // Se a validação falhar, cria um JSON de erro
                    ObjectNode errorNode = mapper.createObjectNode();
                    errorNode.put("operacao", "erro_validacao");
                    errorNode.put("status", false);
                    errorNode.put("info", e.getMessage());
                    jsonResponse = errorNode.toString();
                }
                System.out.println("Enviando para o cliente: " + jsonResponse);
                out.println(jsonResponse);
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clientSocket.getInetAddress());
        }
    }

    private String processOperation(String jsonRequest) throws IOException {
        JsonNode rootNode = mapper.readTree(jsonRequest);
        String operacao = rootNode.get("operacao").asText();

        switch (operacao) {
            case "usuario_criar":
                return handleUsuarioCriar(rootNode);
            case "usuario_login":
                return handleUsuarioLogin(rootNode);
            case "usuario_ler":
                return handleUsuarioLer(rootNode);
            case "usuario_atualizar":
                return handleUsuarioAtualizar(rootNode);
            case "usuario_deletar":
                return handleUsuarioDeletar(rootNode);
            case "usuario_logout":
                return handleUsuarioLogout(rootNode);
            default:
                return createResponse("operacao_desconhecida", false, "A operação solicitada não é suportada.");
        }
    }

    // --- MÉTODOS DE MANIPULAÇÃO DAS OPERAÇÕES ---

    private String handleUsuarioCriar(JsonNode node) {
        String nome = node.get("nome").asText();
        String cpf = node.get("cpf").asText();
        String senha = node.get("senha").asText();

        if (Server.usuarios.containsKey(cpf)) {
            return createResponse("usuario_criar", false, "Erro: CPF já cadastrado.");
        }

        Usuario novoUsuario = new Usuario(nome, cpf, senha);
        Server.usuarios.put(cpf, novoUsuario);
        return createResponse("usuario_criar", true, "Usuário criado com sucesso.");
    }

    private String handleUsuarioLogin(JsonNode node) {
        String cpf = node.get("cpf").asText();
        String senha = node.get("senha").asText();
        Usuario usuario = Server.usuarios.get(cpf);

        if (usuario != null && usuario.verificarSenha(senha)) {
            String token = UUID.randomUUID().toString();
            Server.sessoesAtivas.put(token, cpf);
            ObjectNode response = mapper.createObjectNode();
            response.put("operacao", "usuario_login");
            response.put("status", true);
            response.put("info", "Login bem-sucedido.");
            response.put("token", token);
            return response.toString();
        }

        return createResponse("usuario_login", false, "CPF ou senha inválidos.");
    }

    private String handleUsuarioLer(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);

        if (cpf == null) {
            return createResponse("usuario_ler", false, "Token inválido ou sessão expirada.");
        }

        Usuario usuario = Server.usuarios.get(cpf);
        ObjectNode response = mapper.createObjectNode();
        response.put("operacao", "usuario_ler");
        response.put("status", true);
        response.put("info", "Dados do usuário recuperados com sucesso.");

        ObjectNode usuarioNode = mapper.createObjectNode();
        usuarioNode.put("nome", usuario.getNome());
        usuarioNode.put("cpf", usuario.getCpf());
        usuarioNode.put("saldo", usuario.getSaldo());
        response.set("usuario", usuarioNode);

        return response.toString();
    }

    private String handleUsuarioAtualizar(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);

        if (cpf == null) {
            return createResponse("usuario_atualizar", false, "Token inválido ou sessão expirada.");
        }

        Usuario usuario = Server.usuarios.get(cpf);
        JsonNode dadosParaAtualizar = node.get("usuario");

        if(dadosParaAtualizar.has("nome")) {
            usuario.setNome(dadosParaAtualizar.get("nome").asText());
        }
        if(dadosParaAtualizar.has("senha")) {
            usuario.setSenha(dadosParaAtualizar.get("senha").asText());
        }

        return createResponse("usuario_atualizar", true, "Usuário atualizado com sucesso.");
    }

    private String handleUsuarioDeletar(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);

        if (cpf == null) {
            return createResponse("usuario_deletar", false, "Token inválido ou sessão expirada.");
        }

        Server.usuarios.remove(cpf);
        Server.sessoesAtivas.remove(token); // Também remove a sessão

        return createResponse("usuario_deletar", true, "Usuário deletado com sucesso.");
    }

    private String handleUsuarioLogout(JsonNode node) {
        String token = node.get("token").asText();

        if (Server.sessoesAtivas.containsKey(token)) {
            Server.sessoesAtivas.remove(token);
            return createResponse("usuario_logout", true, "Logout realizado com sucesso.");
        }

        return createResponse("usuario_logout", false, "Token inválido.");
    }

    // --- MÉTODO AUXILIAR ---

    private String createResponse(String operacao, boolean status, String info) {
        ObjectNode response = mapper.createObjectNode();
        response.put("operacao", operacao);
        response.put("status", status);
        response.put("info", info);
        return response.toString();
    }
}