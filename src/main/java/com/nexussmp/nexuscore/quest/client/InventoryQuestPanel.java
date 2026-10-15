package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.mixin.quest.AbstractContainerScreenAccessor;
import com.nexussmp.nexuscore.quest.client.screen.QuestTheme;
import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * O diario de quests no inventario.
 *
 * <p>Um botao ao lado do livro de receitas abre um painel na lateral esquerda. Ele nasceu como uma
 * lista so, de consulta, onde tudo que uma quest tinha a dizer aparecia empilhado numa linha e
 * apenas quando o mouse parava em cima. Agora e um caderno com <b>abas</b>, do mesmo feitio das do
 * livro de receitas, e cada quest tem a sua <b>ficha</b>: clicar abre a quest ali mesmo, com titulo,
 * pedido e recompensa separados, e os botoes do que da para fazer com ela.
 *
 * <ul>
 *   <li><b>Comigo</b> — o que voce assumiu e as missoes que o servidor te deu. E aqui que se diz
 *       "fiz" — num botao, e com um botao ao lado para voltar atras.</li>
 *   <li><b>Meus pedidos</b> — o que voce publicou. Quem cumpriu levanta a mao, e voce fecha a quest
 *       clicando no nome da pessoa. Antes era o autor que tinha de adivinhar quem havia cumprido.</li>
 *   <li><b>Tudo</b> e <b>Nova missao</b> — so para operador, e so aparecem para quem e.</li>
 * </ul>
 *
 * <p>Assumir quest continua sendo so no mural: isso e sobre estar ali, olhando o quadro.
 */
public final class InventoryQuestPanel {
    private static final int PANEL_WIDTH = 152;
    private static final int PANEL_GAP = 6;
    private static final int PADDING = 6;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 2;
    private static final int FIELD_HEIGHT = 14;
    /** Quanto o inventario anda para a direita com o painel aberto, para o conjunto ficar centralizado. */
    private static final int SHIFT = (PANEL_WIDTH + PANEL_GAP + QuestPanelTab.WIDTH) / 2;
    /** Altura de um rotulo mais o campo dele, na pagina de criar missao. */
    static final int FORM_STEP = 27;
    /** Tamanho maximo de um nick do jogo. */
    private static final int PLAYER_NAME_LENGTH = 16;
    /** Tamanho dos campos de texto livre (o servidor ainda corta cada um no tamanho dele). */
    private static final int TEXT_LENGTH = 256;
    /** Quantos nomes de quem entregou viram botao de confirmar; o resto fica para o comando. */
    private static final int MAX_CONFIRM_BUTTONS = 3;

    /** As paginas do caderno. As duas ultimas so existem para operador. */
    enum Page {
        MINE(false),
        AUTHORED(false),
        ALL(false),
        NEW(true);

        private final boolean form;

        Page(boolean form) {
            this.form = form;
        }

        boolean isForm() {
            return form;
        }

        boolean isAdmin() {
            return this == ALL || this == NEW;
        }
    }

    private static boolean open;
    private static Page page = Page.MINE;
    @Nullable
    private static UUID selected;
    private static int scroll;
    private static int seenVersion = -1;

    // O que foi digitado na pagina de criar missao, guardado entre um desenho e outro da tela.
    private static String formPlayer = "";
    private static String formTitle = "";
    private static String formDescription = "";
    private static String formReward = "";
    private static String search = "";

    /** Os campos desta tela, para saber se alguem esta digitando (e a tecla E nao fechar a tela). */
    private static final List<EditBox> fields = new ArrayList<>();
    @Nullable
    private static QuestPanelBody body;

    // ---- a tela ----

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) {
            return;
        }
        body = null;
        fields.clear();
        if (open) {
            shift(inventory, event.getListenersList());
        }
        event.addListener(new QuestBookButton(inventory, button -> toggle(inventory)));
        if (!open) {
            return;
        }
        if (page.isAdmin() && !QuestClientCache.admin()) {
            // Perdeu a permissao com o caderno aberto: volta para uma pagina que existe para todos.
            page = Page.MINE;
            selected = null;
        }
        build(inventory, event::addListener);
    }

    /**
     * Empurra o inventario (e os botoes que estao nele) para a direita, abrindo espaco a esquerda.
     * E o mesmo efeito do livro de receitas, so que aplicado depois do {@code init} da tela.
     */
    private static void shift(InventoryScreen inventory, List<GuiEventListener> widgets) {
        ((AbstractContainerScreenAccessor) inventory).nexus$setLeftPos(inventory.getGuiLeft() + SHIFT);
        for (GuiEventListener listener : widgets) {
            if (listener instanceof AbstractWidget widget) {
                widget.setX(widget.getX() + SHIFT);
            }
        }
    }

    /** Monta o caderno: abas, corpo e os botoes que aquela pagina pede. */
    private static void build(InventoryScreen inventory, java.util.function.Consumer<GuiEventListener> add) {
        Font font = Minecraft.getInstance().font;
        int panelX = panelX(inventory);
        int panelY = inventory.getGuiTop();
        int panelHeight = inventory.getYSize();

        addTabs(panelX, panelY, add);

        List<Button> buttons = new ArrayList<>();
        collectButtons(panelX, buttons);

        // Os botoes ocupam o rodape; a lista fica com o que sobra. Assim uma ficha com tres botoes
        // nao empurra texto para fora do papel.
        int bottom = panelY + panelHeight - PADDING;
        int used = buttons.isEmpty() ? 0 : buttons.size() * (BUTTON_HEIGHT + BUTTON_GAP);
        for (int i = 0; i < buttons.size(); i++) {
            Button button = buttons.get(i);
            button.setY(bottom - (i + 1) * BUTTON_HEIGHT - i * BUTTON_GAP);
        }

        int top = panelY + 20;
        int listTop = top;
        int listHeight = bottom - used - 2 - listTop;
        boolean searching = page == Page.ALL && selected == null;
        if (searching) {
            // A busca fica no alto da lista, como qualquer campo de pesquisa do jogo, e a lista
            // comeca abaixo dela.
            listTop += FIELD_HEIGHT + 4;
            listHeight -= FIELD_HEIGHT + 4;
        }

        // A ordem daqui para baixo e a ordem em que o jogo desenha: o papel primeiro, e o que se
        // escreve nele depois. O corpo entrando por ultimo cobriria o campo de busca.
        body = new QuestPanelBody(panelX, panelY, PANEL_WIDTH, panelHeight, listTop, Math.max(20, listHeight));
        add.accept(body);

        if (searching) {
            add.accept(field(font, panelX + PADDING, top, PANEL_WIDTH - 2 * PADDING,
                    TEXT_LENGTH, "nexuscore.quest.panel.search", search, value -> search = value));
        }
        if (page.isForm()) {
            addForm(font, panelX, listTop, add);
        }
        buttons.forEach(add::accept);
    }

    private static void addTabs(int panelX, int panelY, java.util.function.Consumer<GuiEventListener> add) {
        int x = panelX - QuestPanelTab.WIDTH + 4;
        int y = panelY + 4;
        for (Page candidate : Page.values()) {
            if (candidate.isAdmin() && !QuestClientCache.admin()) {
                continue;
            }
            Page target = candidate;
            add.accept(new QuestPanelTab(x, y, tabIcon(candidate), tabName(candidate), candidate == page,
                    button -> switchPage(target)));
            y += QuestPanelTab.HEIGHT + 1;
        }
    }

    /** Os botoes daquela pagina, do mais abaixo para o mais acima. */
    private static void collectButtons(int panelX, List<Button> buttons) {
        int x = panelX + PADDING;
        int inner = PANEL_WIDTH - 2 * PADDING;
        UUID self = QuestClientCache.self();

        if (page.isForm()) {
            buttons.add(button(x, inner, "nexuscore.quest.panel.send", InventoryQuestPanel::sendForm));
            return;
        }

        QuestView quest = selectedQuest();
        if (quest == null) {
            return;
        }
        buttons.add(button(x, inner, "nexuscore.quest.panel.back", () -> openDetail(null)));

        switch (page) {
            case MINE -> {
                if (!quest.fromServer() && quest.open()) {
                    buttons.add(button(x, inner, "nexuscore.quest.button.abandon", () -> {
                        QuestActions.abandon(quest.id());
                        openDetail(null);
                    }));
                }
                if (quest.canSubmit(self)) {
                    buttons.add(button(x, inner, "nexuscore.quest.button.submit",
                            () -> QuestActions.submit(quest.id())));
                } else if (quest.open() && quest.submitted(self)) {
                    buttons.add(button(x, inner, "nexuscore.quest.button.unsubmit",
                            () -> QuestActions.unsubmit(quest.id())));
                }
            }
            case AUTHORED, ALL -> {
                buttons.add(button(x, inner, "nexuscore.quest.button.delete", () -> {
                    QuestActions.delete(quest.id());
                    openDetail(null);
                }));
                if (quest.open()) {
                    // Um botao por pessoa que levantou a mao: fechar a quest e clicar no nome dela.
                    List<QuestView.Participant> hands = quest.submissions();
                    for (int i = 0; i < Math.min(hands.size(), MAX_CONFIRM_BUTTONS); i++) {
                        QuestView.Participant who = hands.get(i);
                        buttons.add(new QuestButton(x, 0, inner, BUTTON_HEIGHT,
                                Component.translatable("nexuscore.quest.button.confirm", who.name()),
                                ignored -> {
                                    QuestActions.complete(quest.id(), who.id());
                                    openDetail(null);
                                }));
                    }
                }
            }
            default -> {
            }
        }
    }

    private static void addForm(Font font, int panelX, int top, java.util.function.Consumer<GuiEventListener> add) {
        int x = panelX + PADDING;
        int inner = PANEL_WIDTH - 2 * PADDING;
        int y = top + 10;
        // Nick do jogo tem no maximo 16 letras. O campo nao aceita mais que isso: um nome maior nem
        // cabe no pacote, e o jogo derrubava da conexao quem tentasse mandar.
        add.accept(field(font, x, y, inner, PLAYER_NAME_LENGTH, "nexuscore.quest.panel.form_player", formPlayer,
                v -> formPlayer = v));
        y += FORM_STEP;
        add.accept(field(font, x, y, inner, TEXT_LENGTH, "nexuscore.quest.panel.form_title", formTitle,
                v -> formTitle = v));
        y += FORM_STEP;
        add.accept(field(font, x, y, inner, TEXT_LENGTH, "nexuscore.quest.panel.form_description", formDescription,
                v -> formDescription = v));
        y += FORM_STEP;
        add.accept(field(font, x, y, inner, TEXT_LENGTH, "nexuscore.quest.panel.form_reward", formReward,
                v -> formReward = v));
    }

    /**
     * Um campo de texto, do jeito que o jogo faz.
     *
     * <p>Cheguei a vesti-lo de papel — moldura desligada e letra em tinta sobre o couro. Ficou bonito
     * e <b>ilegivel</b>: o campo do jogo escreve sempre com sombra, e sombra escura embaixo de letra
     * escura em fundo claro borra tudo. Nao ha como desligar essa sombra sem reescrever o campo
     * inteiro, e um campo de texto que nao se le nao vale a economia. Entao ele fica como veio.
     */
    private static EditBox field(Font font, int x, int y, int width, int maxLength, String hint, String value,
                                 java.util.function.Consumer<String> onChange) {
        EditBox box = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.translatable(hint));
        box.setMaxLength(maxLength);
        box.setHint(Component.translatable(hint));
        box.setValue(value);
        box.setResponder(onChange);
        fields.add(box);
        return box;
    }

    private static Button button(int x, int width, String key, Runnable action) {
        return new QuestButton(x, 0, width, BUTTON_HEIGHT, Component.translatable(key), ignored -> action.run());
    }

    // ---- estado ----

    private static void toggle(InventoryScreen inventory) {
        open = !open;
        scroll = 0;
        selected = null;
        if (open) {
            // Abriu o caderno: o que estava marcado como novidade deixa de ser.
            markSeen();
            // So cabe um painel de cada vez naquele espaco: abrindo o nosso, os outros fecham.
            if (inventory.getRecipeBookComponent().isVisible()) {
                inventory.getRecipeBookComponent().toggleVisibility();
            }
            AttributesPanelCompat.close(inventory);
        }
        rebuild(inventory);
    }

    private static void switchPage(Page target) {
        page = target;
        selected = null;
        scroll = 0;
        rebuild(null);
    }

    static void openDetail(@Nullable UUID quest) {
        selected = quest;
        rebuild(null);
    }

    /**
     * Rolar nao remonta a tela: so muda o numero.
     *
     * <p>O corpo le esta posicao a cada quadro, entao mexer nela ja basta. Remontar a cada dente da
     * rodinha custaria um {@code init} inteiro por clique e ainda tiraria o cursor de dentro do campo
     * de busca no meio da digitacao.
     */
    static void scrollBy(int delta) {
        int last = Math.max(0, currentList().size() - 1);
        scroll = Math.clamp(scroll + delta, 0, last);
    }

    /** Refaz a tela: e o {@code init} que decide onde o inventario fica e quais botoes existem. */
    private static void rebuild(@Nullable InventoryScreen inventory) {
        Minecraft minecraft = Minecraft.getInstance();
        InventoryScreen screen = inventory != null ? inventory
                : minecraft.screen instanceof InventoryScreen open ? open : null;
        if (screen != null) {
            screen.resize(minecraft, screen.width, screen.height);
        }
    }

    private static void sendForm() {
        if (formPlayer.isBlank() || formTitle.isBlank()) {
            return;
        }
        QuestActions.give(formPlayer, formTitle, formDescription, formReward);
        formTitle = "";
        formDescription = "";
        formReward = "";
        rebuild(null);
    }

    private static void markSeen() {
        seenVersion = QuestClientCache.version();
        if (QuestClientCache.hasUnread()) {
            QuestActions.seen();
        }
    }

    // ---- o que o corpo do painel pergunta ----

    @Nullable
    static UUID selected() {
        return selected;
    }

    @Nullable
    static QuestView selectedQuest() {
        return page.isForm() ? null : QuestClientCache.byId(selected);
    }

    static Page page() {
        return page;
    }

    static int scroll() {
        return scroll;
    }

    static Component pageTitle() {
        QuestView quest = selectedQuest();
        return quest != null ? Component.translatable("nexuscore.quest.panel.detail") : tabName(page);
    }

    static List<QuestView> currentList() {
        return switch (page) {
            case MINE -> QuestClientCache.acceptedByMe();
            case AUTHORED -> QuestClientCache.createdByMe();
            case ALL -> filtered(QuestClientCache.all());
            case NEW -> List.of();
        };
    }

    /** A busca da aba de administracao: titulo, autor ou quem assumiu. */
    private static List<QuestView> filtered(List<QuestView> quests) {
        String needle = search.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return quests;
        }
        List<QuestView> matches = new ArrayList<>();
        for (QuestView quest : quests) {
            if (quest.title().toLowerCase(Locale.ROOT).contains(needle)
                    || quest.authorName().toLowerCase(Locale.ROOT).contains(needle)
                    || quest.accepted().stream()
                            .anyMatch(p -> p.name().toLowerCase(Locale.ROOT).contains(needle))) {
                matches.add(quest);
            }
        }
        return matches;
    }

    static Component emptyMessage() {
        return Component.translatable(switch (page) {
            case MINE -> "nexuscore.quest.book.empty";
            case AUTHORED -> "nexuscore.quest.panel.no_authored";
            case ALL -> "nexuscore.quest.panel.no_results";
            case NEW -> "nexuscore.quest.panel.no_results";
        });
    }

    /** A segunda linha da fileira: o que importa saber daquela quest sem abri-la. */
    static Component rowLine(QuestView quest) {
        if (!quest.open()) {
            return Component.translatable("nexuscore.quest.entry.completed", quest.completedByName());
        }
        if (quest.pending()) {
            return Component.translatable("nexuscore.quest.panel.hands", quest.submissions().size());
        }
        if (quest.fromServer()) {
            return Component.translatable("nexuscore.quest.entry.server");
        }
        return Component.translatable("nexuscore.quest.entry.line", quest.authorName(), quest.reward());
    }

    static int rowColor(QuestView quest) {
        if (quest.pending()) {
            return QuestTheme.WARNING;
        }
        return quest.fromServer() ? QuestTheme.ACCENT : QuestTheme.MUTED;
    }

    static Component statusLine(QuestView quest) {
        if (!quest.open()) {
            return Component.translatable("nexuscore.quest.details.completed", quest.completedByName());
        }
        if (quest.pending()) {
            return Component.translatable("nexuscore.quest.panel.waiting_names",
                    String.join(", ", quest.submissions().stream().map(QuestView.Participant::name).toList()));
        }
        if (quest.submitted(QuestClientCache.self())) {
            return Component.translatable("nexuscore.quest.state.waiting");
        }
        return Component.translatable("nexuscore.quest.state.doing");
    }

    /** Alguem esta com o cursor dentro de um campo de texto? */
    static boolean typing() {
        for (EditBox box : fields) {
            if (box.isFocused()) {
                return true;
            }
        }
        return false;
    }

    static List<Component> formLabels() {
        return List.of(
                Component.translatable("nexuscore.quest.panel.form_player"),
                Component.translatable("nexuscore.quest.panel.form_title"),
                Component.translatable("nexuscore.quest.panel.form_description"),
                Component.translatable("nexuscore.quest.panel.form_reward"));
    }

    private static Component tabName(Page target) {
        return Component.translatable(switch (target) {
            case MINE -> "nexuscore.quest.panel.mine";
            case AUTHORED -> "nexuscore.quest.panel.authored";
            case ALL -> "nexuscore.quest.panel.all";
            case NEW -> "nexuscore.quest.panel.new";
        });
    }

    private static ItemStack tabIcon(Page target) {
        return new ItemStack(switch (target) {
            case MINE -> Items.APPLE;
            case AUTHORED -> Items.WRITABLE_BOOK;
            case ALL -> Items.SPYGLASS;
            case NEW -> Items.FEATHER;
        });
    }

    // ---- eventos soltos ----

    /**
     * A dica da fileira sob o mouse, desenhada por cima de tudo.
     *
     * <p>E a unica coisa que continua fora do widget: dica tem de ficar acima ate dos botoes, e o
     * "depois de tudo" da tela e exatamente aqui.
     */
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!open || body == null || selected != null || page.isForm()) {
            return;
        }
        QuestView hovered = body.questAt(event.getMouseX(), event.getMouseY());
        if (hovered == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(hovered.title()));
        tooltip.add(Component.translatable("nexuscore.quest.panel.open_hint"));
        graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip,
                event.getMouseX(), event.getMouseY());
    }

    /**
     * A tecla do inventario nao fecha a tela enquanto alguem esta escrevendo.
     *
     * <p>A tela de inventario do jogo trata a propria tecla de abrir como "fechar", e ela e a letra
     * <b>E</b>. Com um campo de texto em foco isso virava um beco: qualquer titulo com "e" fechava o
     * inventario no meio da digitacao. Entao, e so entao, a tecla e barrada aqui — a letra chega ao
     * campo pelo caminho do texto digitado, que e outro, e continua sendo escrita normalmente.
     */
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!open || !(event.getScreen() instanceof InventoryScreen) || !typing()) {
            return;
        }
        if (Minecraft.getInstance().options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    /**
     * Enquanto o caderno esta aberto, novidade nao fica pendurada.
     *
     * <p>Antes o "ja li" so era mandado no instante em que o caderno abria; quem recebesse uma missao
     * com ele ja aberto ficava com o selo aceso ate fechar e abrir de novo. Agora, cada lista nova que
     * chega e lida na hora — e o selo apaga sozinho, na frente da pessoa.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof InventoryScreen inventory)) {
            return;
        }
        if (!open) {
            return;
        }
        if (seenVersion != QuestClientCache.version()) {
            markSeen();
            rebuild(inventory);
        }
        // Um painel por vez: se o jogador abrir o de atributos ou o de receitas, o nosso sai de cena.
        if (AttributesPanelCompat.isOpen() || inventory.getRecipeBookComponent().isVisible()) {
            open = false;
            scroll = 0;
            selected = null;
            rebuild(inventory);
        }
    }

    private static int panelX(InventoryScreen inventory) {
        return Math.max(QuestPanelTab.WIDTH, inventory.getGuiLeft() - PANEL_WIDTH - PANEL_GAP);
    }

    public static void close() {
        open = false;
        scroll = 0;
        selected = null;
        page = Page.MINE;
        body = null;
        fields.clear();
    }

    private InventoryQuestPanel() {}
}
