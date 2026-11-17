package br.edu.ifpr.mcp_server.mcp.utils;

/**
 * Resposta de uma ferramenta, independente do SDK do MCP.
 *
 * @param texto texto legível, para pessoas e para o modelo de IA (content)
 * @param dados os mesmos dados em formato estruturado (structuredContent)
 */
public record RespostaFerramenta(String texto, Object dados) {}
