package br.edu.ifpr.mcp_server.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

class ResultPrinterTest {
   private final ByteArrayOutputStream saida      = new ByteArrayOutputStream();
   private final ResultPrinter         impressora = new ResultPrinter(new Terminal(
                     new ByteArrayInputStream(new byte[0]),
                     new PrintStream(saida,true,StandardCharsets.UTF_8)));

   @Test
   void deveExibirApenasOTextoDoResultado() {
      impressora.exibir(CallToolResult.builder()
                                      .addTextContent("Preço FIPE: R$ 187.432,00")
                                      .structuredContent(Map.of("valor",187432))
                                      .isError(false).build());

      String exibido = saida.toString(StandardCharsets.UTF_8);
      assertTrue(exibido.contains(" RESULTADO "));
      assertTrue(exibido.contains("Preço FIPE: R$ 187.432,00"));
      assertFalse(exibido.contains("TextContent"));
      assertFalse(exibido.contains("187432"));
   }

   @Test
   void deveIdentificarResultadoDeErro() {
      impressora.exibir(CallToolResult.builder().addTextContent("Marca não encontrada.")
                                      .isError(true).build());

      assertTrue(saida.toString(StandardCharsets.UTF_8).contains(" ERRO "));
   }
}
