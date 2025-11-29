package com.projeto_pix.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projeto_pix.common.Validator;

import java.io.*;
import java.net.Socket;

public class PixClientService {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper mapper = new ObjectMapper();
    private String userToken = null;
    private boolean connected = false;

    // Interface para avisar a GUI quando chegar mensagem
    public interface ServerMessageListener {
        void onMessageReceived(JsonNode message);
    }

    private ServerMessageListener listener;

    public void setListener(ServerMessageListener listener) {
        this.listener = listener;
    }

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;

        // Thread para ouvir o servidor
        new Thread(this::listenToServer).start();

        // Protocolo de conexão inicial
        ObjectNode json = mapper.createObjectNode();
        json.put("operacao", "conectar");
        sendJson(json);
    }

    private void listenToServer() {
        try {
            String responseLine;
            while (connected && (responseLine = in.readLine()) != null) {
                System.out.println("Recebido: " + responseLine);
                try {
                    // Valida a mensagem
                    Validator.validateServer(responseLine);
                    JsonNode node = mapper.readTree(responseLine);

                    // Salva o token se for login com sucesso
                    if (node.has("operacao") &&
                            "usuario_login".equals(node.get("operacao").asText()) &&
                            node.get("status").asBoolean()) {
                        this.userToken = node.get("token").asText();
                    }
                    // Limpa token no logout
                    if (node.has("operacao") &&
                            "usuario_logout".equals(node.get("operacao").asText()) &&
                            node.get("status").asBoolean()) {
                        this.userToken = null;
                    }

                    if (listener != null) {
                        listener.onMessageReceived(node);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao processar: " + e.getMessage());
                    // Aqui você poderia notificar a GUI sobre erro de validação se quisesse
                }
            }
        } catch (IOException e) {
            connected = false;
            System.out.println("Desconectado do servidor.");
        }
    }

    public void sendJson(ObjectNode json) {
        if (out != null) {
            System.out.println("Enviando: " + json.toString());
            out.println(json.toString());
        }
    }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    public String getToken() { return userToken; }
    public ObjectMapper getMapper() { return mapper; }
    public boolean isConnected() { return connected; }
}