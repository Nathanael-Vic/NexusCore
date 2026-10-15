# Rádio ao vivo

Uma transmissão por vez, anunciada para o servidor inteiro, com a cara do jogo.

A **música** e a **voz** já tocam: o locutor cola um link do YouTube e fala no microfone, e os dois
saem nos fones de quem está ouvindo, em qualquer lugar do mapa. O sistema de convidados entra
depois, em cima exatamente destas peças — as marcas de quem está no ar e os volumes já existem.

## Quem pode abrir o ar

Rádio é coisa de **locutor**: a moderação nomeia quem apresenta, e a lista fica salva no mundo
(`/nexus radio locutor adicionar <jogadores>`). Operador também pode abrir o ar sem estar na lista.

Quem vira locutor recebe o aviso na hora — o botão da pausa e o comando aparecem para ele no mesmo
instante, sem precisar entrar e sair do servidor.

## O caminho de quem está jogando

1. O locutor abre o ar: **Esc → Rádio ao vivo → Abrir o ar**, que pede o **título do programa** e,
   se ele quiser, um **nome no ar** (vazio = o nick dele). Pelo comando é
   `/nexus radio iniciar <título>`, e aí o nome é sempre o nick.
2. Todo mundo recebe **o aviso de canto**, igual ao de uma conquista: a cara de quem está
   apresentando, a linha "AO VIVO · fulano" e o título. Some sozinho em cinco segundos. A cara vem
   sempre de **quem** está falando (a skin que o servidor tem, já passada pelo SkinRestorer), mesmo
   quando o nome no ar é um nome de programa.
3. No chat chega, junto, uma linha com **`[Ouvir]`** clicável. É o botão que o aviso não pode ter.
4. Enquanto a transmissão existe, fica uma **plaquinha no canto de baixo à direita** — um ponto
   vermelho piscando devagar e "AO VIVO" (ou "OUVINDO", para quem já entrou).
5. Perdeu o aviso e o chat? **Esc** → o botão **"● Rádio ao vivo"** no canto de baixo à esquerda da
   pausa (ele fica sempre lá: **apagado** quando não há nada no ar, aceso com o ponto vermelho
   quando alguém entra — locutor clica mesmo apagado, porque é por ali que ele abre o programa) abre a tela da rádio: painel vanilla com a faixa **AO VIVO** e o tempo no ar, a cara de
   quem apresenta numa moldura de slot, o título, o seu estado, e um **medidor de som** que só fica
   aceso para quem está ouvindo. Embaixo, **Ouvir** / **Sair da transmissão**; quem apresenta
   também vê **Encerrar transmissão**.
6. Quem entra no servidor no meio da transmissão recebe o mesmo aviso e o mesmo convite no chat.
   Operador vê o botão **Encerrar transmissão** em qualquer programa, não só no dele.
7. Quando acaba, a plaquinha some e o botão da pausa some junto.

### Por que não tem botão dentro do aviso

Durante o jogo o mouse pertence à câmera: não existe cursor para clicar em nada da HUD — nem em
aviso, nem em indicador. É por isso que o Minecraft não tem um único botão clicável fora das telas.
Então o "botão OUVIR" existe em dois lugares onde clicar funciona de verdade: **no chat** (link
vanilla, como qualquer servidor manda) e **na pausa**. O aviso continua sendo o que o jogo faz de
melhor: contar que aconteceu alguma coisa e sair da frente.

## A música

O locutor abre a **mesa de som** (Esc → Rádio ao vivo → Mesa de som), cola o link e manda tocar.

O ponto que faz isso funcionar: **o servidor não carrega áudio nenhum**. Ele guarda só o endereço e
o relógio — em que instante aquele link estaria no segundo zero. Cada cliente que está ouvindo toca
por conta própria, pelo VLC que o **WaterMedia** traz, exatamente como os telões do WaterFrames
fazem. É o único caminho que presta: música empurrada pelo canal de voz sairia com som de telefone,
porque o codec de voz é feito para voz.

Quem chega no meio do programa entra no meio da música, porque a posição vai junto com o estado. De
quarenta em quarenta ticks o cliente confere se não se afastou mais que 2,5 s do ponto certo; se
afastou, pula. Pausar guarda a posição, retomar recalcula — ninguém precisa mandar "vá para tal
segundo" o tempo todo.

Sem o WaterMedia instalado nada disso carrega e a rádio segue funcionando só com o resto.

### Os volumes

São três, e nenhum deles encosta no volume do jogo nem no do chat de voz — essa é a regra:

| Quem mexe | No quê | Onde |
| --- | --- | --- |
| **Locutor** (e a moderação) | volume da música, para todo mundo | mesa de som |
| **Locutor** (e a moderação) | volume de **cada voz no ar**, para todo mundo | mesa de som, uma barra por pessoa |
| **Cada ouvinte** | o volume da rádio **para ele mesmo** | a própria tela da rádio |

O que o ouvinte escuta é a conta das duas pontas: a mistura do programa vezes o volume dele. E o
microfone tem chave própria — o locutor fecha o dele na mesa e continua conversando no jogo sem o
servidor inteiro ouvir.

## Os estados

A sessão ([`BroadcastSession`](../src/main/java/com/nexussmp/nexuscore/radio/BroadcastSession.java))
guarda id, apresentador, título, início, duração e estado — `LIVE` ou `ENDED`.

Cada pessoa dentro dela tem três marcas **separadas** ([`Membership`](../src/main/java/com/nexussmp/nexuscore/radio/Membership.java)):

| Marca | O que significa |
| --- | --- |
| `listening` | abriu o áudio da transmissão. Nada além disso |
| `invited` | o apresentador autorizou essa pessoa a participar — autorização não liga microfone |
| `onAir` | a voz dela está indo junto com a do apresentador, **agora** |
| `micMuted` | está no ar, mas com o microfone fechado — continua sendo do programa, só que o que ela falar não entra na transmissão |

O **mudo** existe separado do "no ar" por um motivo prático: quem apresenta precisa poder conversar
com quem está do lado dele no jogo sem o servidor inteiro ouvir. É essa marca que o canal lê para
decidir se manda a voz adiante — e **só** isso: quem está do lado continua ouvindo pela proximidade,
que é justamente o ponto. A rádio nunca mexe no chat de voz; silêncio total é o mudo do próprio
Simple Voice Chat. O botão fica na tela da rádio, só para quem
está no ar, no estilo das opções do jogo (`Microfone: aberto` / `Microfone: MUDO`), e o letreiro
mostra **MUDO** ao lado do tempo enquanto estiver assim.

É essa separação que faz o sistema de convidados caber depois sem refazer nada: dá para estar só
ouvindo, estar convidado sem estar no ar, e sair do ar continuando como ouvinte. O `Role`
(`NOT_LISTENING`, `LISTENING`, `INVITED`, `ON_AIR`) é só a palavra que a tela escreve; quem manda
são as marcas.

A transmissão vive **em memória**: transmissão é coisa do momento, e servidor que desliga leva o ar
junto. Se o apresentador sai do servidor, a transmissão encerra.

## Comandos

```
/nexus radio iniciar <titulo>                 (locutor ou operador)
/nexus radio parar                            (o dono do ar, ou operador)
/nexus radio ouvir
/nexus radio sair
/nexus radio info
/nexus radio avisar [jogadores]               (operador — reenvia o aviso, para testar)
/nexus radio locutor adicionar <jogadores>    (operador)
/nexus radio locutor remover <jogadores>      (operador)
/nexus radio locutor lista                    (operador)
```

`avisar` sem argumento manda o aviso para você mesmo; com argumento, para quem for escolhido. Serve
só para testar a notificação sem ter que abrir uma transmissão nova a cada tentativa — **não
substitui nada**: quando alguém abre o ar, o aviso já sai sozinho para o servidor inteiro.

`ouvir` e `sair` são de todo mundo — é o mesmo caminho que o `[Ouvir]` do chat e os botões da tela
usam por baixo.

## As telas

Tudo é desenhado com peça do próprio jogo: o fundo de janela (`popup/background`), a faixa de título
das conquistas (`advancements/title_box`) virando letreiro de "no ar", e a moldura de vaga do
inventário (`container/slot`) em volta da cara. Botões e barras de volume são os widgets do próprio
menu de opções. O único enfeite próprio é o medidor de som — barras que sobem e descem, desenhadas
como a barra de experiência é desenhada.

O painel é pintado em `renderBackground`, e não no `render`. Parece detalhe e não é: o jogo junta os
retângulos num lote e o texto em outro, então um painel desenhado *depois* dos botões cobria a
moldura deles e deixava só as letras por cima — botão virava texto solto no ar.

O tamanho de tudo é **calculado uma vez**, no `init`, e o desenho e os botões leem a mesma conta.
Era isso que faltava quando o texto aparecia por cima do botão: cada parte escolhia a própria
altura, e em algumas escalas de interface as duas escolhiam o mesmo lugar. Quando a tela é baixa
demais para tudo, o medidor de som sai primeiro — o que importa sempre cabe.

Todo texto passa por chave de tradução (`pt_br` e `en_us`), então a tela segue o idioma do jogo de
quem está olhando.

## As peças

| Arquivo | Papel |
| --- | --- |
| [`BroadcastSession`](../src/main/java/com/nexussmp/nexuscore/radio/BroadcastSession.java) | a transmissão e quem está dentro dela |
| [`Membership`](../src/main/java/com/nexussmp/nexuscore/radio/Membership.java) | ouvindo / convidado / no ar |
| [`RadioManager`](../src/main/java/com/nexussmp/nexuscore/radio/RadioManager.java) | abre, fecha, avisa e guarda o estado (servidor) |
| [`RadioServerHandler`](../src/main/java/com/nexussmp/nexuscore/radio/RadioServerHandler.java) | confere os pedidos que chegam da tela |
| [`RadioPayloads`](../src/main/java/com/nexussmp/nexuscore/radio/network/RadioPayloads.java) | os dois pacotes: estado (→ cliente) e ação (→ servidor) |
| [`RadioClient`](../src/main/java/com/nexussmp/nexuscore/radio/client/RadioClient.java) | o que o cliente sabe do ar |
| [`BroadcastToast`](../src/main/java/com/nexussmp/nexuscore/radio/client/BroadcastToast.java) | o aviso de canto |
| [`RadioHud`](../src/main/java/com/nexussmp/nexuscore/radio/client/RadioHud.java) | a plaquinha de "no ar" |
| [`RadioPauseButton`](../src/main/java/com/nexussmp/nexuscore/radio/client/RadioPauseButton.java) | o botão na tela de pausa |
| [`RadioScreen`](../src/main/java/com/nexussmp/nexuscore/radio/client/screen/RadioScreen.java) | a tela da transmissão |
| [`BroadcastStartScreen`](../src/main/java/com/nexussmp/nexuscore/radio/client/screen/BroadcastStartScreen.java) | abrir o ar: título e nome no ar |
| [`RadioPanel`](../src/main/java/com/nexussmp/nexuscore/radio/client/screen/RadioPanel.java) | painel, letreiro e medidor de som |
| [`RadioMixerScreen`](../src/main/java/com/nexussmp/nexuscore/radio/client/screen/RadioMixerScreen.java) | a mesa de som: microfone, música e volumes |
| [`RadioMusic`](../src/main/java/com/nexussmp/nexuscore/radio/client/RadioMusic.java) | põe a música no ritmo do que o servidor diz |
| [`MusicBox`](../src/main/java/com/nexussmp/nexuscore/compat/watermedia/MusicBox.java) | a única classe que encosta no WaterMedia |
| [`RadioHosts`](../src/main/java/com/nexussmp/nexuscore/radio/RadioHosts.java) | a lista de locutores, salva no mundo |

O servidor manda **o estado inteiro** a cada mudança (e a quem entra no servidor); o cliente nunca
adivinha nada. Por isso o aviso, a plaquinha e a tela nunca discordam entre si.

## A voz

Pelo Simple Voice Chat, em [`radio/voice/`](../src/main/java/com/nexussmp/nexuscore/radio/voice). Sem
o mod de voz instalado, essa parte simplesmente não carrega e o resto da rádio segue igual.

O pacote de microfone de quem está no ar é copiado e mandado a cada ouvinte como **som sem
posição** — o som de um rádio, que vem de todo lado e não enfraquece com a distância. Quem está no
Nether ouve igual a quem está do lado.

**Quem já ouve de perto não recebe a cópia.** Quem está perto do locutor já o escuta pelo chat de
voz, por proximidade; mandar a cópia também faria a pessoa ouvir tudo duas vezes, com um eco de
fração de segundo. Então, para esse ouvinte, a rádio cala a voz e deixa a proximidade trabalhar —
a **música continua**, porque ela é tocada por cada cliente e não passa por aqui.

Toda dúvida vai para o lado de **mandar**, porque ouvir duas vezes é chato e não ouvir é pior. A
cópia só é cortada quando dá para ter certeza de que a pessoa está ouvindo:

| Situação | A rádio manda a voz? |
|---|---|
| Mesmo grupo do chat de voz | não — grupo se ouve em qualquer distância |
| Sem grupo, mesmo mundo, até **60% do alcance** | não — ali a proximidade chega com volume de conversa |
| Beirada do alcance (60–100%) | sim — ali a proximidade é quase um sussurro |
| Grupos diferentes, ou só um em grupo | sim — depende do tipo do grupo, e na dúvida leva |
| Locutor cochichando | sim — o alcance encolhe e a API não diz quanto |
| Outro mundo | sim |

**Os volumes** valem de verdade agora: o que chega aos ouvidos é `volume daquela voz na mesa ×
volume pessoal do ouvinte`, a mesma conta da música. A multiplicação é feita no cliente, com o
áudio já aberto — no servidor ela obrigaria a reabrir e fechar o áudio por pessoa, cinquenta vezes
por segundo. E nada disso encosta nas configurações do Simple Voice Chat.

**Por dentro.** O chat de voz roda na thread dele, cinquenta pacotes por segundo por pessoa falando;
a transmissão muda na thread do jogo. Para uma nunca ler a outra pela metade, o jogo tira uma foto
de quem fala e quem ouve a cada mudança (`RadioVoiceState`), e o chat de voz só olha a foto. Cada
voz vai num canal próprio, derivado do id da pessoa — assim o cliente sabe de quem é cada pedaço de
áudio sem perguntar nada a ninguém.

## O que vem depois

1. **Convidados**: o apresentador convida (`invited`), a pessoa aceita — e aceitar **não** liga
   microfone nenhum. Durante a transmissão ele põe o convidado no ar (`onAir`), com uma confirmação
   antes; tirar do ar devolve a pessoa à condição de ouvinte.

## Config (`config/nexuscore-common.toml`)

```toml
[modules.radio]
    enabled = true
```

Desligado, o comando recusa e nada aparece na tela.
