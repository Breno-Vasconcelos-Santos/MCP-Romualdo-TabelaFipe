package br.edu.ifpr.mcp_server.mcp.tools;

import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.CODIGO_ANO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.CODIGO_MODELO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.MARCA;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TIPO;

import java.util.Map;

import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.mapper.RespostaMapper;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;
import br.edu.ifpr.mcp_server.mcp.utils.Formatos;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;

/** Ferramenta consultar_preco. */
public class ConsultarPrecoTool extends AbstractFipeTool {

   public ConsultarPrecoTool(FipeService service) {
      super(service);
   }

   @Override
   protected String nome() {
      return "consultar_preco";
   }

   @Override
   protected String titulo() {
      return "Consultar preço FIPE";
   }

   @Override
   protected String descricao() {
      return "Consulta o preço de um veículo na Tabela FIPE do mês de " +
             "referência atual. Requer o código do modelo (de buscar_modelos) " +
             "e o código do ano (de listar_anos).";
   }

   @Override
   protected Map<String,Object> esquemaEntrada() {
      return ParametrosFipe.veiculo().build();
   }

   @Override
   protected RespostaFerramenta executar(ToolArguments argumentos) {
      PrecoVeiculo preco = service.consultarPreco(argumentos.tipo(TIPO),
                                                  argumentos.texto(MARCA),
                                                  argumentos.texto(CODIGO_MODELO),
                                                  argumentos.texto(CODIGO_ANO));

      return new RespostaFerramenta(texto(preco),RespostaMapper.paraPreco(preco));
   }

   static String descreverVeiculo(PrecoVeiculo preco) {
      return preco.marca() + " " + preco.modelo() + " – " +
             Formatos.ano(preco.anoModelo()) + " " + preco.combustivel();
   }

   private String texto(PrecoVeiculo preco) {
      return descreverVeiculo(preco) + "\n" +
             "Preço FIPE: " + Formatos.moeda(preco.valor()) + "\n" +
             "Código FIPE: " + preco.codigoFipe() + "\n" +
             "Referência: " + preco.mesReferencia();
   }
}
