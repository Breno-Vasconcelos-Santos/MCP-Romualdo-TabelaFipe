package br.edu.ifpr.mcp_server.mcp.dto;

import java.math.BigDecimal;

//Preço de um veículo para exibição.
public record PrecoResponse(String tipo,
                            String marca,
                            String modelo,
                            String ano,
                            String combustivel,
                            String codigoFipe,
                            BigDecimal valor,
                            String valorFormatado,
                            String mesReferencia) {}
