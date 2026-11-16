package br.edu.ifpr.mcp_server.model;

// Marca de veículo da Tabela FIPE. Ex: código "189", nome "ASTON MARTIN").
public record Marca(String codigo, String nome) {
    public Marca {
        codigo = Validacoes.exigirTexto(codigo,"código da marca");
        nome   = Validacoes.exigirTexto(nome,"nome da marca");
    }
}
