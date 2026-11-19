package br.edu.ifpr.mcp_server.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.exception.EntradaEncerradaException;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.mcp.utils.EsquemaEntrada;

/** Simula o que o usuário digita e verifica os argumentos montados a partir do esquema. */
class ArgumentReaderTest {

   private ArgumentReader leitor(String... linhas) {
      String entrada = String.join("\n",linhas) + "\n";

      return new ArgumentReader(new Terminal(
            new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(new ByteArrayOutputStream(),true,StandardCharsets.UTF_8)));
   }

   @Test
   void deveRepetirObrigatorioVazioEOmitirOpcionalVazio() {
      Map<String,Object> esquema = EsquemaEntrada.objeto()
                                                 .texto("marca","Marca",true)
                                                 .texto("termo","Termo",false)
                                                 .build();

      assertEquals(Map.of("marca","bmw"),leitor("","bmw","").ler(esquema));
   }

   @Test
   void deveEscolherOpcaoPeloNumeroOuPeloValor() {
      Map<String,Object> esquema =
            EsquemaEntrada.objeto()
                          .opcoes("tipo","Tipo",List.of("carros","motos"),true)
                          .opcoes("outro","Outro",List.of("carros","motos"),true)
                          .build();

      // "9" não é uma opção válida, e a pergunta se repete
      assertEquals(Map.of("tipo","motos","outro","carros"),
                   leitor("9","2","CARROS").ler(esquema));
   }

   @Test
   void deveConverterTiposSimplesRepetindoValoresInvalidos() {
      // LinkedHashMap: as perguntas seguem a ordem das propriedades, e o
      // Map.of não garante ordem nenhuma
      Map<String,Object> propriedades = new LinkedHashMap<>();
      propriedades.put("quantidade",Map.of("type","integer"));
      propriedades.put("fator",Map.of("type","number"));
      propriedades.put("ativo",Map.of("type","boolean"));
      Map<String,Object> esquema = Map.of("type","object","properties",propriedades,
                                          "required",List.of("quantidade","fator","ativo"));

      Map<String,Object> argumentos = leitor("abc","10","1,5","talvez","sim").ler(esquema);

      assertEquals(10L,argumentos.get("quantidade"));
      assertEquals(new BigDecimal("1.5"),argumentos.get("fator"));
      assertEquals(true,argumentos.get("ativo"));
   }

   @Test
   void deveLerListaDeObjetosItemAItemRespeitandoOsLimites() {
      EsquemaEntrada veiculo = EsquemaEntrada.objeto()
                                             .texto("marca","Marca",true)
                                             .texto("codigoModelo","Modelo",true);
      Map<String,Object> esquema = EsquemaEntrada.objeto()
                                                 .listaDeObjetos("veiculos","Veículos",
                                                                 veiculo,2,5,true)
                                                 .build();

      // "1" está abaixo do mínimo de 2 itens, e a pergunta se repete
      assertEquals(List.of(Map.of("marca","bmw","codigoModelo","11403"),
                           Map.of("marca","fiat","codigoModelo","9001")),
                   leitor("1","2","bmw","11403","fiat","9001").ler(esquema).get("veiculos"));
   }

   @Test
   void deveInterromperQuandoAEntradaTerminar() {
      Map<String,Object> esquema = EsquemaEntrada.objeto().texto("marca","Marca",true).build();

      assertThrows(EntradaEncerradaException.class,() -> leitor().ler(esquema));
   }
}
