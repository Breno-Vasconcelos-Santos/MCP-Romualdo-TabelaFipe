package br.edu.ifpr.mcp_server.mcp.dto;

/**
 * Ano-modelo para exibição.
 *
 * @param codigo    código a usar em consultar_preco (ex.: "2020-1")
 * @param descricao descrição com "0 km" no lugar de 32000
 * @param ano       ano para exibição ("2020" ou "0 km")
 * @param zeroKm    true se o veículo é 0 km
 */
public record AnoResponse(String codigo,
                          String descricao,
                          String ano,
                          boolean zeroKm) {}
