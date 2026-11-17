package br.edu.ifpr.mcp_server.model;


/**
 * Veículo que não pôde ser consultado durante uma comparação, com o motivo.
 */
public record FalhaConsulta(VeiculoConsulta veiculo, String motivo) {}
