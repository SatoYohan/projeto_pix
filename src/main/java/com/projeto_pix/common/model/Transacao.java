package com.projeto_pix.common.model;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Transacao {
    private int id;
    private double valorEnviado;
    private Usuario usuarioEnviador;
    private Usuario usuarioRecebedor;
    private String criadoEm;

    public Transacao(int id, double valorEnviado, Usuario usuarioEnviador, Usuario usuarioRecebedor) {
        this.id = id;
        this.valorEnviado = valorEnviado;
        this.usuarioEnviador = usuarioEnviador;
        this.usuarioRecebedor = usuarioRecebedor;
        // Armazena no formato ISO 8601 UTC, conforme o protocolo
        this.criadoEm = Instant.now().toString();
    }

    // Getters
    public int getId() { return id; }
    public double getValorEnviado() { return valorEnviado; }
    public Usuario getUsuarioEnviador() { return usuarioEnviador; }
    public Usuario getUsuarioRecebedor() { return usuarioRecebedor; }
    public String getCriadoEm() { return criadoEm; }

    // Método para checar se um CPF está envolvido nesta transação
    public boolean envolveUsuario(String cpf) {
        return usuarioEnviador.getCpf().equals(cpf) || usuarioRecebedor.getCpf().equals(cpf);
    }

    // Método para checar se a transação está dentro do intervalo de datas
    public boolean estaNoIntervalo(Instant dataInicial, Instant dataFinal) {
        Instant dataTransacao = Instant.parse(this.criadoEm);
        return !dataTransacao.isBefore(dataInicial) && !dataTransacao.isAfter(dataFinal);
    }
}