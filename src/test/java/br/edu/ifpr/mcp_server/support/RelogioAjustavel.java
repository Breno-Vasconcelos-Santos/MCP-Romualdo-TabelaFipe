package br.edu.ifpr.mcp_server.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio para testes cujo horário só muda quando o teste pede, permitindo
 * simular a passagem do tempo sem esperar.
 */
public class RelogioAjustavel extends Clock {
   private Instant agora = Instant.parse("2026-11-16T12:00:00Z");

   public void avancar(Duration intervalo) {
      agora = agora.plus(intervalo);
   }

   @Override
   public Instant instant() {
      return agora;
   }

   @Override
   public ZoneId getZone() {
      return ZoneOffset.UTC;
   }

   @Override
   public Clock withZone(ZoneId zona) {
      return this;
   }
}
