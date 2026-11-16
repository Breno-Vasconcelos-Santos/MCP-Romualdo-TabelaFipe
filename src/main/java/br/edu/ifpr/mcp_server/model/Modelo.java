package br.edu.ifpr.mcp_server.model;

// Modelo de veículo de uma marca. Ex: código "11338",nome "DB12 Coupe 4.0 V8 680cv").
public record Modelo(String codigo, String nome) {
    public Modelo {
        codigo = Validacoes.exigirTexto(codigo,"código do modelo");
        nome   = Validacoes.exigirTexto(nome,"nome do modelo");
    }
}
