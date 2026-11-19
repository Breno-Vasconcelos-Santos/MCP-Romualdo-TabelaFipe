package br.edu.ifpr.mcp_server.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Leitura interativa dos argumentos de uma ferramenta
public class ArgumentReader {
   private static final String RECUO = "   ";

   private static final Set<String> TIPOS_SIMPLES =
           Set.of("string","integer","number","boolean");

   private final Terminal   terminal;
   private final JsonMapper json = new JsonMapper();

   public ArgumentReader(Terminal terminal) {
      this.terminal = terminal;
   }

   // retorna os argumentos informados, na ordem dos parâmetros
   public Map<String,Object> ler(Map<String,Object> esquema) {
      if (esquema == null)
         return Map.of();
      return lerObjeto(esquema,"");
   }

   private Map<String,Object> lerObjeto(Map<?,?> esquema, String recuo) {
      Map<String,Object> valores      = new LinkedHashMap<>();
      List<?>            obrigatorios = lista(esquema.get("required"));

      mapa(esquema.get("properties")).forEach((nome,definicao) -> {
         Object valor = lerParametro(String.valueOf(nome),mapa(definicao),
                 obrigatorios.contains(nome),recuo);
         if (valor != null)
            valores.put(String.valueOf(nome),valor);
      });
      return valores;
   }

   private Object lerParametro(String nome, Map<?,?> definicao,
                               boolean obrigatorio, String recuo) {
      String  tipo   = tipo(definicao);
      List<?> opcoes = lista(definicao.get("enum"));

      // a descrição do parâmetro já diz ao usuário o que informar; sem ela,
      // usa-se o nome do parâmetro
      terminal.escrever("");
      terminal.escrever(recuo + texto(definicao.get("description"),nome) +
              (obrigatorio ? "" : " (Enter para pular)"));

      return switch (tipo) {
         case "array"  -> lerLista(definicao,obrigatorio,recuo);
         case "object" -> definicao.containsKey("properties")
                 ? lerObjeto(definicao,recuo + RECUO)
                 : lerValor(tipo,List.of(),obrigatorio,recuo);
         default       -> lerValor(tipo,opcoes,obrigatorio,recuo);
      };
   }

   // lê um valor simples, repetindo a pergunta até obter um valor válido
   private Object lerValor(String tipo, List<?> opcoes, boolean obrigatorio,
                           String recuo) {
      exibirOpcoes(opcoes,recuo);
      if ("boolean".equals(tipo))
         terminal.escrever(recuo + "(responda sim ou não)");
      else if (!TIPOS_SIMPLES.contains(tipo))
         terminal.escrever(recuo + "(informe o valor em JSON)");

      while (true) {
         String entrada = terminal.ler(recuo + "> ");

         if (entrada.isEmpty()) {
            if (!obrigatorio)
               return null;
            terminal.escrever(recuo + "Este campo é obrigatório.");
            continue;
         }
         try {
            return opcoes.isEmpty() ? converter(entrada,tipo)
                    : escolher(entrada,opcoes);
         }
         catch (IllegalArgumentException ex) {
            terminal.escrever(recuo + "Valor inválido: " + ex.getMessage());
         }
      }
   }

   private Object converter(String entrada, String tipo) {
      return switch (tipo) {
         case "string"  -> entrada;
         case "integer" -> Long.valueOf(entrada);
         case "number"  -> new BigDecimal(entrada.replace(',','.'));
         case "boolean" -> booleano(entrada);
         default        -> json(entrada);
      };
   }

   private List<Object> lerLista(Map<?,?> definicao, boolean obrigatorio,
                                 String recuo) {
      Map<?,?>     itens      = mapa(definicao.get("items"));
      int          quantidade = lerQuantidade(definicao,obrigatorio,recuo);
      List<Object> valores    = new ArrayList<>();

      if (quantidade == 0)
         return null;

      for (int i = 1; i <= quantidade; i++) {
         terminal.escrever("");
         terminal.escrever(recuo + "Item " + i + " de " + quantidade);
         valores.add("object".equals(tipo(itens))
                 ? lerObjeto(itens,recuo + RECUO)
                 : lerValor(tipo(itens),lista(itens.get("enum")),true,
                 recuo + RECUO));
      }
      return valores;
   }

   private int lerQuantidade(Map<?,?> definicao, boolean obrigatorio,
                             String recuo) {
      int minimo = inteiro(definicao.get("minItems"),obrigatorio ? 1 : 0);
      int maximo = inteiro(definicao.get("maxItems"),Integer.MAX_VALUE);
      String faixa = maximo == Integer.MAX_VALUE ? "mínimo " + minimo
              : minimo + " a " + maximo;
      while (true) {
         String entrada = terminal.ler(recuo + "Quantidade (" + faixa + "): ");

         if (entrada.isEmpty() && !obrigatorio)
            return 0;
         try {
            int quantidade = Integer.parseInt(entrada);

            if (quantidade >= minimo && quantidade <= maximo)
               return quantidade;
         }
         catch (NumberFormatException ex) {
            // tratado abaixo, com a mesma mensagem
         }
         terminal.escrever(recuo + "Informe um número entre " + faixa + ".");
      }
   }

   private void exibirOpcoes(List<?> opcoes, String recuo) {
      for (int i = 0; i < opcoes.size(); i++)
         terminal.escrever(recuo + "  " + (i + 1) + " - " + opcoes.get(i));
   }

   // aceita o número da opção ou o próprio valor, sem diferenciar maiúsculas
   private static Object escolher(String entrada, List<?> opcoes) {
      for (int i = 0; i < opcoes.size(); i++)
         if (entrada.equals(String.valueOf(i + 1)) ||
                 entrada.equalsIgnoreCase(String.valueOf(opcoes.get(i))))
            return opcoes.get(i);

      throw new IllegalArgumentException("escolha um número de 1 a " +
              opcoes.size() + " ou um dos valores " +
              "listados.");
   }

   private static Boolean booleano(String entrada) {
      return switch (entrada.toLowerCase()) {
         case "sim","s","true"        -> true;
         case "não","nao","n","false" -> false;
         default -> throw new IllegalArgumentException("informe sim ou não.");
      };
   }

   private Object json(String entrada) {
      try {
         return json.readValue(entrada,Object.class);
      }
      catch (JacksonException ex) {
         throw new IllegalArgumentException("JSON inválido.");
      }
   }

   private static String texto(Object valor, String padrao) {
      return valor == null ? padrao : String.valueOf(valor);
   }

   private static String tipo(Map<?,?> definicao) {
      Object tipo = definicao.get("type");
      return tipo == null ? "string" : String.valueOf(tipo);
   }

   private static Map<?,?> mapa(Object valor) {
      return valor instanceof Map<?,?> mapa ? mapa : Map.of();
   }

   private static List<?> lista(Object valor) {
      return valor instanceof List<?> lista ? lista : List.of();
   }

   private static int inteiro(Object valor, int padrao) {
      return valor instanceof Number numero ? numero.intValue() : padrao;
   }
}
