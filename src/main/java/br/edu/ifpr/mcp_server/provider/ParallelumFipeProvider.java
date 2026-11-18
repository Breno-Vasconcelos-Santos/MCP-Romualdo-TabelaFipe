package br.edu.ifpr.mcp_server.provider;

import br.edu.ifpr.mcp_server.config.FipeConfig;
import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.*;
import br.edu.ifpr.mcp_server.dto.CodigoNomeDto;
import br.edu.ifpr.mcp_server.dto.ErroDto;
import br.edu.ifpr.mcp_server.dto.PrecoDto;
import br.edu.ifpr.mcp_server.mapper.FipeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

// Implementação do FipeProvider sobre a API da Parallelum
public class ParallelumFipeProvider implements FipeProvider {
   private static final Logger LOG =
                          LoggerFactory.getLogger(ParallelumFipeProvider.class);

   private static final String CABECALHO_TOKEN = "X-Subscription-Token";

   private static final int HTTP_OK                 = 200;
   private static final int HTTP_BAD_REQUEST        = 400;
   private static final int HTTP_NOT_FOUND          = 404;
   private static final int HTTP_TOO_MANY_REQUESTS  = 429;

   private final FipeConfig config;
   private final HttpClient httpClient;
   private final JsonMapper jsonMapper;

   public ParallelumFipeProvider(FipeConfig config) {
      this.config     = config;
      this.httpClient = HttpClient.newBuilder()
                                  .connectTimeout(config.timeout())
                                  .build();
      // campos não mapeados nos DTOs (ou incluídos futuramente pela API) não
      // devem impedir a leitura da resposta
      this.jsonMapper = JsonMapper.builder()
                                  .disable(DeserializationFeature
                                                .FAIL_ON_UNKNOWN_PROPERTIES)
                                  .build();
   }

   @Override
   public List<Marca> listarMarcas(TipoVeiculo tipo) {
      String corpo = consultar("Nenhuma marca encontrada para o tipo " +
                               tipo.nome() + ".",
                               tipo.caminhoApi(),"brands");

      return lerLista(corpo,FipeMapper::paraMarca);
   }

   @Override
   public List<Modelo> listarModelos(TipoVeiculo tipo, String codigoMarca) {
      String corpo = consultar("Marca não encontrada: código '" + codigoMarca +
                               "' (tipo " + tipo.nome() + ").",
                               tipo.caminhoApi(),"brands",codigoMarca,"models");

      return lerLista(corpo,FipeMapper::paraModelo);
   }

   @Override
   public List<AnoModelo> listarAnos(TipoVeiculo tipo, String codigoMarca,
                                     String codigoModelo) {
      String corpo = consultar("Marca ou modelo não encontrado: marca '" +
                               codigoMarca + "', modelo '" + codigoModelo +
                               "' (tipo " + tipo.nome() + ").",
                               tipo.caminhoApi(),"brands",codigoMarca,"models",
                               codigoModelo,"years");

      return lerLista(corpo,FipeMapper::paraAnoModelo);
   }

   @Override
   public PrecoVeiculo consultarPreco(TipoVeiculo tipo, String codigoMarca,
                                      String codigoModelo, String codigoAno) {
      String corpo = consultar("Ano '" + codigoAno + "' não disponível para o " +
                               "modelo '" + codigoModelo + "' da marca '" +
                               codigoMarca + "' (tipo " + tipo.nome() + "), " +
                               "ou marca/modelo inexistente.",
                               tipo.caminhoApi(),"brands",codigoMarca,"models",
                               codigoModelo,"years",codigoAno);

      return ler(corpo,json -> FipeMapper.paraPreco(tipo,
                                                    jsonMapper.readValue(json,
                                                             PrecoDto.class)));
   }

   // Executa um GET na API e devolve o corpo da resposta em caso de sucesso
   private String consultar(String mensagemNaoEncontrado, String... segmentos) {
      URI                  uri      = montarUri(segmentos);
      HttpResponse<String> resposta = enviar(uri);

      LOG.debug("GET {} -> HTTP {}",uri,resposta.statusCode());

      return switch (resposta.statusCode()) {
         case HTTP_OK                -> resposta.body();
         case HTTP_NOT_FOUND         ->
                       throw new RecursoNaoEncontradoException(mensagemNaoEncontrado);
         case HTTP_BAD_REQUEST       ->
                       throw new ArgumentoInvalidoException("A API FIPE recusou " +
                                                            "a consulta: " +
                                                   mensagemDeErro(resposta.body()));
         case HTTP_TOO_MANY_REQUESTS ->
                       throw new FipeIndisponivelException("Limite de requisições " +
                                                           "da API FIPE atingido. " +
                                                           "Tente novamente mais " +
                                                           "tarde.");
         default                     ->
                       throw new FipeIndisponivelException("A API FIPE respondeu " +
                                                           "com erro HTTP " +
                                                           resposta.statusCode() +
                                                           ".");
      };
   }

   private URI montarUri(String... segmentos) {
      StringBuilder url = new StringBuilder(config.baseUrl().toString());

      for (String segmento : segmentos)
         url.append('/').append(codificar(segmento));

      return URI.create(url.toString());
   }

   // codifica o segmento para a URL; o URLEncoder segue o padrão de
   // formulários, que usa '+' para espaço, inválido em caminhos
   private static String codificar(String segmento) {
      return URLEncoder.encode(segmento,StandardCharsets.UTF_8)
                       .replace("+","%20");
   }

   private HttpResponse<String> enviar(URI uri) {
      HttpRequest.Builder requisicao = HttpRequest.newBuilder(uri)
                                                  .timeout(config.timeout())
                                                  .header("Accept",
                                                          "application/json")
                                                  .GET();
      if (config.possuiToken())
         requisicao.header(CABECALHO_TOKEN,config.token());
      try {
         return httpClient.send(requisicao.build(),
                                HttpResponse.BodyHandlers.ofString(
                                                     StandardCharsets.UTF_8));
      }
      catch (IOException ex) {
         throw new FipeIndisponivelException("Não foi possível conectar à API " +
                                             "FIPE.",ex);
      }
      catch (InterruptedException ex) {
         // preserva o sinal de interrupção para quem estiver acima
         Thread.currentThread().interrupt();
         throw new FipeIndisponivelException("A consulta à API FIPE foi " +
                                             "interrompida.",ex);
      }
   }

   private <T> List<T> lerLista(String corpo,
                                Function<CodigoNomeDto,T> conversor) {
      return ler(corpo,json -> {
         CodigoNomeDto[] itens = jsonMapper.readValue(json,CodigoNomeDto[].class);

         if (itens == null)
            throw new IllegalArgumentException("Lista ausente na resposta.");

         return Arrays.stream(itens).map(conversor).toList();
      });
   }

   /**
    * Aplica a leitura ao corpo da resposta, convertendo JSON malformado ou
    * dados inconsistentes em FipeIndisponivelException
    */
   private <T> T ler(String corpo, Function<String,T> leitura) {
      try {
         return leitura.apply(corpo);
      }
      catch (JacksonException | IllegalArgumentException ex) {
         throw new FipeIndisponivelException("A API FIPE retornou uma resposta " +
                                             "em formato inesperado.",ex);
      }
   }

   // extrai a mensagem do corpo de erro; se não for possível, usa o corpo bruto
   private String mensagemDeErro(String corpo) {
      try {
         ErroDto erro = jsonMapper.readValue(corpo,ErroDto.class);

         if (erro != null && erro.error() != null)
            return erro.error();
      }
      catch (JacksonException ex) {
         LOG.debug("Corpo de erro em formato inesperado: {}",corpo);
      }
      return corpo;
   }
}
