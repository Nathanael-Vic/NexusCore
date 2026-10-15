# Vampiro

O **Vampirism** entra no servidor inteiro, mas cercado. A ideia é uma só, e vale dizer em voz alta:

> **o vampirismo aqui é raro e concedido.**

Um punhado de pessoas — os **lordes** — vive o mod inteiro: sobe de nível, gasta ponto de
habilidade, aprende poder. Quem for mordido por um lorde pega a maldição, e **só** a maldição: o sol
queima, o sangue falta, e não há nível nem poder no fim disso. Caçador não existe, e o mundo não
cria vampiro nenhum por conta própria.

O mod não foi reescrito nem copiado para dentro do NexusCore — nada disso seria necessário. Tudo
aqui passa pelos eventos que a própria API do Vampirism publica, e por isso continua funcionando
quando ele for atualizado.

---

## Os dois papéis

| | **Lorde** | **Vampiro comum** |
|---|---|---|
| Como se chega | `/nexus vampiro lorde adicionar` | mordida de um lorde |
| Sobe de nível | sim, até o teto do mod | **não**, trava no 1 |
| Ponto de habilidade | sim | nenhum (ponto vem de subir de nível) |
| Poderes (morcego, visão, fúria…) | sim | **nenhum**, venha o comando de onde vier |
| Botão dos dentinhos no inventário | sim | **não existe** |
| Árvore de habilidades / roda de poderes | sim | as telas nem abrem |
| Sol | queima | queima |
| Sangue | precisa | precisa |

**Lorde é permissão, não condição.** Nomear alguém lorde não o torna vampiro, e tirar o posto não
cura ninguém — só devolve a pessoa ao nível 1 e apaga o que ela tinha aprendido.

---

## Comandos

```
/nexus vampiro lorde adicionar <jogadores>   quem pode progredir
/nexus vampiro lorde remover <jogadores>     tira o posto, o nível e as habilidades
/nexus vampiro lorde lista
/nexus vampiro tornar <jogadores>            vira vampiro (nível 1)
/nexus vampiro curar <jogadores>             volta a ser humano
/nexus vampiro info [jogador]                condição, nível e posto
```

Tudo pede operador (nível 2), menos `info` sobre você mesmo.

**`tornar` existe por necessidade.** Com as regras deste servidor não sobrou nenhuma outra porta de
entrada: não nasce vampiro no mundo e mordida de bicho não transforma. A primeira pessoa a virar
vampira é sempre por comando; daí em diante, a mordida de um lorde faz o resto.

A lista de lordes fica no arquivo do mundo (`nexuscore_vampire_lords`), ao lado da lista de
locutores da rádio. Lorde continua lorde depois do restart.

**A coroação.** Virar lorde tem cerimônia (uns 2 segundos, tudo do jogo,
[`VampireFx`](../src/main/java/com/nexussmp/nexuscore/vampire/VampireFx.java)): a tela de quem está
sendo coroado escurece e um coração começa a bater, cada vez mais rápido; uma espiral de sangue sobe
dos pés até acima da cabeça, com névoa no chão. O coração para — sino grave da meia-noite, estouro de
sangue e tinta, e uma revoada de oito morcegos sai da pessoa e some em fumaça logo depois. Na tela
dela, **"Lorde Vampiro"** com *"A noite agora te obedece."* embaixo. Quem está em volta vê e ouve
tudo; a escuridão e o título são só de quem foi coroado.

**Perder o posto** é curto: o sangue escorre, vira fumaça, uma alma escapa.

---

## Como se volta a ser humano

**Por comando, e só.** Não existe caminho no mundo — nem receita, nem bloco, nem item.

```
/nexus vampiro curar <jogadores>
```

### O que foi fechado

O Vampirism trazia uma cura embutida que ninguém pediu: a *Injection Chair* com uma *Garlic
Injection* abria a tela "Revert back to normal human". E era barata — a cadeira é 7 ferro, 1 lã e
uma garrafa; a seringa é vidro; o resto é alho. Qualquer vampiro comum se curava sozinho numa tarde,
e a maldição virava um incômodo temporário em vez de uma condição.

A mesma cadeira era também uma **entrada**: com uma *Sanguinare Injection* (seringa + 8 presas) ela
transformava alguém sem mordida nenhuma — contra a regra de que só um lorde transforma.

Porta de mão dupla, os dois sentidos errados. Então a seringa **deixou de funcionar na cadeira**, e
a tela de reverter não abre nem por outro caminho. O corte é no gesto, e não na tela que viria
depois, por um motivo prático: a cadeira consome a seringa antes de abrir qualquer coisa — recusar
só lá na frente devolveria a pessoa igual e com um item a menos.

Quem tentar lê, na barra de ação: *"Nem a maldição nem a cura vêm de uma agulha aqui."*

> **O que sobrou de fora:** quem foi mordido e ainda está com o efeito *Sanguinare* não é vampiro
> ainda — está virando. Cortar essa fase é outro assunto (leite, treinador de caçador de vila
> antiga), e não mexi nisso porque ali ninguém está *desvirando*: está deixando de virar.

### A missão, que é como isso ganha história

O comando existe para o operador decidir; a missão é o que faz a decisão valer alguma coisa.

```
/nexus quests dar Fulano Cura do vampirismo | Traga 10 lingotes de ouro ao altar | Volta a ser humano
/nexus quests pendentes
/nexus quests concluir <id>
/nexus vampiro curar Fulano
```

A missão cai direto no livro de quests da pessoa, sem autor visível, e o botão do livro ganha um
selo de cera até ela abrir. Os detalhes estão em [`QUESTS.md`](QUESTS.md#missões-do-servidor).

---

## O que o NexusCore faz sozinho

Cinco regras, todas em [`vampire/VampireRules.java`](../src/main/java/com/nexussmp/nexuscore/vampire/VampireRules.java):

1. **Caçador não existe.** Entrar na facção é negado na origem (`CanJoinFaction`) e subir nela
   também (`FactionLevelChangePre`). Não é desencorajado — é recusado, venha o pedido do treinador,
   do altar ou de outro mod. As armas de caçador continuam existindo como itens; o que não existe é
   a progressão.
2. **Só lorde progride.** Qualquer subida acima do nível 1 de quem não é lorde é cancelada, com uma
   frase explicando — no máximo uma por minuto, porque o jogo tenta subir de nível a cada gole de
   sangue depois que a barra enche.
3. **E nem por outro caminho.** Se alguma habilidade sobrar de antes, usar também não dá:
   `ActionActivatedEvent` é cancelado para quem não é lorde. O menu de progressão, se alguém chegar
   nele por atalho, é fechado **pelo servidor** — recusar só no cliente deixaria a pessoa com um baú
   aberto que ela não vê.
4. **Nada nasce sozinho.** Vampiro, caçador, fantasma e o resto dos mobs do Vampirism somem do
   nascimento natural, da geração de chunk, dos geradores, das estruturas e do totem de vila. O que
   um operador puser com ovo ou comando continua funcionando, e a criatura convertida pela mordida
   de um lorde também — essa é a graça. O minion de um lorde também fica: é dele.
5. **A agulha não decide nada.** Seringa não faz efeito na cadeira de injeção, nos dois sentidos, e
   a tela de reverter não abre para ninguém — nem para lorde. Entrar no vampirismo é mordida de
   lorde; sair é comando. As duas coisas são decididas por gente.

E três opções do próprio Vampirism são forçadas na memória quando o mundo abre
([`VampirismSettings`](../src/main/java/com/nexussmp/nexuscore/vampire/VampirismSettings.java)):

| Opção | Valor | Por quê |
|---|---|---|
| `sundamageMinLevel` | **1** | o padrão é 4; como o vampiro comum nunca passa do 1, ele seria imune ao sol para sempre — e aí "vampiro capado" vira "humano com barra de sangue" |
| `disableMobBiteInfection` | **true** | mordida de bicho não transforma |
| `disableFangInfection` | **true** | dente no inventário não transforma |

Isso é mexido **na memória, não no arquivo**: quem abrir o `vampirism-balance.toml` vai continuar
vendo o valor que escreveu lá. É de propósito — não é papel deste mod reescrever a configuração de
outro. O que fica registrado é uma linha no log dizendo o que mudou.

---

## O mundo sem o jogo de mob

Aqui o vampirismo é dos jogadores — lorde e vampiro comum — e só. Nada do que o Vampirism põe no
mundo para ser um jogo de mobs existe: nem floresta dos vampiros, nem acampamento, posto ou tenda
de caçador, nem cabana, altar, cripta ou masmorra de vampiro, nem totem e treinador de caçador nas
vilas, nem a igreja que ele põe no lugar do templo. Nada disso precisa de ajuste à mão.

**Geração nova** — chunk que ainda não existia já nasce sem:

| O quê | Como |
|---|---|
| Floresta dos vampiros, acampamento de caçador, templo trocado, totem e treinador (sem Lithostitched) | opções do `vampirism-common.toml` forçadas na memória no setup do jogo, antes de o Vampirism ler ([`VampirismSettings.applyWorldgen`](../src/main/java/com/nexussmp/nexuscore/vampire/VampirismSettings.java)): `addVampireForestToOverworld = false`, `enableHunterTentGeneration = false`, `villageReplaceTemples = false`, `totemWeight = 0`, `villageHunterTrainerWeight = 0` |
| Totem e treinador nas vilas (com Lithostitched, que é o caso do modpack) | os modificadores do Lithostitched que o Vampirism usa são trocados por `lithostitched:no_op` em `data/vampirism/lithostitched/` deste jar. Com o Lithostitched instalado, o Vampirism **ignora** `totemWeight` — por isso o totem continuava aparecendo mesmo com a config em 0 |
| Cabana, altar, cripta, "mother", postos de caçador e masmorra | as tags de bioma de cada uma (`vampirism:has_structure/...`) viram vazias: a estrutura não acha bioma onde nascer |
| Vampiro nascendo à noite pelo mundo | os modificadores de bioma de spawn do Vampirism viram `neoforge:none` |

Os arquivos de `data/vampirism/` só vencem os do Vampirism porque o NexusCore carrega **depois**
dele (`ordering = "AFTER"` no `neoforge.mods.toml`).

**O que já existia** — [`VampireWorld`](../src/main/java/com/nexussmp/nexuscore/vampire/VampireWorld.java):

- **Totem de vila não existe**, de nenhum tipo. O que veio com a vila ("Fragile Village Totem Top")
  e o que alguém fabricou ("Village Totem Top") saem sozinhos, com a base, quando o chunk carrega.
  Colocar um novo é recusado, e as receitas do topo e da base foram tiradas
  (`data/vampirism/recipe/general/`). É o totem que faz a vila ser "dos vampiros" ou "dos
  caçadores" e que põe mob de facção nela.
- **Mob do Vampirism salvo no mundo** (o caçador do acampamento, o baron, o treinador da vila, o
  fantasma) some quando o chunk dele carrega. Fica o que é de jogador: o minion de lorde e a
  criatura convertida por mordida. O que um operador colocar por comando ou ovo **daqui em diante**
  ganha uma marca (`nexuscore_vampirism_allowed`) e continua existindo depois de reiniciar; o que
  foi colocado antes desta versão não tem a marca e sai.
- Estrutura já gerada (tenda, cabana, cripta) e floresta já gerada continuam de pé como construção
  e terreno, sem ninguém dentro. Apagar pedaço de mundo explorado não é papel do mod.

Um valor que **precisa continuar como está**, em `config/vampirism-server.toml`:

```toml
[server]
    # É por aqui que a mordida de um lorde transforma alguém. Desligar isso fecha a única
    # porta de entrada que sobrou.
    playersCanTurnPlayers = true
```

---

## Onde o botão foi parar

O Vampirism põe o botão dos dentinhos a 125 pixels da borda do inventário. O livro de quests daqui
fica a 126 — os dois no mesmo ponto, um por baixo do outro.

O botão dele sai da tela sempre, e para o lorde entra um nosso no lugar: a mesma arte (as imagens do
próprio Vampirism, para não virar um botão estranho no meio dos outros), o mesmo clique, e a **148**
— o passo seguinte da fileira:

```
   [ receitas ]   [ quests ]   [ habilidades ]
       +104         +126           +148
```

Achar o botão dele é feito **pela arte que ele desenha**, não pela posição: posição o jogador muda
na configuração, arte não. Isso custa um campo lido por reflexão no *Minecraft* (`ImageButton.sprites`,
não em nada do Vampirism), resolvido uma vez. Se um dia esse campo sumir, o botão volta a ser o
dele, no lugar de antes — chato, e só.

As duas telas que a tecla de atalho abre (a árvore de habilidades e a roda de poderes) são recusadas
para quem não é lorde. Tirar o botão e deixar a tecla aberta seria esconder a porta sem trancá-la.

---

## Os botões da tela do vampiro

A tela que aquele botão abre tem mais três (quatro para o lorde): habilidades, aparência e as duas
de configurar ações. Todos usam uma moldura antiga do próprio mod — um quadrado preto chapado, que
clareia inteiro para branco ao passar o mouse. Ao lado do livro de receitas, que é o botão claro de
quina arredondada que o jogo usa hoje, eles destoam.

Cada um sai da tela e entra um nosso no lugar, com a moldura do livro de quests — a mesma dos
vizinhos. O clique continua sendo do Vampirism, chamado no botão original guardado fora da tela; a
dica que aparece ao passar o mouse é copiada do botão dele, para não se perder no caminho. Quem
decide se o botão aparece continua sendo o mod: o de tarefas de lacaio só existe para o lorde, o de
aparência só para vampiro.

**Os ícones.** Os arquivos do Vampirism não servem: não são ícones, são **botões inteiros**, 100%
opacos, com a moldura antiga pintada dentro da imagem. Recortar o miolo foi tentado e descartado — a
engrenagem e a gengiva das presas são cinza-claro, desenhadas para fundo escuro, e somem sobre a
moldura clara.

| Botão | Ícone |
|---|---|
| Habilidades | a presa, como item — o mesmo do botão do inventário, que leva ao mesmo lugar |
| Aparência | `vampire/appearance` — cabeça de vampiro redesenhada |
| Configurar ações / tarefas | `vampire/actions` — engrenagem redesenhada |

O estado de mouse em cima fica por conta da moldura, que azula — como no livro de receitas.


---

## O contorno do AppleSkin nas gotas

Quando alguém vira vampiro, o Vampirism esconde a barra de fome e desenha as gotas de sangue no
mesmo lugar. O AppleSkin não sabe disso: continuava desenhando por cima o contorno amarelo de
saturação **no formato das carnes**, e ainda com a saturação da fome comum — que para o vampiro não
quer dizer nada, porque o Vampirism prende a fome dele em 10 e usa o sangue no lugar.

Agora, para o vampiro, o contorno do AppleSkin é cancelado pela própria API dele e entra o nosso: a
mesma linha amarela de 1px, com a mesma paleta (clara em cima, escura embaixo), só que **seguindo a
borda da gota** e contando a **saturação do sangue**. Os quatro níveis parciais sobem de baixo para
cima, como líquido enchendo a gota; qual gota recebe qual nível é a regra do AppleSkin, copiada como
está. A prévia piscando de quanto a comida na mão encheria a barra também sai para o vampiro: eram
coxinhas desenhadas por cima das gotas, e comida não enche sangue.

**Quando desenhar.** Logo depois das gotas, e não na camada do AppleSkin. O contorno fica em cima
da borda preta da gota, pixel por pixel: desenhado antes dela, some inteiro — e a ordem entre as
camadas do AppleSkin e do Vampirism sai de qual mod registra primeiro, coisa que ninguém aqui
controla. Então o contorno é desenhado no aviso que o jogo dá depois da camada da barra de sangue, e
cai sempre por cima. As opções do AppleSkin (contorno ligado, montado num bicho, modo criativo)
continuam valendo, conferidas do mesmo jeito que ele confere.

**No F3**, embaixo da linha `hunger: … sat: …` do AppleSkin, o vampiro ganha uma igual para o
sangue: `blood: 20, blood sat: 3.5`. O `?` no lugar do número quer dizer que o servidor ainda não
mandou nada. É o jeito de conferir: saturação 0 é normal (ela gasta antes do sangue, e beber com a
barra cheia não enche saturação), e aí não há contorno nenhum para desenhar — igual ao AppleSkin.

**O pacote novo.** O Vampirism só manda a saturação do sangue para o cliente quando o *nível* de
sangue muda. Enquanto a saturação vai gastando, o nível fica parado — e o contorno ficaria cheio até
o fim, caindo de uma vez. O remédio é o mesmo que o AppleSkin usa para a saturação comum: o servidor
confere a cada 5 ticks e manda um pacote pequeno quando o número mudou. O número é lido do pacote
que o próprio Vampirism monta (campo `bloodSaturation`), sem reflexão.

Tudo isso só liga com Vampirism **e** AppleSkin instalados. Quando outro mod pede ao Vampirism para
não trocar a barra, as carnes voltam e o contorno de carne do AppleSkin volta junto.

## Ligar e desligar

O módulo é `vampire` em `config/nexuscore-common.toml`. Desligado, o NexusCore não mexe em nada e o
Vampirism volta a ser ele mesmo — o que é útil para conferir se algum comportamento estranho é dele
ou nosso.

Sem o Vampirism instalado, nenhuma classe que fala com ele chega a ser carregada. O pacote de rede
continua sendo registrado mesmo assim, de propósito: o NeoForge compara a lista de pacotes do
cliente e do servidor na entrada, e um registro que aparece só de um lado impediria a conexão.
