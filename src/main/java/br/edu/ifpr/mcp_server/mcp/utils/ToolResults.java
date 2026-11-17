package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/**
 * Fábrica dos resultados de ferramentas no formato do protocolo MCP.
 *
 * Concentra a criação de CallToolResult, para que nenhuma ferramenta precise
 * conhecer os detalhes do SDK.
 */
public final class ToolResults {
   private ToolResults() {}

   /** Resultado de sucesso, com o texto e os dados estruturados. */
   public static CallToolResult sucesso(RespostaFerramenta resposta) {
      return CallToolResult.builder()
                           .addTextContent(resposta.texto())
                           .structuredContent(resposta.dados())
                           .isError(false)
                           .build();
   }

   /**
    * Resultado de erro. O erro é devolvido como resultado (isError = true), e
    * não como exceção, para que o cliente e o modelo de IA possam ler a
    * mensagem e reagir a ela.
    */
   public static CallToolResult erro(String mensagem) {
      return CallToolResult.builder()
                           .addTextContent(mensagem)
                           .isError(true)
                           .build();
   }
}
