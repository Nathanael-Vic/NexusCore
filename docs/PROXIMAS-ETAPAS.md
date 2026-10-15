# NexusCore: próximas etapas

Mod central do SMP (NeoForge 21.1.x · Minecraft 1.21.1 · Java 21).

> Este documento contém **só o que falta fazer**. O que já está pronto (interface do frame do WaterFrames, textura de madeira, port do Xander's Sodium Options, bloco de estatística básico) saiu daqui.
> Onde aparece **(verificar)**, conferir o nome exato da API na 1.21.1 antes de usar.

---

## Índice

1. [Estado atual](#1-estado-atual)
2. [Etapa 1: Backup](#2-etapa-1-backup)
3. [Etapa 2: Correções do painel](#3-etapa-2-correções-do-painel)
4. [Etapa 3: Páginas do painel](#4-etapa-3-páginas-do-painel)
5. [Etapa 4: Dimensão Space (lobby) e quartos](#5-etapa-4-dimensão-space-lobby-e-quartos)
6. [Etapa 5: Estatísticas, recompensas, comunidade e economia](#6-etapa-5-estatísticas-recompensas-comunidade-e-economia)
7. [Regras de arquitetura e código](#7-regras-de-arquitetura-e-código)
8. [Decisões](#8-decisões)

---

## 1. Estado atual

> **Conferido no código em 15/09.** Diferenças entre o documento e o que já existia:
> - **As páginas da Etapa 3 já existiam** (Resumo, Biomas, Estruturas, Recentes, com paginação "1/3" e alternância automática). O que faltava era a navegação por clique, o indicador de página e tirar os totais — foi tudo feito junto com a Etapa 2.
> - **A checklist 3.3 já estava quase toda cumprida** pela Fase 3/4 anterior. Sobrava só `PushReaction.BLOCK`, que entrou agora. O resto (BFS sem carregar chunk, limite de tamanho, update só quando muda, `getRenderBoundingBox`, cache invalidado no F3+T, distância de render, caches limpos no `ServerStoppedEvent`/logout/troca de dimensão, codecs com limite, handlers na thread principal, debounce, leitura tolerante, painel com dimensão própria) já estava no lugar.
> - O bloco se chama **`nexuscore:exploration_panel`** (o documento cita `panel.json`); os arquivos seguem esse nome.
> - `/nexus stats reload` **deixou de existir**: ele só servia para recalcular totais.
> - O overworld agora aparece como **"Nexus"** em painel e comandos.

| Item | Status |
|---|---|
| Painel multiblock (resolução ajusta ao tamanho) | ✅ funcionando |
| Painel mostra: biomas, estruturas, última descoberta | ✅ funcionando |
| Textura do painel conectando entre blocos | ✅ Etapa 2: estados de conexão + modelo multipart |
| Painel mostra totais ("12 / 31") | ✅ Etapa 2: totais removidos do painel, dos comandos e do pacote |
| Páginas (resumo, listas, feed, rankings...) | ✅ Etapa 3: 4 páginas com navegação por clique (as novas ficam para a Etapa 5) |
| Lobby Space e quartos | ✅ 4A (dimensão + céu) · ⏳ 4B (estação e quartos) esperando a construção |
| Estatísticas novas, recompensas, comunidade, economia | ⏳ planejado |

**Ordem:**
1. Backup
2. Correções do painel
3. Páginas do painel (as que usam os dados que já existem)
4. Lobby Space e quartos (antes da etapa 5: spawn, login e dimensões ficam estáveis antes de estatísticas e moeda dependerem deles)
5. Estatísticas novas, recompensas, comunidade e economia (+ páginas novas)

**Fora do escopo agora:** loja de cosméticos e decorações do quarto usando a moeda.

---

## 2. Etapa 1: Backup

**Obrigatório antes de qualquer mudança.** O estado atual funciona, e as etapas seguintes mudam o formato dos dados salvos.

1. **Código:** commit e tag do estado funcional.
   ```bash
   git add -A
   ```
   ```bash
   git commit -m "Painel multiblock, textura do frame e GUI do WaterFrames funcionando"
   ```
   ```bash
   git tag v0.1.0-painel-funcional
   ```
   Sem git: compactar a pasta do projeto inteira em `nexuscore-0.1.0-painel-funcional.zip`.
2. **Jar:** copiar o jar de `build/libs/` para `backups/nexuscore-0.1.0-painel-funcional.jar` (fora da pasta `build`, que o Gradle limpa).
3. **Mundos de teste:** copiar a pasta do mundo (cliente: `saves/<mundo>`; servidor: `world/`), principalmente `data/nexuscore_stats.dat`.
4. Anotar as versões do modpack usado no teste.

**Voltar ao backup:** `git checkout v0.1.0-painel-funcional` (ou descompactar o zip) e restaurar a pasta do mundo.

Repetir o backup (nova tag) ao terminar cada etapa.

**Feito em 15/09:** tag `v0.1.0-painel-funcional`, `backups/nexuscore-0.1.0-painel-funcional.jar`, `backups/mundo-WIP-2026-09-15.zip` (mundo de teste inteiro) e `backups/config-2026-09-15/` (os três `.toml` gerados). A pasta `backups/` fica fora do git.

---

## 3. Etapa 2: Correções do painel

### 3.1 Textura conectada (tirar a "cruz")

**Problema:** cada bloco desenha a própria borda, então 2×2 blocos mostram uma cruz no meio. O certo é parecer **um display único**: fundo contínuo e borda **só no contorno externo**.

**Não precisa do Fusion:**
- O Fusion conecta blocos iguais vizinhos. Duas telas separadas encostadas seriam fundidas visualmente, e blocos que não fecham retângulo conectariam errado. Só o nosso multiblock sabe quais blocos formam a mesma tela.
- Evita uma dependência obrigatória a mais no cliente.

**Solução: estados de conexão no bloco + modelo multipart** (mesmo mecanismo do vidro fino e das cercas):

1. `PanelBlock` ganha 4 booleanos **relativos à face da tela**: `CONNECTED_UP`, `CONNECTED_DOWN`, `CONNECTED_LEFT`, `CONNECTED_RIGHT`. Esquerda/direita calculadas a partir de `FACING`, do ponto de vista de quem olha.
2. Um lado é `true` **somente se o vizinho pertence ao mesmo multiblock** (mesmo controlador), e não só por ser painel encostado.
3. Ao recalcular o multiblock, atualizar todos os membros de uma vez:
   ```java
   if (!state.equals(newState)) {
       level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
   }
   ```
   `UPDATE_KNOWN_SHAPE` evita que cada `setBlock` dispare o recálculo nos vizinhos de novo (cascata).
4. `blockstates/panel.json` em **multipart**:
   - sempre: caixa do bloco + **fundo** na face frontal;
   - borda de cima quando `connected_up=false`; de baixo, esquerda e direita da mesma forma;
   - rotação por `facing` (`"y": 90/180/270`).
5. Texturas (`assets/nexuscore/textures/block/panel/`):
   - `panel_background.png` precisa ser **tileable** (lado direito encaixa no esquerdo, cima no de baixo). Senão o fundo vira xadrez mesmo sem borda;
   - `panel_border_*.png` com transparência e `"render_type": "minecraft:cutout"`;
   - bordas laterais já cobrem os cantos; multiblock é sempre retângulo, então não há canto interno.
6. O BER (`PanelRenderer`) desenha **só o conteúdo** (texto/ícones) por cima, com recuo mínimo contra z-fighting. Se hoje o fundo é desenhado pelo BER bloco a bloco, é essa a origem da cruz: tirar de lá.

**Desempenho:** fundo e borda viram geometria estática do chunk, compilada pelo Sodium só quando a tela muda. Custo por frame: zero. Estados: 4 direções × 16 = 64.

**Testes:**
- [ ] 1×1: borda nos 4 lados
- [ ] 2×1, 1×2, 2×2, 4×3 e tamanho máximo: sem linhas internas, fundo contínuo
- [ ] Duas telas separadas encostadas: cada uma com a própria borda
- [ ] Quebrar um bloco do meio: telas resultantes com bordas corretas
- [ ] As 4 direções
- [ ] Com Sodium e com shaders do Iris

**Como ficou:**
- `PanelBlock` ganhou `connected_up/down/left/right` (64 estados com o `facing`), ligados só quando o vizinho é do mesmo multiblock. `PanelMultiblock` grava com `UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE` e só quando o estado muda.
- `blockstates/exploration_panel.json` virou multipart: o cubo sempre, mais uma borda por lado desconectado, com a rotação do `facing`.
- Texturas em `textures/block/panel/`: `background` (tileável, sem nada nas bordas) e `border_top/bottom/left/right` (transparentes, `render_type: cutout`, `ambientocclusion: false`). As bordas **não se sobrepõem**: as laterais ocupam a coluna inteira com os cantos, a de cima e a de baixo só o miolo — assim nunca há dois overlays no mesmo pixel.
- `front.png` (fundo + as quatro bordas) é usada só no item, no inventário e na mão.
- O renderer **não desenha mais fundo nem moldura**: só texto e linhas, 0,02 bloco à frente da face. Fundo e borda agora são geometria do chunk (custo por frame zero).

### 3.2 Remover totais

**Problema:** o painel mostra "12 / 31". A dimensão é desconhecida: os números só sobem.

- Remover do servidor o cálculo de totais (`possibleBiomes()`, `possibleStructureSets()` ou equivalente).
- Remover os totais do **payload de rede**. Não basta esconder na tela; se chega ao cliente, dá para ler com mod de debug.
- Sem barras de progresso e sem placeholders `???` (a quantidade deles revelaria o total).
- Painel e comandos mostram só contagem: "Biomas descobertos: 12".
- Incrementar a versão do protocolo (`event.registrar("2")`).

**Como ficou:** a classe `PossibleTotals` foi apagada, `DimensionStatsView` não tem mais as listas de possíveis, o painel perdeu barras e `???`, `/nexus stats` mostra só as contagens e o protocolo virou `"2"`. Nada de total sai do servidor.

### 3.3 Revisão do que já existe (bugs, desempenho, segurança)

**Multiblock**
- [ ] BFS **nunca carrega chunk**: checar `level.isLoaded(pos)` antes de ler vizinho
- [ ] Controlador em chunk descarregado: membros não crasham, só não desenham
- [ ] `PushReaction.BLOCK` (pistão não arrasta pedaço da tela)
- [ ] Tamanho máximo respeitado **durante** a BFS (parede gigante de painéis não trava)
- [ ] Update da block entity para o cliente só quando os dados do multiblock mudam

**Render**
- [ ] `getRenderBoundingBox` cobre a tela inteira (verificar onde fica na NeoForge 21.1)
- [ ] Cache de layout invalidado em reload de recursos (F3+T)
- [ ] Nenhuma alocação por frame no `render`
- [ ] Distância máxima de render do conteúdo

**Estado e memória**
- [ ] Caches estáticos do servidor limpos em `ServerStoppedEvent` (em singleplayer, trocar de mundo não herda dados)
- [ ] Cache do cliente limpo ao desconectar
- [ ] Cache por jogador removido no logout e resetado na troca de dimensão

**Rede e dados**
- [ ] Codecs com tamanho máximo em listas e strings
- [ ] Handlers na thread principal
- [ ] Sync com debounce (várias descobertas = 1 pacote)
- [ ] Leitura dos dados tolerante a IDs desconhecidos (mod removido)

**Painel entre dimensões** (necessário para a Etapa 4)
- [ ] A block entity guarda **qual dimensão exibir**, e não assume a dimensão onde o bloco está
- [ ] O payload global envia os dados das dimensões rastreadas para **todos**, sem filtrar pela dimensão atual do jogador

---

## 4. Etapa 3: Páginas do painel

Hoje o painel mostra biomas, estruturas e a última descoberta numa tela só. Passa a ter **páginas**.

### 4.1 Páginas com os dados que já existem (fazer agora)

| Página | Conteúdo |
|---|---|
| **Resumo** | nome da dimensão ("Nexus") · biomas descobertos `12` · tipos de estrutura `5` · estruturas encontradas `27` · última descoberta |
| **Biomas** | lista **só dos descobertos**: nome + pioneiro (quem descobriu primeiro) |
| **Estruturas** | tipos descobertos: nome + quantidade encontrada + pioneiro |
| **Recentes** | últimas N descobertas: "Fulano descobriu *Deep Dark* · há 2h" (sem coordenadas) |

Listas maiores que a tela: paginação interna ("1/3").

### 4.2 Páginas que chegam com a Etapa 5

**Pioneiros**, **Rankings** (uma categoria por vez), **Bestiário**, **Perigo** (mortes, mob mais letal, causas), **Metas**, **Você** (estatísticas pessoais de quem está olhando). Detalhes em [6.9](#69-painel-páginas-novas).

### 4.3 Navegação

- **Clique na tela:** o `BlockHitResult` vira coordenada 2D da tela → "botões" (página anterior/próxima, subpágina).
- Tratado **só no cliente**: cada jogador navega a própria visão da mesma tela. Ninguém muda a página de outro e não existe pacote de rede para abusar.
- Sem interação por X segundos, volta a alternar sozinha entre as páginas.
- Página inicial e páginas habilitadas configuráveis pelo OP na block entity (comando ou item de configuração).

**Como ficou:**
- Clique com a **mão vazia** e sem agachar (`InputEvent.InteractionKeyMappingTriggered`, só cliente): borda esquerda volta uma tela, borda direita avança, meio troca de página. Com item na mão o clique continua colocando bloco; agachado continua sendo a configuração do OP (essa sim vai ao servidor).
- Parado por 20 s, a tela volta a alternar sozinha a cada 10 s, passando de página quando acaba a última tela da lista.
- Indicador: bolinhas no rodapé (uma por página habilitada) e setas `<` `>` nas laterais quando a tela é larga o bastante. Dentro de uma lista longa continua o "1/3" no cabeçalho.
- Páginas habilitadas: `/nexus panel pages <pos> <summary,biomes,structures,recent|all>`; página inicial: `/nexus panel page <pos> <página>`.

### 4.4 Render

- Layout de cada página (linhas, posições, paginação) calculado **só quando os dados ou o tamanho da tela mudam**; no frame é só desenhar.
- Texto que não cabe: truncar com `…` usando `font.width` (medido no layout, não por frame).
- Nomes:
  - biomas: chave `biome.<namespace>.<path>`;
  - estruturas: chave própria `structure.<namespace>.<path>`, com fallback para o ID formatado (`ancient_city` → "Ancient City");
  - overworld: chave própria exibindo **"Nexus"**.
- Indicador discreto de página ("● ○ ○ ○") e setas nas bordas.

### 4.5 Testes
- [ ] Todas as páginas em 1×1, 2×2 e tamanho máximo, texto legível
- [ ] Dois jogadores olhando a mesma tela em páginas diferentes
- [ ] Nova descoberta aparece em ~1 s na página certa
- [ ] Sem queda de FPS com vários painéis grandes

---

## 5. Etapa 4: Dimensão Space (lobby) e quartos

### 5.1 Objetivo

- Ao entrar no servidor, o jogador aparece no **Space**, numa **estação espacial** (lobby), e não no overworld.
- No centro da estação, um **portal** leva ao **Nexus** (o overworld, onde acontece a exploração).
- **Quartos**, um por jogador:
  - jogador novo ganha um quarto criado automaticamente e **nasce dentro dele**;
  - **porta aberta = dono online**, **fechada = offline**;
  - o quarto é o spawn próprio do jogador (e futuramente onde ficam as decorações compradas).
- Painel no Space mostrando estatísticas do Nexus (ver checklist em 3.3).

### 5.2 Jeito nativo de fazer

"Nativo" (ou "data-driven"): do jeito que o próprio jogo foi feito para ser estendido.

| Parte | Como | Onde |
|---|---|---|
| Dimensão (existir, sem mobs, sem chuva, horário fixo) | **JSON de dados** (dimensão, tipo de dimensão, bioma), como o vanilla define Nether e End | dentro do NexusCore (`src/main/resources/data/nexuscore/`). Mod = datapack embutido, sempre ativo |
| Céu espacial | código de cliente para o tipo de dimensão | NexusCore (cliente) |
| Estação e quartos | **estruturas NBT** feitas com structure block no jogo | NexusCore (`data/nexuscore/structure/station/`) |
| Spawn, quartos, portas, portal, proteção | eventos do NeoForge | NexusCore, módulo `lobby` |

Por que não um datapack separado: spawn, quartos e portas precisam de código de qualquer jeito e o céu precisa do cliente; seriam duas coisas para manter em sincronia. Por que não config do servidor: `server.properties` não tem dimensão inicial nem quartos. Por que não outro mod: traria muito mais do que o necessário e não faria quartos/portas.

**Ordem dentro desta etapa:**
- **4A: dimensão + céu** (5.3 e 5.8). Primeiro, sozinha: entrar por comando (`/execute in nexuscore:space run tp @s 0 100 0`) e validar céu, luz, clima e desempenho antes de qualquer quarto.
- **4B: estação, quartos, portas, portal, proteção** (5.4 a 5.7). **Bloqueada até a estação e os modelos estarem construídos** (sem peças provisórias). Ao terminar a construção, seguir [5.4.1](#541-quando-a-construção-estiver-pronta).

### 5.3 A dimensão (dados)

Conferir campos contra os arquivos vanilla da 1.21.1 (principalmente o bioma `minecraft:the_void`).

**Requisitos definidos (15/09):**
- é o **espaço**: **sempre noite**;
- **estrelas iguais às do overworld** (não o céu do End, que não tem estrelas);
- **sem nuvens**;
- **sem gradiente**: nada de horizonte mais claro, pôr do sol, nem metade de baixo ("void") com outra cor. **Uma cor só** em todas as direções + estrelas;
- sem sol, sem lua, sem chuva, sem mobs.

**`data/nexuscore/dimension_type/space.json`**
```json
{
  "ultrawarm": false,
  "natural": false,
  "piglin_safe": false,
  "respawn_anchor_works": false,
  "bed_works": true,
  "has_raids": false,
  "has_skylight": false,
  "has_ceiling": false,
  "coordinate_scale": 1.0,
  "ambient_light": 0.1,
  "fixed_time": 18000,
  "logical_height": 384,
  "min_y": -64,
  "height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "nexuscore:space",
  "monster_spawn_light_level": 0,
  "monster_spawn_block_light_limit": 0
}
```
| Campo | Valor | Por quê |
|---|---|---|
| `fixed_time` | `18000` | meia-noite parada: sempre noite, sem ciclo |
| `has_skylight` | `false` | espaço não tem luz do céu; a estação é iluminada só por blocos de luz (realista e mais barato: o jogo não calcula luz do céu na dimensão) |
| `ambient_light` | `0.1` | um mínimo de claridade para não ficar breu total fora das luzes; ajustar em teste |
| `natural` | `false` | sem spawn natural; bússola e relógio giram |
| `bed_works` | `true` | com `false` a cama **explode** (como no Nether); importa para decoração futura |
| `effects` | `nexuscore:space` | liga o céu customizado do cliente (5.8) |

**`data/nexuscore/worldgen/biome/space.json`**: baseado no `the_void` vanilla:
- `has_precipitation: false` (sem chuva/neve);
- sem spawners, sem features, sem carvers;
- `sky_color` e `fog_color` = **a cor do espaço** (a mesma de 5.8). Assim, mesmo se algum código vanilla usar a cor do bioma, ela já é a certa.

**`data/nexuscore/dimension/space.json`**
```json
{
  "type": "nexuscore:space",
  "generator": {
    "type": "minecraft:flat",
    "settings": {
      "biome": "nexuscore:space",
      "layers": [],
      "lakes": false,
      "features": false,
      "structure_overrides": []
    }
  }
}
```

- Em mundo existente, a dimensão aparece no próximo boot após instalar a versão do mod que a contém (não com `/reload`).
- Dimensão vazia: carregar/gerar chunks é praticamente grátis.
- `nexuscore:space` **nunca** entra nas dimensões rastreadas das estatísticas.

**Como ficou (4A):**
- Arquivos no jar, em `src/main/resources/data/nexuscore/`: `dimension_type/space.json`, `dimension/space.json` e `worldgen/biome/space.json`. Campos conferidos contra `the_end.json` e o bioma `the_void` da 1.21.1.
- A cor do espaço é uma constante só (`SpaceDimensions.SKY_COLOR = #020308`), usada no céu, na névoa e no bioma. Para trocar, muda lá e no `sky_color`/`fog_color` do bioma.
- O Space é **ignorado pelas estatísticas no código**, não só na config: mesmo que alguém coloque o id em `trackedDimensions`, ele é descartado.
- Entrar agora é por comando: `/execute in nexuscore:space run tp @s 0 100 0`. Nascer lá é a 4B — isso é código (evento de login), não dado.
- A dimensão **existe sozinha**: é um datapack embutido no mod, sempre ativo, sem instalar nada à parte.

### 5.4 Estação e quartos (estruturas)

> **Status:** a estação ainda não foi construída e **o layout não está decidido**. Nada desta seção é implementado antes da construção pronta.

**Layout em aberto (15/09).** Possibilidades consideradas, sem decisão:
- corredor único com quartos dos dois lados;
- vários corredores/alas;
- corredores empilhados (andares);
- corredor com curva no final;
- **paredão com vários andares de quartos numa sala sem gravidade**, onde os jogadores flutuam até o próprio quarto.

Construídas no jogo com structure blocks e exportadas como `.nbt`. Os nomes abaixo são **exemplos** do caso "corredor"; a lista real de peças sai do layout escolhido:

| Peça (exemplo) | Conteúdo |
|---|---|
| `station/hub.nbt` | centro com o portal para o Nexus e lugar para painéis |
| `station/room.nbt` | **quarto padrão** |
| peças de conexão | segmento de corredor, fim de corredor, curva, andar, parede de quartos... conforme o layout |

**Quartos agora:** todos iguais (um único modelo).

**Personalização futura** (paredes, teto, chão, decoração; fora do escopo agora). Para não precisar refazer depois, o modelo do quarto já deve seguir:
- **tamanho interno fixo** e alinhado à grade (ex.: 7×5×7 por dentro), com os limites registrados em constantes. É o que a proteção (5.7) e a personalização vão usar;
- **camadas separadas por posição:** chão = camada Y mínima interna, teto = Y máxima, paredes = perímetro. Assim, no futuro, trocar "o material da parede" é substituir blocos numa região conhecida, sem mexer no corredor;
- **nada de decoração fixa no modelo** além de porta, placa, cabeça e ponto de spawn. Decoração vai ser do jogador;
- `templateVersion` no registro (5.5) desde o início.

**Geração dos quartos: isolada numa estratégia de layout.** Como o formato não está decidido, **só a parte "onde fica o quarto N e quais peças colocar"** muda conforme o layout. Todo o resto (registro, spawn, portas, portal, proteção, `/lobby`, respawn) é igual para qualquer layout e só conversa com esta interface:

```java
public interface RoomLayout {
    /** Onde fica o quarto de índice N: origem, rotação e limites internos. Sempre o mesmo resultado para o mesmo N. */
    RoomPlacement placementOf(int index);

    /** Peças que precisam existir para o quarto N aparecer (quarto, corredor, curva, andar...), em ordem. */
    List<PiecePlacement> piecesFor(int index, PlacedPieces alreadyPlaced);

    /** Qual quarto contém esta posição (para portas e proteção). Vazio se não for quarto. */
    OptionalInt roomAt(BlockPos pos);
}

record RoomPlacement(BlockPos origin, Rotation rotation, BoundingBox interior, BlockPos spawn, BlockPos door) {}
record PiecePlacement(ResourceLocation template, BlockPos pos, Rotation rotation, String pieceId) {}
```

Regras que valem para **qualquer** layout:
- A posição sai **só do índice**: nenhuma coordenada de quarto precisa ser salva, e nunca dessincroniza.
- `roomAt(pos)` deve ser **cálculo direto** (O(1) ou quase), sem varrer todos os quartos. Portas e proteção chamam isso com frequência.
- Peças colocadas **uma única vez** (idempotente, registro em 5.5), com os chunks da área carregados antes.
- Peças podem ser **rotacionadas** (`StructurePlaceSettings#setRotation`), o que cobre curvas e lados opostos com o mesmo modelo de quarto.
- Trocar de layout depois que o servidor abriu exige migração (quartos já existem no mundo). Por isso: **decidir o layout antes de abrir**.

Exemplos de implementação (só um será feito):
- `CorridorLayout`: `slot = índice`, lado = `índice % 2`, posição = origem + direção × (índice / 2) × tamanho do segmento. O fim do corredor é movido a cada segmento novo.
- `WallLayout` (paredão/sala sem gravidade): `coluna = índice % quartosPorAndar`, `andar = índice / quartosPorAndar`, posição = origem + coluna × largura + andar × altura.
- Curvas e várias alas: tabela de "trechos" (reto, curva) definida em config, e o índice percorre os trechos em ordem.

Colocação de uma peça:
```java
StructureTemplate template = level.getStructureManager().getOrCreate(piece.template());
StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(piece.rotation());
template.placeInWorld(level, piece.pos(), piece.pos(), settings, level.random, Block.UPDATE_CLIENTS);
```

Depois de colocar o quarto, por código: placa com o nome, cabeça do jogador (`PLAYER_HEAD` com o perfil) e leitura do ponto de spawn e da porta a partir de **marcadores** no modelo (5.4.1).

**Sala sem gravidade (se esse layout for escolhido):** a gravidade reduzida (5.8) passa a valer **só dentro da área da sala** (verificação de região no amostrador do jogador, a cada poucos ticks), e não na dimensão inteira. Voo real (como criativo) não é recomendado: libera voo em qualquer lugar e confunde anti-cheat. Preferir gravidade muito baixa + atributo de velocidade de queda/salto, com limite de altura da sala.

#### 5.4.1 Quando a construção estiver pronta

Checklist para iniciar a 4B:

1. **Decidir o layout** (corredor, alas, andares, curva, paredão sem gravidade).
2. **Construir no mundo de teste** (no Space, depois da 4A pronta):
   - o **hub** com o lugar do portal;
   - o **quarto padrão** (sem decoração; porta, placa e cabeça podem ficar só como marcadores);
   - as **peças de conexão** do layout escolhido (corredor, fim, curva, andar...).
3. **Marcadores dentro do quarto padrão:** usar **blocos de estrutura em modo "Data"** (structure block → modo Data) nas posições:
   - `nexus:spawn`: onde o jogador nasce (e a direção que ele olha);
   - `nexus:door`: porta (a de baixo);
   - `nexus:sign`: placa com o nome;
   - `nexus:head`: cabeça do jogador.
   O mod lê esses marcadores ao colocar o quarto e troca pelo bloco real. Nada de coordenada escrita à mão no código.
4. **Respeitar a grade:** todas as peças com medidas inteiras e iguais entre si onde se encaixam (ex.: largura do corredor idêntica em reto e curva). Anotar as medidas de cada peça (X × Y × Z) e onde é a "entrada" de cada uma.
5. **Salvar cada peça com structure block** (modo Save), com nomes claros: `nexuscore:station/hub`, `nexuscore:station/room`, `nexuscore:station/corridor`...
   - "Incluir entidades": **desligado**.
   - Ar dentro das peças: deixar o structure void (`structure_void`) só onde não deve apagar blocos existentes.
6. **Me enviar:**
   - os arquivos `.nbt` (ficam em `saves/<mundo>/generated/<namespace>/structures/`);
   - prints da estação montada como deveria ficar com uns 3–5 quartos;
   - as medidas e o encaixe de cada peça (item 4);
   - o layout escolhido e, se for paredão, quantos quartos por andar.
7. **Eu entrego** a implementação do `RoomLayout` escolhido e o resto da 4B, e os `.nbt` vão para `src/main/resources/data/nexuscore/structure/station/`.

### 5.5 Registro de quartos

`SavedData` `nexuscore_rooms` (no nível do Space):

```java
record Room(int index, UUID owner, String lastKnownName, long createdAt) {}
// Map<UUID, Room> porDono; int proximoIndice; BitSet segmentosColocados; int templateVersion
```

- Chave = **UUID**: trocar de nick não perde o quarto (placa atualiza no login).
- Índice atribuído no primeiro login e **nunca reutilizado**.
- Exige servidor em **online-mode** (em offline-mode alguém poderia assumir o quarto de outro).

### 5.6 Fluxo do jogador

**Primeiro login**
1. `PlayerEvent.PlayerLoggedInEvent`: sem quarto → cria `Room` → coloca segmento se preciso → placa e cabeça.
2. Teleporta para o spawn do quarto: `player.teleportTo(spaceLevel, x, y, z, yaw, pitch)`.
3. Respawn padrão no quarto: `setRespawnPosition(space, pos, yaw, true, false)`.
4. Abre a porta.

**Logins seguintes:** teleporta para o quarto e abre a porta. Se aparecer um "piscar" do overworld no login, avaliar mixin pontual em `PlayerList#placeNewPlayer` (só se necessário).

**Logout:** fecha a porta.

**Portal para o Nexus** (`NexusPortalBlock` no hub):
- Entidade no bloco → `changeDimension(new DimensionTransition(...))` (nome na 1.21.1; virou `TeleportTransition` na 1.21.2+).
- Destino: última posição salva no Nexus; na primeira vez, o spawn do Nexus. Posição guardada num attachment do jogador ao sair do Nexus.
- Cooldown curto.

**Voltar ao Space:** portal no spawn do Nexus e `/lobby`, com cooldown (ex.: 30 s) e **bloqueado se levou dano nos últimos 10 s** (não vira fuga de combate).

**Morte no Nexus:**
- com cama/âncora válida: renasce nela;
- sem cama ou com cama destruída: renasce **no quarto**. Via evento de posição de respawn do NeoForge (`PlayerRespawnPositionEvent`, **verificar na 21.1.250**) ou reaplicando o respawn forçado no quarto.

### 5.7 Portas e proteção

**Portas**
- `RoomDoorBlock`: visual de porta, não abre por clique nem redstone; só o servidor muda.
- Login → `open=true`; logout → `open=false`.
- Chunk do quarto descarregado: **não** forçar carregamento; em `ChunkEvent.Load` do Space, reconciliar as portas daquele chunk com a lista de online (posição → índice pelo cálculo de 5.4).

**Proteção do Space** (por eventos, sem trocar modo de jogo):

| Evento | Regra |
|---|---|
| `BlockEvent.BreakEvent` / `EntityPlaceEvent` | cancelado, exceto OP (config) e, no futuro, o dono dentro do próprio quarto |
| `PlayerInteractEvent` | bloqueado em blocos da estação conforme lista |
| `LivingIncomingDamageEvent` | dano cancelado (lobby seguro, sem PvP) |
| Queda no void | abaixo do Y mínimo da estação → volta ao quarto antes de levar dano |
| Explosões | não destroem blocos |
| Drop de itens | opcional: impedir |

Limites do quarto calculados pelo índice (teste O(1)): base das decorações futuras.

### 5.8 Céu do espaço (cliente)

**Resultado esperado:** uma cor única e escura em **todas** as direções (cima, horizonte e embaixo) + as estrelas do overworld. Nada mais.

#### Como o céu vanilla funciona (e de onde vem cada coisa que não queremos)

| O que aparece no overworld | Quem desenha | No Space |
|---|---|---|
| Cor do céu (mais clara em cima) | `LevelRenderer#renderSky`, disco do céu | substituído por cor única |
| Horizonte mais claro / "degradê" até a paisagem | **cor da névoa** (`FogRenderer`), misturada com a cor do céu | névoa forçada para a mesma cor única |
| Metade de baixo mais escura ("void") | disco escuro desenhado quando a câmera está abaixo do horizonte | não desenhado |
| Laranja do nascer/pôr do sol | `getSunriseColor` | desligado |
| Sol e lua | `renderSky` | não desenhados |
| Estrelas | buffer de estrelas do `LevelRenderer`, brilho depende da hora e da chuva | **mantidas**, com brilho fixo |
| Nuvens | `renderClouds`, altura vinda dos efeitos da dimensão | desligadas |
| Chuva/neve | `renderSnowAndRain` | desligadas |

#### Implementação

Classe `SpaceSkyEffects extends DimensionSpecialEffects`, registrada só no cliente:

```java
@SubscribeEvent // barramento do mod, Dist.CLIENT
static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
    event.register(ResourceLocation.fromNamespaceAndPath(NexusCore.MOD_ID, "space"), new SpaceSkyEffects());
}
```

Construtor: `super(Float.NaN, false, SkyType.NONE, false, false)`.
- `cloudLevel = Float.NaN` → **sem nuvens** (mesmo mecanismo do Nether e do End).
- `SkyType.NONE` → o vanilla não desenha nada de céu por conta própria; quem desenha é o nosso `renderSky`.

Métodos sobrescritos (assinaturas da extensão do NeoForge 21.1, **verificar**):

| Método | Retorno / comportamento |
|---|---|
| `getBrightnessDependentFogColor(Vec3 cor, float brilho)` | sempre a **cor do espaço** (ignora brilho) |
| `isFoggyAt(int x, int z)` | `false` |
| `getSunriseColor(...)` | `null` (sem laranja do pôr do sol) |
| `renderSky(ClientLevel, ticks, partialTick, modelView, camera, projection, isFoggy, setupFog)` | desenha o céu do espaço (abaixo) e retorna `true` (vanilla não desenha nada por cima) |
| `renderClouds(...)` | `true` sem desenhar nada |
| `renderSnowAndRain(...)` | `true` sem desenhar nada |

**`renderSky` do espaço:**
1. **Fundo de cor única:** não desenhar disco nenhum. A cor de limpeza da tela já é a cor da névoa, e a névoa está forçada para a cor do espaço. Resultado: fundo uniforme em 360°, sem horizonte.
2. **Estrelas do overworld:** reaproveitar o **mesmo buffer de estrelas do vanilla** (mesmas posições) via accessor de mixin só de cliente:
   ```java
   @Mixin(LevelRenderer.class)
   public interface LevelRendererAccessor {
       @Accessor("starBuffer") VertexBuffer nexuscore$getStarBuffer();   // verificar nome do campo na 1.21.1
   }
   ```
   Desenhar com blending aditivo (como o vanilla), shader de posição e cor branca com **brilho fixo** (constante `STAR_BRIGHTNESS`, ex.: 0.8). Não usa `getStarBrightness()` do vanilla, que depende de hora e **chuva**.
   - Rotação: céu **parado** (o horário é fixo). Opcional depois: rotação bem lenta.
   - Plano B, se o accessor der problema: gerar o buffer de estrelas uma vez no cliente com o mesmo algoritmo (1500 pontos em posições aleatórias com semente fixa), fechado ao sair do mundo.
3. **Sem sol, lua ou disco de baixo.**

**Garantia contra gradiente em qualquer situação** (reforço do passo 1):
```java
@SubscribeEvent // barramento do NeoForge, Dist.CLIENT
static void onFogColor(ViewportEvent.ComputeFogColor event) {
    if (isInSpace(event.getCamera())) { event.setRed(R); event.setGreen(G); event.setBlue(B); }
}
```
O vanilla escurece/clareia a névoa em alguns casos (câmera abaixo da altura do horizonte, chuva, efeito de cegueira/visão noturna). Esse evento força a cor final no Space. **Exceção:** câmera dentro d'água/lava mantém a névoa vanilla (senão fica estranho dentro de fluido).

**Chuva do overworld "vazando":** dimensões secundárias compartilham o clima do overworld (`getRainLevel` > 0 quando chove no Nexus). Como o céu do Space usa cor e brilho de estrelas **fixos**, e o bioma não tem precipitação, a chuva do Nexus não afeta o Space (nem partícula, nem escurecimento).

**Cor do espaço:** constante única em `NexusPalette.SPACE_SKY`, usada no céu, na névoa e no bioma. Sugestão: `#000000` (preto puro) ou `#020308` (preto levemente azulado, com leitura mais "espacial"). Decidir vendo no jogo.

#### Custo
- Estrelas: um buffer que já existe no jogo, desenhado uma vez por frame (o overworld faz o mesmo).
- Sem nuvens, sem chuva, sem disco de céu: **mais barato que o overworld**.
- Nada roda no servidor; o servidor só envia o id `nexuscore:space` do tipo de dimensão, que o cliente associa aos efeitos.

#### Shaders (Iris): risco conhecido
Com shader ligado, o **shader pack** desenha o céu, não o nosso `renderSky`. Dimensões desconhecidas costumam ser tratadas como **overworld** pelo pack (sol, lua, nuvens do shader, gradiente).
- Testar com os packs recomendados (Complementary Reimagined, MakeUp Ultra Fast).
- Opções se ficar errado, em ordem de preferência:
  1. o próprio pack suportar dimensão customizada por configuração (`dimension.properties` do pack), mapeando o Space para um perfil sem sol/nuvens;
  2. aceitar que, com shader ligado, o céu do Space é o do shader;
  3. desligar shaders automaticamente ao entrar no Space pela API do Iris (**não recomendado**: recarregar shader trava alguns segundos a cada troca de dimensão).
- Decisão depois do teste.

#### Outros
- **Distant Horizons:** não tem efeito útil no Space (nada ao longe); não precisa de config.
- **Gravidade reduzida (opcional):** modificador no atributo `minecraft:generic.gravity` ao entrar no Space, removido ao sair (`PlayerChangedDimensionEvent`, login, logout).

**Como ficou (céu):**
- `SpaceSkyEffects extends DimensionSpecialEffects` com `SkyType.NONE` e `cloudLevel = NaN`; `renderSky`, `renderClouds` e `renderSnowAndRain` devolvem `true` sem desenhar nada além das estrelas.
- Devolvendo `true` em `renderSky`, o jogo sai da função na hora — inclusive **sem restaurar a névoa**, então o próprio `renderSky` chama o `setupFog` recebido, sempre (com ou sem estrelas).
- Estrelas: o buffer que o jogo já montou, lido por accessor de mixin (`LevelRendererStarsAccessor`), desenhado com a mesma sequência do vanilla (blend aditivo `SRC_ALPHA/ONE`, `depthMask(false)`, `GameRenderer.getPositionShader()`), com brilho fixo `0.8` e sem rotação.
- `ViewportEvent.ComputeFogColor` prende a cor final no Space; dentro d'água ou lava a névoa do jogo continua valendo.
- `modules.space.enabled = false` desliga só o céu próprio (a dimensão continua existindo).

#### Testes do céu
- [ ] Olhar para cima, para o horizonte e para baixo: **mesma cor**, sem faixa nem degradê
- [ ] Estrelas visíveis, nas mesmas posições do céu noturno do overworld, brilho constante
- [ ] Sem nuvens, sol, lua, laranja de pôr do sol
- [ ] Chovendo no Nexus: nada muda no Space
- [ ] Câmera abaixo da estação (Y baixo): céu não escurece
- [ ] Dentro d'água no Space (se houver): névoa normal de água
- [ ] Iluminação: fora das luzes quase escuro; dentro da estação iluminado pelos blocos de luz
- [ ] Com Sodium; com Iris sem shader; com shader (registrar resultado)
- [ ] F3: FPS no Space ≥ overworld na mesma máquina

### 5.9 Comandos e config

| Comando | Permissão | Função |
|---|---|---|
| `/lobby` | todos | voltar ao Space |
| `/nexus room tp <jogador>` | OP 2 | ir ao quarto de alguém |
| `/nexus room info <jogador>` | OP 2 | índice, posição, data de criação |
| `/nexus room rebuild <jogador>` | OP 3 | recolocar o template (reparo) |

Config (`lobby`): ligar/desligar, quartos por ala, tamanho do segmento, cooldowns, Y mínimo anti-void, OP pode construir, gravidade.

### 5.10 Desempenho e segurança

- Dimensão vazia: tick e memória desprezíveis; só os chunks da estação carregam com jogador perto.
- Segmento colocado só no primeiro login de jogador novo.
- Portas por login/logout e carregamento de chunk; nunca varrer todos os quartos.
- Registro por UUID, índice nunca reutilizado.
- `/lobby` e portal com cooldown; `/lobby` bloqueado em combate.
- Template versionado (`templateVersion`): mudar o design do quarto exige comando de migração, nunca sobrescrever quartos automaticamente (preserva decorações).

### 5.11 Testes
- [ ] Jogador novo → nasce no próprio quarto; corredor ganhou o quarto
- [ ] 21º jogador: nova ala criada
- [ ] Porta abre no login, fecha no logout; com chunk descarregado, corrige ao carregar
- [ ] Reiniciar servidor: registro e portas corretos
- [ ] Trocar de nick: mesmo quarto, placa atualizada
- [ ] Portal → Nexus (1ª vez spawn; depois última posição); portal de volta e `/lobby`
- [ ] `/lobby` bloqueado em combate e com cooldown
- [ ] Morte no Nexus sem cama → quarto; com cama → cama
- [ ] Space: sem quebrar/colocar, sem dano, void devolve ao quarto, sem mobs, sem chuva
- [ ] Painel no Space mostra estatísticas do Nexus
- [ ] Testes do céu (5.8)

---

## 6. Etapa 5: Estatísticas, recompensas, comunidade e economia

**Escopo:** recompensa alta para o **primeiro do servidor** e baixa para a **primeira vez pessoal**; chunks únicos; ponto mais distante; maior profundidade e altitude; tempo na dimensão; bestiário (só abates); baús de estrutura (com Lootr); mortes e mob mais letal; comunidade; economia.
**Fora:** dias desde a abertura da dimensão; itens/blocos obtidos pela primeira vez; loja da moeda.

Dimensão rastreada: **overworld ("Nexus")**. Space nunca é rastreado.

### 6.1 Módulos

Trackers **só registram e disparam eventos**; quem reage (moeda, feed, metas, anúncios) escuta e não é importado pelos trackers.

```
stats/
├─ api/event/          # DiscoveryEvent, RecordBrokenEvent, MilestoneEvent, PlayerDeathRecordedEvent
├─ data/               # GlobalStatsData (SavedData), PlayerStatsStore (arquivo por jogador)
├─ tracking/
│  ├─ PlayerSampler    # 1 amostra por jogador a cada N ticks
│  ├─ BestiaryTracker
│  ├─ DangerTracker
│  └─ LootTracker      # baús vanilla
├─ spawn/              # marca spawn não natural
└─ network/            # GlobalStatsPayload, PersonalStatsPayload
compat/lootr/          # só carrega com Lootr presente
economy/               # saldo, recompensas, ledger
community/             # feed, pioneiros, metas, expedições, rankings, títulos
display/               # páginas novas do painel
```

```
trackers / compat.lootr  ──(gravam e disparam)──►  eventos
                                                    ├─► economy   (credita moeda)
                                                    ├─► community (feed, metas, expedições, anúncios)
                                                    └─► sync      (dirty → pacote com debounce)
```

### 6.2 Dados

| Onde | O que | Por quê |
|---|---|---|
| `data/nexuscore_stats.dat` | descobertas globais, pioneiros, contadores globais, feed, metas concluídas, área mapeada global | já existe; vira `DATA_VERSION = 2` com migração da v1 |
| `nexuscore/players/<uuid>.dat` | chunks visitados, recordes, tempo, bestiário, baús, mortes, moeda ganha, contadores semanais | conjunto de chunks é grande; salvar só quem mudou |
| `data/nexuscore_economy.dat` | saldos | loja futura usa sem tocar em estatísticas |
| `nexuscore/economy-ledger.log` | cada crédito/débito (append-only) | auditoria |
| attachment de chunk `nexuscore:placed_containers` | containers colocados por jogador | distinguir baú de jogador |
| attachment de entidade `nexuscore:unnatural_spawn` | mob de ovo/comando/etc. | bestiário e anti-farm |

Regras:
- Arquivo por jogador: escrever em `.tmp` e mover (atômico), no ciclo de save do mundo, só jogadores `dirty`.
- Falha ao ler: renomear para `<uuid>.dat.corrupt-<data>`, logar ERROR, começar vazio. **Nunca derrubar o servidor.**
- Estatística e moeda gravadas no mesmo ciclo.
- Chunks: `LongOpenHashSet` (fastutil, já no jogo), salvo como `LongArrayTag`. 100 mil chunks ≈ 1–2 MB por jogador no pior caso.

### 6.3 Amostrador por jogador

Uma passagem por jogador a cada N ticks (padrão 20, offset pelo id), só em dimensão rastreada, só jogadores que contam (sem criativo/espectador; OP por config):

```
afk = (Util.getMillis() - player.getLastActionTime()) > limiteAfk      // verificar nome na 1.21.1
se !afk: tempoNaDimensao += N

chunk = ChunkPos(pos).toLong()
se chunk != ultimoChunk:
   ultimoChunk = chunk
   se chunksVisitados.add(chunk): marco de chunks; areaGlobal.add(chunk)
   refsEstrutura = structureManager.getAllStructuresAt(pos)

bioma e estrutura (já existem)
distância, profundidade, altitude: comparações de números
```

O(1) por jogador, sem alocação por amostra. Logout e troca de dimensão limpam o cache do jogador.

### 6.4 Estatísticas

**Chunks únicos visitados**
- Por jogador e área mapeada global.
- Recompensa por marco (a cada 100 chunks novos). Voltar a chunk já visitado não conta.

**Ponto mais distante**
- Distância **horizontal** até a origem configurável (padrão `0,0`), guardada ao quadrado (`long`).
- Recorde pessoal e do servidor; recompensa por marco (ex.: +1000 blocos) e ao bater o recorde do servidor.
- **Coordenadas nunca aparecem** em painel, feed ou chat.

**Maior profundidade e altitude**
- Menor/maior Y, limitado a `getMinBuildHeight()`..`getMaxBuildHeight()` (void não vira recorde).
- Recorde do servidor (alta) e marcos pessoais (ex.: a cada 32 blocos, baixa).

**Tempo na dimensão**
- Só soma sem AFK (padrão 5 min sem ação).
- Sem moeda (farmável); só estatística e ranking.

**Bestiário (só abates)**
- `LivingDeathEvent` com prioridade **LOWEST, ignorando cancelados** (totem/revive de outro mod não conta).
- Vítima não é jogador e `source.getEntity()` é `ServerPlayer` (golpe direto ou projétil). Pet domesticado não conta.
- **Spawn não natural não conta:** no `FinalizeSpawnEvent`, mobs de ovo, comando, dispenser, balde e procriação recebem o attachment `unnatural_spawn`. Spawner de estrutura conta (configurável).
- Por jogador: tipos + quantidade. Global: pioneiro por tipo.
- Recompensa: primeiro do servidor (alta), primeiro pessoal (baixa). Repetidos só somam no ranking.

**Mortes e mob mais letal**
- `LivingDeathEvent` (LOWEST, ignorando cancelados) para jogador em dimensão rastreada.
- Causa:
  - entidade viva não jogador → tipo da entidade;
  - jogador → `pvp` (fora do "mob mais letal");
  - sem entidade → tipo de dano (`source.typeHolder().unwrapKey()`).
- Contadores globais e por jogador. Sem recompensa.

**Baús de estrutura saqueados pela primeira vez**

Regra: conta só container **gerado pelo mundo** com loot table. Baú colocado por jogador **nunca** conta, mesmo dentro de estrutura.

1. **Tem loot table** (`RandomizableContainer#getLootTable() != null`). Baú de jogador não tem.
2. **Não foi colocado por jogador:** em `BlockEvent.EntityPlaceEvent`, container colocado entra no attachment `placed_containers` do chunk; sai no `BreakEvent`. Fecha a brecha de item com loot table (criativo/comando) e de mods que carregam baús.
3. Criativo/espectador não conta.

> Não exigir "estar dentro de estrutura": dungeons e alguns baús de mods são *features*, não estruturas. Os critérios 1 e 2 já excluem baús de jogador.

*Sem Lootr (vanilla):*
- `PlayerInteractEvent.RightClickBlock`: container com loot table e fora de `placed_containers` → pendente (jogador, dimensão, posição).
- `PlayerContainerEvent.Open` no mesmo tick, mesmo jogador → confirma e registra. Pendente expira no fim do tick.
- Vanilla: o primeiro leva tudo, só existe "primeiro do servidor".
- Minecart com baú: mesmo fluxo com `PlayerInteractEvent.EntityInteract`.

*Com Lootr (`compat.lootr`, versão alvo `1.21.1-1.11.38.125`):*
- Lootr só converte containers com loot table, então todo container Lootr é de estrutura. O critério 2 continua como defesa extra.
- Cada jogador abre o mesmo baú com o próprio loot:
  - **pioneiro** (primeiro do servidor) → alta;
  - **primeira vez pessoal** dos demais → baixa.
- Só **API pública** do Lootr (`compileOnly`), sem mixin:
  - `RightClickBlock`/`EntityInteract`: alvo implementa `ILootrInfoProvider` (**verificar** block entity e carrinho) → ler `getActualOpeners()` **antes** de abrir: sem o UUID do jogador = pendente pessoal; vazio = pendente pioneiro.
  - `PlayerContainerEvent.Open` confirma; conferir que o UUID entrou em `getActualOpeners()`.
  - Identidade: `getInfoDimension()` + `getInfoPos()` (ou UUID do carrinho).

Testes de baús:
- [ ] Estrutura: pioneiro alta; segundo jogador (Lootr) baixa; reabrir não paga
- [ ] Baú de jogador dentro de estrutura: não conta
- [ ] Item de baú com loot table colocado em criativo: não conta
- [ ] Minecart de mina: conta
- [ ] Hopper/quebra sem jogador: não conta e não trava registro futuro
- [ ] Com e sem Lootr

### 6.5 Comunidade

| Recurso | Como funciona |
|---|---|
| **Mural de pioneiros** | primeiro em cada bioma, tipo de estrutura, tipo de mob, e recordes |
| **Feed** | últimos 50 eventos (quem, o quê, quando), **sem coordenadas** |
| **Anúncios no chat** | só "primeiro do servidor" e recordes do servidor; agrupados (várias em 10 s = 1 mensagem); desligáveis |
| **Metas coletivas** | na config: métrica global + alvo + mensagem + recompensa para quem contribuiu. Só ações internas (anúncio, moeda), **nunca comandos vindos de config** |
| **Expedições** | descoberta de servidor: jogadores da mesma dimensão num raio (padrão 48 blocos) viram **co-pioneiros** e recebem a recompensa pessoal |
| **Rankings** | biomas, tipos de estrutura, estruturas, chunks, distância, profundidade, altitude, tempo, bestiário, baús, moeda ganha. Top 10 |
| **Ranking semanal** | mesmos contadores, zerados toda semana (semana ISO, fuso configurável) |
| **Títulos** | Cartógrafo (chunks), Andarilho (distância), Espeleólogo (profundidade), Alpinista (altitude), Arqueólogo (estruturas), Caçador (bestiário), Saqueador (baús). No painel; prefixo no chat fica para depois |

### 6.6 Eventos públicos

```java
DiscoveryEvent(ServerPlayer player, DiscoveryKind kind, ResourceLocation target,
               ResourceKey<Level> dimension, boolean firstInServer, List<UUID> coDiscoverers)
// kind: BIOME, STRUCTURE_TYPE, STRUCTURE_INSTANCE, MOB_KILL, LOOT_CONTAINER

RecordBrokenEvent(ServerPlayer player, RecordKind kind, long oldValue, long newValue, boolean serverRecord)
// kind: DISTANCE, DEPTH, ALTITUDE

MilestoneEvent(ServerPlayer player, MilestoneKind kind, long value)      // CHUNKS_VISITED...

PlayerDeathRecordedEvent(ServerPlayer player, ResourceLocation cause, boolean byEntity)
```

Só no servidor, disparados depois de o dado estar gravado.

### 6.7 Economia

**Saldo virtual** por UUID (`long`) no servidor: não duplica, não cai ao morrer, não é roubado. A loja futura só debita.

```java
long balance(UUID player);
void credit(UUID player, long amount, RewardReason reason, String referenceId);  // ex.: "biome:first:<dim>:<biome>"
boolean debit(UUID player, long amount, String reason, String referenceId);      // para a loja futura
```

**Recompensas iniciais** (config do servidor, balancear em teste):

| Evento | Primeiro do servidor | Primeira vez pessoal |
|---|---|---|
| Bioma | 100 | 10 |
| Tipo de estrutura | 100 | 10 |
| Instância de estrutura | — | 2 |
| Baú de estrutura | 15 | 5 |
| Tipo de mob abatido | 50 | 5 |
| Recorde de distância / profundidade / altitude | 25 | marco: 5 |
| Marco de chunks (a cada 100) | — | 5 |
| Meta coletiva | — | valor da meta |
| Tempo, mortes, abates repetidos | sem moeda | sem moeda |

**Anti-exploit e segurança**
- Só paga **primeira vez** ou **recorde/marco novo**; repetir nunca paga.
- Excluídos: criativo, espectador, OP (config), spawn não natural, baú de jogador.
- `Math.addExact` e saldo máximo; nunca negativo ou estourado.
- Todo crédito/débito no ledger (data, UUID, valor, motivo, `referenceId`), escrito em thread de IO.
- **Sem `/pay`** por enquanto (golpes e contas alternativas farmando). Reavaliar com a loja.

**Comandos**

| Comando | Permissão | Função |
|---|---|---|
| `/nexus balance` | todos | próprio saldo |
| `/nexus balance <jogador>` | OP 2 | saldo de outro |
| `/nexus eco give/take/set <jogador> <valor> <motivo>` | OP 3 | ajuste manual (vai para o ledger) |
| `/nexus stats [jogador]` | todos | resumo |
| `/nexus stats reset <jogador\|dimensão> confirm` | OP 3 | zerar |

### 6.8 Sincronização

| Payload | Para | Conteúdo | Quando |
|---|---|---|---|
| `GlobalStatsPayload` | todos | contagens, pioneiros, feed (50), top 10 por ranking, top 5 causas de morte, metas | debounce 1 s |
| `PersonalStatsPayload` | só o próprio jogador | estatísticas pessoais, saldo, posição nos rankings | login + debounce por jogador |

- Dados pessoais de um jogador nunca vão para outro além do que aparece em rankings.
- Nenhum payload cliente → servidor.
- Limites de tamanho em listas/strings; protocolo `"3"`.
- Nomes de jogadores offline resolvidos no servidor e enviados prontos.

### 6.9 Painel: páginas novas

Somam-se às da Etapa 3:

| Página | Conteúdo |
|---|---|
| **Pioneiros** | mural (6.5) |
| **Rankings** | uma categoria por vez, top 10, alternando geral/semanal |
| **Bestiário** | tipos abatidos no servidor, pioneiro de cada, total de abates |
| **Perigo** | mortes totais, mob mais letal, causas mais comuns |
| **Metas** | metas ativas e progresso; concluídas |
| **Você** | estatísticas de **quem está olhando** (via `PersonalStatsPayload`): chunks, recordes, tempo, bestiário, baús, saldo, posição nos rankings |

Mesma navegação local da 4.3.

### 6.10 Desempenho

| Parte | Custo |
|---|---|
| Amostrador | O(1) por jogador por segundo, sem alocação |
| Estruturas | consulta cara só em chunk com referência de estrutura |
| Abates/mortes/baús | por evento, O(1) |
| Rankings | recalculados só no envio e só se mudou |
| Save | só `dirty`; ledger em thread de IO |
| Cliente | layout cacheado por versão dos dados |

Validar com spark: 20+ jogadores/bots explorando, NexusCore irrelevante no perfil de tick.

### 6.11 Segurança e robustez

- Servidor é a única autoridade.
- Sem coordenadas em feed, chat, painel ou rankings.
- Configs com limites; valor inválido volta ao padrão com aviso.
- Metas não executam comandos.
- `compat.lootr` isolado; se a API mudar, desliga sozinho (capturar `LinkageError` na inicialização, logar uma vez).
- Arquivo corrompido não derruba o servidor.
- Nada `static` com dados de mundo sobrevive a `ServerStoppedEvent`.
- Migração versionada (`DATA_VERSION`), com backup antes.

### 6.12 Ordem de implementação

1. Dados v2 + migração + arquivos por jogador (testar save/load)
2. Amostrador: chunks, distância, Y, tempo
3. Bestiário + spawn não natural
4. Mortes / mob mais letal
5. Baús vanilla → `compat.lootr`
6. Economia (serviço, ledger, comandos)
7. Comunidade: feed, pioneiros, rankings, semanal, títulos, expedições, metas
8. Payloads + páginas novas
9. Balanceamento + spark

Cada passo: compilar, `runClient` + `runServer`, commit.

### 6.13 Testes
- [ ] Dados v1 migram para v2 sem perder descobertas
- [ ] Ir e voltar em chunks visitados não soma nem paga
- [ ] AFK não soma tempo
- [ ] Void não vira recorde de profundidade
- [ ] Ovo/comando: abate não entra no bestiário
- [ ] Totem: não conta morte
- [ ] Baús: testes de 6.4
- [ ] Expedição: a 30 blocos vira co-pioneiro; a 100 não
- [ ] Meta coletiva dispara uma vez
- [ ] Semanal zera na virada
- [ ] Saldo nunca negativo/estourado; tudo no ledger
- [ ] Nenhum jogador recebe dados pessoais de outro
- [ ] Reinício mantém tudo; arquivo corrompido não derruba
- [ ] Spark: impacto desprezível

---

## 7. Regras de arquitetura e código

- Módulos independentes; integrações opcionais em `compat/` e desligam sozinhas sem o mod alvo.
- Servidor é a autoridade; cliente só recebe e desenha.
- Código de cliente só em pacotes `client` / entrada `Dist.CLIENT` (servidor dedicado nunca carrega render).
- Trackers disparam eventos; economia/comunidade/painel escutam. `display` lê `stats`, nunca o contrário.
- Java 21 (`record`, pattern matching); classes pequenas, sem números mágicos.
- Nada estático mutável fora de caches limpos em `ServerStoppedEvent`/desconexão.
- Textos via `lang` (`pt_br.json` + `en_us.json`); build em **UTF-8**.
- Log: INFO status, WARN compat degradada, DEBUG rastreamento; nunca por tick.
- Mudança no formato de dados → `DATA_VERSION`; mudança de payload → versão do protocolo.
- Cada etapa: `runClient` + `runServer` testados, commit e tag.

---

## 8. Decisões

**Decididas**

| Tema | Decisão |
|---|---|
| Totais no painel | não mostrar; nem `???` |
| Textura conectada | pelo próprio bloco (multipart), sem Fusion |
| Dimensão de exploração | overworld, exibido como "Nexus"; Space nunca rastreado |
| Bestiário | só abates |
| Baús de estrutura | loot table + não colocado por jogador; Lootr por API pública |
| Moeda | saldo virtual; loja fica para depois |
| Céu do Space | sempre noite, estrelas do overworld, sem nuvens, sem sol/lua, **cor única sem gradiente** |
| Quartos | todos iguais por enquanto; personalização (paredes, teto, chão, decoração) no futuro |
| Space: ordem | dimensão + céu primeiro (4A), depois estação e quartos (4B) |
| Descartados | dias desde a abertura; itens/blocos pela primeira vez |

**Em aberto (com sugestão)**

| # | Decisão | Sugestão |
|---|---|---|
| 1 | Login: sempre no quarto ou só no primeiro acesso | sempre no quarto |
| 2 | Morte no Nexus sem cama: quarto ou spawn do Nexus | quarto |
| 3 | Portal do Space leva a: última posição ou spawn | última posição; 1ª vez spawn |
| 4 | Voltar ao Space | portal no spawn do Nexus + `/lobby` |
| 5 | Layout da estação (corredor, alas, andares, curva, paredão sem gravidade) | **em aberto**; decidir antes de abrir o servidor |
| 6 | Gravidade reduzida no Space | opcional, leve |
| 7 | Nether/End rastreados nas estatísticas | decidir |
| 8 | Origem do "ponto mais distante" | coordenada configurável, padrão 0,0 |
| 9 | Pet domesticado conta no bestiário | não |
| 10 | Mob de spawner de estrutura conta | sim |
| 11 | OP conta nas estatísticas/recompensas | não |
| 12 | Valores de recompensa | tabela de 6.7, balancear em teste |
| 13 | Cor exata do espaço | `#000000` ou `#020308`, ver no jogo |
| 14 | Brilho das estrelas | fixo ~0.8, ajustar em teste |
| 15 | Céu do Space com shader ligado | decidir após teste (5.8) |
