package br.edu.ifpr.mcp_server.mcp.dto;

import java.util.List;

import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;

/** Dados estruturados da ferramenta buscar_modelos. */
public record BuscaModelosResponse(String tipo,
                                   Marca marca,
                                   String termo,
                                   int totalEncontrado,
                                   int exibidos,
                                   boolean limitada,
                                   List<Modelo> modelos) {}
