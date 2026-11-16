package br.edu.ifpr.mcp_server.model;

import br.edu.ifpr.mcp_server.util.Textos;

/**
 * Validações das invariantes dos objetos do pacote model.
 * Uma violação aqui indica dado inconsistente (vindo da API ou de erro de
 * programação), por isso é sinalizada com IllegalArgumentException, e não com
 * uma exceção voltada ao usuário.
 */
final class Validacoes {
    private Validacoes() {}

    /**
     * @return o texto com os espaços limpos
     * @throws IllegalArgumentException se o texto for nulo ou vazio
     */
    static String exigirTexto(String valor, String campo) {
        String limpo = Textos.limparEspacos(valor);

        if (limpo.isEmpty())
            throw new IllegalArgumentException("Campo obrigatório não informado: " +
                    campo + ".");
        return limpo;
    }

    /**
     * @return o próprio valor
     * @throws IllegalArgumentException se o valor for nulo
     */
    static <T> T exigirNaoNulo(T valor, String campo) {
        if (valor == null)
            throw new IllegalArgumentException("Campo obrigatório não informado: " +
                    campo + ".");
        return valor;
    }
}
