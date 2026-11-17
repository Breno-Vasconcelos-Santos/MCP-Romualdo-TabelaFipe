package br.edu.ifpr.mcp_server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.exception.RecursoNaoEncontradoException;
import br.edu.ifpr.mcp_server.model.AnoModelo;
import br.edu.ifpr.mcp_server.model.ComparacaoVeiculos;
import br.edu.ifpr.mcp_server.model.FalhaConsulta;
import br.edu.ifpr.mcp_server.model.Marca;
import br.edu.ifpr.mcp_server.model.Modelo;
import br.edu.ifpr.mcp_server.model.PrecoVeiculo;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.model.VeiculoConsulta;
import br.edu.ifpr.mcp_server.provider.FipeProvider;
import br.edu.ifpr.mcp_server.util.Textos;

// Casos de uso de consulta à Tabela FIPE
public class FipeService {
   public static final int LIMITE_MODELOS   = 30;
   public static final int MINIMO_COMPARACAO = 2;
   public static final int MAXIMO_COMPARACAO = 5;

   private static final String APENAS_DIGITOS = "\\d+";

   private final FipeProvider provider;

   public FipeService(FipeProvider provider) {
      this.provider = provider;
   }

   // Lista as marcas de um tipo, opcionalmente filtradas por um termo
   public List<Marca> listarMarcas(TipoVeiculo tipo, String termo) {
      return filtrar(provider.listarMarcas(exigirTipo(tipo)),Marca::nome,termo);
   }

   /**
    * Identifica a marca a partir do código ou do nome informado.
    * A ordem de tentativa é: código (somente dígitos), nome exato e nome que
    * contenha todas as palavras informadas. Maiúsculas, acentos e espaços
    * extras são desconsiderados
    */
   public Marca resolverMarca(TipoVeiculo tipo, String marca) {
      String      procurada = exigirInformado(marca,"marca");
      List<Marca> marcas    = provider.listarMarcas(exigirTipo(tipo));

      if (procurada.matches(APENAS_DIGITOS))
         return porCodigo(marcas,procurada)
                     .orElseThrow(() -> new RecursoNaoEncontradoException(
                                  "Marca de código '" + procurada +
                                  "' não encontrada para o tipo " +
                                  tipo.nome() + "."));

      return porNomeExato(marcas,procurada)
                  .orElseGet(() -> porCorrespondenciaUnica(tipo,marcas,procurada));
   }

   /**
    * Busca os modelos de uma marca que contenham as palavras do termo. Sem
    * termo, considera todos os modelos. O resultado é limitado a
    * {@link #LIMITE_MODELOS} itens
    */
   public BuscaModelos buscarModelos(TipoVeiculo tipo, String marca,
                                     String termo) {
      Marca        marcaResolvida = resolverMarca(tipo,marca);
      List<Modelo> encontrados    = filtrar(provider.listarModelos(tipo,
                                                       marcaResolvida.codigo()),
                                            Modelo::nome,termo);

      return new BuscaModelos(marcaResolvida,Textos.limparEspacos(termo),
                              encontrados.stream().limit(LIMITE_MODELOS).toList(),
                              encontrados.size());
   }

   // Lista os anos-modelo disponíveis para um modelo
   public List<AnoModelo> listarAnos(TipoVeiculo tipo, String marca,
                                     String codigoModelo) {
      String modelo = exigirInformado(codigoModelo,"código do modelo");

      return provider.listarAnos(tipo,resolverMarca(tipo,marca).codigo(),modelo);
   }

   // Consulta o preço de um veículo
   public PrecoVeiculo consultarPreco(TipoVeiculo tipo, String marca,
                                      String codigoModelo, String codigoAno) {
      String modelo = exigirInformado(codigoModelo,"código do modelo");
      String ano    = exigirInformado(codigoAno,"código do ano");

      if (!AnoModelo.formatoValido(ano))
         throw new ArgumentoInvalidoException("Código do ano em formato " +
                                              "inválido: '" + ano + "'. Use " +
                                              "o formato ano-combustível, " +
                                              "como 2024-1.");
      return provider.consultarPreco(tipo,resolverMarca(tipo,marca).codigo(),
                                     modelo,ano);
   }

   //Consulta e compara os preços de 2 a 5 veículos.
   public ComparacaoVeiculos compararVeiculos(List<VeiculoConsulta> veiculos) {
      int quantidade = veiculos == null ? 0 : veiculos.size();

      if (quantidade < MINIMO_COMPARACAO || quantidade > MAXIMO_COMPARACAO)
         throw new ArgumentoInvalidoException("Informe de " + MINIMO_COMPARACAO +
                                              " a " + MAXIMO_COMPARACAO +
                                              " veículos para comparar " +
                                              "(recebidos: " + quantidade + ").");
      List<PrecoVeiculo>  precos = new ArrayList<>();
      List<FalhaConsulta> falhas = new ArrayList<>();

      for (VeiculoConsulta veiculo : veiculos) {
         try {
            precos.add(consultar(veiculo));
         }
         catch (ArgumentoInvalidoException | RecursoNaoEncontradoException ex) {
            falhas.add(new FalhaConsulta(veiculo,ex.getMessage()));
         }
      }
      return new ComparacaoVeiculos(precos,falhas);
   }

   private PrecoVeiculo consultar(VeiculoConsulta veiculo) {
      if (veiculo == null)
         throw new ArgumentoInvalidoException("Veículo não informado.");

      return consultarPreco(veiculo.tipo(),veiculo.marca(),
                            veiculo.codigoModelo(),veiculo.codigoAno());
   }

   private Optional<Marca> porCodigo(List<Marca> marcas, String codigo) {
      return marcas.stream()
                   .filter(marca -> marca.codigo().equals(codigo))
                   .findFirst();
   }

   private Optional<Marca> porNomeExato(List<Marca> marcas, String nome) {
      String procurado = Textos.normalizar(nome);

      return marcas.stream()
                   .filter(marca -> Textos.normalizar(marca.nome())
                                          .equals(procurado))
                   .findFirst();
   }

   private Marca porCorrespondenciaUnica(TipoVeiculo tipo, List<Marca> marcas,
                                         String nome) {
      List<Marca> candidatas = filtrar(marcas,Marca::nome,nome);

      if (candidatas.size() == 1)
         return candidatas.getFirst();

      if (candidatas.isEmpty())
         throw new RecursoNaoEncontradoException("Marca '" + nome + "' não " +
                                                 "encontrada para o tipo " +
                                                 tipo.nome() + ". Consulte " +
                                                 "as marcas disponíveis.");

      throw new ArgumentoInvalidoException("A marca '" + nome + "' é ambígua. " +
                                           "Marcas encontradas:\n" +
                                           listar(candidatas) +
                                           "\nInforme o código ou o nome " +
                                           "completo.");
   }

   private static <T> List<T> filtrar(List<T> itens, Function<T,String> nome,
                                      String termo) {
      return itens.stream()
                  .filter(item -> Textos.contemPalavras(nome.apply(item),termo))
                  .toList();
   }

   private static String listar(List<Marca> marcas) {
      return marcas.stream()
                   .map(marca -> marca.codigo() + " – " + marca.nome())
                   .collect(Collectors.joining("\n"));
   }

   private static TipoVeiculo exigirTipo(TipoVeiculo tipo) {
      if (tipo == null)
         throw new ArgumentoInvalidoException("O tipo de veículo é obrigatório. " +
                                              "Valores aceitos: " +
                                              TipoVeiculo.valoresAceitos() + ".");
      return tipo;
   }

   private static String exigirInformado(String valor, String parametro) {
      String limpo = Textos.limparEspacos(valor);

      if (limpo.isEmpty())
         throw new ArgumentoInvalidoException("O parâmetro '" + parametro +
                                              "' é obrigatório.");
      return limpo;
   }
}
