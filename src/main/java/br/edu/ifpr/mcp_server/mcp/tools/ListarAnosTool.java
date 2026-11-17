package br.edu.ifpr.mcp_server.mcp.tools;

import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.CODIGO_MODELO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.MARCA;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TIPO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.mapper.RespostaMapper;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;

/** Ferramenta listar_anos. */
public class ListarAnosTool extends AbstractFipeTool {

   public ListarAnosTool(FipeService service) {
      super(service);
   }

   @Override
   protected String nome() {
      return "listar_anos";
   }

   @Override
   protected String titulo() {
      return "Listar anos do modelo";
   }

   @Override
   protected String descricao() {
      return "Lista os anos-modelo disponíveis para um modelo da Tabela FIPE, " +
             "com o código de cada ano no formato ano-combustível (ex.: " +
             "\"2020-1\"). O ano 32000 indica veículo 0 km. Use o código do " +
             "ano em consultar_preco.";
   }

   @Override
   protected Map<String,Object> esquemaEntrada() {
      return ParametrosFipe.codigoModelo(
                ParametrosFipe.marca(ParametrosFipe.tipo(EsquemaEntrada.objeto())))
                           .build();
   }

   @Override
   protected RespostaFerramenta executar(ToolArguments argumentos) {
      TipoVeiculo     tipo   = argumentos.tipo(TIPO);
      String          marca  = argumentos.texto(MARCA);
      String          modelo = argumentos.texto(CODIGO_MODELO);
      List<AnoModelo> anos   = service.listarAnos(tipo,marca,modelo);

      return new RespostaFerramenta(texto(marca,modelo,anos),
                                    RespostaMapper.paraListaAnos(tipo,marca,
                                                                 modelo,anos));
   }

   private String texto(String marca, String modelo, List<AnoModelo> anos) {
      return "Anos disponíveis para o modelo " + modelo + " (marca " + marca +
             "):\n" +
             anos.stream()
                 .map(ano -> ano.codigo() + " – " + ano.descricaoExibicao())
                 .collect(Collectors.joining("\n"));
   }
}
