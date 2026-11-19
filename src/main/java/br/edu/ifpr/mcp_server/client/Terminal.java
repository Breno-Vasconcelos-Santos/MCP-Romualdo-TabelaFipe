package br.edu.ifpr.mcp_server.client;

import br.edu.ifpr.mcp_server.exception.EntradaEncerradaException;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

// Interação com o usuário pelo terminal
public class Terminal {
   private final Scanner     entrada;
   private final PrintStream saida;

   public Terminal(InputStream entrada, PrintStream saida) {
      this.entrada = new Scanner(entrada);
      this.saida   = saida;
   }

   // Exibe o texto de solicitação e lê uma linha
   public String ler(String solicitacao) {
      saida.print(solicitacao);
      saida.flush();

      if (!entrada.hasNextLine())
         throw new EntradaEncerradaException();

      return entrada.nextLine().trim();
   }

   public void escrever(String texto) {
      saida.println(texto);
   }
}
