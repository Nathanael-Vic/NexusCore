# Backrooms

Um andar de corredores amarelos numa dimensão própria, e o evento raro de cair nele.

## Como funciona

**A dimensão** (`nexuscore:backrooms`) é um vazio plano — quem levanta o andar é o comando, não a
geração de mundo. Ela é baixa de propósito: `min_y 0`, `height 16`, uma única seção de chunk por
chunk. Um andar de 700×700 passa de dois mil chunks; com uma seção só cada, salvar e carregar isso
sai barato.

**A planta** segue a receita do [davidpcahill/backrooms](https://github.com/davidpcahill/backrooms),
que é a que dá a cara de "Nível 0":

1. **Corredores** — centenas de labirintos parciais sobrepostos. Cada camada nasce num ponto
   qualquer, cresce pelo algoritmo de árvore crescente e fica sem orçamento antes de virar um
   labirinto organizado. Onde uma camada esbarra na outra, metade das vezes ela se junta e metade
   das vezes morre ali. É a sobreposição que produz o corredor sem fim e sem lógica.
2. **Salas** — retangulares, poligonais tortas e halls de pilares carimbados por cima.
3. **Limpeza** — tira ilha de chão presa dentro de parede. Parede solta no meio do chão **fica**:
   ela lê como pilar.

A largura do corredor é a escala da planta (`cellSize`, padrão **3 blocos**). Com 1 o lugar vira um
labirinto apertado de um bloco; 3 é o corredor largo das backrooms.

**O andar**: carpete no chão, rodapé na primeira fiada da parede, papel de parede nas de cima, 4
blocos de pé-direito e o forro fechando em cima. Sem céu e com luz ambiente quase zero: o que
ilumina são as lâmpadas.

**O zumbido** das lâmpadas é um loop do cliente que acompanha a pessoa, com o volume medido pela
lâmpada acesa mais perto (dez blocos de alcance). Debaixo de um painel ele enche o corredor; mais
longe ele cai, mas **nunca abaixo de um piso** — enquanto houver luz acesa por perto o forro não fica
mudo de todo. Quando o skinwalker apaga o andar, aí sim o zumbido morre junto, e o silêncio na
perseguição vira parte do susto de graça. Ele toca no canal **ambiente** do jogo, que é o controle
de volume que manda nele.

**A luz** não é regular. `lampSpacing` é o vão de forro entre um painel e o seguinte (padrão 3), e
`lampChance` é quantos desses lugares realmente ganham luz (padrão 75%). Teto todo aceso fica de
supermercado; o que assusta é o corredor que apaga mais adiante. O sorteio sai da semente do andar,
então as mesmas lâmpadas faltam sempre — inclusive se ele for refeito com a mesma semente.

**As portas de saída** caem em qualquer chão do andar — quarto ou corredor — e o mais longe umas das
outras que o andar permite. O sorteio é o do "melhor candidato": a primeira porta cai num chão
qualquer; para cada uma das seguintes sorteiam-se 96 chãos e fica o que está mais longe de todas as
portas já postas. Continua aleatório, mas nunca escolhe um ponto perto quando havia um longe, e a
distância se ajusta sozinha ao tamanho do andar. Medido em 200 plantas de 400 blocos com 3 portas: a
menor distância entre duas portas fica em **~310 blocos** na média (antes eram ~160), e metade das
portas cai em corredor (antes, 30%).

Quando a porta tem parede do lado, a folha dela fica **encostada na parede** — parece posta ali de
propósito e nunca atravessa um corredor, nem o de um bloco. No meio de um quarto, sem parede em
volta, ela fica solta e virada para qualquer lado. Abrir qualquer porta de madeira lá dentro devolve
a pessoa **exatamente de onde ela caiu**.

**Onde a pessoa cai** é sorteado a cada entrada, em qualquer chão do andar — quarto ou corredor —
longe das bordas e, sempre que o andar permitir, **longe de qualquer porta**: um quinto do lado do
andar, nunca menos de 40 blocos (80 num andar de 400, 140 num de 700). Cair ao lado de
uma saída estragaria o lugar: em dez segundos a pessoa acha a porta e volta, e nada do que foi
construído aconteceu.

**A queda**: a pessoa trava no lugar, a câmera afunda no chão até ficar dentro do bloco e a tela
apaga; o teleporte acontece no escuro, no meio da animação, e do outro lado a câmera volta ao lugar.
A entidade nunca é movida para dentro de bloco nenhum — só o ponto de vista (`mixin/backrooms/CameraMixin`).

## Você entra sem nada

Ao cair, **o inventário inteiro fica guardado** e a pessoa aparece do outro lado de mãos vazias. Ao
voltar — por porta, por ser pega ou por comando — tudo volta exatamente como estava, no lugar onde
ela parou. O guarda-volumes é o [`PlayerVault`](../src/main/java/com/nexussmp/nexuscore/core/vault/PlayerVault.java),
que não é das backrooms: cada guardada tem uma **chave** (`backrooms`, e amanhã `space`), então dois
sistemas nunca disputam o mesmo inventário. Fica no arquivo do mundo, não em memória — cair o
servidor com alguém lá dentro não come as coisas dela.

As 36 vagas de sempre não são tudo: quem instala mods de acessório pendura outras. Por isso o
guarda-volumes aceita **compartimentos** (`VaultCompartment`) — com o **Curios** instalado, colar,
anel, capa e cosméticos vão junto e voltam junto. Sem ele, o compartimento nem é registrado.

## As regras de lá dentro

Nada de fome, nada de quebrar ou colocar bloco (fora do criativo) e **nenhum dano passa**. Morrer
também não existe: se por algum caminho a morte chegar, ela vira uma saída pela porta. O lugar é um
susto, não um desafio de sobrevivência — ninguém perde item nem nível por causa de uma brincadeira.

Os blocos das Backrooms (papel de parede, rodapé, carpete, teto e luminária) são **indestrutíveis
como a bedrock** em qualquer lugar do mundo: não quebram fora do criativo, não caem com explosão,
não são empurrados por pistão nem destruídos pelo Wither, não dropam nada e não têm receita. Só o
gerador (`/nexus backrooms gerar`) e o criativo põem e tiram esses blocos.

## O skinwalker

Ele não está lá quando você chega. Passados alguns minutos de corredor e zumbido (`skinwalkerDelaySeconds`,
padrão 45 s), **alguém aparece ao longe** — com a skin e o nome de uma pessoa do servidor.

1. **Rondando** — ele **te segue**: vem atrás pelo labirinto, apressa o passo quando fica para trás e,
   a uns cinco blocos, para e fica ali te olhando. Nesta fase ele é **100% jogador**: nome por cima da
   cabeça, nenhum som de criatura, e de vez em quando **uma fala daquela pessoa com a voz dela**, sem
   nenhum retoque. A transformação é espontânea — chegar perto dele não antecipa nada.
2. **Se revelando** (~6 s) — trava no lugar, estala osso, as lâmpadas em volta começam a falhar e a
   imagem dele alterna com uma silhueta preta, cada vez mais para o lado do preto. A tela da vítima
   vai fechando e a câmera treme.
3. **Caçando** — fica todo preto, um pouco mais rápido que alguém correndo, e vem em linha reta. Não
   dá para simplesmente correr mais que ele: **a saída é a porta**.

Ser pego não machuca, mas tem cara de morte: a tela corta para o preto **na hora**, sem dar para ver
mais nada do lugar, entra um chiado de sinal perdido (TV fora do ar, com faixas rolando) e aparece
**"Você foi pego."** tremendo em vermelho. Uns quatro segundos depois a pessoa acorda no mundo de
onde saiu, com tudo de volta. A cabeça que fica no lugar sempre cai até o chão, mesmo se a pessoa
foi pega no meio de um pulo. Quem ele imita é escolhido nesta ordem: alguém com voz gravada (aí a imitação vem completa),
qualquer outro online, ou — se não houver ninguém — **você mesmo**.

### A voz

Vem do Simple Voice Chat. O plugin escuta os pacotes de microfone e guarda **os pacotes do jeito que
chegam, comprimidos** — o chat de voz transmite Opus, uns 60 bytes a cada 20 ms. Uma frase de quatro
segundos ocupa cerca de **12 KB** assim, contra 380 KB se fosse guardada como onda. São até
**8 falas por pessoa**, numa fila circular: entra fala nova, sai a mais velha, então o repertório
se renova sozinho enquanto as pessoas conversam. Fala com mais de **30 minutos** vence e sai.

**O corte é o que decide se o susto funciona.** Pedaço curto demais sai picado e ninguém entende o
que foi dito, e o que costuma cortar uma frase no meio é a pausa da respiração. Por isso a fala só
fecha depois de **700 ms** de silêncio (mais que uma respirada, menos que a troca de assunto), o teto
é de **4 segundos** para uma frase inteira caber, e qualquer coisa com menos de **1 segundo** é
jogada fora antes de virar trecho — pigarro não assusta ninguém. Na hora de falar ele sorteia três
trechos e fica com o mais longo: quase sempre sai coisa que dá para entender, e ainda muda de uma vez
para a outra.

Menos de 100 KB por pessoa, e a conta cara (descomprimir) só acontece no instante em que ele resolve
falar. Nada vai para o disco: fala guardada é coisa de sessão — servidor reiniciado, ele emudece até
alguém falar de novo.

Rondando, a voz sai **sem nenhum retoque**: é a pessoa, igualzinha. Transformado, ele continua
falando, aí num tom mais fundo — a mesma voz, num registro que ninguém tem.

Sem o mod de voz instalado nada disso carrega, e ele só fica mudo. Quando alguma fala não sai, o log
diz em que passo parou (`SEM_API`, `SEM_TRECHO`, `SEM_CANAL`), uma linha por motivo.

### Os sons

Vêm do **SP-Backrooms Revamped** (SpacePotato e Herr Chaos), sob LGPL-3.0. Ficam em
`assets/nexuscore/sounds/backrooms/`, com a licença e o crédito na mesma pasta; trocar qualquer um
por um som próprio é só substituir o arquivo.

## Os outros mods do pack

As backrooms são um lugar com regras próprias, e alguns mods do pack passariam por cima delas sem
querer. Cada conversa dessas mora em `compat/` e **nenhuma é obrigatória**: sem o mod instalado, o
código nem é carregado.

| Mod | O que acontece lá dentro | Como |
| --- | --- | --- |
| **Xaero's Minimap / World Map** | Nenhum mapa funciona: nem minimapa, nem mapa-mundi, nem radar, nem waypoints, nem cave maps. | Os próprios mods registram efeitos de status para poderem ser proibidos (`xaerominimap:no_minimap` e companhia). O efeito é aplicado ao entrar, renovado de segundo em segundo e retirado na volta — nada de mixin, nada de dependência. Como o Xaero quase sempre é só de cliente (o servidor não conhece os efeitos dele e por isso não teria como mandá-los), **quem aplica é o cliente em si mesmo**, ao ver que está na dimensão; com os mods também instalados no servidor, ele aplica junto. |
| **PlayerRevive** | Ninguém fica caído. Lá dentro não passa dano, então o estado de abatido nunca começa; e quem **já estava** caído não é levado pelo evento nem pelo comando. | A regra de dano corre antes da dele (`EventPriority.HIGHEST`). O estado é lido pelo attachment `playerrevive:bleeding`. |
| **Corpse** | Não fica corpo no corredor — o único rastro é a cabeça que o skinwalker deixa. | A morte é cancelada antes de o Corpse ouvi-la; e um corpo que tentasse nascer ali é barrado na entrada da dimensão. |
| **Curios** | Acessórios e cosméticos entram no guarda-volumes junto com o inventário. | Compartimento do `PlayerVault`, usando o próprio `saveInventory`/`loadInventory` do Curios. |
| **SkinRestorer** | O skinwalker usa a skin certa, inclusive em servidor offline ou com skin trocada por comando. | O servidor manda o **perfil inteiro** da pessoa imitada (nome, id e textura) junto com a entidade. O cliente não pergunta nada à Mojang: usa o que veio. A cabeça no chão segue o mesmo caminho. |

Duas consequências que valem lembrar: **a morte nas backrooms é sempre uma saída** (a pessoa acorda
no mundo, inteira, com tudo de volta), e **o skinwalker só imita quem está online** — copiar alguém
que saiu daria nome de um com a cara de outro.

## Comandos

```
/nexus backrooms gerar                       (usa a config)
/nexus backrooms gerar 300                   (tamanho: de 100 a 700)
/nexus backrooms gerar 300 4                 (tamanho e portas)
/nexus backrooms gerar 300 4 true            (e o teto)
/nexus backrooms gerar 300 4 true 12345      (e a semente)
/nexus backrooms enviar <jogadores>
/nexus backrooms voltar <jogadores>
/nexus backrooms teto ligar | desligar
/nexus backrooms skinwalker <jogadores>      (chama o bicho na hora, sem esperar)
/nexus backrooms limpar                      (tira as cabeças do chão)
/nexus backrooms info
```

Os valores vêm um depois do outro, na ordem **tamanho, portas, teto, semente**, e todos são
opcionais — o jogo mostra `<tamanho>`, `<portas>` e companhia acima do campo enquanto se digita.
Construir um andar grande leva alguns segundos e o progresso aparece na barra de ação; o servidor
não trava porque a obra anda alguns chunks por tick.

**Dado de dentro das backrooms, `gerar` refaz o andar onde você está**, no mesmo lugar, em vez de
abrir outro num canto novo. A obra cobre o retângulo antigo e o novo ao mesmo tempo, então um andar
menor não deixa pedaço do anterior boiando em volta. `teto ligar`/`desligar` também valem para o
andar em que você está — dá para acender e apagar o forro de dentro dele.

## Config (`<mundo>/serverconfig/nexuscore-server.toml`)

```toml
[backrooms]
    defaultSize = 400          # lado do andar, em blocos (100 a 700)
    defaultDoors = 3
    defaultCeiling = true
    lampSpacing = 3            # vão de forro entre um painel de luz e o seguinte
    lampChance = 75            # % dos lugares de painel que ganham luz
    cellSize = 3               # largura do corredor
    chunksPerTick = 8          # velocidade da obra
    eventOddsPerMinute = 20160 # 1 em N por minuto, por pessoa (0 desliga o evento)
    eventCooldownMinutes = 1440
    skinwalker = true               # ligar o monstro
    skinwalkerDelaySeconds = 45     # solidão antes de ele aparecer
    skinwalkerStalkSeconds = 75     # quanto tempo ele finge ser gente
    skinwalkerRevealSeconds = 6     # duração da revelação
```

`eventOddsPerMinute = 20160` dá mais ou menos **uma queda a cada duas semanas de jogo sem parar**,
por pessoa. O sorteio só acontece no mundo principal, em sobrevivência, e só se já existir um andar
gerado. A espera de `eventCooldownMinutes` só vale para quem já caiu pelo evento; quem nunca caiu
entra no sorteio direto. O comando continua funcionando com o evento desligado.

## Texturas

Ficam em `assets/nexuscore/textures/block/backrooms/`: `wallpaper`, `baseboard`, `carpet`,
`ceiling`, `lamp`. As que estão lá são provisórias, geradas por
`tools/textures/generate-backrooms-textures.cjs` — é só trocar os PNG pelos definitivos, com os
mesmos nomes.

## Limites conhecidos

- Um andar por vez em construção: o comando recusa enquanto outro está sendo levantado.
- O evento manda sempre para o andar **mais novo**.
- O skinwalker não tem animação própria: a transformação é textura, luz, som e tremor. Foi escolha —
  animação de verdade exigiria GeckoLib e um rig.
- Ele persegue **uma pessoa** por vez. Duas pessoas no mesmo andar ganham cada uma a sua aparição.
