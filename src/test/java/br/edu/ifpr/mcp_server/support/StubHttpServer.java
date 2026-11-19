package br.edu.ifpr.mcp_server.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Servidor HTTP local que simula a API FIPE nos testes.
 *
 * Cada caminho recebe uma resposta configurada (status e corpo); caminhos não
 * configurados respondem 404, como a API real. As requisições recebidas ficam
 * registradas para verificação.
 */
public final class StubHttpServer implements AutoCloseable {
   private static final String JSON_ROTA_DESCONHECIDA =
                               "{\"error\":\"rota não configurada no stub\"}";

   /** Requisição recebida pelo stub. */
   public record Requisicao(String caminho, String token) {}

   private record Resposta(int status, String corpo) {}

   private final HttpServer                server;
   private final Map<String,Resposta>      respostas   = new ConcurrentHashMap<>();
   private final List<Requisicao>          requisicoes = new CopyOnWriteArrayList<>();

   private StubHttpServer(HttpServer server) {
      this.server = server;
   }

   /** Inicia o stub em uma porta livre escolhida pelo sistema. */
   public static StubHttpServer iniciar() throws IOException {
      HttpServer     server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
      StubHttpServer stub   = new StubHttpServer(server);

      server.createContext("/",stub::atender);
      server.start();
      return stub;
   }

   /** @return o endereço base do stub (ex.: http://127.0.0.1:54321) */
   public URI url() {
      return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
   }

   public void responder(String caminho, int status, String corpo) {
      respostas.put(caminho,new Resposta(status,corpo));
   }

   public void responderComFixture(String caminho, int status, String fixture) {
      responder(caminho,status,Fixtures.ler(fixture));
   }

   public Requisicao ultimaRequisicao() {
      return requisicoes.getLast();
   }

   @Override
   public void close() {
      server.stop(0);
   }

   private void atender(HttpExchange troca) throws IOException {
      String caminho = troca.getRequestURI().getRawPath();

      requisicoes.add(new Requisicao(caminho,
                                     troca.getRequestHeaders()
                                          .getFirst("X-Subscription-Token")));

      Resposta resposta = respostas.getOrDefault(caminho,
                                 new Resposta(404,JSON_ROTA_DESCONHECIDA));
      byte[]   corpo    = resposta.corpo().getBytes(StandardCharsets.UTF_8);

      troca.getResponseHeaders().add("Content-Type",
                                     "application/json; charset=utf-8");
      troca.sendResponseHeaders(resposta.status(),corpo.length);
      try (OutputStream saida = troca.getResponseBody()) {
         saida.write(corpo);
      }
   }
}
