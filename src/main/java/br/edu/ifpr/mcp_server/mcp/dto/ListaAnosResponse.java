package br.edu.ifpr.mcp_server.mcp.dto;

import java.util.List;

// Dados estruturados da ferramenta listar_anos
public record ListaAnosResponse(String tipo,
                                String marca,
                                String codigoModelo,
                                List<AnoResponse> anos) {}
