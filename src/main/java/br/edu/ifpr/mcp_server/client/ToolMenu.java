package br.edu.ifpr.mcp_server.client;

import java.util.List;
import java.util.Optional;

import io.modelcontextprotocol.spec.McpSchema.Tool;

// Menu de ferramentas
public class ToolMenu {
   public static final String LISTAR = "listar";
   public static final String SAIR   = "sair";

   private final Terminal terminal;

   public ToolMenu(Terminal terminal) {
      this.terminal = terminal;
   }

   public void exibir(List<Tool> ferramentas) {
      terminal.escrever("");
      terminal.escrever("========================================");
      terminal.escrever(" CLIENTE MCP - TABELA FIPE");
      terminal.escrever("========================================");

      if (ferramentas.isEmpty())
         terminal.escrever("Nenhuma ferramenta disponibilizada.");
      else {
         terminal.escrever("Ferramentas disponíveis:");
         for (int i = 0; i < ferramentas.size(); i++)
            terminal.escrever(String.format(" %d - %s",i + 1,rotulo(ferramentas.get(i))));
      }
      terminal.escrever("");
      terminal.escrever("Comandos: " + LISTAR + "  | " +
              SAIR);
   }

   public Optional<Tool> selecionar(List<Tool> ferramentas, String comando) {
      try {
         int indice = Integer.parseInt(comando) - 1;

         return indice >= 0 && indice < ferramentas.size()
                 ? Optional.of(ferramentas.get(indice))
                 : Optional.empty();
      }
      catch (NumberFormatException ex) {
         return ferramentas.stream()
                 .filter(ferramenta -> ferramenta.name()
                         .equalsIgnoreCase(comando))
                 .findFirst();
      }
   }

   public void exibirCabecalho(Tool ferramenta) {
      terminal.escrever("");
      terminal.escrever("----------------------------------------");
      terminal.escrever(rotulo(ferramenta));
      terminal.escrever("----------------------------------------");
   }

   // título para leitura, com o nome técnico entre colchetes
   private static String rotulo(Tool ferramenta) {
      String titulo = ferramenta.title();

      return titulo == null || titulo.isBlank()
              ? ferramenta.name()
              : titulo;
   }
}
