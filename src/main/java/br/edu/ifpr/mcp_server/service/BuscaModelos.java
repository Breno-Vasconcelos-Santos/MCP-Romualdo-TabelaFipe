package br.edu.ifpr.mcp_server.service;

import java.util.List;

import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;

// Resultado de uma busca de modelos
public record BuscaModelos(Marca marca,
                           String termo,
                           List<Modelo> modelos,
                           int totalEncontrado) {
   public BuscaModelos {
      modelos = List.copyOf(modelos);
   }

   // retorna true se havia mais modelos do que os devolvidos
   public boolean limitada() {
      return modelos.size() < totalEncontrado;
   }
}
