# Monólitos

Os monólitos do **Ancient Remnants: Monoliths**, do jeito do NexusSMP: só as estruturas, raras, intocáveis, e o mundo
quebrando em volta delas. Módulo `monolith`, código em `src/main/java/com/nexussmp/nexuscore/monolith/`. Só liga com
o Ancient Remnants instalado.

## O mod desligado

Tudo pelo pacote de dados embutido `src/main/resources/datapacks/monoliths/`, que entra sozinho, por cima de tudo,
em qualquer mundo:

- **Sem bênção:** a lista de bênçãos de cada estrutura fica vazia. O cristal continua lá, brilhando e zumbindo, e não
  dá nada.
- **Sem Codex:** as três receitas do mod desligadas, e os fragmentos de pesquisa somem dos baús de posto de saqueador.
- **Sem spawners:** saem na hora em que a estrutura é gerada (os arquivos do mod não são mexidos).
- Os itens do mod somem do criativo e do JEI.

## Um de cada, bem raro

Seis no mundo inteiro, um de cada (`LoneMonolithPlacement`). Cada um ganha uma direção (os seis dividem a volta em
fatias de 60 graus) e uma distância entre **2.000 e 5.000 blocos** do centro do mundo, e procura ali o bioma em que
pode nascer. Bioma raro (badlands do Hunter, oceano congelado profundo do Sentinel) pode cair fora da fatia ou um pouco
mais longe. Sai da semente: o mesmo mundo sempre dá o mesmo lugar.

O `/locate` não acha nenhum, de propósito. Para operador:

- `/nexus monolito onde`: os seis, com coordenada e distância (clicar sugere o `/tp`).
- `/nexus monolito eco`: o quanto o mundo está errado onde você está, de 0 a 1.

## Intocáveis

Dentro da caixa da estrutura ninguém quebra, coloca nem usa nada: nem explosão, pistão, lava com água, enderman ou
creeper. Só operador no criativo mexe. O cristal já era invulnerável.

## O eco

Quanto mais perto, mais errado (`Echo`). Começa uns **360 blocos** além da borda da estrutura, cresce devagar e chega
ao máximo logo antes dela. **Na estrutura em si, tudo normal.**

| Faixa | O que tem |
|---|---|
| **Estranho** | Uma copa de árvore com folha de outra cor (cerejeira, azaleia, verruga do Nether). Minério no lugar de grama. Bichos parados encarando quem chega perto. |
| **Errado** | Chunks de outro bioma com corte reto (neve, deserto, badlands, cogumelo, cerejeira), com a cor da grama e da água trocando de verdade e o chão um degrau acima ou abaixo. Blocos flutuando. Árvore de cabeça para baixo. Placa encerada com texto de erro brilhando em roxo. Bichos com nome de sistema (`entity_0482`, `placeholder_cow`). |
| **Quebrado** | Chunks vazios até o fundo. Manchas de bloco roxo e preto (`nexuscore:null`, sem nome traduzido de propósito, intocável). Bichos com a textura roxa e preta, uns de cabeça para baixo, outros do tamanho errado. Mapas do Xaero desligados. |

O terreno é gerado **junto com o mundo** (`EchoFeature`, no fim da decoração de cada chunk). É terreno de verdade,
salvo no mundo, e nunca toca um chunk por onde a estrutura passa. Só vale para chunks gerados com o módulo ligado.

**O vazio:** quem cai num chunk vazio é jogado de volta para o último chão firme em que pisou e leva um golpe de 60%
da vida máxima, que atravessa armadura, mas nunca o que mata.

**Na tela** (`EchoClient`), tudo em roxo e sutil: um tom leve nas bordas e, de vez em quando, uma linha fina que
escorrega para o lado. No "quebrado" as coordenadas do F3 viram caracteres embaralhados. Sem mexer em som.

## Limites

- Criaturas de mod com animação própria (GeckoLib) desenham por outro caminho e não pegam a textura roxa e preta.
- O fogo que vem de fora ainda pode queimar madeira da estrutura.
- Num mundo que já existia, só os chunks ainda não gerados ganham monólito e eco.
