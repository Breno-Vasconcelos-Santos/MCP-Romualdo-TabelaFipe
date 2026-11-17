package br.edu.ifpr.mcp_server.model;

// Identificação de um veículo a consultar, como informada pelo usuário.
public record VeiculoConsulta(TipoVeiculo tipo,
                              String marca,
                              String codigoModelo,
                              String codigoAno) {}
