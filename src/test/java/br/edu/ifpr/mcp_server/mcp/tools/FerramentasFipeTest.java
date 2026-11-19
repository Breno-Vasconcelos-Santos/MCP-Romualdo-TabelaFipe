package br.edu.ifpr.mcp_server.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.mcp.dto.ComparacaoResponse;
import br.edu.ifpr.mcp_server.mcp.dto.PrecoResponse;
import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.support.FakeFipeProvider;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Executa as ferramentas pelo método run, como o servidor MCP faz, com um
 * provider em memória no lugar da API.
 */
class FerramentasFipeTest {
   private static final TipoVeiculo CARROS = TipoVeiculo.CARROS;

   private FakeFipeProvider provider;
   private FipeService      service;

   private static PrecoVeiculo preco(String modelo, int ano, String valor) {
      return new PrecoVeiculo(CARROS,"BMW",modelo,ano,"Gasolina","009262-5",
                              new BigDecimal(valor),"novembro de 2026");
   }

   private static Map<String,Object> veiculo(String modelo, String ano) {
      return Map.of("tipo","carros","marca","bmw","codigoModelo",modelo,"codigoAno",ano);
   }

   @BeforeEach
   void preparar() {
      provider = new FakeFipeProvider()
            .comMarcas(CARROS,new Marca("7","BMW"),new Marca("21","Fiat"))
            .comModelos(CARROS,"7",List.of(new Modelo("11403","320i M Sport 2.0"),
                                           new Modelo("11500","X1 sDrive20i")))
            .comAnos(CARROS,"7","11403",new AnoModelo("2020-1","2020 Gasolina"),
                     new AnoModelo("32000-1","32000 Gasolina"))
            .comPreco(CARROS,"7","11403","2020-1",preco("320i M Sport 2.0",2020,"187432.00"))
            .comPreco(CARROS,"7","11500","32000-1",preco("X1 sDrive20i",32000,"374864.00"));
      service  = new FipeService(provider);
   }

   private CallToolResult executar(AbstractFipeTool ferramenta, Map<String,Object> argumentos) {
      return ferramenta.run(null,CallToolRequest.builder(ferramenta.nome())
                                                .arguments(argumentos).build());
   }

   private String texto(CallToolResult resultado) {
      return ((TextContent) resultado.content().getFirst()).text();
   }

   // ===== definição =========================================================

   @Test
   void deveDefinirFerramentaComParametrosObrigatoriosEAnotacoes() {
      ConsultarPrecoTool ferramenta = new ConsultarPrecoTool(service);

      assertEquals(List.of("tipo","marca","codigoModelo","codigoAno"),
                   ferramenta.definicao().inputSchema().get("required"));
      assertTrue(ferramenta.definicao().annotations().readOnlyHint());
   }

   // ===== uma execução por ferramenta =======================================

   @Test
   void deveListarMarcasFiltradas() {
      assertEquals("Marcas de carros com \"fiat\" (1):\n21 – Fiat",
                   texto(executar(new ListarMarcasTool(service),
                                  Map.of("tipo","carros","termo","fiat"))));
   }

   @Test
   void deveBuscarModelosInformandoAMarcaResolvida() {
      String texto = texto(executar(new BuscarModelosTool(service),
                                    Map.of("tipo","carros","marca","bmw","termo","320")));

      assertTrue(texto.contains("1 Modelos encontrados para a marca: BMW (código 7)"));
      assertTrue(texto.contains("11403 – 320i M Sport 2.0"));
   }

   @Test
   void deveListarAnosExibindoZeroKm() {
      assertTrue(texto(executar(new ListarAnosTool(service),
                                Map.of("tipo","carros","marca","bmw","codigoModelo","11403")))
                       .contains("32000-1 – 0 km Gasolina"));
   }

   @Test
   void deveConsultarPrecoComTextoEDadosEstruturados() {
      CallToolResult resultado = executar(new ConsultarPrecoTool(service),
                                          veiculo("11403","2020-1"));

      assertFalse(resultado.isError());
      assertTrue(texto(resultado).contains("Preço FIPE: R$ 187.432,00"));
      assertEquals(new BigDecimal("187432.00"),
                   assertInstanceOf(PrecoResponse.class,resultado.structuredContent())
                         .valor());
   }

   @Test
   void deveCompararVeiculos() {
      CallToolResult resultado = executar(new CompararVeiculosTool(service),
            Map.of("veiculos",List.of(veiculo("11403","2020-1"),veiculo("11500","32000-1"))));

      assertTrue(texto(resultado).contains("Diferença: R$ 187.432,00 (100,00% a mais)"));
      assertEquals("0 km",assertInstanceOf(ComparacaoResponse.class,
                                           resultado.structuredContent()).maisCaro().ano());
   }

   @Test
   void deveInformarComparacaoImpossivel() {
      CallToolResult resultado = executar(new CompararVeiculosTool(service),
            Map.of("veiculos",List.of(veiculo("11403","2020-1"),veiculo("99999","2020-1"))));

      assertTrue(resultado.isError());
      assertTrue(texto(resultado).contains("Não foi possível comparar"));
   }

   // ===== conversão de erros em resultados ==================================

   @Test
   void deveDevolverErroDeArgumentoComoResultado() {
      CallToolResult resultado = executar(new ConsultarPrecoTool(service),
                                          Map.of("tipo","avioes"));

      assertTrue(resultado.isError());
      assertTrue(texto(resultado).contains("carros, motos, caminhoes"));
   }

   @Test
   void deveDevolverErroDaApiComoResultado() {
      provider.falharCom(new FipeIndisponivelException("API FIPE fora do ar."));

      CallToolResult resultado = executar(new ListarMarcasTool(service),
                                          Map.of("tipo","carros"));

      assertTrue(resultado.isError());
      assertEquals("API FIPE fora do ar.",texto(resultado));
   }

   @Test
   void deveOcultarDetalhesDeErroInesperado() {
      // provider nulo provoca um NullPointerException, simulando um defeito
      CallToolResult resultado = executar(new ListarMarcasTool(new FipeService(null)),
                                          Map.of("tipo","carros"));

      assertTrue(resultado.isError());
      assertEquals("Erro interno ao executar a ferramenta 'listar_marcas'.",
                   texto(resultado));
   }
}
