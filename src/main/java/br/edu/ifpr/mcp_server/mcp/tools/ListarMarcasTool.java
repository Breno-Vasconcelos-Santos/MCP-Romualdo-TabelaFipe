package br.edu.ifpr.mcp_server.mcp.tools;

import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TERMO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TIPO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.mapper.RespostaMapper;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;

// Ferramenta listar_marcas
public class ListarMarcasTool extends AbstractFipeTool {

   public ListarMarcasTool(FipeService service) {
      super(service);
   }

   @Override
   protected String nome() {
      return "listar_marcas";
   }

   @Override
   protected String titulo() {
      return "Listar marcas";
   }

   @Override
   protected String descricao() {
      return "Lista as marcas de veículos da Tabela FIPE para um tipo de " +
             "veículo, opcionalmente filtradas por um termo. Normalmente não " +
             "é necessária: as demais ferramentas aceitam o nome da marca " +
             "diretamente.";
   }

   @Override
   protected Map<String,Object> esquemaEntrada() {
      return ParametrosFipe.tipo(EsquemaEntrada.objeto())
                           .texto(TERMO,"Parte do nome da marca (opcional).",false)
                           .build();
   }

   @Override
   protected RespostaFerramenta executar(ToolArguments argumentos) {
      TipoVeiculo tipo   = argumentos.tipo(TIPO);
      String      termo  = argumentos.textoOpcional(TERMO);
      List<Marca> marcas = service.listarMarcas(tipo,termo);

      return new RespostaFerramenta(texto(tipo,termo,marcas),
                                    RespostaMapper.paraListaMarcas(tipo,termo,
                                                                   marcas));
   }

   private String texto(TipoVeiculo tipo, String termo, List<Marca> marcas) {
      String filtro = termo == null ? "" : " com \"" + termo + "\"";

      if (marcas.isEmpty())
         return "Nenhuma marca de " + tipo.nome() + filtro + " encontrada.";

      return "Marcas de " + tipo.nome() + filtro + " (" + marcas.size() + "):\n" +
             marcas.stream()
                   .map(marca -> marca.codigo() + " – " + marca.nome())
                   .collect(Collectors.joining("\n"));
   }
}
