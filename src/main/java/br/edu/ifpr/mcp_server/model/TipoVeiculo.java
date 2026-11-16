package br.edu.ifpr.mcp_server.model;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.util.Textos;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tipos de veículo da Tabela FIPE.
 * Concentra a tradução entre o nome informado pelo usuário e o
 * segmento usado nas URLs da API.
 */
public enum TipoVeiculo {
    CARROS("carros","cars"),
    MOTOS("motos","motorcycles"),
    CAMINHOES("caminhoes","trucks");

    private final String nome;
    private final String caminhoApi;

    TipoVeiculo(String nome, String caminhoApi) {
        this.nome       = nome;
        this.caminhoApi = caminhoApi;
    }

    //retorna o nome do tipo como o usuário o informa (ex.: "carros")
    public String nome() {
        return nome;
    }

    // retorna o segmento da URL da API para este tipo (ex.: "cars")
    public String caminhoApi() {
        return caminhoApi;
    }

    /**
     * Converte o nome informado pelo usuário no tipo correspondente,
     * desconsiderando maiúsculas, acentos e espaços extras.
     * @throws ArgumentoInvalidoException se o nome for vazio ou desconhecido
     */
    public static TipoVeiculo deNome(String nome) {
        String normalizado = Textos.normalizar(nome);

        if (normalizado.isEmpty())
            throw new ArgumentoInvalidoException("O tipo de veículo é obrigatório. " +
                    "Valores aceitos: " +
                    valoresAceitos() + ".");
        for (TipoVeiculo tipo : values())
            if (tipo.nome.equals(normalizado))
                return tipo;

        throw new ArgumentoInvalidoException("Tipo de veículo inválido: '" +
                Textos.limparEspacos(nome) +
                "'. Valores aceitos: " +
                valoresAceitos() + ".");
    }

    // retorna os nomes aceitos, separados por vírgula
    public static String valoresAceitos() {
        return Arrays.stream(values())
                .map(TipoVeiculo::nome)
                .collect(Collectors.joining(", "));
    }
}
