package br.edu.ifpr.mcp_server.main;

import java.util.List;

import br.edu.ifpr.mcp_server.config.FipeConfig;
import br.edu.ifpr.mcp_server.mcp.tools.*;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.provider.CachingFipeProvider;
import br.edu.ifpr.mcp_server.provider.FipeProvider;
import br.edu.ifpr.mcp_server.provider.ParallelumFipeProvider;
import br.edu.ifpr.mcp_server.service.FipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servidor MCP da Tabela FIPE com transporte STDIO.
 * Esta classe é a raiz de composição da aplicação servidor: configura o
 * transporte, cria as ferramentas e as registra no servidor.
 */
public class McpServerTabelaFipe {
    private static final Logger LOG = LoggerFactory.getLogger(McpServerTabelaFipe.class);

    private static final String SERVER_NAME    = "MCP Server Tabela FIPE";
    private static final String SERVER_VERSION = "0.0.1";

    private final McpSyncServer server;

    public McpServerTabelaFipe() throws Exception {
        SyncSpecification<SingleSessionSyncSpecification> srv = configureMcpServer();

        registerTools(srv,createTools());
        this.server = srv.build();
        // encerra o servidor de forma ordenada quando o processo for finalizado
        Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));

        LOG.info("{} {} iniciado via STDIO.",SERVER_NAME,SERVER_VERSION);
    }

    private SyncSpecification<SingleSessionSyncSpecification> configureMcpServer() {
        // cria o objeto para comunicação entre servidor e cliente
        JacksonMcpJsonMapper         jsonMapper   =
                new JacksonMcpJsonMapper(new JsonMapper());
        // comunicação via entrada e saída padrão (STDIO)
        StdioServerTransportProvider transport    =
                new StdioServerTransportProvider(jsonMapper);
        // o servidor disponibiliza ferramentas e envia mensagens de 'logging'
        ServerCapabilities           capabilities = ServerCapabilities.builder()
                .tools(true)
                .logging()
                .build();
        Implementation               info         =
                Implementation.builder(SERVER_NAME,SERVER_VERSION)
                        .build();

        return McpServer.sync(transport)
                .serverInfo(info)
                .capabilities(capabilities);
    }

    /**
     * Cria as ferramentas disponibilizadas pelo servidor. Nas próximas etapas,
     * este método também montará as dependências das ferramentas (provider,
     * cache e serviço) e as injetará pelos construtores.
     */
    private List<McpToolDefinition> createTools() {
        FipeProvider provider = new CachingFipeProvider(
                new ParallelumFipeProvider(FipeConfig.doAmbiente()));
        FipeService  service  = new FipeService(provider);

        return List.of(new ListarMarcasTool(service),
                new BuscarModelosTool(service),
                new ListarAnosTool(service),
                new ConsultarPrecoTool(service),
                new CompararVeiculosTool(service));
    }

    private void registerTools(SyncSpecification<SingleSessionSyncSpecification> spec,
                               List<McpToolDefinition> tools) throws Exception {
        for (McpToolDefinition tool : tools)
            tool.register(spec);
    }

    public static void main(String[] args) throws Exception {
        new McpServerTabelaFipe();
    }
}
