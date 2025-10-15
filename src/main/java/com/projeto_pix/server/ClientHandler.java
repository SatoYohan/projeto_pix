package com.projeto_pix.server;

import java.io.*;
import java.net.Socket;
import com.projeto_pix.common.Validator; // Importe o seu validador

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String jsonRequest;
            while ((jsonRequest = in.readLine()) != null) {
                System.out.println("Recebido do cliente: " + jsonRequest);

                try {
                    // 1. Validar a mensagem do cliente
                    Validator.validateClient(jsonRequest);

                    // 2. Processar a operação (vamos detalhar isso a seguir)
                    String jsonResponse = processOperation(jsonRequest);

                    // 3. Enviar a resposta para o cliente
                    out.println(jsonResponse);

                } catch (Exception e) {
                    // Se a validação falhar, envie uma resposta de erro
                    // Conforme o protocolo, crie um JSON de erro
                    String errorResponse = "{\"operacao\": \"erro\", \"status\": false, \"info\": \"" + e.getMessage() + "\"}";
                    out.println(errorResponse);
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        }
    }

    private String processOperation(String jsonRequest) {
        // Aqui virá a lógica para:
        // - Usar o Jackson para extrair a "operacao" do JSON
        // - Chamar o método certo: cadastrarUsuario(), fazerLogin(), etc.
        // - Montar o JSON de resposta
        return "{\"operacao\": \"placeholder\", \"status\": true, \"info\": \"Operação recebida\"}"; // Resposta temporária
    }
}