package br.edu.ifpr.mcp_server.mcp.dto;

import java.math.BigDecimal;
import java.util.List;

// Dados estruturados da ferramenta comparar_veiculos.
public record ComparacaoResponse(List<PrecoResponse> veiculos,
                                 List<FalhaResponse> falhas,
                                 PrecoResponse maisBarato,
                                 PrecoResponse maisCaro,
                                 BigDecimal diferenca,
                                 String diferencaFormatada,
                                 BigDecimal diferencaPercentual) {}
