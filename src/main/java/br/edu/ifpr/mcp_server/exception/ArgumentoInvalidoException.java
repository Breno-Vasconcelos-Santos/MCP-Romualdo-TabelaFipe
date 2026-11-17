package br.edu.ifpr.mcp_server.exception;

/**
 * Indica que um argumento informado pelo usuário é inválido, por exemplo, um
 * tipo de veículo inexistente ou um código em formato incorreto.
 */
public class ArgumentoInvalidoException extends FipeException {
    private static final long serialVersionUID = 1L;

    public ArgumentoInvalidoException(String message) {
        super(message);
    }
}
