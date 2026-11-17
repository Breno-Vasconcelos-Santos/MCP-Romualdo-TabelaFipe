package br.edu.ifpr.mcp_server.exception;

/**
 * Indica que a API da Tabela FIPE não pôde ser consultada: falha de rede,
 * limite de requisições atingido (HTTP 429), erro do servidor (HTTP 5xx) ou
 * resposta em formato inesperado.
 */
public class FipeIndisponivelException extends FipeException {
    private static final long serialVersionUID = 1L;

    public FipeIndisponivelException(String message) {
        super(message);
    }

    public FipeIndisponivelException(String message, Throwable cause) {
        super(message,cause);
    }
}
