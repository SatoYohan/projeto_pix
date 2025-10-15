// Em: src/main/java/com/projeto_pix/common/model/Usuario.java
package com.projeto_pix.common.model;

public class Usuario {
    private String nome;
    private String cpf;
    private String senha;
    private double saldo;

    public Usuario(String nome, String cpf, String senha) {
        this.nome = nome;
        this.cpf = cpf;
        this.senha = senha;
        this.saldo = 0.0; // Todo novo usuário começa com saldo zero
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    // Método para validar a senha durante o login
    public boolean verificarSenha(String senha) {
        return this.senha.equals(senha);
    }
}