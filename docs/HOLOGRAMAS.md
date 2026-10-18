# Hologramas com captura de movimento

Gravar um jogador de verdade e reproduzir o que ele fez, no mundo, com cara de projeção de ficção científica.

> Conferido e implementado em 15/09. O que este documento decide vale para qualquer gravação: `president_intro`, `captain_briefing`, `emergency_message`...

---

## 1. Os mods analisados

Todos os cinco são 1.21.1 NeoForge. O que cada um resolve e por que entrou ou não:

| Mod | Licença | O que faz | Decisão |
|---|---|---|---|
| **Motion Capture** (`mocap` 1.4-alpha-10) | LGPL-3.0-only | Grava jogadores como uma lista de ações por tick (`Movement`, `HeadRotation`, `ChangePose`, `ChangeItem`, `Swing`, `SetEntityFlags`...) e reproduz com jogadores falsos. Tem API pública (`MocapAPI.createController`) | **Referência, não dependência.** O modelo de dados dele confirmou o desenho que eu queria: capturar no servidor, um registro por tick, itens só quando mudam. Usar como dependência obrigaria todo mundo (servidor e clientes) a instalar um mod em alpha, e o "jogador falso" dele é desenhado como jogador normal — para deixar holográfico eu teria que entrar no renderer dele com mixin, e alpha muda direto |
| **Holographic Renders** | MIT | Projetor que mostra blocos, áreas e entidades escaneadas | **Não.** Não traz shader nenhum (usa os modos de desenho padrão com transparência), então não havia o que aproveitar além da ideia |
| **Hologram Projector** (`holoprojector`) | **All Rights Reserved** | Projetor de hologramas | **Não.** Licença fechada: não dá para copiar nem adaptar código |
| **HoloMap** | MIT | Mapa do mundo em holograma | **Não.** Depende de `sable` e `simulated`; arrastaria dois mods para dentro do SMP |
| **Imitation Core API** | MIT | Base para "imitação" de jogadores | **Não.** Depende de GeckoLib, ManasCore e **Tensura**: um mod de anime inteiro como dependência |

**Conclusão:** tudo dentro do NexusCore. O sistema fica reutilizável, sem dependência nova para os jogadores, e o visual é nosso (que era o ponto: mod nenhum dos cinco faz o efeito de projeção com shader).

---

## 2. Arquitetura

### 2.1 Captura: no servidor, um registro por tick

Quem grava é o servidor, porque é ele que sabe de verdade posição, rotação do corpo, rotação da cabeça, pose e itens — exatamente os dados que os outros jogadores já recebem. Gravar no cliente daria os mesmos números com um round-trip a mais e só funcionaria para quem tivesse o mod ligado.

Por tick são guardados:

| Dado | Como | Por quê |
|---|---|---|
| Posição | 3 × `int`, em milésimos de bloco, **relativa à origem da gravação** | relativo = a mesma gravação toca em qualquer lugar do mundo |
| Rotação do corpo, da cabeça e do olhar | 3 × `byte` | é a mesma precisão com que o jogo manda rotação pela rede: não se perde nada que alguém veria |
| Pose (em pé, agachado, nadando, planando, dormindo...) | 1 `byte` | cobre agachar, nadar, elytra, dormir |
| Correndo e início de golpe de braço | 1 `byte` de bandeiras | o golpe só vale no tick em que começa, senão o holograma repetiria o gesto sem parar |
| Itens e armadura | só quando mudam | trocar de item é raro; guardar 6 slots por tick seria desperdício |

São ~17 bytes por tick: **1 minuto de gravação ≈ 20 KB**.

A animação de andar, o balanço dos braços e a inclinação do corpo **não são gravados**: o jogo os calcula a partir do movimento, igualzinho faz com um jogador. Gravar seria redundante e ficaria pior.

### 2.2 Armazenamento

`config/nexuscore/holograms/<nome>.nbt`, NBT comprimido. Fica na config, e não dentro do mundo, para dar para gravar no mundo de teste e **só copiar o arquivo** para o servidor. Uma gravação carregada fica em memória até o servidor desligar; arquivo quebrado vira aviso no log, nunca crash.

### 2.3 Reprodução: uma entidade de verdade no mundo

O holograma é uma entidade (`nexuscore:hologram`), não uma GUI nem um armor stand com texto. O servidor lê a gravação e move a entidade tick a tick; o jogo sincroniza posição, rotação, pose e itens pelos pacotes de sempre.

Isso resolve três coisas de graça:
- **Multiplayer:** todos veem a mesma apresentação, porque a fonte é o servidor.
- **Quem chega no meio:** a entidade já está lá, no ponto certo. Não precisa de pacote nenhum de "sincronização".
- **Suavidade:** a interpolação de entidade do cliente é a mesma que suaviza os outros jogadores.

A entidade é sincronizada **a cada tick** (`updateInterval(1)`) em vez dos 3 ticks padrão, senão gestos curtos ficariam engolidos. Ela não colide, não empurra, não leva dano, não aparece na lista de alvos e **não é salva no mundo**: se o servidor cair no meio de uma apresentação, não sobra holograma perdido.

Alternativa descartada: mandar a gravação inteira para cada cliente e reproduzir localmente. Seria mais suave em ping alto, mas exigiria enviar arquivo, versionar cache no cliente e resolver sincronização de relógio — muito mais peça para um ganho que a interpolação normal já dá.

### 2.4 Modelo e skin

Modelo de jogador do próprio jogo (braço fino ou grosso, conforme a skin) com a **skin de quem gravou**: se a pessoa está online, o cliente já tem a skin; se não, vale o perfil guardado na gravação.

A gravação guarda o **perfil inteiro** do autor — nome, id e a textura — porque o cliente não sabe descobrir a skin de alguém só pelo id: ele apenas desembrulha a textura que vem junto do perfil. Sem isso, um holograma de quem está offline apareceria com a cara padrão, e num servidor offline ou com mod de skin (SkinRestorer) apareceria errado. Gravações feitas antes disso continuam funcionando; para elas, a skin certa só aparece com o autor online — regravar resolve.

### 2.5 O efeito de holograma

Um shader próprio (`assets/nexuscore/shaders/core/hologram.*`), aplicado por um modo de desenho próprio:

- **cor única**: a skin vira luminância e recebe a cor do holograma (ciano por padrão);
- **transparência** e **sem corte de faces**: dá para ver o interior da projeção, como no filme;
- **linhas de varredura** subindo, presas à altura do mundo (a altura da câmera entra como uniforme, senão as linhas escorregariam quando o jogador pula);
- **oscilação** (o "piscar" da projeção) e **falhas horizontais** ocasionais;
- **silhueta acesa** (efeito de borda), que é o que dá o ar de projeção de luz;
- **brilho próprio**: o mapa de luz é ignorado, então a projeção aparece igual de dia, de noite ou numa caverna;
- **partículas** subindo em volta e faíscas ocasionais.

Tudo ajustável em `nexuscore-client.toml` (cor, transparência, densidade das linhas, oscilação, brilho da silhueta, partículas), sem recompilar.

O item na mão passa pelo mesmo shader, então um mapa ou um papel na mão do orador também fica holográfico.

### 2.6 Áudio

O id do som vai no pacote e o cliente toca direto: **basta pôr o arquivo no resource pack**, sem registrar som no mod nem recompilar para trocar a fala. O som é preso à entidade, então vem da direção certa, tem volume por distância e **para sozinho** quando a apresentação acaba.

A fala usa um tipo de som próprio (`HologramSpeechSound`) e não o do jogo: o holograma é uma entidade *silenciosa* de propósito — é assim que ele não solta grunhido nem passo — e o `EntityBoundSoundInstance` do jogo recusa tocar em entidade silenciosa. A marca vale para os sons de criatura; o discurso é outra coisa.

O alcance vem de `audioRangeBlocks` (padrão 48). O jogo mede alcance de som em múltiplos de 16 blocos, então o valor vira o múltiplo mais próximo por baixo — 16, 32, 48…

Quem chega no meio: vê o holograma no ponto certo, mas o áudio só começa para quem entrou no alcance nos primeiros segundos (`audioJoinWindowSeconds`, padrão 3). Isso é limitação do jogo: não existe API para começar um `.ogg` no meio. Se um dia isso incomodar, o jeito é cortar o discurso em pedaços de ~5 s e tocar do pedaço certo.

### 2.7 Desempenho

Uma entidade, um modelo, um shader barato. A gravação inteira fica em memória (20 KB por minuto). As partículas saem a cada 2 ticks. Custo por frame parecido com o de renderizar mais um jogador.

---

## 3. Como usar

### Gravar
```
/nexus holograma gravar president_intro
... atue: ande, olhe, gesticule, agache, segure o que quiser ...
/nexus holograma gravar parar
```
`/nexus holograma gravar cancelar` joga fora. Sair do servidor no meio salva o que deu tempo. No limite de tempo (`maxRecordingSeconds`, padrão 10 min) ela se salva sozinha.

### Áudio
1. Coloque o `.ogg` no pack **Nexus Resources** (embutido no mod, em `src/main/resources/resourcepacks/nexus_resources/`), em `assets/nexuscore/sounds/speech/president_intro.ogg`.
   O jogo só lê **OGG Vorbis** — não lê mp3, wav nem OGG **Opus** (áudio de WhatsApp é Opus, e o arquivo
   também termina em `.ogg`, então dá para se enganar fácil). Arraste o arquivo para cima de
   `tools/audio/converter-ogg.cmd` que ele sai Vorbis mono, pronto. Mono importa: é o que faz o som
   vir da direção do holograma.
2. Declare em `assets/nexuscore/sounds.json`:
   ```json
   {
     "speech.president_intro": {
       "category": "voice",
       "sounds": [{ "name": "nexuscore:speech/president_intro", "stream": true }]
     }
   }
   ```
   `"stream": true` é importante para falas longas.
3. Amarre à gravação: `/nexus holograma audio president_intro nexuscore:speech.president_intro`

### Reproduzir
```
/nexus holograma tocar president_intro
/nexus holograma tocar president_intro 2.5                          (tamanho)
/nexus holograma tocar president_intro 2.5 90                       (tamanho e rotacao)
/nexus holograma tocar president_intro 2.5 90 10 64 -30             (e a posicao)
/nexus holograma tocar president_intro 2.5 90 10 64 -30 true        (e repetindo)
/nexus holograma parar          (ou parar <nome>, ou parar todos)

Os valores vêm um depois do outro, na ordem **tamanho, rotação, posição, repetir**, e todos são
opcionais. Enquanto se digita, o jogo mostra `<tamanho>`, `<rotacao>` e companhia acima do campo.
/nexus holograma lista | info <nome> | apagar <nome>
```

**Tamanho:** sim, dá. Vale de 0,1x a 10x — de um boneco de mesa a um "presidente" de 18 blocos de altura. Por dentro é o atributo `generic.scale` do próprio jogo, o mesmo dos mobs gigantes: modelo, hitbox e altura dos olhos crescem juntos, e o jogo sincroniza sozinho.

**Onde ele nasce:** o ponto que você passa (ou onde você está) é onde ficam os **pés**; o holograma cresce para cima. Num 10x, a cabeça fica 18 blocos acima do chão — de perto parece que ele está no céu, mas os pés continuam no lugar. A resposta do comando mostra a coordenada e o tamanho para conferir.

---

## 4. Limites conhecidos

| Limite | Detalhe |
|---|---|
| Áudio do meio | quem chega depois da janela configurada não ouve (o jogo não sabe começar um som pelo meio) |
| Armadura | é gravada, mas ainda não é desenhada: a projeção mostra corpo e item na mão |
| Itens especiais | escudo, baú, cabeça e outros que têm renderização própria podem sair com textura errada; desligue com `renderItems = false` se atrapalhar |
| Shader pack (Iris) | com shader ligado, quem manda no desenho das entidades é o pack: o efeito pode ficar diferente ou sumir |
| Usar item | comer, beber e bloquear com escudo não são gravados (posição, olhar, pose, gestos e itens são) |
| Montarias | andar de barco/cavalo não é gravado como montaria; o corpo é gravado do jeito que ficou |
