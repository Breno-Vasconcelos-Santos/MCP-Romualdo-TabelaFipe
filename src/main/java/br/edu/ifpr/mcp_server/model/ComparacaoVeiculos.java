package br.edu.ifpr.mcp_server.model;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resultado da comparação de preços entre veículos.
 * Guarda os preços obtidos e as consultas que falharam. As métricas de
 * comparação só existem quando há pelo menos dois preços.
 *
 * @param precos preços obtidos, na ordem em que os veículos foram informados
 * @param falhas veículos que não puderam ser consultados
 */
public record ComparacaoVeiculos(List<PrecoVeiculo> precos,
                                 List<FalhaConsulta> falhas) {
   /** Quantidade mínima de preços para que haja comparação. */
   public static final int MINIMO_PARA_COMPARAR = 2;

   private static final BigDecimal CEM           = new BigDecimal("100");
   private static final int        CASAS_DECIMAIS = 2;

   public ComparacaoVeiculos {
      // cópias imutáveis: o resultado não pode ser alterado depois de criado
      precos = List.copyOf(Validacoes.exigirNaoNulo(precos,"preços"));
      falhas = List.copyOf(Validacoes.exigirNaoNulo(falhas,"falhas"));
   }

   /** @return true se há preços suficientes para comparar */
   public boolean comparavel() {
      return precos.size() >= MINIMO_PARA_COMPARAR;
   }

   /** @return o veículo de menor preço, se houver comparação */
   public Optional<PrecoVeiculo> maisBarato() {
      return comparavel() ? precos.stream().min(porValor()) : Optional.empty();
   }

   /** @return o veículo de maior preço, se houver comparação */
   public Optional<PrecoVeiculo> maisCaro() {
      return comparavel() ? precos.stream().max(porValor()) : Optional.empty();
   }

   /** @return a diferença, em reais, entre o mais caro e o mais barato */
   public Optional<BigDecimal> diferenca() {
      return maisCaro().flatMap(caro -> maisBarato().map(barato ->
                                 caro.valor().subtract(barato.valor())));
   }

   /**
    * @return quanto o mais caro custa a mais que o mais barato, em percentual
    *         sobre o mais barato, com duas casas decimais; vazio se o mais
    *         barato custar zero (divisão impossível)
    */
   public Optional<BigDecimal> diferencaPercentual() {
      return maisBarato().filter(barato -> barato.valor().signum() > 0)
                         .flatMap(barato -> diferenca().map(dif ->
                                  dif.multiply(CEM)
                                     .divide(barato.valor(),CASAS_DECIMAIS,
                                             RoundingMode.HALF_UP)));
   }

   private static Comparator<PrecoVeiculo> porValor() {
      return Comparator.comparing(PrecoVeiculo::valor);
   }
}
