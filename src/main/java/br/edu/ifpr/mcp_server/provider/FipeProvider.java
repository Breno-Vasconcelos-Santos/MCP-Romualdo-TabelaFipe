package br.edu.ifpr.mcp_server.provider;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.*;

import java.util.List;

/**
 * Fonte de dados da Tabela FIPE.
 * O restante da aplicação depende apenas desta interface, e não de uma API
 * específica (Princípio da Inversão de Dependência).
 *
 * Todos os métodos podem lançar:
 * {@link RecursoNaoEncontradoException} quando o item consultado não existe;
 * {@link ArgumentoInvalidoException} quando a fonte rejeita a requisição;
 * {@link FipeIndisponivelException} quando a fonte não pode ser consultada.
 */
public interface FipeProvider {

   List<Marca> listarMarcas(TipoVeiculo tipo);

   List<Modelo> listarModelos(TipoVeiculo tipo, String codigoMarca);

   List<AnoModelo> listarAnos(TipoVeiculo tipo, String codigoMarca,
                              String codigoModelo);

   PrecoVeiculo consultarPreco(TipoVeiculo tipo, String codigoMarca,
                               String codigoModelo, String codigoAno);
}
