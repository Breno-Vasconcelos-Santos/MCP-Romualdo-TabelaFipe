package br.edu.ifpr.mcp_server.mcp.dto;

// Veículo que não pôde ser consultado em uma comparação
public record FalhaResponse(String tipo,
                            String marca,
                            String codigoModelo,
                            String codigoAno,
                            String motivo) {}
