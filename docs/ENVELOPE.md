# Correio (Envelope)

O NexusCore ajusta o correio do mod **Envelope** (0.7.x) em três pontos: quem vem buscar a carta,
como o pombo chega e vai embora, e o morcego do lorde vampiro.

---

## O que o Envelope já faz sozinho

- Carta na caixa de correio + um **pombo a até 20 blocos**: o pombo vai até a caixa, pega a carta,
  sobe 16 blocos para o lado e 16 para cima, e **vira fumaça no ar**. A viagem acontece "no fundo",
  sem entidade nenhuma, e perto de quem recebe ele surge do mesmo jeito, com fumaça, 16/16 blocos
  acima da caixa, e desce para entregar.
- Pombo **só trabalha de dia e sem chuva**.
- Existe o **pombo de serviço** (o de chapeuzinho): é o que o criativo usa pelo ovo na caixa. Ao
  terminar a entrega, ele vira fumaça onde estiver.
- No meio da viagem, se alguém estiver perto da rota, o Envelope às vezes faz o pombo aparecer
  voando ali, só de enfeite.
- O **pombo carbonizado** (Nether) é outra criatura; não é usado aqui.

O que faltava: sem pombo por perto, a carta ficava parada; o pombo sumia e aparecia perto demais,
com a fumacinha à vista; e não havia nada para o lorde.

---

## O que muda

### 1. O carteiro vem de longe buscar

Carta esperando na caixa por alguns segundos (`courierWaitSeconds`, padrão 5) e nenhum pombo do
Envelope indo buscar: **um pombo de serviço surge longe** — uns 44 blocos da caixa e 20 acima,
num ponto que ninguém está olhando — e vem voando até ela. Chegando, pega a carta pelo mesmo
caminho que qualquer pombo do Envelope pega, e dali em diante é uma entrega normal do Envelope.

Nesses segundos de espera, um pombo que já esteja por perto tem a chance de ir buscar ele mesmo; o
carteiro só é chamado se nenhum estiver indo.

Se a carta sumir antes (alguém tirou, outro pombo levou) ou ele não conseguir chegar em 1min30, ele
desiste e vai embora voando.

### 2. Surge e some fora da vista

- **Partir e chegar.** O ponto onde o pombo some ao partir, e de onde surge ao chegar, vai de 16/16
  para **32/32 blocos** (`ascendDistance`). Com isso a fumaça fica a uns 45 blocos da caixa — além
  dos 32 blocos até onde o jogo manda partícula para alguém — e ninguém perto da caixa vê o pombo
  virar fumaça.
- **Terminar a entrega.** O pombo de serviço não vira mais fumaça na frente de quem recebeu: **vai
  embora voando**, para um ponto alto e longe que ninguém esteja olhando, e some assim que ninguém
  estiver olhando para ele (pode ser antes de chegar lá). Nunca some com alguém a menos de 20
  blocos. Se em 40 segundos não conseguir, some de qualquer jeito — um pombo encalhado para sempre
  seria pior.

**"Ninguém está olhando"** é medido do jeito que uma pessoa nota as coisas: o ponto tem de estar na
frente dela (até 75° do centro da visão, um pouco além do que a tela mostra), a menos de 96 blocos
e sem bloco no caminho. Quem está de costas, longe ou do outro lado de uma parede não conta. Se
nenhum ponto em volta estiver escondido, o carteiro usa o menos visto.

### 3. O morcego do lorde

Carta da caixa de um **lorde vampiro** vai de **morcego**:

- **Só de noite** (e fora de tempestade). O morcego é chamado de longe como o pombo do item 1 —
  não precisa de morcego, pombo nem pombo carbonizado por perto.
- **Nenhum pombo comum leva carta de lorde.** O pombo que procura caixa nem enxerga a do lorde; e
  se algum chegar até ela, volta sem nada. De dia, a carta do lorde espera a noite.
- **Cara e voz de morcego.** O modelo e as animações são os do morcego do jogo; o chiado, o som de
  dano e o de morte também. Passo, bater de asa e bicar comida ficam em silêncio, porque o morcego
  não faz nenhum desses. Por baixo continua sendo o pombo do Envelope: voa, pega a carta, viaja e
  entrega do jeito dele.
- **Nome de morcego no Jade.** Passar o mouse mostra "Morcego" (e "Minecraft" como mod), não
  "Pombo" — o Jade lê a entidade de verdade, e o NexusCore troca a plaquinha.
- **Deixou de ser lorde, volta a ser pombo.** A conta é refeita a cada segundo, em todo carteiro —
  inclusive num que já esteja no meio do caminho. Não tem nada para desfazer à mão.
- **De dia, o morcego não aparece no meio da viagem.** Se uma carta de lorde ainda estiver
  viajando quando amanhece, ela segue invisível e chega do mesmo jeito que o Envelope entrega
  quando não tem ninguém perto.

Quem conta como dono da carta é o **dono da caixa** de onde ela saiu (é o dono que o Envelope guarda
na entrega). Carta posta na caixa de um lorde vai de morcego, mesmo que outra pessoa tenha posto;
carta de um lorde posta na caixa de outra pessoa vai de pombo.

---

## Configuração

`nexuscore-server.toml` (na `serverconfig` do mundo), seção `[envelope]`:

| Opção | Padrão | O que faz |
|---|---|---|
| `summonCouriers` | `true` | chama o carteiro de longe quando não há pombo por perto (item 1) |
| `courierWaitSeconds` | `5` | quanto a carta espera antes de chamar o carteiro (0 a 300) |
| `ascendDistance` | `32` | a que distância da caixa o pombo some e surge (16 a 64; o Envelope usa 16) |
| `lordBats` | `true` | carta de lorde vai de morcego, e só à noite (item 3) |

O módulo liga e desliga em `nexuscore-common.toml`, `[modules.compat.envelope]`. Desligado, o
Envelope volta a ser exatamente o original. O módulo só liga com o Envelope **0.7.5 até 0.8**
(exclusive): ele mexe em partes internas do mod, e numa versão nova elas podem ter mudado. Sem o
Vampirism, o morcego nunca aparece e o resto funciona igual.

---

## Limites

- **O morcego não tem a mochilinha de carta** que o pombo carrega nas costas.
- **Só caixas a até 48 blocos de algum jogador chamam carteiro** — é onde o mundo roda e o pombo
  consegue se mexer. Caixa em lugar que ninguém visita fica esperando alguém passar.
- **Até 24 carteiros chamados ao mesmo tempo** no servidor inteiro.
- **O enfeite do meio da viagem ainda é do Envelope**: quando ele faz o pombo aparecer perto de
  alguém no meio da rota, aparece do jeito dele, com fumacinha. Costuma ser longe e alto, mas não dá
  para garantir que ninguém veja.
- **Morcego andando no chão.** Quando o pombo pousa (em frente à caixa, por exemplo), o morcego
  continua com a animação de voo.

---

## Como funciona

| Arquivo | O quê |
|---|---|
| [`CourierDispatch`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/CourierDispatch.java) | a cada 2 s olha as caixas perto dos jogadores e chama o carteiro; decide o horário; carta de lorde só sai com o carteiro chamado para ela |
| [`CourierGoal`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/CourierGoal.java) | o objetivo do carteiro fora da entrega: vir até a caixa, e depois ir embora voando |
| [`CourierSky`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/CourierSky.java) | onde surgir e sumir sem ser visto; a distância de subida |
| [`Couriers`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/Couriers.java) | as marcas do carteiro (chamado, indo embora, morcego) e de quem é a carta — viajam no NBT do pombo, inclusive durante a viagem invisível |
| [`BatCouriers`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/BatCouriers.java) | quem é morcego, refeito a cada segundo; troca dos sons |
| [`BatCourierClient`](../src/main/java/com/nexussmp/nexuscore/compat/envelope/client/BatCourierClient.java) | desenha um morcego do jogo no lugar do pombo, com a mesma técnica do anel da metamorfose ([`StandMirror`](../src/main/java/com/nexussmp/nexuscore/core/client/StandMirror.java)) |
| [`mixin/envelope`](../src/main/java/com/nexussmp/nexuscore/mixin/envelope/) | os pontos em que o Envelope foi aberto: som do pombo, fim da entrega, distância de subida, busca de caixa, retirada da carta e o enfeite do meio da viagem |

---

## Testes

1. **Carteiro de longe.** De dia, sem pombo por perto, pôr uma carta endereçada na caixa. Em uns 5 s
   um pombo de chapeuzinho surge longe (olhando para a caixa, ele vem de trás ou do lado) e chega
   voando. Pega a carta e parte subindo, e some longe.
2. **Ninguém vê sumir.** Receber uma carta e ficar olhando para o pombo depois da entrega: ele voa
   para longe e só some quando você vira o rosto (ou depois de 40 s).
3. **Pombo por perto.** Com um pombo do Envelope ao lado da caixa, pôr a carta: na maioria das
   vezes ele mesmo busca e nenhum carteiro é chamado. O pombo do Envelope só procura caixa a cada
   5 s ou mais; se ele demorar mais que `courierWaitSeconds`, os dois vêm, o primeiro leva a carta e
   o carteiro vai embora.
4. **Lorde de dia.** Com um lorde dono da caixa, pôr a carta de dia: nada acontece, mesmo com pombo
   ao lado.
5. **Lorde de noite.** Mesma caixa, à noite (`/time set night`): em uns 5 s um **morcego** chega de
   longe, chiando como morcego, e leva a carta.
6. **Deixou de ser lorde.** Com o morcego a caminho, tirar o lorde: em até 1 s ele vira pombo.
7. **Chuva.** Pombo não sai com chuva; morcego sai com chuva, mas não com tempestade.
