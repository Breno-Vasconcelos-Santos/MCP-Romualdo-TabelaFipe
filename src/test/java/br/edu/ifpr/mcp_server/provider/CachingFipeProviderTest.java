package br.edu.ifpr.mcp_server.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.exception.FipeIndisponivelException;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.support.FakeFipeProvider;
import br.edu.ifpr.mcp_server.support.RelogioAjustavel;

class CachingFipeProviderTest {
   private static final Duration VALIDADE = Duration.ofHours(6);

   private FakeFipeProvider    fonte;
   private RelogioAjustavel    relogio;
   private CachingFipeProvider cache;

   @BeforeEach
   void preparar() {
      fonte   = new FakeFipeProvider()
                      .comMarcas(TipoVeiculo.CARROS,new Marca("7","BMW"))
                      .comMarcas(TipoVeiculo.MOTOS,new Marca("81","BMW"));
      relogio = new RelogioAjustavel();
      cache   = new CachingFipeProvider(fonte,VALIDADE,relogio);
   }

   @Test
   void deveConsultarAFonteApenasNaPrimeiraVez() {
      cache.listarMarcas(TipoVeiculo.CARROS);
      cache.listarMarcas(TipoVeiculo.CARROS);

      assertEquals(List.of("listarMarcas:CARROS"),fonte.chamadas());
   }

   @Test
   void deveSepararResultadosPorChave() {
      assertEquals("7",cache.listarMarcas(TipoVeiculo.CARROS).getFirst().codigo());
      assertEquals("81",cache.listarMarcas(TipoVeiculo.MOTOS).getFirst().codigo());
      assertEquals(2,fonte.chamadas().size());
   }

   @Test
   void deveConsultarNovamenteAposExpirar() {
      cache.listarMarcas(TipoVeiculo.CARROS);

      relogio.avancar(VALIDADE.minusSeconds(1));
      cache.listarMarcas(TipoVeiculo.CARROS);          // ainda válido
      relogio.avancar(Duration.ofSeconds(1));
      cache.listarMarcas(TipoVeiculo.CARROS);          // expirou

      assertEquals(2,fonte.chamadas().size());
   }

   @Test
   void naoDeveGuardarErros() {
      fonte.falharCom(new FipeIndisponivelException("fora do ar"));
      assertThrows(FipeIndisponivelException.class,
                   () -> cache.listarMarcas(TipoVeiculo.CARROS));

      fonte.normalizar();
      cache.listarMarcas(TipoVeiculo.CARROS);

      assertEquals(2,fonte.chamadas().size());
   }
}
