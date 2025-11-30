// Em: src/main/java/com/projeto_pix/server/ClientHandler.java
package com.projeto_pix.server;

import java.io.*;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto_pix.common.Validator;
import com.projeto_pix.common.model.Usuario;
import com.projeto_pix.common.model.Transacao;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Server serverInstance; // Referência para o servidor principal (e sua GUI)
    private static final ObjectMapper mapper = new ObjectMapper();
    private String currentUserInfo = null; // Para saber quem remover da lista da GUI

    // Construtor atualizado para receber o Server
    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.serverInstance = server;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                // Log na GUI em vez de System.out
                serverInstance.getGui().log("Req: " + jsonRequest);

                String jsonResponse = "";
                String operacao = "desconhecida";
                JsonNode rootNode = null;

                try {
                    try {
                        rootNode = mapper.readTree(jsonRequest);
                        if (rootNode.has("operacao")) {
                            operacao = rootNode.get("operacao").asText();
                        }
                    } catch (IOException e) {
                        // JSON inválido
                    }

                    Validator.validateClient(jsonRequest);

                    if (rootNode == null) rootNode = mapper.readTree(jsonRequest);

                    jsonResponse = processOperation(rootNode, operacao);

                } catch (Exception e) {
                    ObjectNode errorNode = mapper.createObjectNode();
                    errorNode.put("operacao", operacao);
                    errorNode.put("status", false);
                    errorNode.put("info", e.getMessage());
                    jsonResponse = errorNode.toString();
                }

                // Log da resposta na GUI
                serverInstance.getGui().log("Resp: " + jsonResponse);
                if (jsonResponse != null) {
                    out.println(jsonResponse);
                }
            }
        } catch (IOException e) {
            serverInstance.getGui().log("Cliente desconectado: " + clientSocket.getInetAddress());
        } finally {
            // Remove o usuário da lista visual se ele cair
            if (currentUserInfo != null) {
                serverInstance.getGui().removeUser(currentUserInfo);
            }
        }
    }

    private String processOperation(JsonNode rootNode, String operacao) throws IOException {
        switch (operacao) {
            case "conectar":
                return createResponse("conectar", true, "Servidor conectado com sucesso.");
            case "usuario_login":
                return handleUsuarioLogin(rootNode);
            case "usuario_logout":
                return handleUsuarioLogout(rootNode);

            // As outras operações continuam iguais
            case "depositar": return handleDepositar(rootNode);
            case "transacao_ler": return handleTransacaoLer(rootNode);
            case "transacao_criar": return handleTransacaoCriar(rootNode);
            case "erro_servidor": handleErroServidor(rootNode); return null;
            case "usuario_criar": return handleUsuarioCriar(rootNode);
            case "usuario_ler": return handleUsuarioLer(rootNode);
            case "usuario_atualizar": return handleUsuarioAtualizar(rootNode);
            case "usuario_deletar": return handleUsuarioDeletar(rootNode);

            default:
                return createResponse("operacao_desconhecida", false, "A operação solicitada não é suportada.");
        }
    }

    // --- MÉTODOS QUE INTERAGEM COM A GUI (Login/Logout) ---

    private String handleUsuarioLogin(JsonNode node) {
        String cpf = node.get("cpf").asText();
        String senha = node.get("senha").asText();
        Usuario usuario = Server.usuarios.get(cpf);

        if (usuario != null && usuario.verificarSenha(senha)) {
            String token = UUID.randomUUID().toString();
            Server.sessoesAtivas.put(token, cpf);

            // ATUALIZA A GUI (Item h)
            this.currentUserInfo = usuario.getNome() + " (" + cpf + ")";
            serverInstance.getGui().addUser(currentUserInfo);

            ObjectNode response = mapper.createObjectNode();
            response.put("operacao", "usuario_login");
            response.put("status", true);
            response.put("info", "Login bem-sucedido.");
            response.put("token", token);
            return response.toString();
        }
        return createResponse("usuario_login", false, "CPF ou senha inválidos.");
    }

    private String handleUsuarioLogout(JsonNode node) {
        String token = node.get("token").asText();
        if (Server.sessoesAtivas.containsKey(token)) {
            // REMOVE DA GUI
            if(currentUserInfo != null) {
                serverInstance.getGui().removeUser(currentUserInfo);
                currentUserInfo = null;
            }

            Server.sessoesAtivas.remove(token);
            return createResponse("usuario_logout", true, "Logout realizado.");
        }
        return createResponse("usuario_logout", false, "Token inválido.");
    }

    // --- MÉTODOS MANTIDOS IGUAIS (Só copiei sua lógica) ---

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

    private String handleUsuarioLer(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);
        if (cpf == null) return createResponse("usuario_ler", false, "Token inválido.");
        Usuario usuario = Server.usuarios.get(cpf);
        ObjectNode response = mapper.createObjectNode();
        response.put("operacao", "usuario_ler");
        response.put("status", true);
        response.put("info", "Dados do usuário.");
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
        if (cpf == null) return createResponse("usuario_atualizar", false, "Token inválido.");
        Usuario usuario = Server.usuarios.get(cpf);
        JsonNode dados = node.get("usuario");
        if(dados.has("nome")) usuario.setNome(dados.get("nome").asText());
        if(dados.has("senha")) usuario.setSenha(dados.get("senha").asText());
        return createResponse("usuario_atualizar", true, "Usuário atualizado.");
    }

    private String handleUsuarioDeletar(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);
        if (cpf == null) return createResponse("usuario_deletar", false, "Token inválido.");

        // Remove da GUI antes de apagar
        if(currentUserInfo != null) {
            serverInstance.getGui().removeUser(currentUserInfo);
            currentUserInfo = null;
        }

        Server.usuarios.remove(cpf);
        Server.sessoesAtivas.remove(token);
        return createResponse("usuario_deletar", true, "Usuário deletado.");
    }

    private String handleDepositar(JsonNode node) {
        String token = node.get("token").asText();
        double valorEnviado = node.get("valor_enviado").asDouble();
        String cpf = Server.sessoesAtivas.get(token);
        if (cpf == null) return createResponse("depositar", false, "Token inválido.");

        double valorArredondado = Math.round(valorEnviado * 100.0) / 100.0;
        if (valorArredondado <= 0) return createResponse("depositar", false, "Valor deve ser positivo.");

        Usuario usuario = Server.usuarios.get(cpf);
        usuario.setSaldo(usuario.getSaldo() + valorArredondado);
        int id = Server.transacaoIdCounter.getAndIncrement();
        Transacao t = new Transacao(id, valorArredondado, usuario, usuario);
        Server.transacoes.add(t);
        return createResponse("depositar", true, "Deposito realizado.");
    }

    private String handleTransacaoCriar(JsonNode node) {
        String token = node.get("token").asText();
        String cpfDestino = node.get("cpf_destino").asText();
        double valor = node.get("valor").asDouble();
        String cpfEnviador = Server.sessoesAtivas.get(token);

        if (cpfEnviador == null) return createResponse("transacao_criar", false, "Token inválido.");

        double valorArredondado = Math.round(valor * 100.0) / 100.0;
        if (valorArredondado <= 0) return createResponse("transacao_criar", false, "Valor positivo.");
        if (cpfEnviador.equals(cpfDestino)) return createResponse("transacao_criar", false, "Não pode enviar para si mesmo.");

        Usuario env = Server.usuarios.get(cpfEnviador);
        Usuario rec = Server.usuarios.get(cpfDestino);
        if (rec == null) return createResponse("transacao_criar", false, "Destino não encontrado.");

        synchronized (env) {
            if (env.getSaldo() < valorArredondado) return createResponse("transacao_criar", false, "Saldo insuficiente.");
            env.setSaldo(env.getSaldo() - valorArredondado);
            rec.setSaldo(rec.getSaldo() + valorArredondado);
        }
        int id = Server.transacaoIdCounter.getAndIncrement();
        Transacao t = new Transacao(id, valorArredondado, env, rec);
        Server.transacoes.add(t);
        return createResponse("transacao_criar", true, "Transação realizada.");
    }

    private String handleTransacaoLer(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);
        if (cpf == null) return createResponse("transacao_ler", false, "Token inválido.");

        try {
            Instant ini = Instant.parse(node.get("data_inicial").asText());
            Instant fim = Instant.parse(node.get("data_final").asText());
            if (ChronoUnit.DAYS.between(ini, fim) > 31) return createResponse("transacao_ler", false, "Máximo 31 dias.");

            ArrayNode arr = mapper.createArrayNode();
            synchronized (Server.transacoes) {
                for (Transacao tx : Server.transacoes) {
                    if (tx.envolveUsuario(cpf) && tx.estaNoIntervalo(ini, fim)) {
                        arr.add(criarJsonDaTransacao(tx));
                    }
                }
            }
            ObjectNode resp = mapper.createObjectNode();
            resp.put("operacao", "transacao_ler");
            resp.put("status", true);
            resp.put("info", "Sucesso.");
            resp.set("transacoes", arr);
            return resp.toString();
        } catch (Exception e) {
            return createResponse("transacao_ler", false, "Erro processar datas.");
        }
    }

    private void handleErroServidor(JsonNode node) {
        String op = node.get("operacao_enviada").asText();
        String info = node.get("info").asText();
        serverInstance.getGui().log("ERRO CLIENTE (" + op + "): " + info);
    }

    private String createResponse(String operacao, boolean status, String info) {
        ObjectNode response = mapper.createObjectNode();
        response.put("operacao", operacao);
        response.put("status", status);
        response.put("info", info);
        return response.toString();
    }

    private ObjectNode criarJsonDaTransacao(Transacao tx) {
        ObjectNode txNode = mapper.createObjectNode();
        txNode.put("id", tx.getId());
        txNode.put("valor_enviado", tx.getValorEnviado());
        txNode.put("criado_em", tx.getCriadoEm());
        txNode.put("atualizado_em", tx.getCriadoEm());
        ObjectNode env = mapper.createObjectNode();
        env.put("nome", tx.getUsuarioEnviador().getNome());
        env.put("cpf", tx.getUsuarioEnviador().getCpf());
        txNode.set("usuario_enviador", env);
        ObjectNode rec = mapper.createObjectNode();
        rec.put("nome", tx.getUsuarioRecebedor().getNome());
        rec.put("cpf", tx.getUsuarioRecebedor().getCpf());
        txNode.set("usuario_recebedor", rec);
        return txNode;
    }
}