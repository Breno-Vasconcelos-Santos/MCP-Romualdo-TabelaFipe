package br.edu.ifpr.mcp_server.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.config.FipeConfig;
import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.support.StubHttpServer;

/**
 * Testa o provider contra um servidor HTTP local que devolve as respostas
 * reais da API (fixtures), sem acessar a internet.
 */
class ParallelumFipeProviderTest {
   private static final TipoVeiculo CARROS  = TipoVeiculo.CARROS;
   private static final String      MARCAS  = "/cars/brands";
   private static final String      MODELOS = MARCAS + "/189/models";
   private static final String      ANOS    = MODELOS + "/11338/years";
   private static final String      PRECO   = ANOS + "/2024-1";

   private StubHttpServer api;
   private FipeProvider   provider;

   @BeforeEach
   void iniciarApi() throws IOException {
      api      = StubHttpServer.iniciar();
      provider = criarProvider(null);
   }

   @AfterEach
   void encerrarApi() {
      api.close();
   }

   private FipeProvider criarProvider(String token) {
      return new ParallelumFipeProvider(new FipeConfig(api.url(),token,
                                                       Duration.ofSeconds(5)));
   }

   // ===== consultas com sucesso =============================================

   @Test
   void deveListarMarcas() {
      api.responderComFixture(MARCAS,200,"marcas-carros.json");

      List<Marca> marcas = provider.listarMarcas(CARROS);

      assertEquals(108,marcas.size());
      assertTrue(marcas.contains(new Marca("7","BMW")));
   }

   @Test
   void deveListarModelosComNomesLimpos() {
      api.responderComFixture(MODELOS,200,"modelos-aston-martin.json");

      // a API devolve "Vantage Cupê  4.0 V8 510cv", com espaço duplo
      assertTrue(provider.listarModelos(CARROS,"189")
                         .contains(new Modelo("10144","Vantage Cupê 4.0 V8 510cv")));
   }

   @Test
   void deveListarAnosIdentificandoZeroKm() {
      api.responderComFixture(ANOS,200,"anos-db12.json");

      List<AnoModelo> anos = provider.listarAnos(CARROS,"189","11338");

      assertEquals(4,anos.size());
      assertTrue(anos.getLast().zeroKm());
   }

   @Test
   void deveConsultarPreco() {
      api.responderComFixture(PRECO,200,"preco-db12-2024.json");

      PrecoVeiculo preco = provider.consultarPreco(CARROS,"189","11338","2024-1");

      assertEquals(new BigDecimal("2918759.00"),preco.valor());
      assertEquals("085018-7",preco.codigoFipe());
      assertEquals(2024,preco.anoModelo());
   }

   @Test
   void deveMontarUrlComTipoESegmentosCodificados() {
      api.responder("/motorcycles/brands/1%202%2F3/models",200,"[]");

      provider.listarModelos(TipoVeiculo.MOTOS,"1 2/3");

      assertEquals("/motorcycles/brands/1%202%2F3/models",
                   api.ultimaRequisicao().caminho());
   }

   @Test
   void deveEnviarTokenSomenteQuandoConfigurado() {
      api.responder(MARCAS,200,"[]");

      provider.listarMarcas(CARROS);
      assertNull(api.ultimaRequisicao().token());

      criarProvider("abc123").listarMarcas(CARROS);
      assertEquals("abc123",api.ultimaRequisicao().token());
   }

   // ===== erros da API ======================================================

   @Test
   void deveTraduzirNaoEncontradoConformeOContexto() {
      api.responderComFixture("/cars/brands/999/models",404,"erro-codigo-inexistente.json");
      api.responderComFixture(MODELOS + "/999/years",404,"erro-codigo-inexistente.json");
      api.responderComFixture(ANOS + "/1900-1",404,"erro-ano-invalido.json");

      assertTrue(assertThrows(RecursoNaoEncontradoException.class,
                              () -> provider.listarModelos(CARROS,"999"))
                       .getMessage().contains("Marca não encontrada"));
      assertTrue(assertThrows(RecursoNaoEncontradoException.class,
                              () -> provider.listarAnos(CARROS,"189","999"))
                       .getMessage().contains("Marca ou modelo não encontrado"));
      assertTrue(assertThrows(RecursoNaoEncontradoException.class,
                              () -> provider.consultarPreco(CARROS,"189","11338",
                                                            "1900-1"))
                       .getMessage().contains("'1900-1'"));
   }

   @Test
   void deveRepassarMensagemDaApiQuandoRequisicaoForRecusada() {
      api.responderComFixture(MARCAS,400,"erro-tipo-invalido.json");

      assertTrue(assertThrows(ArgumentoInvalidoException.class,
                              () -> provider.listarMarcas(CARROS))
                       .getMessage().contains("invalid vehicle type"));
   }

   @ParameterizedTest
   @CsvSource({"429, Limite de requisições",
               "500, HTTP 500"})
   void deveInformarApiIndisponivel(int status, String trecho) {
      api.responder(MARCAS,status,"{}");

      assertTrue(assertThrows(FipeIndisponivelException.class,
                              () -> provider.listarMarcas(CARROS))
                       .getMessage().contains(trecho));
   }

   @ParameterizedTest
   @ValueSource(strings = {"{nao e json","null","[null]",
                           "[{\"code\":\"\",\"name\":\"Sem código\"}]"})
   void deveRecusarRespostaMalformada(String corpo) {
      api.responder(MARCAS,200,corpo);

      assertThrows(FipeIndisponivelException.class,
                   () -> provider.listarMarcas(CARROS));
   }

   @Test
   void deveInformarFalhaDeConexaoPreservandoACausa() {
      api.close();

      FipeIndisponivelException ex =
            assertThrows(FipeIndisponivelException.class,
                         () -> provider.listarMarcas(CARROS));

      assertInstanceOf(IOException.class,ex.getCause());
   }
}
