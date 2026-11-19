<!--
Markdown Cheatsheet
https://github.com/adam-p/markdown-here/wiki/markdown-cheatsheet
-->

# MCP Server Tabela FIPE 📃🚗
 
Servidor e cliente MCP ([Model Context Protocol](https://modelcontextprotocol.io)) em Java para consultar a **Tabela FIPE** de carros, motos e caminhões. O servidor disponibiliza cinco ferramentas que permitem a um assistente de IA ou a uma pessoa, pelo cliente do terminal, ir do nome de um veículo até o seu preço FIPE, e comparar preços entre veículos.
 
Os dados vêm da [API FIPE da Parallelum](https://github.com/deividfortuna/fipe), que não exige cadastro nem token.
 
Trabalho final da disciplina **Serviços Web** (IFPR), desenvolvido a partir da aplicação exemplo apresentada em aula, com o [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) e transporte **STDIO**.
 
## Sumário
 
1. [Pré-requisitos](#1-pré-requisitos)
2. [Importação no Eclipse](#2-importação-no-eclipse)
3. [Builds: testes e artefatos](#3-builds-testes-e-artefatos)
4. [Arquitetura](#4-arquitetura)
5. [Decisões de projeto](#5-decisões-de-projeto)
6. [Cliente MCP do projeto](#6-cliente-mcp-do-projeto)
7. [Claude Desktop](#7-claude-desktop)
8. [MCP Inspector](#8-mcp-inspector)
9. [Outros assistentes: Claude Code](#9-outros-assistentes-claude-code)
10. [Ferramentas](#10-ferramentas)
11. [Testes](#11-testes)
12. [Possíveis problemas comuns](#12-possíveis-problemas-comuns)
---
 
## 1. Pré-requisitos
 
| Requisito | Versão | Observação |
| --- | --- | --- |
| JDK | 25 | o projeto é compilado com `release 25` |
| Apache Maven | 3.9 ou superior | confira com `mvn -v` |
| Eclipse IDE | com suporte a Java 25 | para a importação descrita na seção 2 |
| Docker | Docker Desktop ou Docker Engine com Compose | para executar o MCP Inspector |
| Acesso à internet | — | o servidor consulta a API FIPE (os testes não precisam) |
 
### Variáveis de ambiente (opcionais)
 
| Variável | Padrão | Uso |
| --- | --- | --- |
| `FIPE_BASE_URL` | `https://fipe.parallelum.com.br/api/v2` | endereço da API; usada pelos testes de integração para apontar o servidor para uma API simulada |
| `FIPE_TOKEN` | — | token da API, que eleva o limite de 500 para 1.000 requisições por dia, mas não é necessário |
 
---
 
## 2. Importação no Eclipse
 
1. **File → Import → Maven → Existing Maven Projects**.
2. Em **Root Directory**, selecione a pasta do projeto  que contém o `pom.xml` e clique em **Finish**.
Os arquivos de configuração do Eclipse (`.project`, `.classpath`, `.settings`) não estão no repositório de propósito: o Eclipse os gera a partir do `pom.xml` durante a importação, já com o Java 25 e a codificação UTF-8.
 
---
 
## 3. Builds: testes e artefatos
 
Há quatro builds, cada um com um objetivo. Pela linha de comando, execute-os na raiz do projeto; no Eclipse, use o conteúdo da coluna **Goals**.
 
| Objetivo | Linha de comando | Goals no Eclipse | Resultado |
| --- | --- | --- | --- |
| Testes unitários | `mvn test` | `test` | relatório em `target/surefire-reports` |
| Servidor + testes de integração | `mvn clean verify -Pserver` | `clean verify -Pserver` | servidor em `server/` e relatório em `target/failsafe-reports` |
| Apenas o servidor | `mvn clean package -Pserver -DskipTests` | `clean package -Pserver -DskipTests` | `server/mcp_tabela_fipe_server.jar` e `server/libs/` |
| Apenas o cliente | `mvn package -Pclient -DskipTests` | `package -Pclient -DskipTests` | `target/dist-client/mcp_tabela_fipe_client.jar` e `target/dist-client/libs/` |
 
**Ordem:** gere o servidor antes do cliente. O build do servidor com `clean` apaga a pasta `target/`, e com ela o cliente; nesse caso, gere o cliente de novo.
 
**O jar do servidor depende da pasta `libs/` ao lado dele.** Se for copiá-lo para outro lugar, copie a pasta `server/` inteira.
 
Os testes não acessam a internet: a API FIPE é simulada por um servidor HTTP local (seção 11).
 
### Executando no Eclipse
 
1. Clique com o botão direito no projeto → **Run As → Maven build...**
2. Em **Goals**, informe o valor da tabela para o build desejado.
3. Clique em **Run**.
O Eclipse guarda cada configuração criada. Nas próximas vezes, escolha-a na lista que se abre pela seta ao lado do botão **Run** da barra de ferramentas.
 
### Aviso de acesso nativo (opcional)
 
A partir do Java 24, o Maven exibe avisos como `WARNING: A restricted method in java.lang.System has been called`. Eles vêm de bibliotecas internas do próprio Maven, não deste projeto, e **não impedem nenhum build**. Para ocultá-los no Eclipse, há duas opções:
 
- **Uma vez, para todos os builds:** **Window → Preferences → Java → Installed JREs**, selecione o JDK 25, clique em **Edit** e informe `--enable-native-access=ALL-UNNAMED` em **Default VM arguments**.
- **Por configuração:** na tela do **Maven build...**, aba **JRE**, campo **VM arguments**, informe o mesmo valor. Nesse caso, o ajuste vale apenas para aquela configuração e precisa ser repetido em cada uma.
---
 
## 4. Arquitetura
 
O projeto é organizado em camadas. Cada camada conhece apenas a de baixo.
 
```
 Cliente MCP / Claude Desktop / Claude Code / MCP Inspector
                      │  JSON-RPC via STDIO
                      ▼
 mcp.tools        ferramentas (AbstractFipeTool + 5 ferramentas)
                      │
 service          FipeService: resolve a marca, filtra, limita, valida, compara
                      │
 provider         FipeProvider (interface)
                    └─ CachingFipeProvider (cache em memória)
                         └─ ParallelumFipeProvider (HTTP) ── dto + mapper
                      │
 model            conceitos da Tabela FIPE e suas regras
```
 
### Pacotes (`br.edu.ifpr.mcp_server`)
 
| Pacote | Camada | Responsabilidade |
| --- | --- | --- |
| `main` | aplicações | `McpServerTabelaFipe` e `McpClientTabelaFipe`: os pontos de entrada (`main`) do servidor e do cliente; o servidor também monta as dependências |
| `client` | cliente | `ToolMenu`, `ArgumentReader`, `ResultPrinter` e `Terminal`: o menu, a leitura dos parâmetros a partir do esquema de cada ferramenta e a exibição dos resultados |
| `mcp.tools`, `mcp.utils` | protocolo MCP | as ferramentas e sua estrutura comum: traduzem as chamadas do protocolo em chamadas ao serviço, e o resultado em texto e JSON |
| `mcp.dto`, `mcp.mapper` | protocolo MCP | o formato dos dados estruturados devolvidos pelas ferramentas e a conversão do modelo para ele |
| `service` | regras de uso | `FipeService`: resolver a marca pelo nome, filtrar, limitar, validar as entradas e comparar veículos |
| `provider` | acesso a dados | `FipeProvider` (interface), `ParallelumFipeProvider` (requisições HTTP) e `CachingFipeProvider` (cache em memória) |
| `dto`, `mapper` | integração | o formato do JSON da API da Parallelum e a conversão para o modelo |
| `model` | domínio | os conceitos da Tabela FIPE (tipo, marca, modelo, ano, preço, comparação) e suas regras |
| `exception`, `util`, `config` | apoio | erros da aplicação, operações com texto e configuração de acesso à API |
 
### O caminho de uma chamada
 
Quando um assistente chama `consultar_preco`:
 
1. O SDK recebe a mensagem pelo stdin e chama o método `run` da ferramenta.
2. A `AbstractFipeTool` encapsula os argumentos num `ToolArguments` e chama o `executar` da ferramenta.
3. A ferramenta converte o tipo e chama o `FipeService`, que valida as entradas e resolve a marca pelo nome.
4. O serviço consulta o `FipeProvider`. O cache devolve o dado da memória ou repassa ao `ParallelumFipeProvider`, que faz a requisição HTTP e converte o JSON no modelo.
5. A ferramenta monta o texto e os dados estruturados; o `run` os converte no resultado do protocolo, enviado pelo stdout.
Qualquer erro esperado (uma `FipeException`) sobe até o `run`, que o devolve como resultado com `isError: true`.
 
### Padrões de projeto e princípios
 
| Padrão ou princípio | Onde |
| --- | --- |
| Template Method | `AbstractFipeTool`: `register` e `run` são fixos; cada ferramenta define nome, descrição, esquema e execução |
| Decorator | `CachingFipeProvider` acrescenta cache ao `ParallelumFipeProvider` sem modificá-lo |
| Adapter | `ParallelumFipeProvider` e os mappers traduzem a API externa para o modelo e o modelo para a saída |
| Builder | `EsquemaEntrada` monta o JSON Schema das ferramentas |
| Factory | `ToolResults` e `TipoVeiculo.deNome` |
 
---
 
## 5. Decisões de projeto
 
 
**Marca por nome.** A API aceita apenas o código da marca. O serviço resolve o nome digitado sobre a lista de marcas (que fica em cache), o que dispensa uma chamada a `listar_marcas` antes de cada consulta.
 
**Filtro, limite e cache para listas grandes.** Uma marca pode ter centenas de modelos (a BMW tem mais de 300). A busca exige todas as palavras do termo, devolve no máximo 30 itens com o total encontrado, e as listas ficam em cache por 6 horas. Com isso, novas buscas na mesma marca não fazem requisições.
 
**Erros como resultado, e não como exceção.** O assistente de IA lê a mensagem e reage: corrige o tipo, pergunta qual marca o usuário quis dizer ou escolhe outro ano.
 
**Texto e dados estruturados.** Cada resultado traz um texto legível (para pessoas e para o modelo de IA) e o JSON (`structuredContent`) para programas. O cliente do terminal exibe apenas o texto.
 
**Dinheiro em `BigDecimal`.** O preço da API chega como texto (`R$ 2.918.759,00`) e é convertido para `BigDecimal`, evitando os erros de arredondamento do `double` no cálculo das diferenças.
 
**Cliente genérico.** O cliente monta as perguntas a partir do JSON Schema de cada ferramenta, inclusive listas de objetos (lidas item a item, por recursão). Assim, executa qualquer ferramenta do servidor.
 
**Sem bibliotecas geradoras de código.** Records substituem o Lombok, os mappers são escritos à mão e a injeção de dependência é manual, conforme o enunciado.
 
**Log no stderr.** No transporte STDIO, o stdout é exclusivo das mensagens do protocolo; todo o log vai para o stderr.
 
---
 
## 6. Cliente MCP do projeto
 
O cliente inicia o servidor automaticamente e apresenta um menu com as ferramentas que ele disponibiliza.
 
```bash
cd target/dist-client
java -jar mcp_tabela_fipe_client.jar
```
 
Sem argumentos, o cliente procura o servidor em `../../server/mcp_tabela_fipe_server.jar` (por isso o comando acima é executado de dentro de `target/dist-client`). Para executar de qualquer pasta, informe o caminho do servidor:
 
```bash
java -jar mcp_tabela_fipe_client.jar /caminho/do/projeto/server/mcp_tabela_fipe_server.jar
```
 
### Uso
 
```
========================================
 CLIENTE MCP - TABELA FIPE
========================================
Ferramentas disponíveis:
 1 - Listar marcas [listar_marcas]
 2 - Buscar modelos [buscar_modelos]
 3 - Listar anos do modelo [listar_anos]
 4 - Consultar preço FIPE [consultar_preco]
 5 - Comparar veículos [comparar_veiculos]
 
Comandos: listar (atualizar ferramentas) | sair (encerrar)
>
```
 
- Escolha a ferramenta pelo **número** ou pelo **nome**. O que cada uma faz está descrito na seção 10.
- O cliente pergunta cada parâmetro com uma frase que explica o que informar. Campos opcionais trazem a indicação "(Enter para pular)".
- Parâmetros com valores fixos (como o tipo de veículo) aparecem como **opções numeradas**: digite o número ou o valor.
- Listas (como os veículos da comparação) são lidas **item a item**: o cliente pergunta a quantidade e depois os campos de cada item.
- `listar` recarrega as ferramentas; `sair` (ou Ctrl+D no Linux/macOS, Ctrl+Z e Enter no Windows) encerra.
Exemplo, buscando os modelos DB12 da Aston Martin:
 
```
----------------------------------------
Buscar modelos [buscar_modelos]
----------------------------------------
 
Tipo de veículo: carros, motos, caminhoes.
  1 - carros
  2 - motos
  3 - caminhoes
> 1
 
Código ou nome da marca (ex.: "7" ou "BMW"). Não diferencia maiúsculas e acentos.
> aston martin
 
Palavras do nome do modelo (ex.: "320i sport"). Opcional, mas recomendado. (Enter para pular)
> db12
 
=============== RESULTADO ===============
2 Modelos encontrados para a marca: ASTON MARTIN (código 189)
Modelos com "db12":
11338 – DB12 Coupe 4.0 V8 680cv
11339 – DB12 Volante 4.0 V8 680cv
=========================================
```
 
O cliente é genérico: ele não conhece nenhuma ferramenta em particular e monta as perguntas a partir do esquema (JSON Schema) que cada uma declara. Por isso, executa qualquer ferramenta que o servidor venha a disponibilizar.
 
> **Acentos no console do Windows:** se os acentos aparecerem corrompidos, execute `chcp 65001` antes de iniciar o cliente.
 
---
 
## 7. Claude Desktop
 
1. Gere o servidor (seção 3).
2. Abra o arquivo de configuração do Claude Desktop (se não existir, crie-o):
   | Sistema | Local do `claude_desktop_config.json` |
   | --- | --- |
   | Windows (instalação pela Microsoft Store) | `C:\Users\<USUÁRIO>\AppData\Local\Packages\Claude_<código>\LocalCache\Roaming\Claude\` |
   | Windows (instalador do site) | `%APPDATA%\Claude\` |
   | macOS | `~/Library/Application Support/Claude/` |
   | Linux | `~/.config/Claude/` |
   No Windows, o diretório `Users` também pode aparecer como `Usuários`, e o sufixo de `Claude_<código>` varia de uma instalação para outra.
3. Acrescente o servidor à entrada `mcpServers`, com o **caminho absoluto** do jar:
   **Windows** (as barras invertidas são duplicadas no JSON):
```json
{
  "mcpServers": {
    "tabela-fipe": {
      "command": "java",
      "args": ["-jar", "C:\\caminho\\do\\projeto\\server\\mcp_tabela_fipe_server.jar"]
    }
  }
}
```
 
   **Linux e macOS:**
 
```json
{
  "mcpServers": {
    "tabela-fipe": {
      "command": "java",
      "args": ["-jar", "/caminho/do/projeto/server/mcp_tabela_fipe_server.jar"]
    }
  }
}
```
 
4. **Encerre o Claude Desktop completamente** (não basta fechar a janela) e abra-o de novo.
Se o servidor não aparecer, troque `"java"` pelo caminho completo do executável do JDK 25 (por exemplo, `C:\\Program Files\\Java\\jdk-25\\bin\\java.exe`). Aplicativos gráficos nem sempre recebem o mesmo `PATH` do terminal.
 
### Perguntas para testar
 
- "Quais versões do Aston Martin DB12 existem na Tabela FIPE?"
- "Quanto vale um Aston Martin DB12 Coupe 2024?"
- "Compare o preço do Aston Martin DB12 Coupe com o DB12 Volante, ambos 2024."
- "Quais marcas de motos estão na Tabela FIPE?"
O Claude encadeia as ferramentas sozinho: busca o modelo, descobre os anos disponíveis e consulta o preço.
 
---
 
## 8. MCP Inspector
 
O [MCP Inspector](https://github.com/modelcontextprotocol/inspector) é a ferramenta oficial para testar e depurar servidores MCP. O projeto o executa num contêiner Docker, a partir do `Dockerfile` e do `compose.yaml` da raiz.
 
1. Gere o servidor (seção 3): o contêiner usa a pasta `server/`.
2. Com o Docker em execução, na raiz do projeto:
```bash
docker compose up -d
```
 
3. Acesse `http://127.0.0.1:6274` no navegador.
4. Para encerrar: `docker compose down`.
O `compose.yaml` constrói a imagem (se ainda não existir), compartilha a pasta `server/` com o contêiner em modo somente leitura e configura o Inspector para iniciar o servidor. Se o servidor for gerado de novo, não é preciso recriar o contêiner: basta desconectar e conectar novamente na interface.
 
> Em Linux com SELinux (Fedora, por exemplo), o volume já está marcado com `:Z`, que dá ao contêiner acesso à pasta `server/`. Em outros sistemas, essa opção não tem efeito.
 
### Na interface
 
Conecte ao servidor, abra a aba **Tools**, clique em **List Tools** e execute qualquer ferramenta preenchendo o formulário. Para a `comparar_veiculos`, a opção **Edit as JSON** facilita montar a lista de veículos.
 
---
 
## 9. Outros assistentes: Claude Code
 
O [Claude Code](https://code.claude.com/docs/pt/setup) é o assistente de linha de comando da Anthropic. Ele roda em Windows, macOS e Linux, e exige uma conta Claude paga (Pro ou superior) ou créditos da API.
 
1. Instale seguindo a [documentação oficial](https://code.claude.com/docs/pt/setup) e faça o login executando `claude`.
2. Registre o servidor, com o **caminho absoluto** do jar:
```bash
claude mcp add --scope user --transport stdio tabela-fipe -- java -jar /caminho/do/projeto/server/mcp_tabela_fipe_server.jar
```
 
   O `--scope user` deixa o servidor disponível em qualquer pasta. Sem ele, o registro vale só para a pasta em que o comando foi executado.
 
3. Confira a conexão com `claude mcp list` (o servidor deve aparecer como conectado).
4. Abra o `claude` e faça perguntas como as da seção 7. Dentro da sessão, `/mcp` mostra o status do servidor.
Para remover: `claude mcp remove tabela-fipe`.
 
---
 
## 10. Ferramentas
 
As cinco ferramentas formam um fluxo: **descobrir o modelo → escolher o ano → consultar ou comparar o preço**. Todas são somente leitura.
 
| Ferramenta | Para que serve | Quando usar |
| --- | --- | --- |
| `listar_marcas` | mostra as marcas de carros, motos ou caminhões, com seus códigos | quando não se sabe o nome exato da marca |
| `buscar_modelos` | procura, dentro de uma marca, os modelos cujo nome contém as palavras informadas, e mostra o código de cada um | o ponto de partida: encontrar o veículo |
| `listar_anos` | mostra os anos disponíveis de um modelo e o código de cada ano (inclusive 0 km) | depois de escolher o modelo, para saber quais anos existem |
| `consultar_preco` | mostra o preço FIPE de um modelo em um ano, com o código FIPE e o mês de referência | com o modelo e o ano em mãos |
| `comparar_veiculos` | consulta de 2 a 5 veículos e indica o mais barato, o mais caro e a diferença em reais e em percentual | para decidir entre opções de compra |
 
Um uso típico: `buscar_modelos` para achar o código do modelo, `listar_anos` para achar o código do ano e `consultar_preco` para ver o valor. Nos assistentes de IA, esse encadeamento acontece sozinho, a partir de uma pergunta como "quanto vale um Aston Martin DB12 2024?".
 
Em todas elas:
 
- `tipo` aceita exatamente `carros`, `motos` ou `caminhoes`.
- `marca` aceita o **código** (`189`) ou o **nome** (`aston martin`). O nome é procurado sem diferenciar maiúsculas e acentos: primeiro o nome exato, depois uma correspondência parcial única (`volkswagen` encontra "VW - VolksWagen").
- O resultado traz um **texto legível** (`content`) e os mesmos dados em **JSON** (`structuredContent`), para programas e assistentes de IA.
- Erros são devolvidos como resultado com `isError: true` e uma mensagem que explica o problema.
### 10.1 `listar_marcas`
 
Lista as marcas de um tipo de veículo, opcionalmente filtradas. Raramente é necessária, pois as demais ferramentas aceitam o nome da marca.
 
| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | texto (opções) | sim | `carros`, `motos` ou `caminhoes` |
| `termo` | texto | não | parte do nome da marca |
 
**Exemplo:** `{"tipo": "carros", "termo": "aston"}`
 
```
Marcas de carros com "aston" (1):
189 – ASTON MARTIN
```
 
### 10.2 `buscar_modelos`
 
Busca os modelos de uma marca cujo nome contenha **todas as palavras** do termo, em qualquer ordem. Devolve no máximo **30 modelos**; se houver mais, informa o total e pede para refinar o termo.
 
| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | texto (opções) | sim | `carros`, `motos` ou `caminhoes` |
| `marca` | texto | sim | código ou nome da marca |
| `termo` | texto | não (recomendado) | palavras do nome do modelo, como `db12 coupe` |
 
**Exemplo:** `{"tipo": "carros", "marca": "aston martin", "termo": "db12"}`
 
```
2 Modelos encontrados para a marca: ASTON MARTIN (código 189)
Modelos com "db12":
11338 – DB12 Coupe 4.0 V8 680cv
11339 – DB12 Volante 4.0 V8 680cv
```
 
Quando o limite é atingido, a última linha é, por exemplo, `Exibindo 30 de 87. Refine o termo para ver os demais.`
 
### 10.3 `listar_anos`
 
Lista os anos-modelo de um modelo, com o código de cada ano no formato **ano-combustível** (`2024-1`). O ano **32000** é a convenção da FIPE para veículo **0 km** e é exibido como "0 km".
 
| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | texto (opções) | sim | `carros`, `motos` ou `caminhoes` |
| `marca` | texto | sim | código ou nome da marca |
| `codigoModelo` | texto | sim | código obtido com `buscar_modelos` |
 
**Exemplo:** `{"tipo": "carros", "marca": "aston martin", "codigoModelo": "11338"}`
 
```
Anos disponíveis para o modelo 11338 (marca aston martin):
2024-1 – 2024 Gasolina
2025-1 – 2025 Gasolina
2026-1 – 2026 Gasolina
32000-1 – 0 km Gasolina
```
 
### 10.4 `consultar_preco`
 
Consulta o preço de um veículo na tabela do mês de referência atual.
 
| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `tipo` | texto (opções) | sim | `carros`, `motos` ou `caminhoes` |
| `marca` | texto | sim | código ou nome da marca |
| `codigoModelo` | texto | sim | código obtido com `buscar_modelos` |
| `codigoAno` | texto | sim | código obtido com `listar_anos`, como `2024-1` |
 
**Exemplo:** `{"tipo": "carros", "marca": "aston martin", "codigoModelo": "11338", "codigoAno": "2024-1"}`
 
```
ASTON MARTIN DB12 Coupe 4.0 V8 680cv – 2024 Gasolina
Preço FIPE: R$ 2.918.759,00
Código FIPE: 085018-7
Referência: novembro de 2026
```
 
No JSON, o preço aparece como número (`valor`) e formatado (`valorFormatado`).
 
### 10.5 `comparar_veiculos`
 
Compara os preços de **2 a 5 veículos**, indicando o mais barato, o mais caro e a diferença em reais e em percentual. Um veículo não encontrado não interrompe a comparação: ele é listado como "não consultado".
 
| Parâmetro | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `veiculos` | lista (2 a 5 itens) | sim | cada item com `tipo`, `marca`, `codigoModelo` e `codigoAno` |
 
**Exemplo:**
 
```json
{
  "veiculos": [
    {"tipo": "carros", "marca": "aston martin", "codigoModelo": "11338", "codigoAno": "2024-1"},
    {"tipo": "carros", "marca": "aston martin", "codigoModelo": "11339", "codigoAno": "2024-1"}
  ]
}
```
 
O resultado segue este formato (os valores dependem da tabela vigente):
 
```
Comparação de 2 veículos (referência: novembro de 2026):
1. ASTON MARTIN DB12 Coupe 4.0 V8 680cv – 2024 Gasolina: R$ 2.918.759,00
2. ASTON MARTIN DB12 Volante 4.0 V8 680cv – 2024 Gasolina: R$ ...
Mais barato: ...
Mais caro: ...
Diferença: R$ ... (...% a mais)
```
 
### 10.6 Erros possíveis
 
Os argumentos passam por duas validações. Primeiro, o SDK do MCP confere a **estrutura** contra o esquema da ferramenta: parâmetros obrigatórios, valores permitidos e quantidade de itens. Depois, o próprio servidor aplica as **regras de negócio** que o esquema não expressa: formato do código do ano, marca ambígua ou inexistente, modelo ou ano que não existe.
 
| Situação | Ferramentas | Mensagem |
| --- | --- | --- |
| Argumentos fora do esquema: parâmetro obrigatório ausente, tipo fora das opções, lista com menos de 2 ou mais de 5 itens | todas | gerada pelo SDK do MCP, que valida os argumentos contra o esquema antes de executar a ferramenta; lista cada problema, por exemplo `/tipo: não possui valor na enumeração ["carros", "motos", "caminhoes"]` e `propriedade obrigatória 'marca' não encontrada` |
| Marca ambígua | as que recebem `marca` | `A marca 'chery' é ambígua. Marcas encontradas:` seguida das candidatas, com os códigos |
| Marca inexistente | as que recebem `marca` | `Marca 'xyz' não encontrada para o tipo carros. Consulte as marcas disponíveis.` |
| Código de marca inexistente | as que recebem `marca` | `Marca de código '999' não encontrada para o tipo carros.` |
| Modelo inexistente | `listar_anos` | `Marca ou modelo não encontrado: marca '189', modelo '99999' (tipo carros).` |
| Ano em formato inválido | `consultar_preco`, `comparar_veiculos` | `Código do ano em formato inválido: '2020'. Use o formato ano-combustível, como 2024-1.` |
| Ano indisponível para o modelo | `consultar_preco`, `comparar_veiculos` | `Ano '1900-1' não disponível para o modelo '11338' da marca '189' (tipo carros), ou marca/modelo inexistente.` |
| Menos de 2 veículos encontrados | `comparar_veiculos` | `Não foi possível comparar: menos de 2 veículos foram encontrados.` seguida dos não consultados |
| Limite diário da API atingido | todas | `Limite de requisições da API FIPE atingido. Tente novamente mais tarde.` |
| API fora do ar ou com erro | todas | `A API FIPE respondeu com erro HTTP 500.` ou `Não foi possível conectar à API FIPE.` |
| Resposta inesperada da API | todas | `A API FIPE retornou uma resposta em formato inesperado.` |
| Erro interno | todas | `Erro interno ao executar a ferramenta 'nome'.` (o detalhe vai para o log do servidor) |
 
---
 
## 11. Testes
 
| Tipo | Classes | Comando |
| --- | --- | --- |
| Unitários (`*Test`) | modelo, mapper, configuração, provider, cache, serviço, ferramentas e cliente | `mvn test` |
| Integração (`*IT`) | `McpServerTabelaFipe01IT`: executa o jar do servidor e chama as cinco ferramentas pelo protocolo | `mvn clean verify -Pserver` |
 
Nenhum teste acessa a internet:
 
- **`StubHttpServer`** é um servidor HTTP local (do próprio JDK) que responde com **respostas reais da API**, salvas em `src/test/resources/fixtures`. O provider é testado fazendo requisições HTTP de verdade a ele.
- No **teste de integração**, o servidor MCP é iniciado com a variável `FIPE_BASE_URL` apontando para esse stub.
- **`FakeFipeProvider`** substitui a API nos testes do serviço e das ferramentas, registrando as chamadas recebidas.
- **`RelogioAjustavel`** simula a passagem do tempo nos testes de expiração do cache.
---
 
## 12. Possíveis problemas comuns
 
| Sintoma | Causa provável | Solução |
| --- | --- | --- |
| `JAR do servidor não encontrado` no cliente | o cliente foi executado fora de `target/dist-client` | execute de dentro dessa pasta ou informe o caminho do servidor como argumento |
| Servidor não aparece no Claude Desktop | caminho relativo, barras simples no JSON ou `java` fora do `PATH` | use o caminho absoluto, barras duplicadas no Windows e, se preciso, o caminho completo do `java` |
| `CONNECTION_CLOSED` no Claude Code | caminho do jar errado no registro | confira com `claude mcp get tabela-fipe` e registre de novo com o caminho absoluto |
| `UnsupportedClassVersionError` | o `java` usado é anterior ao 25 | instale o JDK 25 ou use o caminho completo do executável dele |
| `NoClassDefFoundError` ao iniciar o servidor | o jar foi copiado sem a pasta `libs/` | copie a pasta `server/` inteira |
| Acentos corrompidos no console do Windows | codificação do console | `chcp 65001` antes de executar |
| `Limite de requisições da API FIPE atingido` | as 500 requisições diárias da API sem token foram usadas | aguarde a renovação diária ou defina `FIPE_TOKEN` |
 
