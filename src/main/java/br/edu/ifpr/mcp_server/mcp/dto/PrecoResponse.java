package br.edu.ifpr.mcp_server.mcp.dto;

import java.math.BigDecimal;

/**
 * Preço de um veículo para exibição.
 *
 * @param valor          valor numérico, para cálculos de quem recebe
 * @param valorFormatado valor em reais para leitura (ex.: "R$ 187.432,00")
 */
public record PrecoResponse(String tipo,
                            String marca,
                            String modelo,
                            String ano,
                            String combustivel,
                            String codigoFipe,
                            BigDecimal valor,
                            String valorFormatado,
                            String mesReferencia) {}
