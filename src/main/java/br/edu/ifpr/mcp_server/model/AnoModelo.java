package br.edu.ifpr.mcp_server.model;

import java.util.regex.Pattern;

import br.edu.ifpr.mcp_server.util.Textos;

// Ano-modelo disponível para um veículo seguindo a convenção da FIPE. Ex: 2024-1
public record AnoModelo(String codigo, String descricao) {
    /** Ano usado pela FIPE para identificar veículos 0 km. */
    public static final int ANO_ZERO_KM = 32000;

    private static final Pattern FORMATO_CODIGO = Pattern.compile("\\d{4,5}-\\d+");
    private static final String  ZERO_KM        = "0 km";

    public AnoModelo {
        codigo    = Validacoes.exigirTexto(codigo,"código do ano");
        descricao = Validacoes.exigirTexto(descricao,"descrição do ano");

        if (!formatoValido(codigo))
            throw new IllegalArgumentException("Código de ano em formato inválido: " +
                    codigo + ".");
    }

    //retorna o ano contido no código (32000 para veículos 0 km)
    public int ano() {
        return Integer.parseInt(codigo.substring(0,codigo.indexOf('-')));
    }

    //retorna true se o ano-modelo corresponde a um veículo 0 km
    public boolean zeroKm() {
        return ehZeroKm(ano());
    }

    // retorna a descrição para exibição, com "0 km" no lugar de "32000"
    public String descricaoExibicao() {
        if (!zeroKm())
            return descricao;

        return descricao.replaceFirst(String.valueOf(ANO_ZERO_KM),ZERO_KM);
    }

    /** @return true se o ano informado corresponde a um veículo 0 km */
    public static boolean ehZeroKm(int ano) {
        return ano == ANO_ZERO_KM;
    }

    /**
     * Verifica se um código de ano está no formato da FIPE ("2024-1"). Útil
     * para validar a entrada do usuário antes de consultar a API.
     */
    public static boolean formatoValido(String codigo) {
        return FORMATO_CODIGO.matcher(Textos.limparEspacos(codigo)).matches();
    }
}
