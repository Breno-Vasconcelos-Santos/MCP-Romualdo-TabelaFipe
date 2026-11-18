package br.edu.ifpr.mcp_server.mcp.mapper;

import java.util.List;

import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.ComparacaoVeiculos;
import br.edu.ifpr.mcp_server.model.FalhaConsulta;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.model.VeiculoConsulta;
import br.edu.ifpr.mcp_server.service.BuscaModelos;
import br.edu.ifpr.mcp_server.mcp.dto.AnoResponse;
import br.edu.ifpr.mcp_server.mcp.dto.BuscaModelosResponse;
import br.edu.ifpr.mcp_server.mcp.dto.ComparacaoResponse;
import br.edu.ifpr.mcp_server.mcp.dto.FalhaResponse;
import br.edu.ifpr.mcp_server.mcp.dto.ListaAnosResponse;
import br.edu.ifpr.mcp_server.mcp.dto.ListaMarcasResponse;
import br.edu.ifpr.mcp_server.mcp.dto.PrecoResponse;
import br.edu.ifpr.mcp_server.mcp.utils.Formatos;

// Converte os objetos do modelo nos DTOs de saída das ferramentas.
public final class RespostaMapper {
   private RespostaMapper() {}

   public static ListaMarcasResponse paraListaMarcas(TipoVeiculo tipo,
                                                     String termo,
                                                     List<Marca> marcas) {
      return new ListaMarcasResponse(tipo.nome(),termo,marcas.size(),marcas);
   }

   public static BuscaModelosResponse paraBuscaModelos(TipoVeiculo tipo,
                                                       BuscaModelos busca) {
      return new BuscaModelosResponse(tipo.nome(),busca.marca(),busca.termo(),
                                      busca.totalEncontrado(),
                                      busca.modelos().size(),busca.limitada(),
                                      busca.modelos());
   }

   public static ListaAnosResponse paraListaAnos(TipoVeiculo tipo, String marca,
                                                 String codigoModelo,
                                                 List<AnoModelo> anos) {
      return new ListaAnosResponse(tipo.nome(),marca,codigoModelo,
                                   anos.stream().map(RespostaMapper::paraAno)
                                       .toList());
   }

   public static AnoResponse paraAno(AnoModelo ano) {
      return new AnoResponse(ano.codigo(),ano.descricaoExibicao(),
                             Formatos.ano(ano.ano()),ano.zeroKm());
   }

   public static PrecoResponse paraPreco(PrecoVeiculo preco) {
      return new PrecoResponse(preco.tipo().nome(),preco.marca(),preco.modelo(),
                               Formatos.ano(preco.anoModelo()),
                               preco.combustivel(),preco.codigoFipe(),
                               preco.valor(),Formatos.moeda(preco.valor()),
                               preco.mesReferencia());
   }

   public static ComparacaoResponse paraComparacao(ComparacaoVeiculos comparacao) {
      return new ComparacaoResponse(
            comparacao.precos().stream().map(RespostaMapper::paraPreco).toList(),
            comparacao.falhas().stream().map(RespostaMapper::paraFalha).toList(),
            comparacao.maisBarato().map(RespostaMapper::paraPreco).orElse(null),
            comparacao.maisCaro().map(RespostaMapper::paraPreco).orElse(null),
            comparacao.diferenca().orElse(null),
            comparacao.diferenca().map(Formatos::moeda).orElse(null),
            comparacao.diferencaPercentual().orElse(null));
   }

   private static FalhaResponse paraFalha(FalhaConsulta falha) {
      VeiculoConsulta veiculo = falha.veiculo();

      if (veiculo == null)
         return new FalhaResponse(null,null,null,null,falha.motivo());

      return new FalhaResponse(veiculo.tipo() == null ? null : veiculo.tipo().nome(),
                               veiculo.marca(),veiculo.codigoModelo(),
                               veiculo.codigoAno(),falha.motivo());
   }
}
