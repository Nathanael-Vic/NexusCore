# Relíquias

Itens únicos que um operador entrega (`/give <jogador> nexuscore:<id>`), que fazem coisas que nada
mais no jogo faz, e que **nunca saem de quem os carrega**.

| Relíquia | Id | Como usa |
|---|---|---|
| Chave do Refúgio | `refuge_key` | clique direito |
| Pena dos Ventos | `wind_feather` | vestida nas costas (vaga *back* do Curios) |
| Coração de Brasa | `ember_heart` | vestido no pescoço (vaga *necklace*) |
| Máscara Astral | `astral_mask` | clique direito |
| Amuleto da Pureza | `purity_amulet` | vestido como berloque (vaga *charm*) |
| Pedra Inabalável | `unyielding_stone` | colocada como bloco |
| Bolsa Sem Fundo | `bottomless_bag` | clique direito |

O Anel da Metamorfose (`morph_ring`) também é relíquia e segue as mesmas regras abaixo.

---

## O que toda relíquia tem

- **Fica com você ao morrer.** Não vai para o chão nem para o corpo do Corpse — não dá para ser
  saqueada. As do inventário voltam quando você renasce; as vestidas no Curios nem saem da vaga.
  Se o servidor cair entre a morte e o renascer, elas voltam quando você entrar de novo.
- **Não dá para perder.** Jogada no chão, não queima em lava nem fogo, não quebra com explosão nem
  cacto, não some com o tempo, e se cair no vazio volta para quem a jogou.
- **Uma por vaga**, nome em roxo, e a última linha da dica lembra que é relíquia.
- **A recarga mora no item**, não no jogador: relogar ou morrer não zera a espera.

Por dentro: [`RelicKeeper`](../src/main/java/com/nexussmp/nexuscore/relic/RelicKeeper.java) tira as
relíquias da lista de drops antes de qualquer mod (é dali que o Corpse monta o corpo), e as vestidas
avisam o Curios que ficam (`ALWAYS_KEEP`). A lista do que é relíquia é a tag
`data/nexuscore/tags/item/relics.json`.

---

## Chave do Refúgio

Clique direito: uma porta branca aparece **na sua frente por 10 segundos**. Quem atravessar enquanto
ela está ali vai parar no **seu refúgio** — dá para levar amigos (e um estranho perto também entra).

- **O refúgio** é uma sala só sua: **13×13×13** livres, paredes, chão e teto de um branco liso
  **indestrutível**, e a própria parede ilumina a sala (nenhum bicho nasce lá dentro). Parede e porta
  não quebram com picareta, explosão, pistão, Wither nem dragão — nem no criativo; só admin no
  criativo passa, para consertar uma sala.
- **Dentro dá para construir:** baús, cama, decoração. Só as paredes são intocáveis. A cama marca o
  ponto de nascer ali (de dia; não dá para dormir).
- **Para sair**, a porta no meio da parede leva cada pessoa de volta para **onde ela entrou**.
- **Ninguém sai pelo lado:** quem aparecer fora da sala (fruta do coro, bug, o que for) volta para
  dentro, na frente da porta.
- A porta precisa de dois blocos livres e chão firme na frente. A chave descansa **30 s**, não
  funciona dentro do refúgio nem nas backrooms.

Cada dono tem uma sala, erguida na primeira visita, numa dimensão própria (`nexuscore:refuge`), a
512 blocos uma da outra.

---

## Pena dos Ventos

Vestida nas costas, faz exatamente o que a elytra faz: **pulou no ar, abre o voo**. Por cima, ela
segura um pouco mais no ar — a mesma descida rende quase o dobro de distância.

- **Descansa 8 s** depois de pousar.
- **Fogos de artifício não funcionam** com ela: é planar, não foguete. (Quem tem elytra de verdade
  no peito continua usando fogos com a elytra.)
- Deixa um rastro de vento enquanto plana.

---

## Coração de Brasa

Vestido no pescoço. **Quanto mais perto de morrer, mais forte.**

| Vida | Golpe dado | Dano recebido |
|---|---|---|
| metade ou mais | normal | normal |
| um quarto | +35% | −22% |
| um décimo ou menos | **+70%** | **−45%** |

- Abaixo da metade da vida, brasas saem de você — mais quanto mais perto do fim — e, bem perto,
  você ouve o coração batendo.
- **Sobreviveu?** Quem passou pela brasa e saiu da luta (8 s sem bater nem apanhar) recupera
  **40% do que perdeu**, uma vez por luta.
- **Morreu com ele no pescoço:** ele vira **cinzas por 30 minutos** — não acende, não cura, e a
  textura fica cinza.

---

## Máscara Astral

Clique direito: você **sai do corpo**. O corpo fica parado onde estava, de cabeça baixa, com a sua
skin, o seu nome e o que você vestia. Você vira espectador: voa, ninguém te vê, e você enxerga o
próprio corpo e os outros jogadores. Os outros só veem o corpo.

- **Atravessa o que é construído, não a terra:** parede de casa, porta, vidro, tronco, a alma passa
  (por dentro da parede a tela fica coberta, como no jogo normal). Pedra, terra, areia, cascalho e
  minério ela não atravessa: dá para espiar uma base, não descer pelo chão até as cavernas. Caverna,
  só entrando pela boca dela. A lista do que conta como terra está na tag `nexuscore:astral_solid`.
- **Sem mapa:** o minimapa e o mapa-mundi do Xaero ficam desligados enquanto você está fora, como
  nas backrooms.
- **Preso por um fio:** passou de **32 blocos** do corpo, volta para o último ponto dentro do fio.
  Não abre baú, não entra na cabeça de bicho, não se teletransporta para jogadores pelo menu de
  espectador.
- **Não enxerga longe:** ao sair, uma névoa escura, quase preta, fecha a visão: começa logo à
  frente e apaga tudo em uns **28 blocos**, que encolhem até **16** com o tempo. A borda dos chunks
  nunca aparece, nem com a névoa desligada no Sodium Extra.
- **Voltar:** encoste no próprio corpo, ou aperte **B** (dá para trocar em *Controles → NexusCore*).
  Voltar tonteia — mais quanto mais tempo ficou fora.
- **Bateram no corpo?** Você é arrancado de volta na hora e leva o golpe com **1,5×** a força.
- **A loucura.** Fora do corpo a cabeça vai cedendo, e tudo cresce com o tempo:
  - logo: um véu roxo e as bordas escurecendo;
  - um terço: sussurros vindo de lugar nenhum, a câmera pendendo, a visão embaçando às vezes;
  - metade: o coração batendo alto, frases surgindo e sumindo pela tela, chiado de TV, a imagem
    rasgando em faixas, a visão partida (o olhar da aranha);
  - perto do fim: cores invertendo, tudo tremendo e girando, o coração disparado, um grito ao
    fundo e um **VOLTE** enorme no meio da tela;
  - **passou de 30 segundos:** a maldição do guardião, tela vermelha — e você começa a se desfazer:
    dano a cada meio segundo, cada vez maior (de vida cheia, uns 2 s até o fim), que atravessa armadura e qualquer proteção, até voltar ou
    morrer. Morrendo assim, você cai **no lugar do corpo** ("se perdeu fora do próprio corpo").
- **Descansa 8 minutos** depois de usar. Não funciona nas backrooms nem no refúgio, montado,
  dormindo, fora do sobrevivência/aventura, ou logo depois de apanhar.
- Sair do servidor no meio te devolve ao corpo; se o servidor cair, você volta para o corpo (e para
  o modo de jogo de antes) ao entrar de novo.

---

## Amuleto da Pureza

Vestido como berloque. **Três gotas de água clara** presas no cristal.

- Cada efeito ruim que tentar pegar você — veneno, wither, fraqueza, lentidão, cegueira, náusea,
  fome, a escuridão do warden — é **bebido por uma gota** antes de pegar. Efeito bom não gasta nada.
- **As gotas voltam sozinhas**, uma a cada **1 min 30 s**, enquanto ele está vestido.
- Dá para ver quantas restam no próprio amuleto: cada gota acesa é uma carga.
- É proteção para o susto, não imunidade: três golpes de aranha da caverna seguidos já secam o
  amuleto.

---

## Pedra Inabalável

Um bloco só, feito para trolar (ou proteger um ponto).

- **Quem coloca vira o dono.** Na mão, ela é de ninguém.
- **Ninguém quebra:** picareta, explosão, pistão, Wither, dragão, enderman — nada. Mais dura que
  a bedrock.
- **Só o dono tira:** agachado, clique direito com **as duas mãos vazias** — ela volta direto
  para o inventário.
- **Ninguém sabe de quem é.** O nome do dono nunca sai do servidor, a não ser para **admin**, que
  vê "Dono: Fulano" no Jade.
- Admin no criativo consegue quebrar, para uma pedra esquecida não ficar para sempre num lugar
  errado.

---

## Bolsa Sem Fundo

Clique direito abre **o espaço de um baú duplo (54 vagas)**. O que está dentro mora no próprio item
— e como toda relíquia ela fica com você na morte, **com tudo o que tem dentro**.

- A própria bolsa não se mexe enquanto está aberta (nem tecla de número, nem jogar fora).
- **Bolsa não entra em bolsa**, nem caixa de shulker — seria espaço infinito.
- Tudo o que muda lá dentro vai na hora para o item.

---

## Texturas

Geradas por [`tools/textures/generate-relic-textures.cjs`](../tools/textures/generate-relic-textures.cjs)
(`node tools/textures/generate-relic-textures.cjs [pasta]` — com uma pasta, salva também uma prévia
ampliada). O coração tem a versão em cinzas e o amuleto uma por quantidade de gotas; o modelo do
item escolhe sozinho.

---

## Teste em jogo (pendente)

- [ ] Morrer com cada relíquia (no inventário e vestida): nenhuma no corpo do Corpse, todas de volta.
- [ ] Chave: porta abre, amigo entra junto, sala erguida, sair volta para cada um; fruta do coro devolve.
- [ ] Pena: abre no ar, plana mais que elytra, recarga ao pousar, fogos recusados.
- [ ] Coração: mais forte com pouca vida, cura depois da luta, cinzas ao morrer.
- [ ] Máscara: corpo visível, atravessa construção mas não terra, mapas desligados, fio de 32 blocos, névoa escura limitando a visão, B volta, encostar volta, golpe no corpo puxa; efeitos escalando; morte pelo limite cai no corpo; relogar no meio.
- [ ] Amuleto: veneno absorvido, gotas recarregando, textura mudando.
- [ ] Pedra: ninguém quebra, dono recolhe, Jade mostra o dono só para admin.
- [ ] Bolsa: guarda, não perde na morte, não entra em si mesma.
