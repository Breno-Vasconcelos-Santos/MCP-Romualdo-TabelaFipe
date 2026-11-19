package br.edu.ifpr.mcp_server.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.exception.FipeException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.provider.FipeProvider;

/**
 * FipeProvider em memória para testes: devolve os dados cadastrados, lança
 * RecursoNaoEncontradoException para o que não foi cadastrado e registra as
 * chamadas recebidas. Também pode ser configurado para falhar.
 */
public class FakeFipeProvider implements FipeProvider {
   private final Map<TipoVeiculo,List<Marca>>  marcas  = new HashMap<>();
   private final Map<String,List<Modelo>>      modelos = new HashMap<>();
   private final Map<String,List<AnoModelo>>   anos    = new HashMap<>();
   private final Map<String,PrecoVeiculo>      precos  = new HashMap<>();
   private final List<String>                  chamadas = new ArrayList<>();

   private FipeException falha;

   // ===== cadastro =====

   public FakeFipeProvider comMarcas(TipoVeiculo tipo, Marca... itens) {
      marcas.put(tipo,List.of(itens));
      return this;
   }

   public FakeFipeProvider comModelos(TipoVeiculo tipo, String marca,
                                      List<Modelo> itens) {
      modelos.put(chave(tipo,marca),List.copyOf(itens));
      return this;
   }

   public FakeFipeProvider comAnos(TipoVeiculo tipo, String marca, String modelo,
                                   AnoModelo... itens) {
      anos.put(chave(tipo,marca,modelo),List.of(itens));
      return this;
   }

   public FakeFipeProvider comPreco(TipoVeiculo tipo, String marca, String modelo,
                                    String ano, PrecoVeiculo preco) {
      precos.put(chave(tipo,marca,modelo,ano),preco);
      return this;
   }

   /** Faz todas as consultas seguintes lançarem a exceção informada. */
   public void falharCom(FipeException excecao) {
      this.falha = excecao;
   }

   /** Remove a falha configurada. */
   public void normalizar() {
      this.falha = null;
   }

   /** @return as chamadas recebidas, no formato "metodo:arg1:arg2..." */
   public List<String> chamadas() {
      return List.copyOf(chamadas);
   }

   // ===== FipeProvider =====

   @Override
   public List<Marca> listarMarcas(TipoVeiculo tipo) {
      registrar("listarMarcas",tipo);
      return encontrar(marcas.get(tipo));
   }

   @Override
   public List<Modelo> listarModelos(TipoVeiculo tipo, String codigoMarca) {
      registrar("listarModelos",tipo,codigoMarca);
      return encontrar(modelos.get(chave(tipo,codigoMarca)));
   }

   @Override
   public List<AnoModelo> listarAnos(TipoVeiculo tipo, String codigoMarca,
                                     String codigoModelo) {
      registrar("listarAnos",tipo,codigoMarca,codigoModelo);
      return encontrar(anos.get(chave(tipo,codigoMarca,codigoModelo)));
   }

   @Override
   public PrecoVeiculo consultarPreco(TipoVeiculo tipo, String codigoMarca,
                                      String codigoModelo, String codigoAno) {
      registrar("consultarPreco",tipo,codigoMarca,codigoModelo,codigoAno);
      return encontrar(precos.get(chave(tipo,codigoMarca,codigoModelo,codigoAno)));
   }

   private void registrar(String metodo, Object... argumentos) {
      chamadas.add(metodo + ":" + chave(argumentos));
      if (falha != null)
         throw falha;
   }

   private static <T> T encontrar(T valor) {
      if (valor == null)
         throw new RecursoNaoEncontradoException("não cadastrado no fake");
      return valor;
   }

   private static String chave(Object... partes) {
      StringBuilder chave = new StringBuilder();

      for (Object parte : partes)
         chave.append(chave.isEmpty() ? "" : ":").append(parte);
      return chave.toString();
   }
}
