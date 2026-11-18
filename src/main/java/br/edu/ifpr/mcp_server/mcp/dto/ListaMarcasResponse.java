package br.edu.ifpr.mcp_server.mcp.dto;

import java.util.List;

import br.edu.ifpr.mcp_server.model.Marca;

// Dados estruturados da ferramenta listar_marcas
public record ListaMarcasResponse(String tipo,
                                  String termo,
                                  int total,
                                  List<Marca> marcas) {}
