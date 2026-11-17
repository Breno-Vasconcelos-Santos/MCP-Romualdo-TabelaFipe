package br.edu.ifpr.mcp_server.mcp.utils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;
import br.edu.ifpr.mcp_server.model.TipoVeiculo;
import br.edu.ifpr.mcp_server.util.Textos;

/**
 * Acesso tipado e validado aos argumentos recebidos por uma ferramenta.
 *
 * Os argumentos chegam como um mapa JSON genérico; esta classe converte cada
 * valor no tipo esperado e lança ArgumentoInvalidoException, com mensagem
 * clara, quando o valor está ausente ou em formato incorreto.
 */
public final class ToolArguments {
   private final Map<String,Object> valores;

   public ToolArguments(Map<String,Object> valores) {
      this.valores = valores == null ? Map.of() : valores;
   }

   /**
    * @return o texto do parâmetro, sem espaços extras
    * @throws ArgumentoInvalidoException se o parâmetro estiver ausente ou vazio
    */
   public String texto(String nome) {
      String valor = textoOpcional(nome);

      if (valor == null)
         throw new ArgumentoInvalidoException("O parâmetro '" + nome +
                                              "' é obrigatório.");
      return valor;
   }

   /**
    * Aceita texto ou número (modelos de IA às vezes enviam códigos numéricos
    * como números, ex.: 11403 em vez de "11403").
    *
    * @return o texto do parâmetro, ou null se estiver ausente ou vazio
    */
   public String textoOpcional(String nome) {
      Object valor = valores.get(nome);

      if (valor == null)
         return null;
      if (valor instanceof Number numero)
         return new BigDecimal(numero.toString()).stripTrailingZeros()
                                                 .toPlainString();
      if (valor instanceof String texto) {
         String limpo = Textos.limparEspacos(texto);
         return limpo.isEmpty() ? null : limpo;
      }
      throw new ArgumentoInvalidoException("O parâmetro '" + nome +
                                           "' deve ser um texto.");
   }

   /**
    * @throws ArgumentoInvalidoException se o tipo estiver ausente ou inválido
    */
   public TipoVeiculo tipo(String nome) {
      return TipoVeiculo.deNome(textoOpcional(nome));
   }

   /**
    * @return cada item da lista como um novo conjunto de argumentos
    * @throws ArgumentoInvalidoException se o parâmetro estiver ausente, não
    *                                    for uma lista ou contiver itens que
    *                                    não sejam objetos
    */
   public List<ToolArguments> listaDeObjetos(String nome) {
      Object valor = valores.get(nome);

      if (valor == null)
         throw new ArgumentoInvalidoException("O parâmetro '" + nome +
                                              "' é obrigatório.");
      if (!(valor instanceof List<?> lista))
         throw new ArgumentoInvalidoException("O parâmetro '" + nome +
                                              "' deve ser uma lista.");
      return lista.stream()
                  .map(item -> paraArgumentos(nome,item))
                  .toList();
   }

   private static ToolArguments paraArgumentos(String nome, Object item) {
      if (!(item instanceof Map<?,?> mapa))
         throw new ArgumentoInvalidoException("Cada item de '" + nome +
                                              "' deve ser um objeto.");
      Map<String,Object> convertido = new LinkedHashMap<>();

      mapa.forEach((chave,valor) -> convertido.put(String.valueOf(chave),valor));
      return new ToolArguments(convertido);
   }
}
