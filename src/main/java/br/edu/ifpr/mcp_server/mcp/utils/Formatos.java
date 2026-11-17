package br.edu.ifpr.mcp_server.mcp.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import br.edu.ifpr.mcp_server.model.AnoModelo;

/**
 * Formatação de valores para exibição no padrão brasileiro.
 */
public final class Formatos {
   private static final Locale BRASIL = Locale.of("pt","BR");

   private Formatos() {}

   /** @return o valor como moeda, ex.: "R$ 187.432,00" */
   public static String moeda(BigDecimal valor) {
      // padrão explícito: o formato de moeda do Locale usa espaço não
      // separável, que atrapalha a leitura e a comparação de textos
      return novoFormato("R$ #,##0.00").format(valor);
   }

   /** @return o percentual, ex.: "33,33%" */
   public static String percentual(BigDecimal valor) {
      return novoFormato("#,##0.00").format(valor) + "%";
   }

   /** @return o ano para exibição, com "0 km" no lugar de 32000 */
   public static String ano(int ano) {
      return AnoModelo.ehZeroKm(ano) ? "0 km" : String.valueOf(ano);
   }

   // DecimalFormat não é thread-safe; por isso, uma instância por uso
   private static DecimalFormat novoFormato(String padrao) {
      return new DecimalFormat(padrao,DecimalFormatSymbols.getInstance(BRASIL));
   }
}
