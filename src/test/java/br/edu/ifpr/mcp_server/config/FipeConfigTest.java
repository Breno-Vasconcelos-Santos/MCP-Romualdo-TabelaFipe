package br.edu.ifpr.mcp_server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** No mesmo pacote do FipeConfig porque usa o método deVariaveis (package-private). */
class FipeConfigTest {

   @Test
   void deveUsarValoresPadraoSemVariaveisDeAmbiente() {
      FipeConfig config = FipeConfig.deVariaveis(Map.of());

      assertEquals(URI.create(FipeConfig.URL_PADRAO),config.baseUrl());
      assertFalse(config.possuiToken());
   }

   @Test
   void deveUsarUrlETokenInformados() {
      FipeConfig config = FipeConfig.deVariaveis(
            Map.of(FipeConfig.VARIAVEL_BASE_URL,"http://localhost:8080/",
                   FipeConfig.VARIAVEL_TOKEN," abc123 "));

      assertEquals(URI.create("http://localhost:8080"),config.baseUrl());
      assertTrue(config.possuiToken());
      assertEquals("abc123",config.token());
   }

   @Test
   void deveRecusarUrlInvalida() {
      IllegalArgumentException ex =
            assertThrows(IllegalArgumentException.class,
                         () -> FipeConfig.deVariaveis(
                               Map.of(FipeConfig.VARIAVEL_BASE_URL,"http://a b")));

      assertTrue(ex.getMessage().contains(FipeConfig.VARIAVEL_BASE_URL));
   }
}
