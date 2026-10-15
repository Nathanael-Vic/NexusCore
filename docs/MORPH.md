# Anel da Metamorfose

Um anel que copia a forma de uma criatura — ou de outro jogador — e transforma quem o veste.

- **Na mão**, bater numa criatura guarda a forma dela no anel. O golpe não acontece: nem dano, nem
  empurrão. O anel carregado brilha como item encantado, e a dica diz qual forma ele guarda.
- **No dedo** (a vaga de anel do Curios), quem veste vira aquela forma, e todo mundo em volta vê.
  Para vestir, basta o **clique direito** com o anel na mão, como uma armadura: ele vai para a
  primeira vaga de anel livre, e com todas ocupadas troca pelo que estava na primeira.
  Tirar o anel desfaz.
- **Sem receita.** Quem entrega é um operador: `/give <jogador> nexuscore:morph_ring`.

---

## O que muda em quem se transforma

| | Bicho | Outro jogador |
|---|---|---|
| Aparência | o modelo, a textura e as animações do próprio mod do bicho | a skin da pessoa copiada |
| Nome em cima da cabeça | **nenhum** — disfarce completo | o **nome da pessoa copiada** |
| Hitbox e altura dos olhos | as do bicho (filhote sai pequeno, slime sai do tamanho que era) | as de jogador |
| Sombra no chão | a do bicho | a de jogador |
| Sons | os do bicho: dano, morte, passo e o som de fundo dele | os de jogador |
| Partículas | as do bicho (fumaça do blaze, portal do enderman…) | — |
| Habilidades | voar, nadar rápido, respirar na água, não queimar — conforme o bicho | — |
| Braço em primeira pessoa | some (o item na mão continua) | continua |
| Itens na mão e armadura | não aparecem | aparecem, como em qualquer jogador |

O que vai junto na cópia é **tudo o que faz o bicho ter a cara que tem**: filhote, cor da ovelha,
variante do sapo e do gato, tipo e profissão do aldeão, o que o esqueleto segura, o tamanho do
slime — e o que cada mod de bicho guardar. O que fica de fora é o que não é aparência: onde ele
está, quanta vida tem, de quem ele é, o que carrega e o que lembra
([`MorphCapture`](../src/main/java/com/nexussmp/nexuscore/morph/MorphCapture.java)). A cópia tem teto
de 16 KB; um bicho com NBT gigante perde primeiro o que segura e veste.

**Espaço.** A transformação só acontece se a criatura couber onde a pessoa está. Vestir um enderman
dentro de uma casa poria os olhos dentro do teto, e o jogo sufoca quem tem a cabeça num bloco. Sem
espaço, aparece um aviso e a transformação espera: acontece assim que houver lugar.

---

## Habilidades

Só o básico do corpo novo — ataque e poder especial ficam de fora (o blaze não atira, o creeper
não explode).

| Habilidade | Para quem | Como |
|---|---|---|
| **Voar** | bicho que voa | o voo do criativo, com dois pulos |
| **Nadar rápido** | bicho da água | o dobro da velocidade de nado |
| **Respirar na água** | bicho que respira nela (peixe, lula, afogado, esqueleto…) | o ar não acaba debaixo d'água |
| **Não queimar** | bicho que não pega fogo (blaze, ghast, cubo de magma, strider…) | fogo e lava não machucam |

**Quem voa e quem nada** não vem escrito em lugar nenhum; o jeito de saber é olhar como o bicho se
move. Bicho que voa anda com o "GPS" de voo do jogo; bicho da água, com o de água ou o anfíbio. É o
que os bichos de mod também usam, então a regra vale para eles sem lista nenhuma. Alguns do jogo
voam do jeito deles (morcego, blaze, vex) e entram pelo nome. O golfinho nada rápido mas respira ar,
como no jogo.

O voo e o nado são atributos do NeoForge, tirados sem mexer em quem está no criativo de verdade.
Quem perde o voo no meio do ar (tirou o anel voando) desce.

---

## Como funciona

**Vestir.** O servidor confere o anel de cada jogador a cada meio segundo, em vez de ouvir o Curios
trocar de item. Pega todos os caminhos de uma vez: vestir, tirar, morrer com o anel, entrar no
servidor já com ele, e a configuração mudar depois.

**A criatura solta.** Cada pessoa transformada em bicho tem uma cópia da criatura que nunca entra no
mundo, dos dois lados ([`MorphStands`](../src/main/java/com/nexussmp/nexuscore/morph/MorphStands.java)).
É a ela que se pergunta tudo o que depende do bicho — o tamanho, os sons, as habilidades — e, no
cliente, é ela que é desenhada.

**O tamanho** precisa bater exatamente dos dois lados, senão o servidor corrige o movimento a cada
passo. Servidor e cliente fazem a mesma conta pelo mesmo caminho: criam a criatura com o NBT do anel
e perguntam o tamanho a ela.

**O desenho.** Quando o jogo vai desenhar um jogador transformado, o desenho dele é cancelado e o da
criatura entra no lugar, com o desenhista da própria criatura. Antes de cada quadro, ela recebe da
pessoa tudo o que as animações leem: posição, rotação do corpo e da cabeça, o balanço das pernas, a
idade em ticks (que move asas, caudas e as animações do GeckoLib), se está no chão, correndo,
agachada, levando dano ou batendo.

**A criatura vive.** A cada tick, no cliente, a criatura roda o tick dela. É daí que saem as
partículas do bicho e as animações que andam com o tempo e não com o passo (a galinha batendo asa,
o sapo, o camelo, os bichos de mod que animam no tick). Viver inclui empurrar quem está encostado —
e quem está encostado é a própria pessoa —, então o empurrão é desligado só nas criaturas do anel.
Pessoa invisível não solta partícula: seria um rastro dizendo onde ela está.

**Os sons.** Dano e morte só trocam a *resposta* à pergunta "que som você faz?" — o jogo continua
tocando do jeito dele. O passo é tocado pelo bicho, no servidor, para todo mundo em volta
(inclusive a pessoa), com o volume e o ritmo dele. O som de fundo usa o mesmo sorteio que o bicho
faz a cada tick, com o intervalo dele. Tudo sai com o volume e o tom de voz do bicho: o ghast se
ouve de longe, o filhote tem a voz mais fina. A troca mora nos mixins de
[`mixin/morph`](../src/main/java/com/nexussmp/nexuscore/mixin/morph).

**O que quebrar fica de fora.** Bicho de mod é desenhado e movido por código do mod, que nunca
esperou ser usado assim. Se um tipo não puder ser criado no cliente, ou o desenho dele quebrar, esse
tipo é posto de lado até sair do servidor: a pessoa aparece como jogador. Se só o tick quebrar, o
bicho continua aparecendo, sem as partículas. Nos dois casos o log diz qual foi, uma vez
(`[NexusCore] morph: ...`). Um bicho teimoso nunca derruba o jogo de ninguém.

---

## Configuração

`config/nexuscore-server.toml`, seção `[morph]`:

| Opção | Padrão | O que faz |
|---|---|---|
| `blockedEntities` | `["minecraft:ender_dragon", "minecraft:wither"]` | o que o anel não copia; vale também para quem já tinha a forma guardada |
| `allowPlayers` | `true` | se o anel copia outros jogadores |
| `flight` | `true` | quem vira bicho que voa pode voar |
| `swim` | `true` | quem vira bicho da água nada rápido e respira debaixo d'água |
| `fireImmunity` | `true` | quem vira bicho que não queima não se machuca com fogo nem lava |

O módulo liga e desliga em `[modules.morph]`. Desligado, o anel vira um item qualquer.

---

## Testes

**Base** (testado e funcionando):

- [x] copiar sem machucar, vestir, virar o bicho para todo mundo ver, hitbox e altura dos olhos
- [x] andar, olhar em volta, bater e levar dano com a animação do bicho
- [x] copiar outro jogador, com a skin e o nome dele

**Sons, partículas, habilidades e sombra:**

- [ ] vaca: muge sozinha de vez em quando; ao levar dano e morrer faz som de vaca; passo de vaca
- [ ] outra pessoa ouve os mesmos sons, vindo de onde a vaca está
- [ ] blaze: solta fumaça e não queima em fogo nem lava; enderman: partículas de portal
- [ ] galinha: bate as asas ao cair
- [ ] morcego, abelha, papagaio, ghast: dois pulos e voa; tirar o anel no ar faz descer
- [ ] peixe e lula: nadam rápido e não se afogam; golfinho nada rápido mas precisa subir para respirar
- [ ] ghast tem sombra grande no chão; filhote tem sombra pequena
- [ ] um bicho de cada mod do modpack, olhando se aparece, se anima e se solta as partículas dele
- [ ] ficar invisível (poção) transformado: nada aparece, nem partícula
