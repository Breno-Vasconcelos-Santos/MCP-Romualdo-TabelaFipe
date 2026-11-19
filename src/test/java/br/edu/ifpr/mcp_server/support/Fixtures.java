package br.edu.ifpr.mcp_server.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Leitura das respostas reais da API FIPE salvas em src/test/resources/fixtures.
 */
public final class Fixtures {
   private Fixtures() {}

   public static String ler(String nome) {
      String caminho = "/fixtures/" + nome;

      try (InputStream entrada = Fixtures.class.getResourceAsStream(caminho)) {
         if (entrada == null)
            throw new IllegalArgumentException("Fixture não encontrada: " + caminho);

         return new String(entrada.readAllBytes(),StandardCharsets.UTF_8);
      }
      catch (IOException ex) {
         throw new UncheckedIOException(ex);
      }
   }
}
