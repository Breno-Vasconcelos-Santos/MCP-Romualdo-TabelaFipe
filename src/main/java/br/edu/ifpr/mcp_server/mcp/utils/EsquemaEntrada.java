package br.edu.ifpr.mcp_server.mcp.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Builder do JSON Schema que descreve os parâmetros de uma ferramenta
public final class EsquemaEntrada {
   private final Map<String,Object> propriedades = new LinkedHashMap<>();
   private final List<String>       obrigatorios = new ArrayList<>();

   private EsquemaEntrada() {}

   public static EsquemaEntrada objeto() {
      return new EsquemaEntrada();
   }

   public EsquemaEntrada texto(String nome, String descricao,
                               boolean obrigatorio) {
      return adicionar(nome,Map.of("type","string","description",descricao),
                       obrigatorio);
   }

   // Texto restrito a uma lista de valores (enum do JSON Schema)
   public EsquemaEntrada opcoes(String nome, String descricao,
                                List<String> valores, boolean obrigatorio) {
      return adicionar(nome,Map.of("type","string","description",descricao,
                                   "enum",List.copyOf(valores)),
                       obrigatorio);
   }

   // Lista de objetos, cada um descrito por outro esquema
   public EsquemaEntrada listaDeObjetos(String nome, String descricao,
                                        EsquemaEntrada itens, int minimo,
                                        int maximo, boolean obrigatorio) {
      return adicionar(nome,Map.of("type","array","description",descricao,
                                   "items",itens.build(),
                                   "minItems",minimo,"maxItems",maximo),
                       obrigatorio);
   }

   public Map<String,Object> build() {
      Map<String,Object> esquema = new LinkedHashMap<>();

      esquema.put("type","object");
      esquema.put("properties",new LinkedHashMap<>(propriedades));
      esquema.put("required",List.copyOf(obrigatorios));
      esquema.put("additionalProperties",false);
      return esquema;
   }

   private EsquemaEntrada adicionar(String nome, Map<String,Object> definicao,
                                    boolean obrigatorio) {
      propriedades.put(nome,definicao);
      if (obrigatorio)
         obrigatorios.add(nome);
      return this;
   }
}
