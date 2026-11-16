package br.edu.ifpr.mcp_server.dto;

/**
 * Item de lista da API FIPE: marcas, modelos e anos têm o mesmo formato
 * ({"code": "...", "name": "..."}), por isso um único DTO atende aos três.
 *
 * Os nomes dos componentes seguem os nomes do JSON, o que dispensa anotações.
 */
public record CodigoNomeDto(String code, String name) {}
