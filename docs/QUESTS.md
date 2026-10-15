# Mural de quests

Um quadro onde qualquer jogador publica um pedido e outro assume. A recompensa é **texto livre**: o
combinado é feito entre as pessoas, que é o que faz sentido num servidor de comunidade.

---

## Decisões

| Tema | Decisão |
|---|---|
| Conclusão | o **autor** marca a quest como concluída e escolhe quem cumpriu. A quest sai do mural e fica no histórico dos dois |
| Limite | **3 quests abertas** por jogador (configurável) |
| Escopo | **uma lista só** no servidor inteiro: todo quest board mostra as mesmas quests |
| Onde se assume | **só no quest board**. Desistir funciona em qualquer lugar, pelo livro do inventário |
| Vagas | de 1 a 10 por quest; quando lota, a quest **some do mural** de quem não assumiu |
| Apagar | o autor apaga a sua; operador apaga qualquer uma (`/nexus quest delete <id>`) |

## Como funciona

**O bloco** `nexuscore:quest_board` (aba NexusCore do criativo, sem receita). Clique com o botão
direito para abrir o mural.

**Tela do mural:** lista das quests que dá para assumir, detalhes ao lado (autor, vagas, recompensa,
descrição) e três botões: **Assumir**, **Criar quest** e **Minhas quests**.

**Criar quest:** título, descrição, recompensa e o número de vagas. O servidor corta texto grande,
confere o limite de quests abertas e exige estar perto do mural.

**Minhas quests:** duas abas.
- *Criadas* — **Concluir** (escolhe quem cumpriu numa lista de quem assumiu) e **Excluir**.
- *Assumidas* — **Desistir**, que libera a vaga para outra pessoa.

**No inventário:** um botão ao lado do livro de receitas abre o **diário de quests** na lateral
esquerda. Ele tem abas, do mesmo feitio das do livro de receitas, e cada quest tem a sua ficha.
Assumir quest continua sendo só no mural — isso é sobre estar ali, olhando o quadro.

**Um painel por vez.** Aquele espaço à esquerda do inventário é disputado: o livro de receitas do
jogo, o painel de atributos do Apothic Attributes (o botão de espadinha) e o nosso. Abrir um fecha
os outros — tanto clicando no nosso botão quanto no deles.

---

## O diário no inventário

| Aba | Ícone | O que tem |
|---|---|---|
| **Comigo** | livro escrito | o que você assumiu e as missões que o servidor te deu |
| **Meus pedidos** | livro e pena | o que você publicou, e onde você fecha o que já foi cumprido |
| **Todas as quests** | luneta | *(operador)* tudo que existe, com busca |
| **Nova missão** | pena | *(operador)* entregar uma missão a alguém, sem passar pela barra de comando |

As duas últimas **só aparecem para quem é operador** — e não é a tela que decide isso: o servidor é
quem diz, no mesmo pacote que manda a lista. Quem não tem permissão não recebe o aviso e não vê as
abas; e mesmo que visse, toda ação é conferida de novo do outro lado.

### A ficha

Clicar numa quest abre ela **ali mesmo**, no lugar da lista. Título, quem pediu, o pedido e a
recompensa viram blocos separados, com espaço entre eles — antes era tudo uma linha só que só se
revelava com o mouse parado em cima.

Embaixo ficam os botões do que dá para fazer com aquela quest, e só eles:

- **Comigo** — *Marcar como feita*, *Desmarcar* (dá para voltar atrás) e *Desistir*.
- **Meus pedidos** e **Todas** — um botão **por pessoa que levantou a mão**: fechar a quest é clicar
  no nome de quem cumpriu. Mais *Excluir*.

### Quem cumpriu levanta a mão

Essa é a mudança que faz o resto funcionar. Antes, o autor tinha de **adivinhar** quem havia
cumprido e escolher da lista de quem assumiu. Agora quem fez diz que fez, e o autor só confirma:

```
a pessoa cumpre  →  Marcar como feita  →  o autor (ou um operador) clica no nome dela
```

A quest fica marcada em vermelho na lista enquanto espera conferência, dos dois lados. Depois de
fechada ela ainda aparece uns **20 segundos** e some sozinha do diário — some da lista, não da
história: ela continua no histórico e na aba de administração.

### Nada de digitar UUID

Fechar quest pendente pela barra de comando era ruim de propósito: ninguém digita um UUID à mão. O
caminho normal agora é a aba **Todas as quests** — busca por título, autor ou quem assumiu, e um
clique no nome fecha. O comando continua existindo para quem preferir, e o `Tab` já oferece os ids
que estão esperando, com o título e o nome de quem entregou ao lado.

---

## Missões do servidor

Um segundo tipo de quest, que **não passa pelo mural**: ela cai direto no livro de alguém, por
comando. Serve para o que o servidor pede a uma pessoa em particular — a prova para voltar a ser
humano depois de virar vampiro, por exemplo.

Pela tela: aba **Nova missão**, que só operador enxerga. Pelo comando:

```
/nexus quests dar <jogadores> <título> | <descrição> | <recompensa>
```

Título, descrição e recompensa no mesmo campo, separados por barra; os dois últimos são opcionais.
Uma barra só por campo, o que deixa escrever a frase inteira de uma vez em vez de encher a tela de
argumentos:

```
/nexus quests dar Fulano Cura do vampirismo | Traga 10 lingotes de ouro ao altar | Volta a ser humano
```

Cada pessoa recebe **a sua** missão, não uma compartilhada: cada um cumpre no seu tempo e é
conferido separadamente. Elas não contam no limite de quests abertas de ninguém — o autor é o
servidor.

**Não aparece quem mandou.** A missão não tem autor: só aparecem as informações. No lugar de
"pedido por Fulano", a linha diz em que pé ela está.

### Como se fecha

O pedido costuma ser uma coisa do mundo ("traga 10 lingotes", "construa uma ponte") — nada que um
código saiba medir. Então o fluxo é honesto sobre isso:

1. A pessoa cumpre e clica em **Marcar como feita**, na ficha da quest dentro do diário.
2. O operador abre a aba **Todas as quests** e vê a missão marcada como pendente — ou, se preferir a
   barra de comando:
   ```
   /nexus quests pendentes
   ```
3. Confere no jogo e fecha clicando no nome de quem cumpriu (ou `/nexus quests concluir <id>`, com o
   `Tab` oferecendo os ids pendentes).

Para olhar tudo que uma pessoa recebeu, feito ou não:

```
/nexus quests dadas <jogador>
```

Três estados, e só: **em andamento** → **aguardando conferência** → **conferida**.

### O selo no botão

Quando alguma coisa entra no livro e ainda não foi lida — uma missão que o operador mandou, ou uma
quest que a pessoa acabou de assumir no mural — o canto de cima do botão ganha um **selo de cera**:
cinco pixels de lacre vermelho, com contorno escuro e um brilho no alto, desenhado pixel a pixel
como o resto da interface. É um selo, e não um alerta, porque o que ele marca é um recado fechado —
some no instante em que o livro é aberto.

O "já li" fica no **servidor**, junto com a quest, e não no cliente: sair e voltar não faz tudo
parecer novo de novo, e o selo é o mesmo em qualquer máquina.

E ele apaga **na hora**, mesmo com o diário já aberto: antes o "já li" só era mandado no instante em
que o diário abria, então quem recebesse uma missão com ele aberto ficava com o selo aceso até
fechar e abrir de novo.

---

## O bloco

O mural tem **três blocos de largura por dois de altura** — é o modelo do Blockbench, de 42×29
pixels. Como forma de colisão só vale dentro do próprio bloco, ele ocupa seis: o que você coloca
(embaixo, no meio) desenha o modelo inteiro, e os outros cinco recebem pedaços invisíveis
(`quest_board_part`) com a fatia da estrutura que cai em cada um — os postes, a placa e a tampa de
cima, recortados e girados conforme a direção.

Colocar exige espaço para os seis; sem ele o bloco não é colocado e o item não é gasto. Clicar em
qualquer parte abre a tela, e quebrar qualquer parte derruba o mural inteiro (o item sai do bloco
principal, uma vez só).

## Por dentro

- **Dados:** `QuestData` (`SavedData` do mundo, arquivo `nexuscore_quests.dat`). Uma lista só, salva
  junto com o mundo.
- **Regras no servidor:** a tela só pede; quem valida limite, vagas, autoria e distância do mural é o
  servidor. Assumir e criar exigem um quest board por perto (`boardRange`), com a dimensão e o
  bloco conferidos — não adianta forjar o pacote de longe. Desistir, dizer que fez, conferir e apagar
  o que é seu valem de qualquer lugar, pelo livro do inventário.
- **Sincronização:** cada jogador recebe **a sua** lista (quest cheia nem chega para quem não assumiu),
  com as mudanças agrupadas em meio segundo. As telas abertas se refazem sozinhas quando a lista muda.
- **Visual:** o mesmo painel das telas do jogo (contorno preto, chanfro claro em cima e escuro embaixo),
  com os botões, campos e listas do próprio Minecraft. Texturas geradas por
  `tools/textures/generate-quest-textures.cjs`.

## Config do servidor

```toml
[quest]
    maxOpenPerPlayer = 3
    maxSlots = 10
    boardRange = 6
    historyLimit = 50
```

## Comandos (operador)

| Comando | Função |
|---|---|
| `/nexus quest list` | lista todas as quests com o id |
| `/nexus quest delete <id>` | apaga qualquer quest |

## Limites conhecidos

- Quem assume não recebe aviso quando a quest é apagada ou concluída: ela só some (ou muda de estado) na lista.
- Sem prazo de validade: quest aberta fica até alguém apagar ou concluir.
- A recompensa não é entregue pelo jogo — é combinada entre as pessoas, como você pediu.
