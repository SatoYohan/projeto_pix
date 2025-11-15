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
    private static final ObjectMapper mapper = new ObjectMapper();

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
                String operacao = "desconhecida";
                JsonNode rootNode = null;

                try {
                    try {
                        rootNode = mapper.readTree(jsonRequest);
                        if (rootNode.has("operacao")) {
                            operacao = rootNode.get("operacao").asText();
                        }
                    } catch (IOException e) {
                        // Se o JSON for inválido, o validador vai pegar
                    }

                    // 2. Agora, chamamos o validador.
                    // Se ele falhar (ex: data errada), a variável 'operacao' já terá o valor correto (ex: "transacao_ler")
                    Validator.validateClient(jsonRequest);

                    // 3. (Já que rootNode foi lido acima, reutilizamos)
                    if (rootNode == null) rootNode = mapper.readTree(jsonRequest); // Segurança caso a primeira leitura falhe

                    jsonResponse = processOperation(rootNode, operacao);

                } catch (Exception e) {
                    // 4. O 'catch' agora usa o valor CORRETO de 'operacao'.
                    ObjectNode errorNode = mapper.createObjectNode();
                    errorNode.put("operacao", operacao);
                    errorNode.put("status", false);
                    errorNode.put("info", e.getMessage());
                    jsonResponse = errorNode.toString();
                }

                System.out.println("Enviando para o cliente: " + jsonResponse);
                if (jsonResponse != null) {
                    out.println(jsonResponse);
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clientSocket.getInetAddress());
        }
    }

    // --- CORREÇÃO DE ASSINATURA ---
    // O método agora aceita 'JsonNode' e 'String' para bater com a chamada
    private String processOperation(JsonNode rootNode, String operacao) throws IOException {

        switch (operacao) {
            case "conectar":
                return createResponse("conectar", true, "Servidor conectado com sucesso.");
            case "depositar":
                return handleDepositar(rootNode);
            case "transacao_ler":
                return handleTransacaoLer(rootNode);
            case "transacao_criar":
                return handleTransacaoCriar(rootNode);
            case "erro_servidor":
                handleErroServidor(rootNode);
                return null;

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

    // --- MÉTODOS DE MANIPULAÇÃO DAS OPERAÇÕES (EP-1) ---

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

    /**
     * (Itens f, g) Lida com o depósito e atualiza o saldo.
     */
    private String handleDepositar(JsonNode node) {
        String token = node.get("token").asText();
        double valorEnviado = node.get("valor_enviado").asDouble();
        String cpf = Server.sessoesAtivas.get(token);

        if (cpf == null) {
            return createResponse("depositar", false, "Token inválido ou sessão expirada.");
        }

        // Arredonda o valor para 2 casas decimais
        double valorArredondado = Math.round(valorEnviado * 100.0) / 100.0;

        if (valorArredondado <= 0) {
            return createResponse("depositar", false, "Valor do depósito deve ser positivo.");
        }

        Usuario usuario = Server.usuarios.get(cpf);
        usuario.setSaldo(usuario.getSaldo() + valorArredondado);

        int transacaoId = Server.transacaoIdCounter.getAndIncrement();
        Transacao deposito = new Transacao(transacaoId, valorArredondado, usuario, usuario);
        Server.transacoes.add(deposito);

        return createResponse("depositar", true, "Deposito realizado com sucesso.");
    }

    /**
     * (Protocolo 4.7) Lida com a criação de uma transferência (PIX).
     */
    private String handleTransacaoCriar(JsonNode node) {
        String token = node.get("token").asText();
        String cpfDestino = node.get("cpf_destino").asText();
        double valor = node.get("valor").asDouble();
        String cpfEnviador = Server.sessoesAtivas.get(token);

        if (cpfEnviador == null) {
            return createResponse("transacao_criar", false, "Token inválido ou sessão expirada.");
        }

        double valorArredondado = Math.round(valor * 100.0) / 100.0;

        if (valorArredondado <= 0) {
            return createResponse("transacao_criar", false, "Valor da transação deve ser positivo.");
        }
        if (cpfEnviador.equals(cpfDestino)) {
            return createResponse("transacao_criar", false, "Você não pode enviar um PIX para si mesmo.");
        }

        Usuario usuarioEnviador = Server.usuarios.get(cpfEnviador);
        Usuario usuarioRecebedor = Server.usuarios.get(cpfDestino);

        if (usuarioRecebedor == null) {
            return createResponse("transacao_criar", false, "CPF de destino não encontrado.");
        }

        synchronized (usuarioEnviador) {
            if (usuarioEnviador.getSaldo() < valorArredondado) {
                return createResponse("transacao_criar", false, "Saldo insuficiente.");
            }
            usuarioEnviador.setSaldo(usuarioEnviador.getSaldo() - valorArredondado);
            usuarioRecebedor.setSaldo(usuarioRecebedor.getSaldo() + valorArredondado);
        }

        int transacaoId = Server.transacaoIdCounter.getAndIncrement();
        Transacao transacao = new Transacao(transacaoId, valorArredondado, usuarioEnviador, usuarioRecebedor);
        Server.transacoes.add(transacao);

        return createResponse("transacao_criar", true, "Transação realizada com sucesso.");
    }
    /**
     * (Item h) Lida com o pedido de extrato (transacao_ler).
     */
    private String handleTransacaoLer(JsonNode node) {
        String token = node.get("token").asText();
        String cpf = Server.sessoesAtivas.get(token);

        if (cpf == null) {
            return createResponse("transacao_ler", false, "Token inválido ou sessão expirada.");
        }

        try {
            Instant dataInicial = Instant.parse(node.get("data_inicial").asText());
            Instant dataFinal = Instant.parse(node.get("data_final").asText());

            if (ChronoUnit.DAYS.between(dataInicial, dataFinal) > 31) {
                return createResponse("transacao_ler", false, "Erro: O período máximo para extrato é de 31 dias.");
            }

            ArrayNode transacoesArray = mapper.createArrayNode();
            synchronized (Server.transacoes) {
                for (Transacao tx : Server.transacoes) {
                    if (tx.envolveUsuario(cpf) && tx.estaNoIntervalo(dataInicial, dataFinal)) {
                        transacoesArray.add(criarJsonDaTransacao(tx));
                    }
                }
            }

            ObjectNode response = mapper.createObjectNode();
            response.put("operacao", "transacao_ler");
            response.put("status", true);
            response.put("info", "Transações recuperadas com sucesso.");
            response.set("transacoes", transacoesArray);
            return response.toString();

        } catch (Exception e) {
            return createResponse("transacao_ler", false, "Erro ao processar datas ou transações.");
        }
    }
    /**
     * (Protocolo 4.11) Apenas loga o erro reportado pelo cliente.
     */
    private void handleErroServidor(JsonNode node) {
        String operacaoOriginal = node.get("operacao_enviada").asText();
        String infoErro = node.get("info").asText();

        System.err.println("--- ERRO REPORTADO PELO CLIENTE ---");
        System.err.println("Operação Original: " + operacaoOriginal);
        System.err.println("Info: " + infoErro);
        System.err.println("-------------------------------------");
    }
    // --- MÉTODO AUXILIAR ---

    private String createResponse(String operacao, boolean status, String info) {
        ObjectNode response = mapper.createObjectNode();
        response.put("operacao", operacao);
        response.put("status", status);
        response.put("info", info);
        return response.toString();
    }

    // Novo auxiliar para montar o JSON de uma transação (conforme protocolo 4.8)
    private ObjectNode criarJsonDaTransacao(Transacao tx) {
        ObjectNode txNode = mapper.createObjectNode();
        txNode.put("id", tx.getId());
        txNode.put("valor_enviado", tx.getValorEnviado());
        txNode.put("criado_em", tx.getCriadoEm());
        txNode.put("atualizado_em", tx.getCriadoEm());

        ObjectNode enviadorNode = mapper.createObjectNode();
        enviadorNode.put("nome", tx.getUsuarioEnviador().getNome());
        enviadorNode.put("cpf", tx.getUsuarioEnviador().getCpf());
        txNode.set("usuario_enviador", enviadorNode);

        ObjectNode recebedorNode = mapper.createObjectNode();
        recebedorNode.put("nome", tx.getUsuarioRecebedor().getNome());
        recebedorNode.put("cpf", tx.getUsuarioRecebedor().getCpf());
        txNode.set("usuario_recebedor", recebedorNode);

        return txNode;
    }
}