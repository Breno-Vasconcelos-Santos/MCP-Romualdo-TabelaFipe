package br.edu.ifpr.mcp_server.mcp.tools;

import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.MARCA;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TERMO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TIPO;

import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.service.BuscaModelos;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.mapper.RespostaMapper;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;

// Ferramenta buscar_modelos
public class BuscarModelosTool extends AbstractFipeTool {

   public BuscarModelosTool(FipeService service) {
      super(service);
   }

   @Override
   protected String nome() {
      return "buscar_modelos";
   }

   @Override
   protected String titulo() {
      return "Buscar modelos";
   }

   @Override
   protected String descricao() {
      return "Busca os modelos de uma marca na Tabela FIPE cujo nome contenha " +
             "todas as palavras do termo, em qualquer ordem, sem diferenciar " +
             "maiúsculas e acentos. Retorna no máximo " +
             FipeService.LIMITE_MODELOS + " modelos com seus códigos; se " +
             "houver mais, refine o termo. Use o código do modelo em " +
             "listar_anos.";
   }

   @Override
   protected Map<String,Object> esquemaEntrada() {
      return ParametrosFipe.marca(ParametrosFipe.tipo(EsquemaEntrada.objeto()))
                           .texto(TERMO,"Palavras do nome do modelo (ex.: " +
                                        "\"320i sport\"). Opcional, mas " +
                                        "recomendado.",false)
                           .build();
   }

   @Override
   protected RespostaFerramenta executar(ToolArguments argumentos) {
      TipoVeiculo  tipo  = argumentos.tipo(TIPO);
      BuscaModelos busca = service.buscarModelos(tipo,argumentos.texto(MARCA),
                                                 argumentos.textoOpcional(TERMO));

      return new RespostaFerramenta(texto(busca),
                                    RespostaMapper.paraBuscaModelos(tipo,busca));
   }

   private String texto(BuscaModelos busca) {
      String marca  = "Marca: " + busca.marca().nome() + " (código " +
                      busca.marca().codigo() + ")\n";
      String filtro = busca.termo().isEmpty() ? "" : " com \"" + busca.termo() + "\"";

      if (busca.modelos().isEmpty())
         return marca + "Nenhum modelo" + filtro + " encontrado.";

      String texto = busca.totalEncontrado() + marca + "Modelos" + filtro + ":\n" +
                     busca.modelos().stream()
                          .map(modelo -> modelo.codigo() + " – " + modelo.nome())
                          .collect(Collectors.joining("\n"));
      if (busca.limitada())
         texto += "\nExibindo " + busca.modelos().size() + " de " +
                  busca.totalEncontrado() + ". Refine o termo para ver os demais.";
      return texto;
   }
}
