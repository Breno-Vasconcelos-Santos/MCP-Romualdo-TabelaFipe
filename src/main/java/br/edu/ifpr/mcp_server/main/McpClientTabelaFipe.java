package br.edu.ifpr.mcp_server.main;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.client.ArgumentReader;
import br.edu.ifpr.mcp_server.exception.EntradaEncerradaException;
import br.edu.ifpr.mcp_server.client.ResultPrinter;
import br.edu.ifpr.mcp_server.client.Terminal;
import br.edu.ifpr.mcp_server.client.ToolMenu;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

// Cliente MCP da Tabela FIPE com transporte STDIO
public class McpClientTabelaFipe {
    private static final Logger LOG = LoggerFactory.getLogger(McpClientTabelaFipe.class);

    // Caminho do servidor a partir de target/dist-client, se nenhum for informado
    private static final String SERVIDOR_PADRAO = "../../server/mcp_tabela_fipe_server.jar";

    private final McpSyncClient  client;
    private final Terminal       terminal;
    private final ToolMenu       menu;
    private final ArgumentReader leitor;
    private final ResultPrinter  impressora;

    public McpClientTabelaFipe(String caminhoServidor, Terminal terminal) {
        this.client     = criarCliente(caminhoServidor);
        this.terminal   = terminal;
        this.menu       = new ToolMenu(terminal);
        this.leitor     = new ArgumentReader(terminal);
        this.impressora = new ResultPrinter(terminal);
    }

    private McpSyncClient criarCliente(String caminhoServidor) {
        File jar = new File(caminhoServidor);

        if (!jar.isFile())
            throw new IllegalStateException("JAR do servidor não encontrado: " +
                    jar.getAbsolutePath() + "\nInforme o " +
                    "caminho como argumento: java -jar " +
                    "mcp_tabela_fipe_client.jar " +
                    "<caminho do servidor>");
        LOG.debug("Servidor MCP: {}",jar.getAbsolutePath());

        ServerParameters     parametros = ServerParameters.builder("java")
                .args("-jar",
                        jar.getAbsolutePath())
                .build();
        StdioClientTransport transporte =
                new StdioClientTransport(parametros,
                        new JacksonMcpJsonMapper(new JsonMapper()));
        // o log do servidor só aparece com o nível DEBUG, para não poluir o menu
        transporte.setStdErrorHandler(linha -> LOG.debug("[SERVIDOR] {}",linha));

        return McpClient.sync(transporte)
                .loggingConsumer(this::exibirNotificacao)
                .build();
    }

    // notificações enviadas pelo servidor durante a execução das ferramentas
    private void exibirNotificacao(LoggingMessageNotification notificacao) {
        LOG.info("[NOTIFICAÇÃO MCP] Origem: {} | Nível: {} | Mensagem: {}",
                notificacao.logger(),notificacao.level(),notificacao.data());
    }

    // Executa o laço do menu até o usuário sair ou a entrada terminar
    public void executar() {
        client.initialize();
        client.setLoggingLevel(LoggingLevel.INFO);

        List<Tool> ferramentas = carregarFerramentas();
        try {
            while (true) {
                menu.exibir(ferramentas);
                String comando = terminal.ler("> ");

                if (comando.isEmpty())
                    continue;
                if (ToolMenu.SAIR.equalsIgnoreCase(comando))
                    return;
                if (ToolMenu.LISTAR.equalsIgnoreCase(comando)) {
                    ferramentas = carregarFerramentas();
                    continue;
                }
                Optional<Tool> ferramenta = menu.selecionar(ferramentas,comando);

                if (ferramenta.isPresent())
                    executarFerramenta(ferramenta.get());
                else
                    terminal.escrever("Ferramenta não encontrada: " + comando);
            }
        }
        catch (EntradaEncerradaException ex) {
            terminal.escrever("");
        }
    }

    private List<Tool> carregarFerramentas() {
        return client.listTools().tools();
    }

    private void executarFerramenta(Tool ferramenta) {
        menu.exibirCabecalho(ferramenta);

        Map<String,Object> argumentos = leitor.ler(ferramenta.inputSchema());
        try {
            impressora.exibir(client.callTool(CallToolRequest.builder(ferramenta.name())
                    .arguments(argumentos)
                    .build()));
        }
        catch (RuntimeException ex) {
            LOG.debug("Falha ao executar a ferramenta '{}'",ferramenta.name(),ex);
            terminal.escrever("Não foi possível executar a ferramenta: " +
                    ex.getMessage());
        }
    }

    public void encerrar() {
        client.closeGracefully();
    }

    public static void main(String... args) {
        String   caminhoServidor = args.length > 0 ? args[0] : SERVIDOR_PADRAO;
        Terminal terminal        = new Terminal(System.in,System.out);

        try {
            McpClientTabelaFipe cliente = new McpClientTabelaFipe(caminhoServidor,
                    terminal);
            try {
                cliente.executar();
            }
            finally {
                cliente.encerrar();
            }
        }
        catch (IllegalStateException ex) {
            terminal.escrever(ex.getMessage());
            System.exit(1);
        }
    }
}
