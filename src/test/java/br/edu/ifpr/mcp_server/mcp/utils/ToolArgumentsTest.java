package br.edu.ifpr.mcp_server.mcp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_server.exception.ArgumentoInvalidoException;

class ToolArgumentsTest {

   @Test
   void deveLerTextoAceitandoCodigosEnviadosComoNumero() {
      ToolArguments argumentos = new ToolArguments(Map.of("marca","  BMW ",
                                                          "inteiro",11403,
                                                          "decimal",11403.0));

      assertEquals("BMW",argumentos.texto("marca"));
      assertEquals("11403",argumentos.texto("inteiro"));
      assertEquals("11403",argumentos.texto("decimal"));
   }

   @Test
   void deveExigirParametroObrigatorio() {
      ToolArguments argumentos = new ToolArguments(Map.of("marca","   "));

      assertThrows(ArgumentoInvalidoException.class,() -> argumentos.texto("marca"));
      assertThrows(ArgumentoInvalidoException.class,() -> argumentos.texto("ausente"));
      assertNull(argumentos.textoOpcional("ausente"));
   }

   @Test
   void deveLerListaDeObjetosERecusarFormatoInvalido() {
      List<ToolArguments> itens = new ToolArguments(
            Map.of("veiculos",List.of(Map.of("marca","bmw"),Map.of("marca","fiat"))))
            .listaDeObjetos("veiculos");

      assertEquals("fiat",itens.get(1).texto("marca"));
      assertThrows(ArgumentoInvalidoException.class,
                   () -> new ToolArguments(Map.of("veiculos","[]")).listaDeObjetos("veiculos"));
      assertThrows(ArgumentoInvalidoException.class,
                   () -> new ToolArguments(Map.of("veiculos",List.of("bmw")))
                               .listaDeObjetos("veiculos"));
   }
}
