package br.edu.ifpr.mcp_server.mcp.tools;

import java.util.Arrays;

import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;

/**
 * Nomes e descrições dos parâmetros comuns às ferramentas FIPE.
 *
 * Centraliza as definições para que todas as ferramentas descrevam o mesmo
 * parâmetro da mesma forma (DRY).
 */
final class ParametrosFipe {
   static final String TIPO          = "tipo";
   static final String MARCA         = "marca";
   static final String TERMO         = "termo";
   static final String CODIGO_MODELO = "codigoModelo";
   static final String CODIGO_ANO    = "codigoAno";
   static final String VEICULOS      = "veiculos";

   private ParametrosFipe() {}

   static EsquemaEntrada tipo(EsquemaEntrada esquema) {
      return esquema.opcoes(TIPO,"Tipo de veículo: " + TipoVeiculo.valoresAceitos() +
                                 ".",
                            Arrays.stream(TipoVeiculo.values())
                                  .map(TipoVeiculo::nome).toList(),
                            true);
   }

   static EsquemaEntrada marca(EsquemaEntrada esquema) {
      return esquema.texto(MARCA,"Código ou nome da marca (ex.: \"7\" ou " +
                                 "\"BMW\"). Não diferencia maiúsculas e " +
                                 "acentos.",true);
   }

   static EsquemaEntrada codigoModelo(EsquemaEntrada esquema) {
      return esquema.texto(CODIGO_MODELO,"Código do modelo, obtido com " +
                                         "buscar_modelos (ex.: \"11403\").",true);
   }

   static EsquemaEntrada codigoAno(EsquemaEntrada esquema) {
      return esquema.texto(CODIGO_ANO,"Código do ano-modelo, obtido com " +
                                      "listar_anos (ex.: \"2020-1\").",true);
   }

   /** Esquema completo de um veículo: tipo, marca, modelo e ano. */
   static EsquemaEntrada veiculo() {
      return codigoAno(codigoModelo(marca(tipo(EsquemaEntrada.objeto()))));
   }
}
