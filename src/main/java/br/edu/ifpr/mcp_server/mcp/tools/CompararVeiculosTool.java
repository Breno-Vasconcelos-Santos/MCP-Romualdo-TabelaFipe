package br.edu.ifpr.mcp_server.mcp.tools;

import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.CODIGO_ANO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.CODIGO_MODELO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.MARCA;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.TIPO;
import static br.edu.ifpr.mcp_server.mcp.tools.ParametrosFipe.VEICULOS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.model.ComparacaoVeiculos;
import br.edu.ifpr.mcp_server.model.FalhaConsulta;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.VeiculoConsulta;
import br.edu.ifpr.mcp_server.service.FipeService;
import br.edu.ifpr.mcp_server.mcp.mapper.RespostaMapper;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;
import br.edu.ifpr.mcp_server.mcp.utils.Formatos;
import br.edu.ifpr.mcp_server.mcp.utils.RespostaFerramenta;
import br.edu.ifpr.mcp_server.mcp.utils.ToolArguments;

/** Ferramenta comparar_veiculos. */
public class CompararVeiculosTool extends AbstractFipeTool {

   public CompararVeiculosTool(FipeService service) {
      super(service);
   }

   @Override
   protected String nome() {
      return "comparar_veiculos";
   }

   @Override
   protected String titulo() {
      return "Comparar veículos";
   }

   @Override
   protected String descricao() {
      return "Compara os preços FIPE de " + FipeService.MINIMO_COMPARACAO +
             " a " + FipeService.MAXIMO_COMPARACAO + " veículos, indicando o " +
             "mais barato, o mais caro e a diferença em reais e em percentual. " +
             "Cada veículo exige tipo, marca, código do modelo (de " +
             "buscar_modelos) e código do ano (de listar_anos). Veículos não " +
             "encontrados são informados sem interromper a comparação.";
   }

   @Override
   protected Map<String,Object> esquemaEntrada() {
      return EsquemaEntrada.objeto()
                           .listaDeObjetos(VEICULOS,"Veículos a comparar.",
                                           ParametrosFipe.veiculo(),
                                           FipeService.MINIMO_COMPARACAO,
                                           FipeService.MAXIMO_COMPARACAO,true)
                           .build();
   }

   @Override
   protected RespostaFerramenta executar(ToolArguments argumentos) {
      List<VeiculoConsulta> veiculos   = argumentos.listaDeObjetos(VEICULOS)
                                                   .stream()
                                                   .map(this::paraConsulta)
                                                   .toList();
      ComparacaoVeiculos    comparacao = service.compararVeiculos(veiculos);

      if (!comparacao.comparavel())
         throw new ArgumentoInvalidoException("Não foi possível comparar: menos " +
                                              "de " +
                                              ComparacaoVeiculos.MINIMO_PARA_COMPARAR +
                                              " veículos foram encontrados." +
                                              textoFalhas(comparacao.falhas()));
      return new RespostaFerramenta(texto(comparacao),
                                    RespostaMapper.paraComparacao(comparacao));
   }

   private VeiculoConsulta paraConsulta(ToolArguments veiculo) {
      return new VeiculoConsulta(veiculo.tipo(TIPO),veiculo.textoOpcional(MARCA),
                                 veiculo.textoOpcional(CODIGO_MODELO),
                                 veiculo.textoOpcional(CODIGO_ANO));
   }

   private String texto(ComparacaoVeiculos comparacao) {
      PrecoVeiculo barato = comparacao.maisBarato().orElseThrow();
      PrecoVeiculo caro   = comparacao.maisCaro().orElseThrow();
      List<PrecoVeiculo> precos = comparacao.precos();

      StringBuilder texto = new StringBuilder("Comparação de " + precos.size() +
                                              " veículos (referência: " +
                                              precos.getFirst().mesReferencia() +
                                              "):\n");
      for (int i = 0; i < precos.size(); i++)
         texto.append(i + 1).append(". ")
              .append(ConsultarPrecoTool.descreverVeiculo(precos.get(i)))
              .append(": ").append(Formatos.moeda(precos.get(i).valor()))
              .append('\n');

      texto.append("Mais barato: ").append(ConsultarPrecoTool.descreverVeiculo(barato))
           .append('\n')
           .append("Mais caro: ").append(ConsultarPrecoTool.descreverVeiculo(caro))
           .append('\n')
           .append("Diferença: ")
           .append(Formatos.moeda(comparacao.diferenca().orElseThrow()));
      comparacao.diferencaPercentual()
                .ifPresent(percentual -> texto.append(" (")
                                              .append(Formatos.percentual(percentual))
                                              .append(" a mais)"));
      texto.append(textoFalhas(comparacao.falhas()));
      return texto.toString();
   }

   private String textoFalhas(List<FalhaConsulta> falhas) {
      if (falhas.isEmpty())
         return "";

      return "\nNão consultados:\n" +
             falhas.stream()
                   .map(falha -> "- " + falha.motivo())
                   .collect(Collectors.joining("\n"));
   }
}
