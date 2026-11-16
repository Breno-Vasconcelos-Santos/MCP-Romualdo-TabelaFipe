package br.edu.ifpr.mcp_server.dto;

// Resposta da consulta de preço da API FIPE.
public record PrecoDto(String price,
                       String brand,
                       String model,
                       Integer modelYear,
                       String fuel,
                       String codeFipe,
                       String referenceMonth) {}
