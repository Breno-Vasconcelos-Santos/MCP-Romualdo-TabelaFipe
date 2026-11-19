package br.edu.ifpr.mcp_server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.ComparacaoVeiculos;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.model.VeiculoConsulta;
import br.edu.ifpr.mcp_server.support.FakeFipeProvider;

class FipeServiceTest {
   private static final TipoVeiculo CARROS = TipoVeiculo.CARROS;

   private static final Modelo       M320      = new Modelo("11403","320iA Sport GP 2.0 Turbo");
   private static final PrecoVeiculo PRECO_320 = preco("320iA Sport","150000.00");
   private static final PrecoVeiculo PRECO_X1  = preco("X1","200000.00");

   private final VeiculoConsulta v320 = new VeiculoConsulta(CARROS,"bmw","11403","2020-1");
   private final VeiculoConsulta vX1  = new VeiculoConsulta(CARROS,"7","11500","2020-1");

   private FakeFipeProvider provider;
   private FipeService      service;

   private static PrecoVeiculo preco(String modelo, String valor) {
      return new PrecoVeiculo(CARROS,"BMW",modelo,2020,"Gasolina","000000-0",
                              new BigDecimal(valor),"novembro de 2026");
   }

   @BeforeEach
   void preparar() {
      // 40 modelos genéricos além do 320, para testar o limite de resultados
      List<Modelo> modelos = new ArrayList<>(List.of(M320));
      IntStream.rangeClosed(1,40).forEach(i -> modelos.add(new Modelo("90" + i,"Série " + i)));

      provider = new FakeFipeProvider()
            .comMarcas(CARROS,new Marca("7","BMW"),new Marca("13","Citroën"),
                       new Marca("59","VW - VolksWagen"),new Marca("245","Caoa Chery"),
                       new Marca("161","Caoa Chery/Chery"),new Marca("167","MG"))
            .comModelos(CARROS,"7",modelos)
            .comAnos(CARROS,"7","11403",new AnoModelo("2020-1","2020 Gasolina"))
            .comPreco(CARROS,"7","11403","2020-1",PRECO_320)
            .comPreco(CARROS,"7","11500","2020-1",PRECO_X1);
      service  = new FipeService(provider);
   }

   private long chamadasA(String metodo) {
      return provider.chamadas().stream().filter(c -> c.startsWith(metodo)).count();
   }

   // ===== resolução da marca ================================================

   @ParameterizedTest
   @CsvSource({"7,          7",     // código
               "' BMW ',    7",     // nome exato, sem diferenciar maiúsculas
               "citroen,    13",    // sem acento
               "volkswagen, 59",    // parte do nome
               "caoa chery, 245",   // o nome exato tem prioridade sobre o parcial
               "mg,         167"})  // nome curto resolvido pelo exato
   void deveResolverMarcaPorCodigoOuNome(String marca, String codigo) {
      assertEquals(codigo,service.resolverMarca(CARROS,marca).codigo());
   }

   @Test
   void deveRecusarMarcaAmbiguaListandoAsCandidatas() {
      String mensagem = assertThrows(ArgumentoInvalidoException.class,
                                     () -> service.resolverMarca(CARROS,"chery"))
                              .getMessage();

      assertTrue(mensagem.contains("245 – Caoa Chery"));
      assertTrue(mensagem.contains("161 – Caoa Chery/Chery"));
   }

   @Test
   void deveInformarMarcaInexistentePorNomeOuCodigo() {
      assertThrows(RecursoNaoEncontradoException.class,
                   () -> service.resolverMarca(CARROS,"ferrari"));
      assertThrows(RecursoNaoEncontradoException.class,
                   () -> service.resolverMarca(CARROS,"999"));
   }

   @ParameterizedTest
   @NullAndEmptySource
   void deveExigirMarcaSemConsultarAApi(String marca) {
      assertThrows(ArgumentoInvalidoException.class,
                   () -> service.resolverMarca(CARROS,marca));
      assertEquals(0,chamadasA("listarMarcas"));
   }

   // ===== listas ============================================================

   @Test
   void deveFiltrarMarcasPorTermo() {
      assertEquals(2,service.listarMarcas(CARROS,"chery").size());
   }

   @Test
   void deveBuscarModelosPorPalavrasEmQualquerOrdem() {
      BuscaModelos busca = service.buscarModelos(CARROS,"bmw","sport 320");

      assertEquals("BMW",busca.marca().nome());
      assertEquals(List.of(M320),busca.modelos());
      assertFalse(busca.limitada());
   }

   @Test
   void deveLimitarModelosSemTermo() {
      BuscaModelos busca = service.buscarModelos(CARROS,"bmw",null);

      assertEquals(FipeService.LIMITE_MODELOS,busca.modelos().size());
      assertEquals(41,busca.totalEncontrado());
      assertTrue(busca.limitada());
   }

   @Test
   void deveListarAnosResolvendoAMarca() {
      assertEquals(1,service.listarAnos(CARROS,"bmw","11403").size());
      assertTrue(provider.chamadas().contains("listarAnos:CARROS:7:11403"));
   }

   // ===== preço =============================================================

   @Test
   void deveConsultarPreco() {
      assertEquals(PRECO_320,service.consultarPreco(CARROS,"bmw","11403","2020-1"));
   }

   @Test
   void deveRecusarEntradasInvalidasSemConsultarAApi() {
      assertThrows(ArgumentoInvalidoException.class,
                   () -> service.consultarPreco(CARROS,"bmw","11403","2020"));
      assertThrows(ArgumentoInvalidoException.class,
                   () -> service.consultarPreco(CARROS,"bmw"," ","2020-1"));
      assertEquals(0,chamadasA("consultarPreco"));
   }

   // ===== comparação ========================================================

   @Test
   void deveCompararVeiculos() {
      ComparacaoVeiculos comparacao = service.compararVeiculos(List.of(v320,vX1));

      assertEquals(List.of(PRECO_320,PRECO_X1),comparacao.precos());
      assertEquals(PRECO_320,comparacao.maisBarato().orElseThrow());
   }

   @Test
   void deveRegistrarFalhasSemInterromperAComparacao() {
      VeiculoConsulta inexistente = new VeiculoConsulta(CARROS,"bmw","99999","2020-1");

      ComparacaoVeiculos comparacao =
            service.compararVeiculos(List.of(v320,inexistente,vX1));

      assertEquals(2,comparacao.precos().size());
      assertEquals(inexistente,comparacao.falhas().getFirst().veiculo());
   }

   @Test
   void deveRecusarQuantidadeForaDoIntervalo() {
      assertThrows(ArgumentoInvalidoException.class,
                   () -> service.compararVeiculos(List.of(v320)));
      assertThrows(ArgumentoInvalidoException.class,
                   () -> service.compararVeiculos(List.of(v320,v320,v320,v320,v320,v320)));
   }

   @Test
   void deveInterromperComparacaoQuandoApiEstiverIndisponivel() {
      provider.falharCom(new FipeIndisponivelException("fora do ar"));

      assertThrows(FipeIndisponivelException.class,
                   () -> service.compararVeiculos(List.of(v320,vX1)));
   }
}
