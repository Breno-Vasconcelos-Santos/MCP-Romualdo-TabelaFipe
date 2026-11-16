package br.edu.ifpr.mcp_server.model;

import java.math.BigDecimal;

// Preço de um veículo na Tabela FIPE em um mês de referência.
public record PrecoVeiculo(TipoVeiculo tipo,
                           String marca,
                           String modelo,
                           int anoModelo,
                           String combustivel,
                           String codigoFipe,
                           BigDecimal valor,
                           String mesReferencia) {
    public PrecoVeiculo {
        Validacoes.exigirNaoNulo(tipo,"tipo do veículo");
        marca         = Validacoes.exigirTexto(marca,"marca");
        modelo        = Validacoes.exigirTexto(modelo,"modelo");
        combustivel   = Validacoes.exigirTexto(combustivel,"combustível");
        codigoFipe    = Validacoes.exigirTexto(codigoFipe,"código FIPE");
        mesReferencia = Validacoes.exigirTexto(mesReferencia,"mês de referência");
        Validacoes.exigirNaoNulo(valor,"valor");

        if (anoModelo <= 0)
            throw new IllegalArgumentException("Ano do modelo inválido: " +
                    anoModelo + ".");
        if (valor.signum() < 0)
            throw new IllegalArgumentException("O valor não pode ser negativo: " +
                    valor + ".");
    }

    // returna true se o preço corresponde a um veículo 0 km
    public boolean zeroKm() {
        return AnoModelo.ehZeroKm(anoModelo);
    }
}
