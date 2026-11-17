package br.edu.ifpr.mcp_server.exception;

// Indica que a marca, o modelo ou o ano consultado não existe na Tabela FIPE
public class RecursoNaoEncontradoException extends FipeException {
    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}