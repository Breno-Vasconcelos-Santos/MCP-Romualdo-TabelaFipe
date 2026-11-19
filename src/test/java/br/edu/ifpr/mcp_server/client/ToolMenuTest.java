package br.edu.ifpr.mcp_server.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.Tool;

class ToolMenuTest {
   private final ToolMenu menu = new ToolMenu(new Terminal(new ByteArrayInputStream(new byte[0]),
                                                           new PrintStream(new ByteArrayOutputStream())));

   private final List<Tool> ferramentas = List.of(Tool.builder("listar_marcas",Map.of()).build(),
                                                  Tool.builder("consultar_preco",Map.of()).build());

   @Test
   void deveSelecionarPeloNumeroOuPeloNome() {
      assertEquals("consultar_preco",menu.selecionar(ferramentas,"2").orElseThrow().name());
      assertEquals("listar_marcas",
                   menu.selecionar(ferramentas,"LISTAR_MARCAS").orElseThrow().name());
   }

   @Test
   void naoDeveSelecionarOpcaoInexistente() {
      assertTrue(menu.selecionar(ferramentas,"0").isEmpty());
      assertTrue(menu.selecionar(ferramentas,"3").isEmpty());
      assertTrue(menu.selecionar(ferramentas,"comprar").isEmpty());
   }
}
