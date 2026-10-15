# Papagaio de Ombro

Um papagaio que um operador põe no ombro de alguém. Ele fica ali para sempre e, de vez em quando,
repete com voz de papagaio o que **outras pessoas** falaram perto dele no chat de voz.

- **Só operador** (nível 2) usa o comando.
- O papagaio **nunca sai do ombro**: não toma dano, não se afoga, não pega fogo, não some com a
  distância, não aceita coleira, biscoito, semente nem etiqueta de nome.
- Passar o mouse nele mostra **o nome dele no Jade** (sem barra de vida e sem "NexusCore").
- O Simple Voice Chat é **opcional**: sem ele o papagaio fica no ombro e pia, só não repete ninguém.

---

## Comandos

| Comando | O que faz |
|---|---|
| `/nexus papagaio colocar <jogadores> <cor> <nome>` | Põe (ou troca) o papagaio. O nome é o resto da linha, com espaços, até 32 caracteres. |
| `/nexus papagaio tirar <jogadores>` | Tira o papagaio. Funciona com gente offline. A memória dele vai junto. |
| `/nexus papagaio esquecer <jogadores>` | O papagaio fica, mas esquece tudo o que tinha ouvido. |
| `/nexus papagaio lista` | Quem tem papagaio, com nome e cor. |

Cores: `vermelho`, `azul`, `verde`, `ciano`, `cinza` — as cinco do jogo (o autocompletar mostra).

Exemplo: `/nexus papagaio colocar Fulano verde Seu Lourival`

---

## Os efeitos

| Quando | O que se vê |
|---|---|
| Chega pelo comando | Desce do alto girando numa espiral de penas da cor dele, cresce passando do tamanho e assenta com um pulinho elástico. Pio + sininho. |
| Volta sozinho (entrou no servidor, renasceu, trocou de mundo) | Só um "pop" curto, sem festa. |
| Sai (comando `tirar`, ou o dono morreu) | Estufa, encolhe girando cada vez mais rápido e some num estalo de penas. |
| Troca (comando `colocar` em quem já tinha) | O velho encolhe enquanto o novo chega. |
| Está repetindo alguém | Estufa e balança no ritmo da fala. Às vezes solta um pio no fim. |

Morreu, ele sai voando; renasceu, ele volta. Quem carrega não vê o próprio papagaio em primeira
pessoa (ele estaria grudado na câmera), não consegue mirar nele sem querer e não vê a plaquinha
dele no Jade — a mira passa direto. Só quem está de fora vê o nome.

**A cabeça é dele.** O corpo vira com o do dono, mas a cabeça olha para onde ele quer, como qualquer
mob: quase sempre para quem estiver perto (até 8 blocos), às vezes para um ponto qualquer em volta,
às vezes só para a frente. Quando está repetindo alguém, olha para quem está ouvindo. Para o dono
ele nunca olha.

**Onde ele fica.** No ombro esquerdo, em pé ou agachado. Quem não tem ombro no momento — virou bicho
no anel da metamorfose, está nadando, planando com elytra ou deitado — leva o papagaio nas costas.

**Numa briga ele não atrapalha.** Golpe no papagaio vai para o dono, e flecha atravessa ele.

---

## A voz

### O que ele ouve

- A voz de **outra pessoa** falando a até **12 blocos** do papagaio. Nunca a do dono.
- **Cochicho não entra**, nem conversa de **grupo** do chat de voz — é o que as pessoas em volta
  também não ouvem.
- Ele **não decora tudo**: cada frase ouvida tem 45% de chance de ficar na memória.
- Frases de meio a três segundos. Fala comprida vira pedaços, e ele pega um deles — o "...que
  isso?!" é mais engraçado que a frase inteira.
- Lembra até **12 frases**, por até **3 horas**, mesmo depois de quem falou ir embora. Entrou uma
  nova, sai a mais velha.

### Quando ele fala

Uma vez por segundo cada papagaio rola um dado:

| Situação | Chance por segundo | Média |
|---|---|---|
| Alguém a até 24 blocos | 1 em 150 | uma fala a cada ~2,5 min, puxando para o que acabou de ouvir |
| Só o dono | 1 em 360 | uma fala a cada ~6 min, de qualquer coisa que ainda lembre |

Além disso, quando uma frase nova acaba de ficar na memória, em 30% das vezes ele a **repete na
hora**, um a quatro segundos depois — o eco de papagaio.

**Entre duas falas ele descansa pelo menos 45 segundos**, aconteça o que acontecer. É o que garante
que nunca vira barulho constante.

### Como soa

A voz da pessoa **mais fina, quase na mesma velocidade** — fina como papagaio, sem virar disco
acelerado. Cada papagaio tem o seu tom (entre 1,22× e 1,38×, tirado do id do dono), com uma variação
pequena a cada fala, e fala só 6% mais rápido que a pessoa.

Por dentro ([`ParrotVoice`](../src/main/java/com/nexussmp/nexuscore/parrot/voice/ParrotVoice.java)):
a voz é tocada mais depressa (sobe o tom e deixa a "boca" menor, o jeito de papagaio) e depois
esticada de volta ao tempo original sem mexer no tom, remontada em pedaços de 40 ms que se encaixam
onde a onda mais combina (WSOLA) — é isso que evita o som metálico. Leva uns 5 ms por frase, numa
thread própria: o tick do servidor não espera. A voz sai do
papagaio e anda junto com o dono, com alcance de 24 blocos.

Ela entra numa categoria própria do chat de voz, **"Papagaios"**: cada pessoa pode baixar ou mutar
nas configurações do Simple Voice Chat, sem mexer no resto.

### Privacidade

Nada vai para o disco: é tudo memória do servidor. Reiniciou, ele esquece. O arquivo do mundo só
guarda **quem tem papagaio, a cor e o nome**. `/nexus papagaio esquecer` limpa a memória na hora.

---

## Como funciona por dentro

| Arquivo | Papel |
|---|---|
| [`ShoulderParrot`](../src/main/java/com/nexussmp/nexuscore/parrot/ShoulderParrot.java) | O papagaio. É **passageiro** do dono, que é o único jeito de o jogo levar uma entidade grudada em outra sem atraso. Nunca é salvo. |
| [`ParrotKeeper`](../src/main/java/com/nexussmp/nexuscore/parrot/ParrotKeeper.java) | A cada 5 ticks: quem tem papagaio no arquivo, está online, vivo e fora do espectador, tem um no ombro; quem não, não. Morte, relog, portal e restart caem todos nessa regra. |
| [`ParrotOwners`](../src/main/java/com/nexussmp/nexuscore/parrot/ParrotOwners.java) | O que vai para o arquivo do mundo (`nexuscore_parrots`). |
| [`EntityShoulderParrotMixin`](../src/main/java/com/nexussmp/nexuscore/mixin/parrot/EntityShoulderParrotMixin.java) | Para o jogo, quem leva passageiro é montaria — e montaria perde o nome em cima da cabeça e não é empurrada. Um passageiro que é só papagaio de ombro não conta. |
| [`ParrotMemory`](../src/main/java/com/nexussmp/nexuscore/parrot/voice/ParrotMemory.java) | O que um papagaio ouviu e guardou. Pacotes Opus crus, como nas backrooms (12 frases < 150 KB). |
| [`ParrotBrain`](../src/main/java/com/nexussmp/nexuscore/parrot/voice/ParrotBrain.java) | Quem ouve quem (calculado na thread do jogo e publicado pronto para a thread do chat de voz), o que fica na memória e quando falar. Todos os números desta página estão no topo dele. |
| [`ParrotVoicechatPlugin`](../src/main/java/com/nexussmp/nexuscore/parrot/voice/ParrotVoicechatPlugin.java) | O ouvido e o bico. Plugin **separado** do das backrooms: os dois escutam o mesmo microfone, mas mexer num nunca muda o outro. |
| [`ShoulderParrotRenderer`](../src/main/java/com/nexussmp/nexuscore/parrot/client/ShoulderParrotRenderer.java) | As animações de chegada, saída e fala. |

---

## Jade: o nome de quem está transformado

Junto com o papagaio veio uma correção no Jade para o anel da metamorfose: antes, quem virava
morcego mostrava o **próprio nick** ao passar o mouse, e o disfarce caía na hora. Agora a plaquinha
mostra o nome da forma — "Morcego", ou o nome do jogador copiado
([`NexusJadePlugin`](../src/main/java/com/nexussmp/nexuscore/compat/jade/NexusJadePlugin.java)).

---

## Teste em jogo (pendente)

- [ ] `colocar`: a chegada com festa aparece para quem carrega (terceira pessoa) e para os outros.
- [ ] O papagaio acompanha andando, correndo, agachando, pulando, nadando, de elytra, a cavalo e de barco, sem atraso.
- [ ] O nome em cima da cabeça do dono continua aparecendo.
- [ ] Jade no papagaio mostra só o nome; Jade em alguém transformado mostra a forma.
- [ ] Morrer → ele sai; renascer → ele volta. Relog, portal do Nether e restart → ele volta.
- [ ] Com outra pessoa falando perto, ele repete às vezes; a categoria "Papagaios" aparece no chat de voz.
- [ ] Bater no papagaio acerta o dono; flecha atravessa.
