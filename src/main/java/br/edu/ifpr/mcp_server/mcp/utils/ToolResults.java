package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

// Fábrica dos resultados de ferramentas no formato do protocolo MCP
public final class ToolResults {
   private ToolResults() {}

   // Resultado de sucesso, com o texto e os dados estruturados
   public static CallToolResult sucesso(RespostaFerramenta resposta) {
      return CallToolResult.builder()
                           .addTextContent(resposta.texto())
                           .structuredContent(resposta.dados())
                           .isError(false)
                           .build();
   }

   // Resultado de erro
   public static CallToolResult erro(String mensagem) {
      return CallToolResult.builder()
                           .addTextContent(mensagem)
                           .isError(true)
                           .build();
   }
}
