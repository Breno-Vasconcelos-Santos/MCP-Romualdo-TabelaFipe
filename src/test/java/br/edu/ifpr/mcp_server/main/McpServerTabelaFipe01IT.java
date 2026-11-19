package br.edu.ifpr.mcp_server.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.support.StubHttpServer;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testes de integração do servidor MCP da Tabela FIPE.
 *
 * O servidor é executado a partir do JAR gerado pelo profile "server", como
 * fazem o cliente, o Claude e o MCP Inspector, e as ferramentas são chamadas
 * pelo protocolo. A API FIPE é simulada por um servidor HTTP local (a
 * variável FIPE_BASE_URL aponta o servidor MCP para ele), o que torna os
 * testes independentes da internet e do limite diário de requisições.
 */
class McpServerTabelaFipe01IT {
    private static final Logger LOG = LoggerFactory.getLogger(McpServerTabelaFipe01IT.class);

    private static final String SERVER_PATH =
            System.getProperty("server.jar","server/mcp_tabela_fipe_server.jar");

    private static final String MODELOS = "/cars/brands/189/models";

    private static StubHttpServer api;
    private static McpSyncClient  client;

    @BeforeAll
    static void iniciar() throws IOException {
        api = StubHttpServer.iniciar();
        configurarApi();

        File serverJar = new File(SERVER_PATH);
        assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
                serverJar.getAbsolutePath());

        ServerParameters     params    = ServerParameters.builder("java")
                .args("-jar",serverJar.getAbsolutePath())
                .addEnvVar("FIPE_BASE_URL",
                        api.url().toString())
                .build();
        StdioClientTransport transport =
                new StdioClientTransport(params,new JacksonMcpJsonMapper(new JsonMapper()));

        transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] {}",line));

        client = McpClient.sync(transport).build();
        client.initialize();
    }

    // respostas reais da API (fixtures) para o Aston Martin DB12
    private static void configurarApi() {
        api.responderComFixture("/cars/brands",200,"marcas-carros.json");
        api.responderComFixture(MODELOS,200,"modelos-aston-martin.json");
        api.responderComFixture(MODELOS + "/11338/years",200,"anos-db12.json");
        api.responderComFixture(MODELOS + "/11338/years/2024-1",200,"preco-db12-2024.json");
        api.responder(MODELOS + "/11339/years/2024-1",200,
                "{\"price\":\"R$ 3.000.000,00\",\"brand\":\"ASTON MARTIN\"," +
                        "\"model\":\"DB12 Volante 4.0 V8 680cv\",\"modelYear\":2024," +
                        "\"fuel\":\"Gasolina\",\"codeFipe\":\"085019-5\"," +
                        "\"referenceMonth\":\"novembro de 2026\"}");
        api.responderComFixture(MODELOS + "/99999/years",404,"erro-codigo-inexistente.json");
        api.responder("/motorcycles/brands",500,"{}");
    }

    @AfterAll
    static void encerrar() {
        if (client != null)
            client.closeGracefully();
        if (api != null)
            api.close();
    }

    private CallToolResult chamar(String ferramenta, Map<String,Object> argumentos) {
        return client.callTool(CallToolRequest.builder(ferramenta).arguments(argumentos).build());
    }

    private String texto(CallToolResult resultado) {
        return ((TextContent) resultado.content().getFirst()).text();
    }

    private static Map<String,Object> db12(String modelo) {
        return Map.of("tipo","carros","marca","aston martin","codigoModelo",modelo,
                "codigoAno","2024-1");
    }

    // ===== disponibilização ==================================================

    @Test
    void deveDisponibilizarAsCincoFerramentas() {
        Set<String> nomes = client.listTools().tools().stream()
                .map(Tool::name).collect(Collectors.toSet());

        assertEquals(Set.of("listar_marcas","buscar_modelos","listar_anos",
                "consultar_preco","comparar_veiculos"),nomes);
    }

    // ===== execução de cada ferramenta =======================================

    @Test
    void deveListarMarcasFiltradas() {
        CallToolResult resultado = chamar("listar_marcas",Map.of("tipo","carros","termo","aston"));

        assertFalse(resultado.isError());
        assertTrue(texto(resultado).contains("189 – ASTON MARTIN"));
    }

    @Test
    void deveBuscarModelosResolvendoAMarcaPeloNome() {
        CallToolResult resultado = chamar("buscar_modelos",
                Map.of("tipo","carros","marca","aston martin",
                        "termo","db12 coupe"));

        assertFalse(resultado.isError());
        assertTrue(texto(resultado).contains("11338 – DB12 Coupe 4.0 V8 680cv"));
    }

    @Test
    void deveListarAnosExibindoZeroKm() {
        CallToolResult resultado = chamar("listar_anos",
                Map.of("tipo","carros","marca","aston martin",
                        "codigoModelo","11338"));

        assertTrue(texto(resultado).contains("32000-1 – 0 km Gasolina"));
    }

    @Test
    void deveConsultarPreco() {
        CallToolResult resultado = chamar("consultar_preco",db12("11338"));

        assertFalse(resultado.isError());
        assertTrue(texto(resultado).contains("Preço FIPE: R$ 2.918.759,00"));
        assertTrue(texto(resultado).contains("Código FIPE: 085018-7"));
    }

    @Test
    void deveCompararVeiculos() {
        CallToolResult resultado = chamar("comparar_veiculos",
                Map.of("veiculos",List.of(db12("11338"),db12("11339"))));

        assertFalse(resultado.isError());
        assertTrue(texto(resultado).contains("Diferença: R$ 81.241,00"));
    }

    // ===== erros devolvidos como resultado ===================================

    @Test
    void deveRecusarArgumentosForaDoEsquema() {
        // o SDK valida os argumentos contra o JSON Schema da ferramenta antes de
        // executá-la: aqui, um tipo fora das opções e parâmetros obrigatórios ausentes
        CallToolResult resultado = chamar("consultar_preco",Map.of("tipo","avioes"));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("caminhoes"));
        assertTrue(texto(resultado).contains("codigoAno"));
    }

    @Test
    void deveInformarAnoEmFormatoInvalidoSemConsultarAApi() {
        // argumentos válidos para o esquema, mas o código do ano não segue o
        // formato da FIPE: a regra é do serviço, que recusa antes de consultar a API
        CallToolResult resultado = chamar("consultar_preco",
                Map.of("tipo","carros","marca","aston martin",
                        "codigoModelo","11338","codigoAno","2024"));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("Código do ano em formato inválido"));
    }

    @Test
    void deveInformarModeloInexistente() {
        CallToolResult resultado = chamar("listar_anos",
                Map.of("tipo","carros","marca","aston martin",
                        "codigoModelo","99999"));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("Marca ou modelo não encontrado"));
    }

    @Test
    void deveInformarApiIndisponivel() {
        CallToolResult resultado = chamar("listar_marcas",Map.of("tipo","motos"));

        assertTrue(resultado.isError());
        assertTrue(texto(resultado).contains("HTTP 500"));
    }
}