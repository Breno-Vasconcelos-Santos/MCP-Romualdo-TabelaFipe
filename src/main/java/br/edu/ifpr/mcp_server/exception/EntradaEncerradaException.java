package br.edu.ifpr.mcp_server.exception;

// Indica que a entrada padrão foi encerrada
public class EntradaEncerradaException extends RuntimeException {
   private static final long serialVersionUID = 1L;

   public EntradaEncerradaException() {
      super("Entrada encerrada pelo usuário.");
   }
}
