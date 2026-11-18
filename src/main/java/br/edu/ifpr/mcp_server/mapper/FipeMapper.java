package br.edu.ifpr.mcp_server.mapper;

import br.edu.ifpr.mcp_server.model.*;
import br.edu.ifpr.mcp_server.dto.CodigoNomeDto;
import br.edu.ifpr.mcp_server.dto.PrecoDto;
import br.edu.ifpr.mcp_server.util.Textos;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Converte os DTOs da API FIPE nos objetos do modelo.
 *
 * Dados inconsistentes da API são sinalizados com IllegalArgumentException,
 * lançada aqui ou pelas validações do próprio modelo.
 */
public final class FipeMapper {
   // formato monetário brasileiro: "R$ 2.918.759,00", "R$ 950,00"
   private static final Pattern FORMATO_VALOR =
                         Pattern.compile("R\\$ ?\\d{1,3}(\\.\\d{3})*,\\d{2}");

   private FipeMapper() {}

   public static Marca paraMarca(CodigoNomeDto dto) {
      exigirPresente(dto);
      return new Marca(dto.code(),dto.name());
   }

   public static Modelo paraModelo(CodigoNomeDto dto) {
      exigirPresente(dto);
      return new Modelo(dto.code(),dto.name());
   }

   public static AnoModelo paraAnoModelo(CodigoNomeDto dto) {
      exigirPresente(dto);
      return new AnoModelo(dto.code(),dto.name());
   }

   public static PrecoVeiculo paraPreco(TipoVeiculo tipo, PrecoDto dto) {
      exigirPresente(dto);
      if (dto.modelYear() == null)
         throw new IllegalArgumentException("Ano do modelo ausente na resposta.");

      return new PrecoVeiculo(tipo,dto.brand(),dto.model(),dto.modelYear(),
                              dto.fuel(),dto.codeFipe(),
                              converterValor(dto.price()),dto.referenceMonth());
   }

   /**
    * Converte um valor monetário no formato brasileiro ("R$ 2.918.759,00") em
    * BigDecimal (2918759.00).
    */
   public static BigDecimal converterValor(String texto) {
      String limpo = Textos.limparEspacos(texto);

      if (!FORMATO_VALOR.matcher(limpo).matches())
         throw new IllegalArgumentException("Valor monetário em formato " +
                                            "inesperado: '" + limpo + "'.");
      String numero = limpo.substring(2)          // remove "R$"
                           .trim()
                           .replace(".","")       // separador de milhar
                           .replace(',','.');     // separador decimal

      return new BigDecimal(numero);
   }

   // um item nulo (ex.: "[null]" ou corpo "null") indica resposta inconsistente
   private static void exigirPresente(Object dto) {
      if (dto == null)
         throw new IllegalArgumentException("Item ausente na resposta da API.");
   }
}
