package br.edu.ifpr.mcp_server.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Operações sobre textos usadas na limpeza dos dados da API e na comparação
 * de textos informados pelo usuário.
 */
public final class Textos {
    // espaços em branco comuns e o espaço não separável (U+00A0), que o padrão
    // \s do Java não reconhece
    private static final Pattern ESPACOS = Pattern.compile("[\\s\\u00A0]+");
    // acentos separadas da letra após a normalização NFD
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");

    private Textos() {}

    // Remove espaços do início e do fim e reduz espaços repetidos a um só.
    public static String limparEspacos(String texto) {
        if (texto == null)
            return "";

        return ESPACOS.matcher(texto).replaceAll(" ").trim();
    }

    /**
     * Prepara um texto para comparação: limpa os espaços, remove os acentos e
     * converte para minúsculas.
     */
    public static String normalizar(String texto) {
        String decomposto = Normalizer.normalize(limparEspacos(texto),
                Normalizer.Form.NFD);

        return ACENTOS.matcher(decomposto).replaceAll("").toLowerCase(Locale.ROOT);
    }

    public static boolean contemPalavras(String texto, String termo) {
        String termoNormalizado = normalizar(termo);

        if (termoNormalizado.isEmpty())
            return true;

        String textoNormalizado = normalizar(texto);

        return Arrays.stream(termoNormalizado.split(" "))
                .allMatch(textoNormalizado::contains);
    }
}
