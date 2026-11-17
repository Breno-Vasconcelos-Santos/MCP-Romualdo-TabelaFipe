package br.edu.ifpr.mcp_server.config;

import br.edu.ifpr.mcp_server.util.Textos;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * Configuração de acesso à API FIPE.
 * Os valores vêm de variáveis de ambiente, todas opcionais:
 * FIPE_BASE_URL (endereço da API, usado principalmente pelos testes) e
 * FIPE_TOKEN (token de acesso, que eleva o limite diário de requisições).
 *
 * @param baseUrl endereço base da API, sem barra no final
 * @param token   token de acesso, ou null se não houver
 * @param timeout tempo máximo de espera de cada requisição
 */
public record FipeConfig(URI baseUrl, String token, Duration timeout) {
   public static final String   URL_PADRAO     =
                                         "https://fipe.parallelum.com.br/api/v2";
   public static final Duration TIMEOUT_PADRAO = Duration.ofSeconds(10);

   static final String VARIAVEL_BASE_URL = "FIPE_BASE_URL";
   static final String VARIAVEL_TOKEN    = "FIPE_TOKEN";

   public FipeConfig {
      if (baseUrl == null || timeout == null)
         throw new IllegalArgumentException("Endereço da API e timeout são " +
                                            "obrigatórios.");
      // padroniza o endereço sem a barra final para montar as URLs
      String url = baseUrl.toString();
      if (url.endsWith("/"))
         baseUrl = URI.create(url.substring(0,url.length() - 1));

      token = Textos.limparEspacos(token);
      if (token.isEmpty())
         token = null;
   }

   /** @return true se um token de acesso foi configurado */
   public boolean possuiToken() {
      return token != null;
   }

   /** Cria a configuração a partir das variáveis de ambiente do processo. */
   public static FipeConfig doAmbiente() {
      return deVariaveis(System.getenv());
   }

   /**
    * Cria a configuração a partir de um mapa de variáveis. Separado de
    * doAmbiente() para permitir testes sem alterar o ambiente real.
    *
    * @throws IllegalArgumentException se FIPE_BASE_URL não for uma URL válida
    */
   static FipeConfig deVariaveis(Map<String,String> variaveis) {
      String url = Textos.limparEspacos(variaveis.get(VARIAVEL_BASE_URL));

      if (url.isEmpty())
         url = URL_PADRAO;
      try {
         return new FipeConfig(URI.create(url),variaveis.get(VARIAVEL_TOKEN),
                               TIMEOUT_PADRAO);
      }
      catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException(VARIAVEL_BASE_URL + " inválida: " +
                                            url,ex);
      }
   }
}
