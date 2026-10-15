# Magia (Iron's Spells 'n Spellbooks)

O NexusCore fecha o **Iron's Spells 'n Spellbooks** (linha 3.16 da 1.21.1) para quem não é mago.
O mod continua inteiro no mundo: estruturas, magos inimigos, livros e pergaminhos nos baús, os mobs
e as magias que eles lançam. O que muda é quem pode **usar**: só quem a moderação pôs na lista de
magos. Para todo o resto, magia é uma língua que não se lê.

---

## Quem é mago

A lista fica no arquivo do mundo (`data/nexuscore_mages.dat`): mago continua mago depois do
restart. Ninguém nasce mago, e não há item, bioma ou missão que dê magia. A porta é o comando.

| Comando | Quem usa | O que faz |
|---|---|---|
| `/nexus mago adicionar <jogadores>` | operador (nível 2) | Põe na lista. Aceita quem está offline (quem já entrou no servidor alguma vez). |
| `/nexus mago remover <jogadores>` | operador | Tira da lista. |
| `/nexus mago lista` | operador | Mostra os magos. |
| `/nexus mago info` | qualquer um | Diz se você sabe magia. |
| `/nexus mago info <jogador>` | operador | Diz se aquela pessoa sabe magia. |

Quem está online sente na hora:

- **Virou mago — o despertar** (uns 2 segundos, tudo do jogo): a pessoa sobe um palmo do chão e
  flutua; as letras da mesa de encantamento vêm de todo lado e entram nela, enquanto um anel roxo
  gira em volta, fechando, e um arpejo de ametista sobe nota por nota. No fim, um estouro de luz e
  de letras para fora, o som de feitiço lançado, e ela desce devagar como pena. Na tela dela,
  **"Mago"** com *"Os símbolos pararam de dançar."* embaixo, e no chat *"Os símbolos param de
  dançar. Você agora sabe magia."* Os nomes das magias ficam legíveis e a barra de mana aparece,
  sem precisar relogar. Quem está offline ao ser adicionado não vê o despertar.
- **Deixou de ser:** fumaça, o som do sinalizador apagando, e *"A magia se esvai de você."* Em até
  1 s as joias de magia voltam para a mochila e qualquer lançamento em curso para.

---

## O que quem não é mago vê

É o mesmo esquema que o Iron's Spells já usa nas magias *eldritch* (as que exigem o manuscrito),
só que para **todas** as magias:

- **Todo nome de magia embaralhado, o tempo todo**, nas runas do mod: no nome do pergaminho, no
  tooltip, no livro de magias, nas mensagens. Os números da magia (dano, duração, alcance) também.
- **Toda linha de tooltip que fala de magia vira rabisco**, de qualquer item de qualquer mod: o
  "+1 nível de Bola de Fogo" do anel de afinidade, a "Mana máxima" e o "Poder de magia" de uma
  armadura de mago, a descrição da magia. O que decide é o texto, então vale até para a espada
  vanilla em que alguém gravou uma magia na bigorna arcana.
- **Sem HUD de magia:** a barra de mana, a barra de magias e as barras de lançamento não aparecem.
  A pessoa só passa a ter barra de mana quando vira maga.
- **A roda de magias não abre**, e o atalho de lançar não tem o que lançar: a lista de magias para
  escolher fica vazia (dos dois lados, cliente e servidor).
- **No JEI**, as páginas de descrição das magias (a que o Iron's Spells prende a cada pergaminho)
  somem. Voltam quando a pessoa vira maga.

## O que quem não é mago não consegue fazer

Recusa com um chiado: um fio de fumaça, o som de brasa apagando (no máximo uma vez por segundo) e
uma frase roxa na barra de ação.

- **Lançar magia**, de qualquer jeito: livro de magias, pergaminho, cajado, arma com magia gravada,
  atalho, roda. É barrado em três pontos independentes: na checagem do próprio mod (a mesma que
  recusa a eldritch não aprendida), no evento de antes do lançamento e no próprio lançamento. O
  `/cast` do operador continua funcionando.
- **Ler:** pergaminho, livros de lore, a crônica, o diário do arquievocador e o manuscrito eldritch.
  *"As letras dançam na página."*
- **Usar objeto mágico:** Cajado dos Nove, Vendaval do Crepúsculo, Convocador de Almas Cinosas,
  Núcleo Energizado, tinturas, elixires, chá de conjurador, Cerveja de Fogo.
- **Vestir joia de magia:** anel, amuleto, talismã e livro de magias não entram nas vagas do Curios,
  e o que já estava volta para a mochila.
- **Mesas de magia:** mesa de inscrição, forja de pergaminho, bigorna arcana, caldeirão alquímico,
  caldeirão de sangue, moldura de portal e runa de alma não abrem. Gravar magia no livro também não.

## O que continua normal para todo mundo

- **Armas e armaduras** do mod servem como arma e armadura: bater e se proteger não é magia. A
  magia gravada numa espada é que não sai.
- **Objetos comuns:** a besta autocarregável, a bússola errante, o mapa, as chaves dos cofres das
  estruturas, os discos, a comida, os blocos de construir e os ovos de spawn.
- **Material:** tinta, essência arcana, orbes. Clicar com eles na mão não faz nada (como no mod), e
  por isso também não recebe recusa.
- **Negociar com aldeão, montar no cavalo, abrir porta** segurando qualquer coisa do mod.
- **Mobs** lançam magia normalmente, e magia de mob acerta quem não é mago normalmente.

---

## Como funciona

| Onde | O quê |
|---|---|
| `magic/Mages` | A lista de magos (SavedData) e a pergunta `isMage`, que vale nos dois lados: no servidor pela lista, no cliente pelo que o servidor contou. |
| `mixin/magic/AbstractSpellMixin` | Quatro pontos de `AbstractSpell`: `obfuscateStats` (nome e números embaralhados), `canBeCastedBy` (a recusa com frase), `canBeCraftedBy` e `castSpell` (a última tranca). |
| `magic/MagicRules` | Eventos: antes de lançar, gravar magia, lista de seleção, clique em item/bloco/criatura, começar a usar, vaga do Curios e a conferência de 1 em 1 s. |
| `magic/client/MagicClient` | HUD escondido, tooltip embaralhado e a roda refeita quando muda o status. |
| `magic/client/jei/MagicJeiPlugin` | Esconde/mostra as páginas das magias no JEI. |

Objeto "mágico" = item do Iron's Spells que não é bloco, ovo, armadura, mapa, bússola, besta, disco,
chave de cofre ou comida (bebida de alquimista conta como mágica). Clique no ar só é barrado se o
objeto faz alguma coisa ao clique; clique em criatura, só se ele age na criatura (hoje, a tintura
de proteção do Nether).

O mixin mexe em métodos internos, então a versão fica presa em `[1.21.1-3.16, 1.21.1-3.17)`. Fora
dela o mixin não é aplicado e as regras não ligam (sem crash); o log diz qual versão achou.

## Ligar e desligar

O módulo é `magic` em `config/nexuscore-common.toml`. Desligado, todo mundo é mago e o Iron's
Spells volta a ser o de sempre.

## Testes

Precisa do perfil **SMP** (é o que tem o Iron's Spells, o Curios e o JEI).

1. **Nomes.** Sem ser mago, abrir o criativo na aba do Iron's Spells: todo pergaminho e todo livro
   com magia com o nome em runas que mudam sem parar. Passar o mouse num anel de afinidade e numa
   armadura de mago: as linhas de magia e de mana também.
2. **HUD.** Segurar um cajado: nenhuma barra de mana, nenhuma barra de magias.
3. **Lançar.** Usar um pergaminho, equipar um livro com magia e apertar o atalho, segurar uma
   espada com magia gravada: nada sai, fumaça e frase roxa.
4. **Objetos.** Beber elixir, ler livro de lore, abrir a mesa de inscrição, pôr anel no Curios: tudo
   recusado. Negociar com aldeão segurando um pergaminho: funciona.
5. **Virar mago.** `/nexus mago adicionar <você>`: efeito, mensagem, os nomes legíveis e a barra de
   mana aparecem na hora. Lançar funciona. No JEI, a página do pergaminho aparece (tecla U).
6. **Deixar de ser.** `/nexus mago remover <você>` com um anel equipado e no meio de um lançamento
   longo: o lançamento para, o anel volta para a mochila, a barra some.
7. **Restart.** Reiniciar o servidor: quem era mago continua mago.
8. **Offline.** `/nexus mago adicionar <quem está offline>`: ao entrar, a pessoa já é maga.
