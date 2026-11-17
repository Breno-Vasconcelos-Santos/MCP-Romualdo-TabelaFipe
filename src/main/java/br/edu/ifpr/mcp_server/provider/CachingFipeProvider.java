package br.edu.ifpr.mcp_server.provider;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;

/**
 * Decorator que guarda em memória os resultados de outro FipeProvider.
 * Cada consulta é feita ao provider decorado apenas na primeira vez, ou após
 * expirar a validade. As seguintes são respondidas pela memória.
 */
public class CachingFipeProvider implements FipeProvider {
   // Validade padrão de cada resultado guardado
   public static final Duration VALIDADE_PADRAO = Duration.ofHours(6);

   // chaves de cada tipo de consulta
   private record ChaveModelos(TipoVeiculo tipo, String marca) {}
   private record ChaveAnos(TipoVeiculo tipo, String marca, String modelo) {}
   private record ChavePreco(TipoVeiculo tipo, String marca, String modelo,
                             String ano) {}

   private record Entrada<V>(V valor, Instant expiraEm) {}

   private final Map<TipoVeiculo,Entrada<List<Marca>>>      marcas  =
                                                      new ConcurrentHashMap<>();
   private final Map<ChaveModelos,Entrada<List<Modelo>>>    modelos =
                                                      new ConcurrentHashMap<>();
   private final Map<ChaveAnos,Entrada<List<AnoModelo>>>    anos    =
                                                      new ConcurrentHashMap<>();
   private final Map<ChavePreco,Entrada<PrecoVeiculo>>      precos  =
                                                      new ConcurrentHashMap<>();

   private final FipeProvider delegado;
   private final Duration     validade;
   private final Clock        relogio;

   public CachingFipeProvider(FipeProvider delegado) {
      this(delegado,VALIDADE_PADRAO,Clock.systemUTC());
   }

   public CachingFipeProvider(FipeProvider delegado, Duration validade,
                              Clock relogio) {
      this.delegado = delegado;
      this.validade = validade;
      this.relogio  = relogio;
   }

   @Override
   public List<Marca> listarMarcas(TipoVeiculo tipo) {
      return obter(marcas,tipo,() -> delegado.listarMarcas(tipo));
   }

   @Override
   public List<Modelo> listarModelos(TipoVeiculo tipo, String codigoMarca) {
      return obter(modelos,new ChaveModelos(tipo,codigoMarca),
                   () -> delegado.listarModelos(tipo,codigoMarca));
   }

   @Override
   public List<AnoModelo> listarAnos(TipoVeiculo tipo, String codigoMarca,
                                     String codigoModelo) {
      return obter(anos,new ChaveAnos(tipo,codigoMarca,codigoModelo),
                   () -> delegado.listarAnos(tipo,codigoMarca,codigoModelo));
   }

   @Override
   public PrecoVeiculo consultarPreco(TipoVeiculo tipo, String codigoMarca,
                                      String codigoModelo, String codigoAno) {
      return obter(precos,new ChavePreco(tipo,codigoMarca,codigoModelo,codigoAno),
                   () -> delegado.consultarPreco(tipo,codigoMarca,codigoModelo,
                                                 codigoAno));
   }

   /**
    * Devolve o valor guardado para a chave, se ainda válido; caso contrário,
    * executa a busca e guarda o resultado. Se a busca lançar uma exceção, nada
    * é guardado.
    */
   private <K,V> V obter(Map<K,Entrada<V>> cache, K chave, Supplier<V> busca) {
      Instant   agora   = relogio.instant();
      Entrada<V> entrada = cache.get(chave);

      if (entrada != null && agora.isBefore(entrada.expiraEm()))
         return entrada.valor();

      V valor = busca.get();
      cache.put(chave,new Entrada<>(valor,agora.plus(validade)));
      return valor;
   }
}
