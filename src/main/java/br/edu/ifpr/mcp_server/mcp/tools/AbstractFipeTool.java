package br.edu.ifpr.mcp_server.mcp.tools;

import java.util.Map;

import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.exception.FipeException;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;
import br.edu.ifpr.mcp_server.mcp.utils.ToolResults;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.ToolAnnotations;

/**
 * Base das ferramentas FIPE (padrão Template Method).
 *
 * Define o algoritmo comum a todas as ferramentas: registrar-se no servidor,
 * ler os argumentos, executar e converter o resultado ou o erro para o
 * protocolo MCP. Cada ferramenta concreta informa apenas o que a diferencia:
 * nome, descrição, parâmetros e o que fazer ao ser executada.
 */
public abstract class AbstractFipeTool implements McpToolDefinition {
   private static final Logger LOG = LoggerFactory.getLogger(AbstractFipeTool.class);

   protected final FipeService service;

   protected AbstractFipeTool(FipeService service) {
      this.service = service;
   }

   /** @return o nome da ferramenta no protocolo (ex.: "consultar_preco") */
   protected abstract String nome();

   /** @return um título curto para exibição */
   protected abstract String titulo();

   /** @return a descrição lida pelo modelo de IA para decidir quando usá-la */
   protected abstract String descricao();

   /** @return o JSON Schema dos parâmetros */
   protected abstract Map<String,Object> esquemaEntrada();

   /** Executa a ferramenta com os argumentos já encapsulados. */
   protected abstract RespostaFerramenta executar(ToolArguments argumentos);

   /** @return a definição da ferramenta no formato do protocolo */
   public Tool definicao() {
      return Tool.builder(nome(),esquemaEntrada())
                 .title(titulo())
                 .description(descricao())
                 // as ferramentas só consultam dados (não alteram nada) e
                 // dependem de um serviço externo (a API FIPE)
                 .annotations(ToolAnnotations.builder()
                                             .readOnlyHint(true)
                                             .openWorldHint(true)
                                             .build())
                 .build();
   }

   @Override
   public final void register(SyncSpecification<SingleSessionSyncSpecification> mcp) {
      mcp.tools(SyncToolSpecification.builder()
                                     .tool(definicao())
                                     .callHandler(this::run)
                                     .build());
   }

   /**
    * Executa a ferramenta. Erros esperados (FipeException) viram resultados de
    * erro com a mensagem original; erros inesperados viram uma mensagem
    * genérica e são registrados no log com o detalhe completo.
    */
   @Override
   public final CallToolResult run(McpSyncServerExchange exchange,
                                   CallToolRequest request) {
      try {
         return ToolResults.sucesso(executar(new ToolArguments(request.arguments())));
      }
      catch (FipeException ex) {
         LOG.info("Ferramenta '{}' não concluída: {}",nome(),ex.getMessage());
         return ToolResults.erro(ex.getMessage());
      }
      catch (RuntimeException ex) {
         LOG.error("Erro inesperado na ferramenta '{}'",nome(),ex);
         return ToolResults.erro("Erro interno ao executar a ferramenta '" +
                                 nome() + "'.");
      }
   }
}
