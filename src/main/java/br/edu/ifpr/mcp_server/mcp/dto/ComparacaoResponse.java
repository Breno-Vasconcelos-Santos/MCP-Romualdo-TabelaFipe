package br.edu.ifpr.mcp_server.mcp.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados estruturados da ferramenta comparar_veiculos.
 *
 * @param diferencaPercentual quanto o mais caro custa a mais que o mais
 *                            barato, em percentual; null se o mais barato
 *                            custar zero
 */
public record ComparacaoResponse(List<PrecoResponse> veiculos,
                                 List<FalhaResponse> falhas,
                                 PrecoResponse maisBarato,
                                 PrecoResponse maisCaro,
                                 BigDecimal diferenca,
                                 String diferencaFormatada,
                                 BigDecimal diferencaPercentual) {}
