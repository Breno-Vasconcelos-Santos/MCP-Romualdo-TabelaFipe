package br.edu.ifpr.mcp_server.client;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Exibição do resultado de uma ferramenta.
 *
 * Mostra apenas o texto de cada conteúdo: os dados estruturados
 * são destinados a programas e assistentes de IA, e não
 * a leitura.
 */
public class ResultPrinter {
   private static final String LINHA = "=========================================";

   private final Terminal terminal;

   public ResultPrinter(Terminal terminal) {
      this.terminal = terminal;
   }

   public void exibir(CallToolResult resultado) {
      String titulo = Boolean.TRUE.equals(resultado.isError()) ? " ERRO "
                                                               : " RESULTADO ";
      terminal.escrever("");
      terminal.escrever("===============" + titulo + "===============");
      resultado.content().forEach(conteudo -> terminal.escrever(textoDe(conteudo)));
      terminal.escrever(LINHA);
   }

   private static String textoDe(Content conteudo) {
      if (conteudo instanceof TextContent texto)
         return texto.text();
      return "[conteúdo do tipo " + conteudo.getClass().getSimpleName() +
             " não exibido]";
   }
}
