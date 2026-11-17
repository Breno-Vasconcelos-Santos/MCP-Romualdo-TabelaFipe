package br.edu.ifpr.mcp_server.exception;

// Exceção base para os erros de consulta à Tabela FIPE.
public abstract class FipeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected FipeException(String message) {
        super(message);
    }

    protected FipeException(String message, Throwable cause) {
        super(message,cause);
    }
}
