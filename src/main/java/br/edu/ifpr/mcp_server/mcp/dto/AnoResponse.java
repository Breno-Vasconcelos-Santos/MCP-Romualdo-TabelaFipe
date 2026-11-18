package br.edu.ifpr.mcp_server.mcp.dto;

// Ano-modelo para exibição.
public record AnoResponse(String codigo,
                          String descricao,
                          String ano,
                          boolean zeroKm) {}
