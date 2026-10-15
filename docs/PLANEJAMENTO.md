# NexusCore

Mod central do SMP: integrações com outros mods, configurações gerais, recursos visuais e o sistema de estatísticas da exploração.

- **Minecraft:** 1.21.1
- **Loader:** NeoForge 21.1.x
- **Java:** 21
- **Build:** ModDevGradle (`net.neoforged.moddev`)
- **Lado:** cliente + servidor (um único jar; o que é só de cliente fica isolado)
- **Mod ID:** `nexuscore`

> Documento de planejamento, seguido **na ordem**.
> Onde aparece **(verificar)**, é uma API do Minecraft/NeoForge cujo nome exato precisa ser conferido nos mapeamentos da 1.21.1 antes de usar.

### Status

| Fase | Estado |
|---|---|
| 0. Levantamento | feito: [`docs/FASE-0-levantamento.md`](FASE-0-levantamento.md) (faltam os prints de referência) |
| 1. Migração + revisão | **código pronto, aguardando teste em jogo** |
| 2. Nexus Resources | **pronto** (resource pack externo, ver [Fase 2](#fase-2-nexus-resources-resource-pack-externo)) |
| 2B. Interface do WaterFrames | **código pronto, aguardando teste em jogo** (desvios do plano em [Implementação](#implementação-o-que-mudou-em-relação-ao-plano)) |
| 3. Estatísticas | **código pronto, aguardando teste em jogo** (ver [Implementação da Fase 3](#implementação-da-fase-3)) |
| 4. Painel | **código pronto, aguardando teste em jogo** (ver [Implementação da Fase 4](#implementação-da-fase-4)) |
| 5. Futuro | só ideias, nada será feito agora |

As etapas seguintes (correções do painel, páginas, dimensão Space e lobby, estatísticas novas e economia) estão em [`docs/PROXIMAS-ETAPAS.md`](PROXIMAS-ETAPAS.md).

Hologramas com captura de movimento de jogador (gravar, reproduzir, áudio e efeito de projeção): [`docs/HOLOGRAMAS.md`](HOLOGRAMAS.md).

Mural de quests (publicar, assumir, concluir e o livro no inventário): [`docs/QUESTS.md`](QUESTS.md).

Backrooms (a dimensão, o gerador e o evento raro): [`docs/BACKROOMS.md`](BACKROOMS.md).
Rádio ao vivo (sessão, aviso e interface): [`docs/RADIO.md`](RADIO.md).
Vampiros (lordes, o cerco ao Vampirism e o que ficou de fora): [`docs/VAMPIRO.md`](VAMPIRO.md).
Anel da metamorfose (copiar e virar criaturas e jogadores): [`docs/MORPH.md`](MORPH.md).
Correio do Envelope (carteiro que vem de longe e o morcego do lorde): [`docs/ENVELOPE.md`](ENVELOPE.md).
Magia só para magos (Iron's Spells fechado para quem não é mago): [`docs/MAGIA.md`](MAGIA.md).
Papagaio de ombro (repete o que ouviu no chat de voz): [`docs/PAPAGAIO.md`](PAPAGAIO.md).
Relíquias (chave do refúgio, pena, coração, máscara, amuleto, pedra e bolsa): [`docs/RELIQUIAS.md`](RELIQUIAS.md).

### Compilar e instalar

```powershell
.\gradlew.bat build
```

- `build/libs/nexuscore-<versão>.jar` → pasta `mods` do modpack (substitui o jar do Xander's Sodium Options e o `vanillaframes`; os dois juntos aplicariam mixins duplicados). YACL e Yumi vão embutidos.
- `build/resourcepacks/Nexus Resources.zip` → pasta `resourcepacks` do modpack (ou use a pasta `resourcepacks/Nexus Resources` direto).

---

## Índice

1. [Visão geral](#1-visão-geral)
2. [Arquitetura](#2-arquitetura)
3. [Preparação: modpack e ambiente (antes de codar)](#preparação-modpack-e-ambiente-antes-de-codar)
4. [Fase 0: Levantamento do mod existente](#fase-0-levantamento-do-mod-existente)
5. [Fase 1: Migração para NexusCore + revisão de código](#fase-1-migração-para-nexuscore--revisão-de-código)
6. [Fase 2: Nexus Resources (resource pack externo)](#fase-2-nexus-resources-resource-pack-externo)
7. [Fase 2B: Interface do WaterFrames estilo Minecraft](#fase-2b-interface-do-waterframes-estilo-minecraft)
8. [Fase 3: Estatísticas do servidor (backend)](#fase-3-estatísticas-do-servidor-backend)
9. [Fase 4: Painel de exibição no mundo](#fase-4-painel-de-exibição-no-mundo)
10. [Fase 5: Futuro (leaderboard, moeda, interação)](#fase-5-futuro-leaderboard-moeda-interação)
11. [Ideias de estatísticas para o SMP](#ideias-de-estatísticas-para-o-smp)
12. [Padrões de código](#padrões-de-código)
13. [Decisões em aberto](#decisões-em-aberto)

---

## 1. Visão geral

| Módulo | O que faz | Lado | Depende de |
|---|---|---|---|
| `compat.sodium` | Código do Xander's Sodium Options portado para 1.21.1 e embutido no Core (substitui o jar do XSO) | Cliente | Sodium (opcional), YACL |
| `compat.waterframes` | Refaz o visual da interface do WaterFrames no estilo inventário do Minecraft (painel, botões, ícones cinza), mantendo layout e ordem dos controles | Cliente | WaterFrames 2.1.x + CreativeCore (opcional) |
| `compat.options` | Devolve a tela de opções ao layout de fábrica: tira do cabeçalho os botões que outros mods penduram ali (o Sable pendura o "Sub-Level Settings", que estoura a altura fixa do cabeçalho e joga o título contra o topo da tela) | Cliente | nenhum |
| `compat.cosmeticarmor` | Põe os botões do Cosmetic Armor no desenho do Curios: o quadrado preto de visibilidade vira o ícone do Curios na posição que ele usa (com a dica "Toggle Visibility"), e o peitoral que abre a tela fica dourado no hover, sem o nome escrito por cima ("CosmeticArmor" e "Normal") | Cliente | Cosmetic Armor Reworked |
| `compat.waterframes` (textos) | Correções de texto da interface do WaterFrames ("SALVAR" → "Salvar", "emitir luz" → "luz", "Shader mode" → minúsculo) via `assets/waterframes/lang/pt_br.json` **dentro do jar**: o `neoforge.mods.toml` declara `ordering = "AFTER"` para o waterframes, então os recursos do NexusCore vencem os dele. Não depende do resource pack estar ligado | Cliente | WaterFrames |
| *(fora do jar)* | Resource pack **Nexus Resources**, distribuído à parte no modpack: textura antiga de **madeira** do frame do WaterFrames (no lugar da azul) e futuros overrides de outros mods | Cliente | nenhum |
| `stats` | Rastreia biomas e estruturas descobertos, salva no mundo, sincroniza com clientes | Servidor + cliente | nenhum |
| `display` | Bloco "painel" que desenha as estatísticas na face do bloco, sem HUD e sem texto flutuante | Servidor + cliente | `stats` |
| `vampire` | O Vampirism cercado: só **lordes** (nomeados por comando) sobem de nível e usam habilidades; quem é mordido por um lorde vira vampiro comum, que só queima no sol e precisa de sangue. Caçador não existe, e nada de vampiro/caçador nasce sozinho no mundo | Servidor + cliente | Vampirism (opcional; sem ele o módulo não faz nada) |
| `backrooms` | Dimensão própria com um andar de corredores amarelos gerado por comando (planta no estilo do davidpcahill/backrooms), portas de saída soltas no meio do nada, o evento raro de atravessar o chão e o **skinwalker** — que usa a skin e a voz de alguém do servidor antes de virar outra coisa | Servidor + cliente | Simple Voice Chat (opcional, só para a voz); Xaero, PlayerRevive, Corpse e Curios (opcionais, ver [`docs/BACKROOMS.md`](BACKROOMS.md)) |
| `radio` | Rádio ao vivo: uma transmissão por vez, com aviso de canto no estilo conquista, plaquinha de "no ar" na HUD, botão na tela de pausa e **mesa de som** (música do YouTube pelo WaterMedia, volume da música e de cada voz). A voz pelo Simple Voice Chat vem depois | Servidor + cliente | WaterMedia (opcional, só para a música) |
| `morph` | O **anel da metamorfose**: bater numa criatura com ele na mão copia a forma dela; vestido na vaga de anel do Curios, transforma quem usa — modelo, animação, hitbox, sons, partículas e o básico do corpo (voar, nadar, respirar na água, não queimar), para todo mundo ver. Copia também outros jogadores (skin e nome). Ver [`docs/MORPH.md`](MORPH.md) | Servidor + cliente | Curios |
| `compat.envelope` | O correio do Envelope sem precisar de pombo por perto: carta esperando na caixa chama um **pombo carteiro** que surge longe, num ponto que ninguém está olhando, e vem voando buscar; ele some e surge longe da caixa (sem fumacinha à vista) e, ao terminar, vai embora voando em vez de sumir na frente de quem recebeu. Carta de **lorde vampiro** vai de **morcego**, e só à noite; quem deixa de ser lorde volta ao pombo sozinho. Ver [`docs/ENVELOPE.md`](ENVELOPE.md) | Servidor + cliente | Envelope 0.7.5+ (abaixo de 0.8); Vampirism (opcional, só para o morcego) |
| `magic` | O Iron's Spells 'n Spellbooks só para **magos** (nomeados por `/nexus mago`): para o resto, o nome de toda magia fica embaralhado nas runas do mod o tempo todo, as linhas de magia dos tooltips viram rabisco, não há barra de mana nem roda de magias, e nada mágico funciona — lançar, ler, beber, vestir joia, abrir mesa de magia. Estruturas, mobs e loot do mod continuam no mundo | Servidor + cliente | Iron's Spells 3.16 (opcional; sem ele o módulo não faz nada), Curios, JEI (opcional) |

Princípios:

- **Cada módulo é independente.** Se um mod de integração não estiver instalado, o módulo dele simplesmente não carrega: sem crash e sem mixin aplicado.
- **Servidor é a autoridade.** O cliente nunca informa estatística; só recebe.
- **Nada de código de cliente no servidor dedicado.** Classes de render/GUI só são carregadas em `Dist.CLIENT`.
- **Tudo configurável e desligável** por módulo.

---

## 2. Arquitetura

### 2.1 Estrutura de pastas

```
nexuscore/
├─ build.gradle.kts
├─ gradle.properties
├─ settings.gradle.kts
├─ docs/                                # levantamento da Fase 0
├─ resourcepacks/Nexus Resources/       # resource pack EXTERNO (ver Fase 2); o build gera o zip
├─ tools/sprites/                       # gerador dos sprites da interface do WaterFrames
├─ src/stubs/java/net/fabricmc/api/     # so para o javac: cala o aviso do @Environment (nao vai no jar)
└─ src/main/
   ├─ java/com/nexussmp/nexuscore/
   │  ├─ NexusCore.java                 # @Mod comum (servidor + cliente)
   │  ├─ NexusCoreClient.java           # @Mod(dist = Dist.CLIENT)
   │  ├─ core/
   │  │  ├─ module/                     # NexusModule, ModuleManager, ModuleIds
   │  │  ├─ config/                     # specs de config (CLIENT / COMMON / SERVER)
   │  │  ├─ network/                    # registro central de payloads (Fase 3)
   │  │  └─ util/                       # ModPresence, CompatRequirements
   │  ├─ compat/
   │  │  ├─ sodium/                     # ex-port do Xander's Sodium Options
   │  │  └─ waterframes/                # interface estilo Minecraft (Fase 2B)
   │  ├─ stats/
   │  │  ├─ data/                       # SavedData, records de descoberta
   │  │  ├─ tracking/                   # BiomeTracker, StructureTracker
   │  │  ├─ event/                      # eventos próprios (BiomeDiscoveredEvent...)
   │  │  ├─ network/                    # payloads de sync
   │  │  ├─ command/                    # /nexus estatisticas ...
   │  │  └─ client/                     # cache de estatísticas no cliente
   │  ├─ display/
   │  │  ├─ block/                      # PanelBlock
   │  │  ├─ blockentity/                # PanelBlockEntity
   │  │  ├─ multiblock/                 # detecção de tela NxM
   │  │  └─ client/render/              # PanelRenderer, layouts de página
   │  └─ mixin/
   │     ├─ NexusMixinPlugin.java       # só aplica mixins se o mod alvo existir
   │     ├─ sodium/
   │     └─ waterframes/
   └─ resources/
      ├─ META-INF/neoforge.mods.toml    # processado no build (versões vêm do gradle.properties)
      ├─ nexuscore.mixins.json
      ├─ assets/nexuscore/              # lang, blockstates, models, textures do mod
      └─ data/nexuscore/                # receitas, loot tables, tags
```

Pacote: `com.nexussmp.nexuscore` (decidido).

### 2.2 Sistema de módulos

Um contrato simples. Nada de framework: só organização e um ponto único de liga/desliga.

```java
public interface NexusModule {
    String id();                              // "compat.sodium", "stats"...
    default List<ModRequirement> requirements() { return List.of(); }   // mod id + faixa de versão testada
    default boolean clientOnly() { return false; }
    void register(IEventBus modBus, ModContainer container);
}
```

`ModuleManager` na inicialização:

1. Para cada módulo, verifica se os mods de `requirements()` estão carregados **e dentro da faixa testada** (`CompatRequirements`). Fora da faixa: módulo ignorado com aviso, sem crash.
2. Verifica se está habilitado na config (`modules.<id>.enabled`).
3. Se `clientOnly()` e estamos no servidor dedicado, pula.
4. Chama `register(...)` e loga **uma linha** por módulo: `[NexusCore] stats: ativo` / `compat.sodium: ignorado (sodium ausente)`.

> Configs do NeoForge são carregadas depois do construtor do mod. Então o registro de coisas obrigatórias (blocos, payloads) acontece sempre, e o "desligado" controla **comportamento** (tracking não roda, painel não renderiza). Registrar blocos condicionalmente quebraria mundos e a conexão cliente-servidor.

### 2.3 Pontos de entrada

```java
@Mod(NexusCore.MOD_ID)
public final class NexusCore {
    public static final String MOD_ID = "nexuscore";
    public NexusCore(IEventBus modBus, ModContainer container) { /* módulos comuns */ }
}

@Mod(value = NexusCore.MOD_ID, dist = Dist.CLIENT)
public final class NexusCoreClient {
    public NexusCoreClient(IEventBus modBus, ModContainer container) { /* módulos de cliente */ }
}
```

Assim o servidor dedicado nunca toca em `Minecraft`, `Screen`, renderers etc.

### 2.4 Configuração

Via `ModConfigSpec`, registrada com `container.registerConfig(...)`:

| Arquivo | Tipo | Conteúdo |
|---|---|---|
| `config/nexuscore-client.toml` | CLIENT | opções do port do Sodium, cores WaterFrames, qualidade do painel |
| `config/nexuscore-common.toml` | COMMON | liga/desliga de módulos |
| `<mundo>/serverconfig/nexuscore-server.toml` | SERVER | dimensões rastreadas, intervalo de checagem, quem conta (criativo/espectador), limites do painel |

Toda opção numérica com `defineInRange` (limites mínimo/máximo), para config corrompida ou editada à mão não quebrar nada.

### 2.5 Mixins opcionais

`neoforge.mods.toml`:

```toml
[[mixins]]
config = "nexuscore.mixins.json"

[[dependencies.nexuscore]]
modId = "sodium"
type = "optional"
versionRange = "[0,)"   # aberto de propósito: ver nota abaixo
ordering = "AFTER"
side = "CLIENT"
```

> **Decidido na implementação:** se o `versionRange` do toml fosse a faixa testada, o NeoForge **recusaria abrir o jogo** com um Sodium/WaterFrames de outra versão. Para cumprir "fora da faixa o módulo se desativa com aviso", o toml aceita qualquer versão e a faixa testada fica em `CompatRequirements` (Sodium `[0.8.12,0.9)`, WaterFrames `[2.1.23,2.2)`, CreativeCore `[2.13.13,2.14)`), checada pelo `ModuleManager` e pelo plugin de mixin.

`NexusMixinPlugin` (implementa `IMixinConfigPlugin`) decide se cada mixin é aplicado **pelo pacote**, usando a mesma checagem de versão:

```java
@Override
public boolean shouldApplyMixin(String target, String mixinClass) {
    if (mixinClass.contains(".mixin.sodium.integration.")) return ok(SODIUM) && ok(integração);
    if (mixinClass.contains(".mixin.sodium."))             return ok(SODIUM);
    if (mixinClass.contains(".mixin.waterframes."))        return ok(WATERFRAMES) && ok(CREATIVECORE);
    return true;
}
// ModList ainda não existe nessa etapa; LoadingModList sim (ModPresence)
```

Regras para mixins:
- Preferir **MixinExtras** (já vem no NeoForge): `@WrapOperation`, `@ModifyExpressionValue`, `@WrapMethod`. Evitar `@Overwrite` e `@Redirect`, que conflitam com outros mods.
- Mixins de compat com `require = 0` só onde a falha for aceitável; logar aviso uma única vez.
- Mixins de cliente ficam na lista `"client"` do JSON, nunca em `"mixins"`.

---

## Preparação: modpack e ambiente (antes de codar)

Objetivo: chegar na Fase 0 com versões travadas, uma instância limpa para testar e todo o material de referência em mãos.

### P.1 Backup e duas instâncias

1. **Backup** da instância atual do SMP (pasta `mods`, `config`, `resourcepacks`, `options.txt`) e do mundo do servidor.
2. **Instância "SMP Referência"**: cópia exata do modpack atual, **com o mod antigo**. Não mexer nela; serve para tirar prints e comparar comportamento.
3. **Instância "NexusCore Dev"**: modpack mínimo abaixo, para testar o NexusCore isolado (menos mods = log limpo e bugs fáceis de achar). Depois de estável, testar também no modpack completo.

### P.2 Decisão que vem antes de tudo: versão do Sodium

A 1.21.1 tem **duas linhas** de Sodium, e a escolha define a base do port do Xander's (Fase 1.3):

| | Sodium **0.6.13** (abr/2025) | Sodium **0.8.13** (ago/2026) |
|---|---|---|
| API de opções | antiga (mesma do XSO original 2.4.2 para 1.21.1) | **Config API nova** (backport da 1.21.6+, a mesma usada pelo XSO Continued) |
| Iris compatível | **1.8.12, estável** | 1.8.14-beta.1 (beta) |
| Base do port XSO | XSO original 2.4.2 (1.21.1 Fabric) | lado Sodium do XSO Continued; lado Minecraft precisa voltar para as APIs da 1.21.1 |
| Risco | baixo, tudo estável | Iris ainda beta; outros addons do Sodium podem não suportar 0.8 ainda |

**Decidido: Sodium 0.8.x** (0.8.13), que é a linha em que o port atual já roda, com **Iris 1.8.14-beta.1**. O SMP usa Iris.

- Enquanto o Iris para Sodium 0.8 estiver em beta, testar shaders a cada atualização dele.
- Antes do SMP abrir, atualizar Iris (e Sodium, se sair versão nova) para as versões estáveis, rodar os testes da Fase 1 de novo e só então travar as versões do modpack final.
- Na Fase 0, confirmar no `build.gradle` do mod antigo contra qual versão exata do Sodium ele compila.

### P.3 Modpack mínimo "NexusCore Dev"

Versões mais recentes para NeoForge 1.21.1 no momento (set/2026). **Travar essas versões** e só atualizar de propósito, uma de cada vez.

| Mod | Versão | Lado | Por quê |
|---|---|---|---|
| NeoForge | `21.1.250` (a mesma do perfil do SMP no Modrinth App; servidor igual) | ambos | base |
| Sodium | `0.8.13` (ver P.2) | cliente | alvo do `compat.sodium` |
| YetAnotherConfigLib (YACL) | `3.8.1+1.21.1-neoforge` (vem **dentro** do jar do port do XSO, via Jar-in-Jar) | cliente | tela do XSO; não instalar separado |
| Iris | `1.8.14-beta.1` (trocar pela estável quando sair) | cliente | usado no SMP; XSO tem compat com Iris |
| WaterFrames | `NEOFORGE-mc1.21.1-v2.1.23` | ambos | alvo do `compat.waterframes` |
| CreativeCore | `2.13.46` | ambos | GUI do WaterFrames (subiu sem erro com ela) |
| WaterMedia | **`2.1.37`** (exatamente essa) | ambos | única versão que satisfaz WaterFrames, WaterVision e o plugin do YouTube ao mesmo tempo (ver abaixo) |
| Plugin do YouTube do WaterMedia | `2.1.2` | ambos | vídeos do YouTube no WaterFrames |
| WaterVision | `NF-mc1.21.1-v0.1.0-alpha` | ambos | dependência obrigatória do WaterFrames desde a 2.1.22 |

**Não instalar / remover da instância Dev:**

| Mod | Motivo |
|---|---|
| Mod antigo (port do XSO + WaterFrames) | o NexusCore substitui; os dois juntos aplicam mixins duplicados. Fica só na "SMP Referência" |
| Xander's Sodium Options (original ou Continued) | a funcionalidade vai estar dentro do NexusCore |
| Resource pack antigo do frame (se estiver separado) | a textura vai para o Nexus Resources |
| Simply Screens | substituído pelo painel do NexusCore (Fase 4). No SMP real pode ficar até o painel estar pronto |
| `watermedia-3.0.0.23.jar` | linha 3.x (beta), incompatível com WaterFrames 2.1.23 |
| `watermedia_binaries-3.0.0.6.jar` | peça exclusiva da linha 3.x (exige WaterMedia ≥ 3.0.0.22); na 2.1.x os binários já vêm embutidos na WaterMedia |

**Problema já resolvido: WaterMedia na instalação do WaterFrames**

Sintoma: ao instalar WaterFrames + dependências "no mais atualizado" pelo Modrinth App, o jogo não abre:

```
Mod watervision requires watermedia 2.1.30 or above, and below 2.2 — Currently, watermedia is 3.0.0.23
Mod waterframes requires watermedia 2.1.34 or above, and below 2.2 — Currently, watermedia is 3.0.0.23
```

Causa: o launcher instala a WaterMedia **3.x** (beta), mas o WaterFrames 2.1.23 só aceita a **2.1.x**. Faixas lidas de dentro dos jars:

| Mod | Exige WaterMedia |
|---|---|
| WaterFrames 2.1.23 | `[2.1.34, 2.2)` |
| WaterVision 0.1.0-alpha | `[2.1.30, 2.2)` |
| Plugin do YouTube 2.1.2 | `[2.1.37, 2.2)` |
| WaterMedia Binaries 3.0.0.6 | `[3.0.0.22, )` → só serve na 3.x |

A interseção dá **exatamente 2.1.37**. Correção (só jars, **sem código**):

1. Instalar **WaterMedia 2.1.37** e **plugin do YouTube 2.1.2**.
2. Desativar `watermedia-3.0.0.23` e `watermedia_binaries-3.0.0.6` (no Modrinth App, desligar; ou renomear para `.jar.disabled`).
3. Nenhuma versão do Minecraft precisa voltar.

Isso **não é regressão**: na 3.x todas as versões da WaterMedia são beta e o WaterFrames ainda não suporta; na 2.1.x WaterMedia, WaterFrames e plugin do YouTube são todos release. Se o YouTube parar de funcionar um dia, normalmente é mudança do lado do YouTube (o extrator quebra de tempos em tempos), não versão do mod.

> Cuidado ao atualizar mods pelo Modrinth App: ele vai sugerir a WaterMedia 3.x de novo. **Não atualizar a WaterMedia** até o WaterFrames suportar a 3.x oficialmente.

**No servidor dedicado:** NeoForge + WaterFrames + CreativeCore + WaterMedia 2.1.37 + WaterVision + NexusCore. Sodium, Iris e YACL são só de cliente e não vão no servidor.

### P.3b Pacote de otimização (base do modpack de ~150 mods)

Objetivo: rodar em PC fraco **sem ficar feio**. Critérios da seleção (pesquisa em set/2026, versões conferidas no Modrinth para NeoForge 1.21.1):
- só mods com build **NeoForge 1.21.1** ativo e, de preferência, **release**;
- compatível com **Sodium 0.8.x** (quando o mod depende do Sodium, a faixa foi conferida);
- nada que substitua o que outro já faz (dois mods de culling de folhas, dois motores de chunk etc.);
- nada que mude gameplay sem config explícita.

Dependências (Cloth Config, Almanac, Configurable, Resourceful Config, YACL) o Modrinth App instala sozinho. **Continua valendo: não deixar ele atualizar a WaterMedia.**

#### Cliente (vão só no modpack dos jogadores)

| Mod | Versão | Função |
|---|---|---|
| Sodium | `0.8.13` | motor de renderização (a base de tudo) |
| Iris | `1.8.14-beta.1` | shaders (opcional por jogador; PC fraco joga sem) |
| **Sodium Extra** | `mc1.21.1-0.9.3+neoforge` | exige Sodium ≥ 0.8.12 (conferido no `mods.toml`). Dá ao PC fraco controles finos: partículas, animações de textura, neblina, nuvens, sombras de entidade. Aparece dentro da tela do XSO |
| ImmediatelyFast | `1.6.14` | acelera GUI, texto, mapas e HUD |
| EntityCulling | `1.10.5` | não desenha entidades/block entities escondidas atrás de blocos |
| More Culling | `1.0.10` | culling de folhas, blocos e molduras (requer Cloth Config) |
| Dynamic FPS | `3.11.4` | derruba FPS quando o jogo está minimizado/AFK |
| BadOptimizations | `2.4.1` | otimizações fora da renderização (tick de luz do cliente, etc.) |
| Model Gap Fix | `1.21-1.10` | corrige frestas nos modelos (visual) |
| Crash Assistant | `1.11.12` | com 150 mods, mostra na hora qual mod causou crash e o log |
| NexusCore | — | inclui o port do XSO (tela de opções do Sodium) |

#### Ambos (cliente **e** servidor)

| Mod | Versão | Função |
|---|---|---|
| FerriteCore | `7.0.3` | reduz muito o uso de RAM (essencial com 150 mods) |
| ModernFix | `5.27.24` | carregamento mais rápido, menos RAM, correções gerais |
| Lithium | `mc1.21.1-0.15.4-neoforge` | lógica do jogo (IA, física, blocos) mais leve, sem mudar comportamento. Versão **oficial** NeoForge |
| Packet Fixer | `3.3.1` | evita kick por pacote/NBT grande, comum em modpack grande |
| spark | `1.10.124-neoforge-1.21.1` | profiler para medir lag de verdade (`/spark profiler`) |
| Clumps | `19.0.0.1` | junta orbes de XP, menos entidades |

#### Servidor (e singleplayer; não precisam ir para o cliente em multiplayer)

| Mod | Versão | Função | Observação |
|---|---|---|---|
| **Chunky** | `1.4.23` | pré-gera chunks | **O maior ganho de todos** para um SMP de exploração (ver abaixo) |
| Structure Layout Optimizer | `1.0.12+1.21.1-neoforge` | geração de estruturas jigsaw mais rápida | bom com mods de estruturas; requer Resourceful Config |
| Let Me Despawn | `1.5.0` | mobs que pegaram itens voltam a despawnar | devolve os itens no chão; requer Almanac |
| Neruina | `3.3.3` | entidade/bloco que crasharia o servidor é congelado em vez de derrubar tudo | requer Configurable |
| Alternate Current | `neoforge-mc1.21-1.9.0` | redstone dust muito mais leve | ordem de atualização da redstone muda (farms técnicas extremas podem se comportar diferente) |

#### Visual leve (cliente, opcional, "continuar bonito")

| Mod / pack | Versão | Custo | Observação |
|---|---|---|---|
| Fusion | `1.3.15a` | baixo | texturas conectadas (vidro, estantes) com resource packs compatíveis |
| Better Clouds | `1.13.7` | baixo | nuvens bonitas estilo vanilla; requer YACL |
| LambDynamicLights | `4.8.11` | médio | tocha na mão ilumina; **testar primeiro**: se o Modrinth App pedir Forgified Fabric API para ele, não usar. Jogador pode desligar nas opções |
| Shader para PC médio | Complementary Reimagined (perfil Low/Potato) | alto | opcional por jogador |
| Shader para PC fraco | MakeUp Ultra Fast | médio | opcional por jogador |

#### Fase de teste 2: motores de chunk (**não** entram no pack inicial)

São os maiores ganhos de servidor, mas todos estão em **alpha/beta** e substituem partes centrais do jogo. Só testar depois do pack estável, medindo com spark antes e depois.

| Caminho | Mods | Status |
|---|---|---|
| **A** | C2ME NeoForge `0.4.0-alpha.0.120` + ScalableLux `0.3.0-alpha.0.8` + Fast Noise `1.0.13` | alpha, atualizações frequentes |
| **B** | Moonrise `0.1.0-beta.15` | beta, sem atualização para 1.21.1 desde out/2025 |

**Nunca misturar A com B.** Moonrise é incompatível com C2ME, ScalableLux, Starlight, Canary e Not Enough Crashes; Fast Noise é incompatível com Moonrise, NoisiumForked e AntiXray.

Também para depois: **ServerCore** `1.5.19` (tudo que muda gameplay vem desligado; ativar activation range/distância dinâmica com cuidado) e **AI Improvements** `0.5.3` (parado desde 2024).

#### Não instalar

| Mod | Motivo |
|---|---|
| Embeddium, Rubidium, Oculus, Embeddium Extra | substituídos por Sodium/Iris oficiais; conflitam |
| Reese's Sodium Options, Sodium Options API, Sodium Options Mod Compat | conflitam com o XSO |
| Sodium Dynamic Lights | parado desde jan/2025, feito para Sodium 0.6 |
| Canary, Radium, Starlight (NeoForge) | obsoletos; Lithium oficial já existe para NeoForge |
| Cull Leaves, Sodium Leaf Culling | duplicam o More Culling |
| BetterGrassify | sem atualização desde jun/2025 e puxa Forgified Fabric API inteira |
| Particle Core, AsyncParticles, GPUBooster, Flerovium, Ixeris | sem garantia com Sodium 0.8 / muito novos; puxam libs extras |
| Remove Reloading Screen | só beta para NeoForge 1.21.1 |
| Saturn, LazyDFU, Memory Leak Fix | obsoletos na 1.21.1 |
| Distant Horizons | pesado para PC fraco; se um dia entrar, só como opcional para PC forte |
| OptiFine | incompatível com tudo acima |

#### Configurações recomendadas

**Memória (Modrinth App → instância → Configurações → Java e memória):**

| Máquina | RAM alocada |
|---|---|
| PC com 8 GB de RAM | 4–5 GB |
| PC com 16 GB ou mais | 6–8 GB |
| Servidor (~150 mods, 10–20 jogadores) | 8–10 GB |

Alocar demais piora (pausas longas do coletor de lixo). Java 21 com os argumentos padrão já é bom no cliente.

**Padrão do modpack (`options.txt` + Sodium), pensado para PC fraco rodar de primeira:**
- Distância de renderização **8**, distância de simulação **6**
- Gráficos "Fancy" (folhas bonitas, o culling compensa), nuvens do Better Clouds ligadas
- Shaders **desligados** por padrão; quem tem PC bom liga
- Sodium Extra: partículas "Decreased" no preset leve; animações de textura ligadas

**Servidor (`server.properties`):** `view-distance=8`, `simulation-distance=6`.

**Pré-geração com Chunky (antes de abrir o SMP):** gerar a dimensão de exploração e o overworld dentro de uma borda. Chunk já gerado não custa CPU quando alguém explora, que é o que mais pesa num SMP de pioneiros.

```
/chunky world <id_da_dimensão>
/chunky center 0 0
/chunky radius 5000
/chunky start
```

Depois, `/worldborder` do mesmo tamanho (ou um pouco maior) para ninguém sair da área pré-gerada. Raio 5000 = ~2,5 GB de disco por dimensão (estimativa; ajustar conforme o espaço).

#### Como testar sem se perder

1. Adicionar em blocos, abrindo o jogo a cada bloco: **Cliente** → **Ambos** → **Servidor** → **Visual leve**.
2. Medir antes e depois num mesmo lugar do mundo: FPS no F3, e `/spark profiler start --timeout 120` andando/voando por chunks novos.
3. Se algo crashar, o Crash Assistant aponta o mod; tirar só ele e anotar aqui.
4. Só com tudo estável começar a colocar os ~130 mods de conteúdo, também em blocos.

### P.3c Chunks à distância (LOD): opcional

Na NeoForge 1.21.1 existem só duas opções reais (Voxy e o Bobby original são só Fabric):

| | **Distant Horizons** | **Bobby ReNeoForged** |
|---|---|---|
| Versão | `3.2.0-b-1.21.1` (beta; não existe release para 1.21.1) | `5.2.4-0.4+mc1.21` (release) |
| Como funciona | gera **LOD** (versão simplificada do terreno) e desenha até 256+ chunks | guarda os chunks que o servidor já mandou e desenha eles de verdade além da distância do servidor |
| Custo em distâncias grandes | **baixo** na GPU (LOD é leve), usa mais CPU/RAM para gerar | **alto**: é geometria completa, cada chunk extra pesa igual a um chunk normal |
| Custo em distância pequena (ex.: servidor 8 → cliente 16) | médio (threads de geração) | baixo |
| O que mostra | com DH no servidor, terreno ainda não visitado também (pré-gerado) | só o que o jogador já viu |
| Shaders | só shaders com suporte a DH (ex.: Complementary Reimagined) | qualquer |
| Problemas | beta; testar com Sodium 0.8 | **port não oficial**; **incompatível com EntityCulling** (que está no pack); incompatível com outros mods de distância |
| Lado | opcional no cliente **e** no servidor (quem não tem entra normal) | só cliente |

**Recomendação: Distant Horizons** como opcional. Bobby está fora porque conflita com o EntityCulling e é port não oficial. Nenhum dos dois entra no "Cliente Leve".

> **Atenção para o SMP de exploração:** com DH no servidor e a dimensão pré-gerada pelo Chunky, o servidor manda LOD de áreas que ninguém explorou, e o jogador vê o horizonte do mapa inteiro. Para manter o mistério, desligar a geração distante no servidor (`distantGeneration` / "Distant Generation" nas configs do DH): assim o LOD só existe do que o próprio jogador já visitou. Decisão em aberto.

Quem usar DH: somar ~1–2 GB de RAM ao que já foi recomendado.

### P.3d Modpacks "Servidor" e "Cliente" (até agora)

Serão **3 modpacks**: **Servidor**, **Cliente** e **Cliente Leve** (este fica para depois, quando entrarem os mods de partículas/efeitos que ele vai remover). A lista abaixo é **só o que já foi decidido**; revisar quando a primeira parte do modpack de conteúdo estiver pronta.

Lado conferido no Modrinth (campos client/server) e, onde importa, nas dependências declaradas.

#### Servidor (`NeoForge 21.1.250`)

| Mod | Versão | Por que está no servidor |
|---|---|---|
| **WaterFrames** | `NEOFORGE-mc1.21.1-v2.1.23` | blocos e sincronização dos vídeos: obrigatório nos dois lados |
| **CreativeCore** | `2.13.46` | dependência obrigatória do WaterFrames (GUI e rede) |
| **WaterVision** | `NF-mc1.21.1-v0.1.0-alpha` | obrigatório nos dois lados |
| WaterMedia | `2.1.37` | **testar sem**: o Modrinth marca como só cliente. Se o servidor não subir pedindo `watermedia`, adicionar (sem o plugin do YouTube) |
| **NexusCore** | quando existir | estatísticas, painel, blocos e rede |
| FerriteCore | `7.0.3` | menos RAM |
| ModernFix | `5.27.24` | inicialização e RAM |
| Lithium | `mc1.21.1-0.15.4-neoforge` | lógica do jogo |
| Packet Fixer | `3.3.1` | limites de pacote/NBT |
| spark | `1.10.124-neoforge-1.21.1` | profiler |
| Clumps | `19.0.0.1` | orbes de XP |
| Chunky | `1.4.23` | pré-geração |
| Structure Layout Optimizer | `1.0.12+1.21.1-neoforge` | geração de estruturas |
| Let Me Despawn | `1.5.0` | despawn de mobs com itens |
| Neruina | `3.3.3` | entidade bugada não derruba o servidor |
| Alternate Current | `neoforge-mc1.21-1.9.0` | redstone |
| *Distant Horizons (opcional)* | `3.2.0-b-1.21.1` | só se for mandar LOD aos jogadores (ver P.3c) |

Dependências que vêm junto automaticamente: **Almanac** (Let Me Despawn), **Configurable** (Neruina), **Resourceful Config** (Structure Layout Optimizer).

**Não vão no servidor:** Sodium, Iris, Sodium Extra, port do XSO, YACL, ImmediatelyFast, EntityCulling, More Culling, Cloth Config, Dynamic FPS, BadOptimizations, Model Gap Fix, Crash Assistant, plugin do YouTube, Fusion, Better Clouds, LambDynamicLights. Todos são só de cliente; alguns nem iniciam em servidor dedicado.

#### Cliente (`NeoForge 21.1.250`)

**Precisam bater com o servidor** (senão o jogador não entra):

| Mod | Versão |
|---|---|
| WaterFrames | `NEOFORGE-mc1.21.1-v2.1.23` |
| CreativeCore | `2.13.46` |
| WaterVision | `NF-mc1.21.1-v0.1.0-alpha` |
| NexusCore | quando existir |

**Só cliente:**

| Mod | Versão | Grupo |
|---|---|---|
| WaterMedia | `2.1.37` | vídeo (não atualizar) |
| WaterMedia: Youtube Extension | `2.1.2` | vídeo |
| Sodium | `0.8.13` | renderização |
| Iris | `1.8.14-beta.1` | shaders |
| Sodium Extra | `mc1.21.1-0.9.3+neoforge` | renderização |
| Port do XSO | `3.4.2+1.21.1-neoforge` (recompilado com UTF-8) | tela do Sodium; **sai quando o NexusCore existir** (YACL vem dentro) |
| ImmediatelyFast | `1.6.14` | otimização |
| EntityCulling | `1.10.5` | otimização |
| More Culling | `1.0.10` | otimização (+ Cloth Config) |
| Dynamic FPS | `3.11.4` | otimização |
| BadOptimizations | `2.4.1` | otimização |
| Model Gap Fix | `1.21-1.10` | visual |
| Crash Assistant | `1.11.12` | diagnóstico |
| Fusion | `1.3.15a` | visual leve |
| Better Clouds | `1.13.7` | visual leve (+ YACL) |
| LambDynamicLights | `4.8.11` | visual leve (testar; ver P.3b) |
| *Distant Horizons (opcional)* | `3.2.0-b-1.21.1` | distância |

**Também no cliente, mas só ajudam em singleplayer/LAN** (leves, ficam para testar mundo local): FerriteCore e ModernFix (esses **ajudam o cliente sempre**, RAM e carregamento), Lithium, Clumps, Packet Fixer (recomendado nos dois lados), spark (para medir FPS/lag do cliente).

**Não vão no cliente:** Chunky, Structure Layout Optimizer, Let Me Despawn (o Almanac dele é só servidor), Neruina, Alternate Current. São de servidor; em singleplayer não fazem falta para jogar no SMP.

**Mod antigo do WaterFrames** (cores + crafting + textura): não entra em nenhum dos dois. A remoção de crafting foi descartada e o resto vira NexusCore. Se quiser a textura de madeira antes disso, usar temporariamente só a pasta de textura como resource pack no cliente.

**Outros mods do perfil do SMP** (testar o NexusCore com eles antes de lançar, principalmente os que mexem em renderização de GUI):

| Mod | Atenção para |
|---|---|
| **ImmediatelyFast** 1.6.14 | agrupa o desenho da GUI; testar a Fase 2B (nine-slice e ícones cinza) com ele ligado |
| ModernFix 5.27.24 | carregamento dinâmico de recursos; testar a textura dessaturada após F3+T |
| More Culling 1.0.10, EntityCulling 1.10.5 | culling de block entities; testar o painel da Fase 4 (não pode sumir de vista) |
| BadOptimizations 2.4.1, Dynamic FPS 3.11.4, FerriteCore 7.0.3, Model Gap Fix 1.10, Cloth Config 15.0.140 | só smoke test |

### P.4 Material para a Fase 0

Existem **dois projetos antigos**. Mandar os dois, de preferência o **projeto-fonte inteiro** (pasta com `build.gradle`); se só existir o `.jar`, mandar o jar mesmo.

| Projeto antigo | O que tem | O que aproveitamos |
|---|---|---|
| **Fork recompilado do Xander's Sodium Options** (port 1.21.1) | todo o trabalho de fazer o XSO rodar na 1.21.1 com Sodium 0.8. Projeto em `Desktop\xso-port` (build multi-versão, alvo `versions/1.21.1-neoforge`); gera `xanders-sodium-options-3.4.2+1.21.1-neoforge.jar`, ou seja, partiu do **XSO Continued 3.4.2**. Embute via Jar-in-Jar: YACL 3.8.1, Yumi MC Foundation 1.0.0-alpha.15 e MixinSquared 0.2.0 | **Essencial.** É a base do módulo `compat.sodium`. Sem ele, o port teria que ser refeito do zero |
| **Mod do WaterFrames** (cores da tela + remoção de crafting + pasta de textura) | textura antiga de madeira; mixins que já funcionaram no WaterFrames | **Textura: essencial** (é a arte pronta). Mixins: referência de quais alvos funcionam. Visual da tela: será refeito (Fase 2B). Crafting: **descartado** (ver [Fora de escopo](#fora-de-escopo)) |

Base confirmada: **XSO Continued 3.4.2** (pelo nome do jar). Se o projeto tiver git, a diferença exata do que foi alterado sai com um `git diff` contra a tag/commit da 3.4.2.

**Bug conhecido do port antigo:** o botão "Open Original Screen ➔" aparecia como "Open Original Screen åž". O código tem `Component.literal("➔")` (U+2794); o build não definia encoding e o `javac` leu os `.java` como windows-1252 (padrão do Windows pt-BR), quebrando a seta em três caracteres. A correção foi adicionar `options.encoding = "UTF-8"` no `build.neoforge.gradle.kts` e recompilar. **Confirmar na Fase 0 se o jar instalado já é o recompilado.** No NexusCore isso já nasce configurado (ver P.5).

- [ ] Projeto/jar do fork do Xander's Sodium Options
- [ ] Projeto/jar do mod do WaterFrames, **incluindo a pasta da textura de madeira**
- [ ] Listagem da pasta `mods` do SMP real (nomes dos arquivos = versões exatas)
- [ ] Pasta `config` do mod antigo
- [ ] `logs/latest.log` de um boot limpo da "SMP Referência"
- [ ] Prints de todas as telas do WaterFrames (frame, TV, projetor, controle remoto, playlist), incluindo o **ícone de status ao lado da URL** em cada estado: sem URL, carregando, funcionando, erro
- [ ] Print do frame no mundo com a textura antiga de madeira
- [ ] Print da tela de opções do Sodium com o XSO

### P.5 Ambiente de desenvolvimento

- **JDK 21** (Temurin ou Microsoft Build of OpenJDK)
- **IntelliJ IDEA** + plugin *Minecraft Development*
- Template oficial: **MDK 1.21.1 ModDevGradle** (repositório `NeoForgeMDKs/MDK-1.21.1-ModDevGradle`)
- **Git** desde o primeiro commit (repositório público, por causa da LGPL do código do XSO)
- **Encoding UTF-8 obrigatório** no build (lição do bug "åž" do port antigo). No `build.gradle`:
  ```groovy
  tasks.withType(JavaCompile).configureEach { options.encoding = 'UTF-8' }
  ```
  e arquivos `.java`/`.json`/`.toml` salvos em UTF-8 (`.editorconfig` com `charset = utf-8`)
- **Jar-in-Jar** das bibliotecas que o port do XSO já embute (YACL 3.8.1, Yumi MC Foundation; o MixinSquared saiu porque nenhum mixin do NexusCore precisa), para os jogadores não instalarem nada à parte
- Dependências dos outros mods puxadas pelo **Maven do Modrinth** (`https://api.modrinth.com/maven`), com as mesmas versões da tabela P.3:
  - `compileOnly` para compilar contra WaterFrames/CreativeCore/Sodium/YACL
  - `runtimeOnly` (ou pasta `run/mods`) para testar com eles no `runClient`

**Pronto quando:** as duas instâncias abrem sem crash, a "SMP Referência" está com os prints feitos e o projeto MDK compila e abre com `runClient`.

---

## Fase 0: Levantamento do mod existente

**Entrada:** os dois projetos antigos (ver [P.4](#p4-material-para-a-fase-0)):
1. fork recompilado do Xander's Sodium Options para 1.21.1;
2. mod do WaterFrames (cores da tela, remoção de crafting, textura de madeira).

Antes de mexer em qualquer coisa, gerar um inventário:

- [ ] Mod ID, nome e pacote atuais
- [ ] Lista de **todos os mixins**: alvo, tipo de injeção, motivo
- [ ] Versões de Sodium, YACL e WaterFrames para as quais foi feito
- [ ] De qual versão do XSO o port partiu (original 1.21.x Fabric ou Continued 1.21.6+)
- [ ] Arquivo(s) de config atuais e suas chaves
- [ ] Onde está a textura antiga (madeira) do frame e **qual caminho** ela sobrescreve (esperado: `assets/waterframes/textures/block/frame.png`)
- [ ] Como a mudança de cor do WaterFrames é feita hoje (mixin? evento? resource pack?)
- [ ] Versão exata do WaterFrames e do CreativeCore instalados no SMP; abrir o jar do WaterFrames e confirmar as classes listadas na Fase 2B
- [ ] Prints de **todas** as telas do WaterFrames (frame, TV, projetor, controle remoto, playlist) no estado atual, incluindo a tela/aviso de "sem vídeo"
- [ ] Chaves de tradução (`lang`) usadas
- [ ] Comportamento atual documentado com prints, para comparar depois da refatoração
- [ ] Separar o que é da remoção de crafting (receitas, datapack, mixins de receita) e **marcar como não migrar**

**Pronto quando:** existe uma lista "funcionalidades que não podem mudar", usada como checklist de regressão na Fase 1.

### Fora de escopo

- **Desativar crafting dos blocos do WaterFrames** (exceto o frame): existia no mod antigo, **não entra no NexusCore**. Vai ser feito depois por outro meio (outro mod ou configs/datapack do servidor).

---

## Fase 1: Migração para NexusCore + revisão de código

### 1.1 Renomear

- [x] Mod ID → `nexuscore`, nome → `NexusCore`
- [x] Pacote → `com.nexussmp.nexuscore`, reorganizado nas pastas da seção 2.1
- [x] `nexuscore.mixins.json`, `neoforge.mods.toml`, `gradle.properties` (`mod_id`, `mod_name`, `mod_group_id`)
- [x] Namespace de assets/lang → `nexuscore` (chaves `xso.*` mantidas para não perder as traduções)
- [x] **Migração de config:** `config/xanders-sodium-options.json` é copiado para `config/nexuscore-sodium-options.json` na primeira inicialização
- [x] Remover o jar antigo do modpack (o `[WIP]` não tem XSO nem `vanillaframes`)

### 1.2 Revisão: o que será verificado em cada arquivo

**Eficiência**
- Nada de alocação dentro de métodos chamados a cada frame (render, tick): listas, `String.format`, lambdas que capturam variáveis, `Component.literal` recriado.
- Nada de reflection em caminho quente; se inevitável, resolver uma vez e guardar em `MethodHandle` estático.
- Valores de config lidos uma vez e cacheados, atualizados em `ModConfigEvent.Reloading`.
- Nada de trabalho de cliente rodando no servidor e vice-versa.

**Segurança e robustez**
- Nenhuma classe de cliente referenciada por código comum (evita crash no servidor dedicado).
- Todo acesso a mod opcional protegido (plugin de mixin + `ModList`).
- Entradas de config com limites; valores inválidos voltam ao padrão com aviso.
- Sem `catch (Exception e) {}` silencioso: logar uma vez com contexto.
- Sem arquivos lidos/escritos fora de `config/` e do save do mundo.

**Código limpo**
- Classes `final` e construtores privados em utilitários.
- `record` para dados imutáveis.
- Nomes descritivos; métodos curtos; sem código morto nem comentários desatualizados.
- Constantes em vez de números mágicos (cores, tamanhos).
- Logger único: `private static final Logger LOGGER = LogUtils.getLogger();`

### 1.3 Módulo `compat.sodium`

**Decisão de arquitetura:** o código do Xander's Sodium Options (XSO) passa a viver **dentro do NexusCore**, como código-fonte do módulo. O jar do XSO **não** é instalado no modpack; o Core substitui ele.

Descartado: "Core adaptando o jar do XSO 1.21.6+/1.21.11 em tempo de execução". Motivos:
- O NeoForge recusa carregar o jar (ele declara Minecraft/NeoForge de outra versão).
- Mixin altera classes existentes; não resolve chamadas do XSO para classes/métodos que não existem na 1.21.1 (GUI reescrita na 1.21.6, eventos de input mudaram depois, API do Sodium diferente conforme a versão, YACL de outra versão).
- Fazer funcionar exigiria reescrever o bytecode de centenas de chamadas (transformer ASM): um port invisível, frágil e impossível de depurar.

Como funciona o XSO: um mixin intercepta a abertura da tela de opções do Sodium e, no lugar dela, monta uma tela do **YACL** (YetAnotherConfigLib) convertendo cada página/grupo/opção do Sodium em opções do YACL. Tem compat extra para Iris, Sodium Extra etc.

Implementação:
- Base: **o código do mod atual** (Fase 0), um port do **XSO Continued 3.4.2** que já roda com Sodium 0.8. Se algum trecho precisar ser refeito, a referência é o próprio **XSO Continued**: usa a mesma Config API do Sodium (backportada para 1.21.1 na 0.8.12), então só a parte de GUI/input do Minecraft precisa voltar para as APIs da 1.21.1. O XSO original 2.4.2 **não** serve de base, porque foi feito para a API antiga do Sodium 0.6.
- Sempre que possível, integrar pela **Config API oficial do Sodium** em vez de mixin em classes internas. É mais estável entre atualizações do Sodium.
- `versionRange` do Sodium travado na linha 0.8 (`[0.8.12,0.9)`), porque a API muda entre linhas.
- Dependências do módulo: Sodium (opcional) + YACL para NeoForge 1.21.1. Sem qualquer um dos dois, o módulo se desativa sem crash. Avaliar embutir o YACL via Jar-in-Jar do NeoForge para os jogadores não precisarem instalar à parte.
- Mantém 100% das telas/opções do port atual.
- Todos os mixins do Sodium em `mixin/sodium/`, aplicados só com Sodium presente.
- Se a versão do Sodium for incompatível (fora do `versionRange`), o módulo se desativa com aviso em vez de crashar.
- **Licença:** o XSO é LGPLv3. Os arquivos derivados dele mantêm o cabeçalho/aviso LGPL e o código-fonte do NexusCore deve ficar disponível (ex.: repositório público). Alternativa simples: licenciar o NexusCore inteiro como LGPL-3.0.

### 1.4 Módulo `compat.waterframes`

- Na Fase 1 só migra o que já existe (cores vanilla), sem regressão.
- Mixins em `mixin/waterframes/`, aplicados só com WaterFrames presente.
- O redesenho completo da interface é a [Fase 2B](#fase-2b-interface-do-waterframes-estilo-minecraft), que substitui a troca de cores atual.

### 1.5 Testes de aceitação da Fase 1

- [ ] `runClient` **com** Sodium e WaterFrames → tudo igual ao mod antigo (checklist da Fase 0)
- [ ] `runClient` **sem** Sodium → inicia sem erro, módulo marcado como ignorado
- [ ] `runClient` **sem** WaterFrames → idem
- [ ] `runServer` (servidor dedicado) → inicia sem erro de classe de cliente
- [ ] Nenhum aviso de mixin no log além dos esperados
- [ ] Config antiga migrada corretamente

---

## Fase 2: Nexus Resources (resource pack externo)

### Nome

**Nexus Resources**. Simples, diz o que é e não amarra o pack ao frame, já que ele vai receber mais texturas.

### Decisão: pack normal, fora do jar

O plano original embutia o pack no mod (`AddPackFindersEvent`). **Mudou:** o Nexus Resources é um **resource pack comum**, distribuído na pasta `resourcepacks` do modpack. Motivo: ele vai sobrescrever texturas de **outros mods** também, então precisa existir e ser editável independente do NexusCore.

Divisão das texturas:

| Onde | O quê |
|---|---|
| **jar do NexusCore** (`src/main/resources/assets/nexuscore/`) | arte própria do mod: sprites da interface do WaterFrames (Fase 2B) e, depois, o bloco do painel (Fase 4). O mod funciona completo sem o pack |
| **Nexus Resources** (`resourcepacks/Nexus Resources/`) | overrides de texturas de outros mods (hoje: WaterFrames). Também pode sobrescrever sprites do `nexuscore`, para ajustar o visual sem recompilar |

### Estrutura

```
resourcepacks/Nexus Resources/
├─ pack.mcmeta
├─ pack.png                                 # face do frame antigo, ampliada
└─ assets/
   └─ waterframes/textures/block/
      ├─ frame.png                          # textura ANTIGA de madeira do frame (substitui a azul atual)
      └─ displays.png                       # partícula/quebra dos blocos de display, no mesmo estilo
```

**Textura do frame:** é a textura de madeira do projeto anterior (`Desktop\waterframes-skin`), **mantida byte a byte**, já com a correção da face invertida. Caminhos confirmados na Fase 0.

> `displays.png` é compartilhada por **todos** os blocos de display do WaterFrames (TV, projetor...). Como o SMP só libera o frame, não afeta nada; se um dia liberarem os outros, as partículas deles também ficam de madeira.

`pack.mcmeta` (1.21.1 usa `pack_format` 34 para resource packs):

```json
{
  "pack": {
    "pack_format": 34,
    "description": "Texturas do SMP (NexusCore)"
  }
}
```

### Distribuição

- `.\gradlew.bat build` gera `build/resourcepacks/Nexus Resources.zip` (task `packNexusResources`).
- No modpack: colocar o zip (ou a pasta) em `resourcepacks/` e ativar na tela de Resource Packs.
- Para vir ativo por padrão: incluir `"file/Nexus Resources.zip"` na lista `resourcePacks` do `options.txt` padrão do modpack.

### Regra para futuras texturas

Cada substituição fica no caminho original do asset (`assets/<mod>/textures/...`) e é registrada numa tabela no fim deste README (asset, mod, motivo). Assim sabemos o que quebra quando um mod mudar de versão.

---

## Fase 2B: Interface do WaterFrames estilo Minecraft

### Objetivo

Hoje a tela do WaterFrames parece "post-it roxo/azul": painéis de cor chapada, borda grossa de várias camadas, ícones azulados. Queremos que ela pareça **uma tela do próprio Minecraft** (inventário/baú):

- Painel cinza claro com borda chanfrada (claro em cima/esquerda, escuro embaixo/direita) e contorno preto arredondado.
- Botões, checkboxes, sliders e campos de texto **iguais aos do vanilla**.
- Ícones **monocromáticos (cinza)**, inclusive o seletor de posição do vídeo.
- Cores neutras em tudo; nada de azul/roxo. **Exceção: vermelho continua indicando erro** (decidido).
- Escopo: **só o WaterFrames**. A tela do Sodium não muda; o visual vanilla dela já vem do XSO (decidido).
- **Layout, ordem dos controles e funcionamento continuam idênticos.** Só muda o visual.

### Como a interface do WaterFrames é feita (importante)

**Não é YACL.** O YACL é usado só pela tela de opções do Sodium (XSO). O WaterFrames 2.1.x (versão para NeoForge 1.21.1) desenha as telas com a biblioteca de GUI do **CreativeCore**.

Classes relevantes (conferidas no código-fonte da 2.1.22; confirmar no jar da versão instalada na Fase 0):

| Classe | Papel |
|---|---|
| `me.srrapero720.waterframes.common.screens.DisplayScreen` | Tela do frame/TV/projetor. Sobrescreve `getStyle()` e os métodos de fundo/borda |
| `...screens.RemoteControlScreen` / `PlayListScreen` | Tela do controle remoto / playlist |
| `...screens.styles.ScreenStyles` | **Todas as cores** (`BLUE_BORDER`, `DARK_BLUE_BACKGROUND`, `SCREEN_BACKGROUND`...) e os `GuiStyle` `DISPLAYS` / `REMOTE_CONTROL` com `borderThickWidth = 6`, que é a borda grossa |
| `...screens.styles.IconStyles` | Todos os ícones, recortados de uma grade 16×16 em `assets/waterframes/textures/screen_atlas.png` |
| `...screens.widgets.WidgetClickableArea` | Seletor de posição do vídeo (3×3). Desenha `POS_BASE` + `POS_ICON` do atlas; tooltip usa `ChatFormatting.AQUA` (azul) |
| `...screens.widgets.WidgetStatusIcon` | Ícone de status ao lado da URL (ok, carregando, **idle/sem URL**, falha de vídeo, buffering...) |
| `team.creative.creativecore.common.gui.style.GuiStyle` | Estilo do CreativeCore: campos públicos `background`, `secondaryBackground`, `border`, `borderThick`, `clickable`, `clickableHighlight`, `disabled`, `fontColor`..., cada um um `StyleDisplay` |
| `...style.display.*` | Implementações prontas: `DisplayColor`, `DisplayTexture`, `DisplayTextureRepeat`, `DisplayTextureStretch` |

Consequência: **dá pra refazer todo o visual sem reescrever as telas.** Basta trocar o estilo (o que é desenhado) e os ícones (a textura), deixando a lógica e o layout do WaterFrames intactos.

### Referência visual: a tela de inventário

Alvo aprovado: o **fundo e a borda** da tela de inventário do Minecraft (sem os slots). Os controles do WaterFrames continuam nos mesmos lugares.

O "degradê" do inventário não é um degradê de verdade: é um **chanfro em pixel art**, com faixas de cor sólida que dão a sensação de relevo. Anatomia (em pixels da GUI, escala 1):

```
contorno   1px  #000000  (cantos arredondados: pixel do canto transparente)
luz        2px  #FFFFFF  em cima e à esquerda   → borda "clara"
sombra     2px  #555555  embaixo e à direita    → borda "escura"
transição       #C6C6C6 nos cantos onde luz e sombra se encontram (canto sup-dir e inf-esq)
miolo           #C6C6C6
título          #404040, sem sombra (como "Crafting")
```

Áreas internas "afundadas" (como o quadro preto do jogador ou os slots) usam o chanfro **invertido**: sombra `#373737` em cima/esquerda, luz `#FFFFFF` embaixo/direita, miolo `#8B8B8B`. Isso vira o sprite `inset`, usado atrás do seletor de posição, do ícone de status e das seções.

### É possível com CreativeCore?

**Sim.** O CreativeCore só decide **onde** cada fundo e borda aparece. **O que** é desenhado vem do `StyleDisplay`, e podemos colocar o nosso (`NineSliceDisplay`), que desenha exatamente os sprites acima. O resultado é pixel a pixel igual ao inventário.

Pontos que exigem cuidado para ficar bonito (conferir na Fase 0 com o jar em mãos):

| Ponto | Risco | Solução |
|---|---|---|
| **Coordenadas fracionadas** | CreativeCore trabalha com `double` e escala própria; nine-slice em posição quebrada fica com borda de 1px borrada ou irregular | arredondar x/y/largura/altura para inteiros no `NineSliceDisplay` antes de desenhar |
| **Sombra no texto** | o CreativeCore pode desenhar rótulos sempre com sombra; texto escuro com sombra fica feio no cinza claro | se o estilo não permitir desligar, mixin pontual no desenho de texto do CreativeCore só quando o estilo ativo for o nosso |
| **Cores fixas em widgets** | algum widget (slider, campo de texto, barra de progresso/seek) pode pintar cor direto no código, sem passar pelo `GuiStyle` | listar na Fase 0; mixin só nesses widgets |
| **Borda dupla** | o CreativeCore desenha `border` + `borderThick` além do fundo; com o sprite já tendo borda, sobraria contorno extra | `border`/`borderThick` transparentes e `borderThickWidth` = espessura da borda do sprite |
| **Escala da GUI** | sprite pensado para escala 1 pode ficar desproporcional em escala 4 | é o comportamento vanilla (tudo escala junto); testar escalas 1 a 4 |

### Estratégia técnica

#### 1. Sprites nine-slice (visual de inventário)

O Minecraft 1.21.1 já suporta sprites de GUI que esticam só o miolo e preservam as bordas (nine-slice), via `.png.mcmeta`:

```json
{ "gui": { "scaling": { "type": "nine_slice", "width": 16, "height": 16, "border": 4 } } }
```

Criamos sprites próprios (arte nossa, mesmo estilo do vanilla) em `assets/nexuscore/textures/gui/sprites/waterframes/`:

| Sprite | Uso | Referência visual |
|---|---|---|
| `panel` | fundo da janela | inventário (ver [anatomia](#referência-visual-a-tela-de-inventário)): `#C6C6C6`, chanfro `#FFFFFF`/`#555555`, contorno `#000000` arredondado |
| `inset` | seções internas, seletor de posição, ícone de status | slot: `#8B8B8B`, chanfro invertido `#373737`/`#FFFFFF` |
| `field` | campos de texto/número | campo vanilla: fundo `#000000`, borda `#A0A0A0` |

Para **botões, checkbox e slider** usamos direto os sprites do próprio jogo (`minecraft:widget/button`, `button_highlighted`, `button_disabled`, `checkbox`, `checkbox_selected`, `slider`, `slider_handle`...). Assim, se o jogador usar um resource pack que muda os botões do Minecraft, o WaterFrames acompanha.

#### 2. `NineSliceDisplay`: um `StyleDisplay` nosso

O CreativeCore desenha tudo chamando `StyleDisplay.render(...)` **(verificar assinatura exata)**. Criamos uma implementação que desenha um sprite nine-slice:

```java
public record NineSliceDisplay(ResourceLocation sprite) implements StyleDisplay {
    @Override
    public void render(GuiGraphics graphics, double x, double y, double width, double height) {
        graphics.blitSprite(sprite, (int) x, (int) y, (int) width, (int) height);
    }
}
```

Com estado (normal / hover / desativado) resolvido por campos diferentes do `GuiStyle` (`clickable`, `clickableHighlight`, `clickableInactive`...).

#### 3. `NexusWaterFramesStyle`: o estilo novo

Um único `GuiStyle` montado pelo Core, com tudo neutro:

```java
GuiStyle s = new GuiStyle();
s.background          = new NineSliceDisplay(sprite("panel"));
s.secondaryBackground = new NineSliceDisplay(sprite("inset"));
s.clickable           = new NineSliceDisplay(mc("widget/button"));
s.clickableHighlight  = new NineSliceDisplay(mc("widget/button_highlighted"));
s.clickableInactive   = new NineSliceDisplay(mc("widget/button_disabled"));
s.border              = DisplayColor.NONE;   // borda vem do próprio sprite (verificar como "sem borda" é representado)
s.borderThickWidth    = 4;                   // = borda do nine-slice, em vez dos 6px de camadas
s.fontColor           = /* #404040 sobre painel claro */;
s.fontColorHighlight  = /* #FFFFA0 igual hover vanilla */;
s.fontColorDisabled   = /* #A0A0A0 */;
```

Valores de cor centralizados em `NexusPalette` (constantes), sem número mágico espalhado.

> Atenção à legibilidade: texto branco com sombra (padrão do CreativeCore) fica ilegível sobre `#C6C6C6`. Rótulos no painel usam `#404040` sem sombra (como o título "Inventário"). Texto dentro de botões e campos continua branco, igual ao vanilla.

#### 4. Aplicar o estilo (mixins mínimos)

Em `mixin/waterframes/`, só com WaterFrames presente:

| Mixin | Alvo | O que faz |
|---|---|---|
| `DisplayScreenMixin` | `DisplayScreen#getStyle` + métodos de fundo/borda | retorna `NexusWaterFramesStyle` / displays nossos |
| `RemoteControlScreenMixin` | idem em `RemoteControlScreen` | idem |
| `PlayListScreenMixin` | idem em `PlayListScreen` | idem |
| `ScreenStylesMixin` | `<clinit>` de `ScreenStyles` (TAIL) com `@Shadow @Mutable @Final` | troca `BLUE_*` e `DARK_BLUE_*` por cinzas. `RED_*` **continua vermelho** (erro), só ajustado para um tom próximo do vermelho vanilla. Pega qualquer widget que use essas cores direto, sem precisar achar um por um |
| `WidgetClickableAreaMixin` | `getTooltip` (`@ModifyArg`/`@WrapOperation` no `ChatFormatting.AQUA`) | tooltip da posição em cinza/branco |

Preferência por `@ModifyReturnValue`/`@WrapOperation` (MixinExtras). Nenhum `@Overwrite`.

Alternativa sem mixin para parte disso: `GuiStyle` tem campos públicos, então alterar `ScreenStyles.DISPLAYS` e `REMOTE_CONTROL` direto no `FMLClientSetupEvent` já cobre boa parte. Usar isso onde der e mixin só no que for `final` ou sobrescrito nas telas.

#### 5. Ícones em cinza

Todos os ícones vêm de **uma única textura** (`waterframes:textures/screen_atlas.png`). Duas opções:

| Opção | Como | Prós | Contras |
|---|---|---|---|
| **A. Dessaturar em tempo de execução** (recomendada para começar) | Um reload listener (`RegisterClientReloadListenersEvent`) lê o atlas original do WaterFrames, converte cada pixel para cinza (luminância `0.299R + 0.587G + 0.114B`, mantendo alpha) e registra o resultado no `TextureManager` no mesmo `ResourceLocation` | Zero arte para fazer; acompanha atualizações do WaterFrames; **não redistribui** a textura dele (WaterFrames é All-Rights-Reserved) | Alguns ícones podem perder contraste; ajustável com curva de brilho |
| **B. Atlas próprio no Nexus Resources** | Desenhar ícones nossos na mesma grade 16×16 e colocar em `assets/waterframes/textures/screen_atlas.png` | Controle total do visual, pode ficar bem "pixel art vanilla" | Trabalho de arte; se o WaterFrames mudar a grade, quebra |

Plano: começar com **A**. Se algum ícone ficar ruim, desenhar só aquele e trocar pela opção B aos poucos.

**Ícones de erro continuam vermelhos:** a dessaturação pula as células do atlas que indicam erro, definidas numa lista única:

```java
// células (coluna, linha) da grade 16×16 que mantêm a cor original
static final Set<Cell> KEEP_COLOR = Set.of(
    new Cell(4, 1),   // STATUS_ERROR
    new Cell(3, 4),   // STATUS_INTERNAL_ERROR
    new Cell(4, 4)    // STATUS_INTERNAL_ERROR_2
);
```

Confirmar na Fase 0 (com os prints) se `failed.video` e `not_working` usam essas células; se usarem outra, ela entra na lista. Aviso (`STATUS_WARN`) e botões de ação como remover ficam cinza.

Para a opção A, a textura dessaturada é gerada **uma vez por reload** (F3+T / troca de pack), nunca por frame. O `NativeImage` original é fechado depois do uso (`try-with-resources`).

> Se o jogador ativar um pack que já substitui o atlas (inclusive o nosso, na opção B), a dessaturação usa a versão do pack, ou pode ser desligada na config (`waterframes.grayscaleIcons`).

#### 6. Seletor de posição do vídeo

- Fundo 3×3 (`POS_BASE`) e marcador (`POS_ICON`) ficam cinza automaticamente pelo item 5.
- A área recebe o sprite `inset` por trás, pra parecer um slot do inventário.
- Tooltip sem azul (item 4).

#### 7. "Sem vídeo" / status (confirmado: fica na área da URL)

É o `WidgetStatusIcon`, o ícone de 30×30 ao lado do campo de URL na `DisplayScreen`. Ele troca de ícone e tooltip conforme o estado:

| Estado | Chave de tradução | Visual novo |
|---|---|---|
| Sem URL | `waterframes.status.idle` | ícone cinza |
| Desligado | `waterframes.status.off` | ícone cinza |
| Carregando / buffering | `waterframes.status.loading` / `buffering` | ícone cinza |
| Funcionando / cache | `waterframes.status.operative` / `cache` | ícone cinza |
| Player de vídeo indisponível | `waterframes.status.failed.video` | **vermelho** (erro) |
| Não funciona / erro | `waterframes.status.not_working` / `download.exception.invalid` | **vermelho** (erro) |

Resolve com os itens 4 e 5: ícones cinza exceto erro, fundo `inset` atrás do ícone e tooltip sem cores azuis (título em branco, descrição em cinza `#A0A0A0`, erro em vermelho).

### Configuração (cliente)

```toml
# config/nexuscore-client.toml
[waterframes]
    vanillaStyle = true       # painel/botões estilo Minecraft
    grayscaleIcons = true     # ícones em cinza
    keepErrorRed = true       # vermelho só em erro (decidido); false = tudo cinza

# config/nexuscore-common.toml  (liga/desliga geral, igual aos outros módulos)
[modules.compat.waterframes]
    enabled = true            # false = volta tudo ao visual original do WaterFrames
```

### Implementação: o que mudou em relação ao plano

Conferido no bytecode do WaterFrames 2.1.23 e CreativeCore 2.13.46 (detalhes em [`docs/FASE-0-levantamento.md`](FASE-0-levantamento.md)):

| Plano | Realidade | O que foi feito |
|---|---|---|
| `NineSliceDisplay` como `record implements StyleDisplay` | `StyleDisplay` é **classe abstrata** | `final class NineSliceDisplay extends StyleDisplay`, arredondando posição e tamanho |
| painel em `background`, `border` transparente | o CreativeCore desenha a borda no retângulo inteiro e o fundo **encolhido** pela largura da borda | painel vai em `borderThick` (largura 4 = borda do nine-slice), `background` = `NONE` |
| `fontColor` do estilo pinta os rótulos | `CompiledText` tem cor e sombra próprias (branco com sombra) e ignora o `GuiStyle` | `GuiLabelMixin`/`PanelText`: rótulo direto no painel sai `#404040` sem sombra, só quando o estilo ativo é o nosso |
| sprites `minecraft:widget/checkbox*` | checkbox do CreativeCore tem **7×7** fixo; o vanilla é 20×20 | sprites próprios `checkbox`, `checkbox_highlighted` e `checkmark` em 7×7 |
| sprite próprio `field` | vanilla 1.21.1 já tem `widget/text_field(_highlighted)` | usa o vanilla; só `field_error` (borda vermelha) é nosso |
| `ScreenStylesMixin` trocando cores estáticas | `ScreenStyles` é compartilhado por widgets com papéis diferentes (URL, playlist, controle remoto) | mixins pontuais por widget, sem mexer nas constantes; `ChatFormatting` azul/roxo/verde vira neutro, vermelho só em erro |
| — | slider, barra de rolagem e campo de texto do CreativeCore desenham com `clickable` | `GuiSliderMixin`, `GuiScrollYMixin`, `GuiTextfieldMixin` com sprites vanilla (`slider`, `slider_handle`, `scroller`, `text_field`) |
| MixinSquared embutido | nenhum mixin do NexusCore precisa | removido |

Todos os mixins do CreativeCore só agem quando `getStyle()` devolve **o nosso** `GuiStyle` (comparação por identidade). Outros mods que usam CreativeCore não são afetados.

Sprites próprios gerados por código em `tools/sprites/generate-waterframes-sprites.cjs` (`node tools/sprites/generate-waterframes-sprites.cjs`).

### Riscos e compatibilidade

- **Depende de classes internas do WaterFrames 2.1.x.** Fixar `versionRange` da dependência na faixa testada; fora dela o módulo se desativa sozinho, com aviso.
- **WaterFrames 3.0** (em alpha no GitHub) troca o CreativeCore por um sistema próprio (WaterUI) que já tem temas, inclusive um tema "minecraft". Quando o SMP migrar para a 3.x, este módulo é reescrito e provavelmente vira só um tema.
- **Licença:** WaterFrames é All-Rights-Reserved. Não copiamos código nem texturas dele. Usamos mixins, arte própria e transformação da textura em tempo de execução.

### Testes de aceitação da Fase 2B

- [ ] Frame, TV, projetor, controle remoto e playlist: layout e ordem idênticos aos prints da Fase 0
- [ ] Nenhum azul/roxo visível em nenhuma tela (checar hover, desativado, tooltip)
- [ ] Texto legível em todos os fundos
- [ ] Seletor de posição funciona igual (clicar e arrastar nas 9 posições)
- [ ] Interface do jogo em escala 1, 2, 3 e automática sem bordas quebradas
- [ ] F3+T recarrega ícones cinza sem vazamento de memória
- [ ] Com `compat.waterframes.enabled = false`, visual original volta
- [ ] Sem WaterFrames instalado, o jogo inicia normalmente

---

## Fase 3: Estatísticas do servidor (backend)

Objetivo: saber, para o servidor inteiro e por jogador, **quais biomas e estruturas foram descobertos, por quem e quando**, na(s) dimensão(ões) de exploração.

### 3.1 Definições

| Termo | Definição |
|---|---|
| **Bioma descoberto** | Um jogador que conta (config: sobrevivência/aventura; criativo e espectador excluídos por padrão) esteve fisicamente dentro do bioma. |
| **Estrutura descoberta** | O jogador entrou na caixa de uma **peça** da estrutura (mesmo critério das conquistas vanilla, tipo "encontrar uma Fortaleza"). Só passar perto não conta. |
| **Tipo de estrutura** | `minecraft:ancient_city`, `mod:ruina_x`... (conta "tipos descobertos") |
| **Instância de estrutura** | Uma estrutura específica no mapa, identificada por `tipo + chunk de origem`. (conta "quantas foram encontradas") |
| **Pioneiro** | Primeiro jogador a descobrir aquele bioma/tipo de estrutura no servidor. |

### 3.2 Modelo de dados

Salvo no save do mundo via `SavedData` (arquivo `data/nexuscore_stats.dat`, dentro da pasta do mundo principal):

```java
record Discovery(UUID discoverer, long gameTime, long epochMillis) {}

final class ExplorationData extends SavedData {
    // por dimensão rastreada
    Map<ResourceKey<Level>, Map<ResourceLocation, Discovery>> biomes;           // bioma -> pioneiro
    Map<ResourceKey<Level>, Map<ResourceLocation, Discovery>> structureTypes;    // tipo -> pioneiro
    Map<ResourceKey<Level>, Map<StructureInstanceId, Discovery>> structureInstances;
    // por jogador (para leaderboard futuro)
    Map<UUID, PlayerExploration> players;   // conjuntos de biomas/tipos/instâncias do jogador
}

record StructureInstanceId(ResourceLocation type, long startChunkPos) {}
```

Acesso:

```java
ExplorationData data = server.overworld().getDataStorage().computeIfAbsent(
    new SavedData.Factory<>(ExplorationData::new, ExplorationData::load, null),
    "nexuscore_stats");
```

- Escrita via `save(CompoundTag, HolderLookup.Provider)`; `setDirty()` a cada nova descoberta. O jogo salva junto com o mundo.
- IDs sempre como `ResourceLocation` (string). Nunca salvar `Holder`/objetos de registro.
- Leitura tolerante: IDs de biomas/estruturas de mods removidos são mantidos no arquivo (histórico) e marcados como "desconhecido" na exibição.
- Guardado por UUID, o que já inclui jogadores offline para leaderboards.

### 3.3 Rastreamento de biomas

Evento `PlayerTickEvent.Post` (barramento do jogo, só no servidor):

```
a cada N ticks (config, padrão 20) por jogador, com offset pelo id do jogador para espalhar a carga:
  se jogador não conta (modo de jogo / dimensão não rastreada) → sai
  pos = player.blockPosition()
  biomeKey = level.getBiome(pos).unwrapKey()
  se biomeKey == últimoBiomeDoJogador → sai            // cache em memória, sem custo
  últimoBiomeDoJogador = biomeKey
  registrar(jogador, biomeKey)                          // O(1) em HashMap
```

`registrar`:
1. Adiciona ao conjunto do jogador. Se já tinha, termina.
2. Se o bioma não tinha pioneiro no servidor, grava `Discovery` e dispara `BiomeDiscoveredEvent(player, biome, firstInServer = true)`.
3. Senão dispara `BiomeDiscoveredEvent(..., firstInServer = false)`.
4. `setDirty()` e marca "estatísticas mudaram" para o sync (3.6).

**Total possível** (para exibir "12 / 31"):

```java
level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()
```

Calculado uma vez quando o servidor inicia e cacheado. Funciona com biomas de mods e datapacks.

### 3.4 Rastreamento de estruturas

Mais caro que bioma, então em duas etapas:

```
ao mudar de chunk (comparar ChunkPos com o último do jogador):
  refs = level.structureManager().getAllStructuresAt(pos)     // Map<Structure, LongSet>: só referências do chunk, barato
  guarda refs no cache do jogador (vazio = nenhuma estrutura por perto)

a cada N ticks, SÓ se refs não estiver vazio:
  para cada structure em refs:
    start = level.structureManager().getStructureWithPieceAt(pos, structure)
    se start.isValid():
      id = StructureInstanceId(chaveDoRegistro(structure), start.getChunkPos().toLong())
      registrar(jogador, tipo, id)
```

- A maioria dos chunks não tem estrutura, então na prática o custo é quase zero.
- Chave do registro: `level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure)`.
- **Total de tipos possíveis** na dimensão: via `level.getChunkSource().getGeneratorState().possibleStructureSets()` **(verificar)**. É um teto teórico: alguns tipos podem não gerar por causa de biomas.
- `registrar` segue a mesma lógica dos biomas, disparando `StructureDiscoveredEvent`.

### 3.5 Eventos próprios (base para leaderboard e moeda)

Disparados no `NeoForge.EVENT_BUS`, **só no servidor**:

```java
public final class BiomeDiscoveredEvent extends Event {
    ServerPlayer player(); ResourceKey<Level> dimension();
    ResourceLocation biome(); boolean firstInServer();
}
public final class StructureDiscoveredEvent extends Event {
    ServerPlayer player(); ResourceKey<Level> dimension();
    ResourceLocation structureType(); StructureInstanceId instance();
    boolean firstTypeInServer(); boolean firstInstanceForPlayer();
}
```

Moeda, anúncios no chat, conquistas, sons etc. **escutam** esses eventos. O tracker não sabe que eles existem, e isso mantém tudo desacoplado.

### 3.6 Sincronização com o cliente

Networking da 1.21.1: `CustomPacketPayload` + `RegisterPayloadHandlersEvent`.

```java
public record StatsSnapshotPayload(List<DimensionStats> dimensions) implements CustomPacketPayload {
    public static final Type<StatsSnapshotPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(NexusCore.MOD_ID, "stats_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsSnapshotPayload> STREAM_CODEC = /* ... */;
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

// registro
PayloadRegistrar registrar = event.registrar("1");   // versão do protocolo
registrar.playToClient(StatsSnapshotPayload.TYPE, StatsSnapshotPayload.STREAM_CODEC, ClientStatsHandler::handle);
```

Fluxo:
- **Login** (`PlayerEvent.PlayerLoggedInEvent`): envia snapshot completo para aquele jogador.
- **Mudança:** o tracker só marca `dirty`. Um tick do servidor com debounce (config, padrão 1 s) envia o snapshot/delta para todos com `PacketDistributor.sendToAllPlayers(...)`. Dez descobertas no mesmo segundo viram um pacote só.
- O conteúdo enviado é **só o que o painel exibe**: contagens, lista de IDs, nome do pioneiro, últimas N descobertas. Não é o arquivo inteiro.

Segurança:
- Nenhum payload cliente → servidor nesta fase.
- Codecs com limites: `ByteBufCodecs.stringUtf8(maxLen)`, listas com tamanho máximo. Um pacote malformado não aloca memória infinita.
- Handlers rodam na thread principal (padrão do NeoForge 21.x); nada de estado compartilhado entre threads.

### 3.7 Cache no cliente

`ClientStatsCache` (só cliente):
- Guarda o último snapshot recebido e um `int version` que incrementa a cada atualização.
- Limpo ao desconectar (`ClientPlayerNetworkEvent.LoggingOut`).

### 3.8 Comandos

Registrados em `RegisterCommandsEvent`:

| Comando | Permissão | Função |
|---|---|---|
| `/nexus estatisticas` | todos | resumo no chat |
| `/nexus estatisticas jogador <jogador>` | todos | estatísticas de um jogador |
| `/nexus estatisticas zerar <dimensão>` | OP nível 3 | zera dados (pede confirmação com `confirmar`) |


### Implementação da Fase 3

**Como a contagem funciona (nada é escaneado).** O servidor não procura estruturas pelo mapa. Ele só olha onde cada jogador está e registra o que é novo. As contagens começam em zero e sobem conforme o pessoal explora.

| Contagem | Tem máximo? | De onde vem o máximo |
|---|---|---|
| Biomas descobertos | sim (`12 / 54`) | lista de biomas que o gerador da dimensão consegue criar |
| Tipos de estrutura | sim (`5 / 34`) | conjuntos de estrutura que o gerador consegue criar (já filtrados pelos biomas da dimensão) |
| Estruturas encontradas (instâncias) | **não** | o mapa é infinito; é só um contador |

Os máximos são tetos teóricos: um bioma raro pode não existir dentro da área. Contar quantas vilas *existem* numa área exigiria simular a geração de cada ponto candidato (o mesmo trabalho do `/locate`, repetido centenas de vezes), então ficou de fora.

**Área rastreada (quadrado a partir do centro).** Só conta descoberta feita com o jogador dentro do quadrado `|x − centerX| ≤ radius` e `|z − centerZ| ≤ radius`. Padrão: centro `0, 0`, `radius = 5000` (quadrado de 10000 × 10000). `radius = 0` = sem limite. Vale em cada dimensão rastreada, nas coordenadas dela.

**Custo por jogador**, a cada `checkIntervalTicks` (padrão 20 = 1 s, com deslocamento pelo id do jogador para espalhar a carga): uma leitura de bioma (só registra se mudou) e, ao trocar de chunk, a lista de estruturas que tocam aquele chunk. A verificação "está dentro de uma peça" só roda quando essa lista não é vazia.

Config do servidor (`<mundo>/serverconfig/nexuscore-server.toml`):

```toml
[stats]
    trackedDimensions = ["minecraft:overworld"]
    checkIntervalTicks = 20
    countCreative = false
    countSpectator = false
    syncDelayTicks = 20
    [stats.area]
        centerX = 0
        centerZ = 0
        radius = 5000
```

Diferenças do plano:
- `Discovery` guarda também o **nome** do jogador na hora da descoberta, para painel e chat não dependerem do cache de perfis.
- O "descobertas recentes" guarda as últimas 50 primeiras-vezes do servidor (bioma, tipo de estrutura, nova instância); o pacote leva as 20 mais novas.
- `StructureDiscoveredEvent` tem também `firstInstanceInServer()`.
- Operadores **contam** (se estiverem no sobrevivência). Criativo e espectador não, por padrão.

### 3.9 Testes de aceitação da Fase 3

- [ ] Entrar em bioma novo registra uma vez; sair e voltar não duplica
- [ ] Entrar numa estrutura registra tipo + instância; outra instância do mesmo tipo registra só a instância
- [ ] Passar ao lado da estrutura sem entrar não registra
- [ ] Criativo/espectador não conta (com config padrão)
- [ ] Reiniciar o servidor mantém tudo
- [ ] Remover um mod de biomas não corrompe o arquivo
- [ ] Com 20+ jogadores simulados, o tracker não aparece no profiler (`/debug start` ou Spark)

---

## Fase 4: Painel de exibição no mundo

Uma tela física no mundo. As informações são desenhadas **na face do bloco**, como o texto de uma placa: sem HUD, sem armor stands, sem texto flutuante.

### 4.1 Bloco e block entity

- `PanelBlock`: direção horizontal (`FACING`), emite luz baixa (opcional), sem colisão especial.
- `PanelBlockEntity` guarda:
  - `page`: `SUMMARY`, `BIOMES`, `STRUCTURES`, `RECENT` (futuro: `LEADERBOARD`)
  - `dimension`: qual dimensão exibir
  - dados do multiblock (controlador, largura, altura)
- Sincronizado com o cliente por `getUpdateTag(HolderLookup.Provider)` + `ClientboundBlockEntityDataPacket.create(this)`.
- Configuração da página: por comando ou item (clique com item de configuração) na Fase 4; por clique na tela na Fase 5.
- Quem pode colocar/configurar: config do servidor (padrão: só OP, para evitar spam de painéis).

### 4.2 Multiblock (telas maiores)

Ao colocar/remover um painel (`onPlace`, `onRemove`, `neighborChanged`):

1. Busca (BFS limitada) painéis vizinhos no mesmo plano e com a mesma direção.
2. Encontra o maior retângulo completo que contém o bloco.
3. Define o **controlador** (canto inferior esquerdo, do ponto de vista de quem olha) e grava `largura × altura` nele. Os demais apontam para o controlador.
4. Tamanho máximo configurável (padrão 8 × 5) para limitar custo de render e BFS.

Só o controlador renderiza, usando a área inteira. Os outros blocos não desenham nada além da própria textura.

### 4.3 Render

`PanelRenderer implements BlockEntityRenderer<PanelBlockEntity>`, registrado em `EntityRenderersEvent.RegisterRenderers` (só cliente).

```
render(be, partialTick, poseStack, buffers, packedLight, overlay):
  se não é controlador → return
  se jogador está longe (> distância de config) → return        // economia
  poseStack.push
    translada para a face frontal (+ 0.001 para evitar z-fighting)
    rotaciona conforme FACING
    escala: 1 bloco = 1/16 * fator (texto nítido)
    desenha fundo: quad texturizado (textura em assets/nexuscore/textures/panel/)
    layout = cacheDeLayout(be.page, ClientStatsCache.version, largura, altura)
    para cada linha do layout:
      font.drawInBatch(linha, x, y, cor, false, pose, buffers,
                       Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT)
    ícones (item/bloco representativo) se couber
  poseStack.pop
```

Detalhes:
- `LightTexture.FULL_BRIGHT`: a tela "brilha" e fica legível à noite e em cavernas.
- **Cache de layout:** as linhas (`FormattedCharSequence`), posições e paginação só são recalculadas quando `ClientStatsCache.version` ou o tamanho da tela mudam. No frame a frame é só desenhar: zero alocação.
- **Área de render:** sobrescrever `getRenderBoundingBox` para cobrir a tela inteira **(verificar onde fica na NeoForge 21.1: renderer ou extensão da block entity)**. Sem isso, a tela some quando o bloco controlador sai da câmera.
- **Nomes:**
  - Biomas: chave de tradução `biome.<namespace>.<path>` (vanilla e a maioria dos mods já têm).
  - Estruturas não têm tradução vanilla: usar `structure.<namespace>.<path>` do nosso lang; fallback para o ID formatado (`ancient_city` → "Ancient City").
- Texto que não cabe: truncar com `…` usando `font.width` (medido e cacheado no layout, não por frame).

### 4.4 Páginas iniciais

| Página | Conteúdo |
|---|---|
| **Resumo** | Nome da dimensão · Biomas `12 / 31` com barra de progresso · Tipos de estrutura `5 / 14` · Estruturas encontradas `27` · Última descoberta |
| **Biomas** | Grade: descobertos com nome + pioneiro; não descobertos como `???` |
| **Estruturas** | Tipos descobertos, quantidade de cada, pioneiro |
| **Recentes** | Últimas N descobertas: "Fulano descobriu *Deep Dark* · há 2h" |

Mostrar `???` para o que falta descobrir cria curiosidade sem dar spoiler do conteúdo da dimensão.

### Implementação da Fase 4

- **Bloco:** `nexuscore:exploration_panel` ("Painel de Exploração"), sem receita. Fica na aba **NexusCore** do criativo; todo item que o mod registrar entra nela automaticamente.
- **Texturas:** arte própria no jar, geradas por `tools/textures/generate-panel-textures.cjs`. A moldura da frente usa as mesmas cores da tela desenhada (`PanelColors`), então de longe (sem render) o bloco continua parecido.
- **Colocar/configurar:** operador nível 2 **ou jogador no criativo** (`[panel] requireOperator = true`). O criativo foi incluído porque no servidor só um operador coloca alguém no criativo, e assim o singleplayer sem cheats também funciona.
- **Trocar página:** agachar + clicar com a mão vazia (Resumo → Biomas → Estruturas → Recentes). Clique normal fica livre para a interação da Fase 5.
- **Comandos (OP 2):** `/nexus painel pagina <posicao> <summary|biomes|structures|recent>` e `/nexus painel dimensao <posicao> <dimensão>`.
- **Multibloco:** painéis vizinhos no mesmo plano e mesma direção viram uma tela. Recalcula só ao colocar/remover (um tick depois). Tamanho máximo `[panel] maxWidth = 8`, `maxHeight = 5` (config do servidor). Ao juntar/separar telas, a nova herda página/dimensão da maior tela antiga.
- **Render:** moldura com a luz do mundo, tela e texto com brilho próprio; texto maior em telas maiores. Listas longas (biomas, estruturas, recentes) alternam de página a cada 10 s, sincronizado pelo tempo do mundo. Layout refeito só quando os dados, a página, o tamanho, o idioma ou o minuto (para "há 2 min") mudam. Distância de desenho: `[panel] renderDistance = 48` na config do cliente.
- **Área de render:** `getRenderBoundingBox` fica no renderer (`IBlockEntityRendererExtension`) na NeoForge 21.1.

### 4.5 Testes de aceitação da Fase 4

- [ ] Painel 1×1 e multiblock até o máximo renderizam corretamente nas 4 direções
- [ ] Quebrar um bloco do meio reorganiza a tela sem crash
- [ ] Nova descoberta aparece na tela em até ~1 s, sem relogar
- [ ] Texto legível de perto e de ~10 blocos
- [ ] Sem queda de FPS com 10 painéis grandes na tela (comparar com F3 / Spark)
- [ ] Servidor dedicado não carrega nenhuma classe de render

---

## Fase 5: Futuro (leaderboard, moeda, interação)

Tudo aqui se pluga nos eventos da Fase 3, sem mexer no tracker.

### 5.1 Interação com a tela

- Ao clicar no painel, `useWithoutItem` recebe o `BlockHitResult`. A posição exata do clique convertida para coordenadas 2D da tela diz qual "botão" foi clicado (próxima página, aba, rolagem).
- Isso roda no servidor, que valida distância (`player.canInteractWithBlock`), permissão e existência do bloco, e só então muda `page`/`scroll` na block entity.
- Nenhum payload customizado precisa ser aberto para o cliente, então não há superfície de ataque nova.

### 5.2 Leaderboard

- Dados já existem por UUID (3.2).
- Página `LEADERBOARD` com ranking: biomas pessoais, estruturas pessoais, títulos de pioneiro.
- Nomes de jogadores offline via `server.getProfileCache()` (cacheado).

### 5.3 Sistema de moeda (ideia)

| Ação | Recompensa sugerida |
|---|---|
| Pioneiro de bioma | alta |
| Pioneiro de tipo de estrutura | alta |
| Primeira vez pessoal em bioma/estrutura | baixa |
| Nova instância de estrutura | muito baixa |

Anti-exploit:
- Recompensa só por **primeira** descoberta (pessoal ou do servidor). Não há como farmar repetindo.
- Criativo/espectador não conta; OP pode ser excluído por config.
- Log de toda recompensa (arquivo ou console) para auditoria.

Formato da moeda (decidir depois): **item físico** (pode ser trocado e perdido, é imersivo) ou **saldo virtual** no `SavedData` (seguro, exige comandos/loja). Também dá para integrar com um mod de economia existente se o SMP já usar um.

---

## Ideias de estatísticas para o SMP

Pensadas para um servidor de **pioneiros explorando uma dimensão nova**. São só sugestões.

**Exploração**
- Chunks únicos visitados (global e por jogador) → "área mapeada"
- Ponto mais distante do spawn já alcançado → "a fronteira"
- Maior profundidade e maior altitude alcançadas
- Tempo total passado na dimensão
- Dias desde a abertura da dimensão

**Descobertas (codex)**
- Bestiário: mobs novos encontrados/abatidos pela primeira vez
- Itens e blocos novos obtidos pela primeira vez (quem achou primeiro)
- Baús de estrutura saqueados pela primeira vez

**Perigo**
- Mortes na dimensão e a causa mais comum → "o que mais mata os pioneiros"
- Mob mais letal

**Comunidade**
- Mural de pioneiros: quem descobriu o quê primeiro
- Feed de descobertas recentes
- Metas coletivas: "descobrir 20 biomas desbloqueia X" (evento, loja, área nova)
- Expedições: grupo que descobriu algo junto (jogadores próximos no momento da descoberta)

**Economia/leaderboard**
- Ranking semanal de exploração (zera toda semana, dá chance a quem entrou depois)
- Títulos: "Primeiro a pisar em...", "Cartógrafo" (mais chunks), "Arqueólogo" (mais estruturas)

---

## Padrões de código

- Java 21: `record`, `switch` com pattern matching, `var` só quando o tipo é óbvio.
- Uma responsabilidade por classe; módulos não se importam entre si, exceto via eventos ou API explícita (`display` lê `stats`, nunca o contrário).
- Nada estático mutável fora de caches bem definidos (e esses são limpos no desligamento do servidor / desconexão).
- Código de cliente só em pacotes `client` ou em `NexusCoreClient`.
- Todo texto visível ao jogador via `lang` (`pt_br.json` + `en_us.json`).
- Log: `INFO` para status de módulos, `WARN` para compat degradada, `DEBUG` para rastreamento. Nunca logar por tick.
- Build limpo sem warnings de compilação; `runClient` e `runServer` testados antes de cada release.
- Versionamento: `MAJOR.MINOR.PATCH`; mudança no formato do `SavedData` ou no protocolo de rede incrementa a versão correspondente (`DATA_VERSION`, `event.registrar("N")`).

---

## Decisões em aberto

| # | Decisão | Sugestão |
|---|---|---|
| 8 | Moeda: item físico ou saldo virtual | decidir na Fase 5 |

**Já decidido:**
- (4) Criativo e espectador não contam (configurável); operador no sobrevivência conta.
- (5) Painéis: operador ou jogador no criativo coloca/configura (configurável).
- (6) Painel sem receita; vem da aba NexusCore do criativo.
- (7) O que falta descobrir aparece como `???`.
- (9) Descobertas anteriores à instalação não contam (não há como saber).
- Rastreamento limitado a um quadrado a partir de `0, 0` (raio padrão 5000, configurável; 0 = sem limite).
- (1) Pacote Java: `com.nexussmp.nexuscore`.
- (2) Dimensão rastreada por padrão: **só `minecraft:overworld`** (lista editável na config do servidor).
- (3) Nexus Resources é um **resource pack normal, fora do jar**, ativo por padrão no modpack. Sem `AddPackFindersEvent`.
- (10) Ícones do WaterFrames: dessaturação em tempo real (opção A).
- Texturas: arte própria do mod fica no jar (`assets/nexuscore`); overrides de outros mods ficam no Nexus Resources.
- Entrega: Fases 1, 2 e 2B primeiro, teste em jogo, depois Fases 3 e 4. Fase 5 é só ideia.
- Sodium 0.8.x (0.8.13) + Iris 1.8.14-beta.1, atualizando para as estáveis antes do SMP abrir.
- Vermelho continua só para erro no WaterFrames; o resto fica cinza/neutro.
- Visual "inventário" é só para o WaterFrames. A tela do Sodium fica com o visual do XSO.
- O aviso de "sem vídeo" é o ícone de status na área da URL.
- Licença do NexusCore: **LGPL-3.0-or-later** (herda do XSO).
- WaterMedia fica na linha **2.1.x** (2.1.37 + YouTube plugin 2.1.2) enquanto o WaterFrames for 2.1.x; a 3.x crasha com ele.

---

## Tabela de assets sobrescritos (Nexus Resources)

| Asset | Mod | Versão conferida | Motivo |
|---|---|---|---|
| `assets/waterframes/textures/block/frame.png` | WaterFrames | 2.1.23 | textura antiga de madeira do frame, no lugar da azul |
| `assets/waterframes/textures/block/displays.png` | WaterFrames | 2.1.23 | partícula/quebra no mesmo estilo (compartilhada por todos os displays) |

Não são overrides (ficam no jar do NexusCore, arte própria):

| Asset | Uso |
|---|---|
| `assets/nexuscore/textures/gui/sprites/waterframes/{panel,inset,inset_highlighted,field_error}` | nine-slice da interface do WaterFrames (Fase 2B) |
| `assets/nexuscore/textures/gui/sprites/waterframes/{checkbox,checkbox_highlighted,checkmark}` | checkbox 7×7 do CreativeCore |
| `assets/nexuscore/textures/gui/sprites/sodium/kofi-icon.png` | botão de doação herdado do XSO |
| `assets/nexuscore/textures/block/exploration_panel_{front,side}.png` | Painel de Exploração (Fase 4) |

`waterframes:textures/screen_atlas.png` **não** é sobrescrito: vira cinza em tempo de execução (opção A). Se um dia for desenhado (opção B), entra na primeira tabela.
