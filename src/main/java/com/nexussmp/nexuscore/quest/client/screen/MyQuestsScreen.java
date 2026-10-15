package com.nexussmp.nexuscore.quest.client.screen;

import com.nexussmp.nexuscore.quest.client.QuestActions;
import com.nexussmp.nexuscore.quest.client.QuestClientCache;
import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.gui.GuiGraphics;
import com.nexussmp.nexuscore.quest.client.QuestButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * As quests do jogador: as que ele criou (com concluir e excluir) e as que ele assumiu (com desistir).
 *
 * <p>Concluir e do autor: ele escolhe quem cumpriu, a quest sai do mural e fica no historico dos dois.
 */
public final class MyQuestsScreen extends QuestScreen {
    private static final int LIST_WIDTH = 190;

    private int version = -1;
    private boolean showingCreated = true;
    private QuestList list;
    private Button createdTab;
    private Button acceptedTab;
    private Button completeButton;
    private Button deleteButton;
    private Button abandonButton;

    public MyQuestsScreen() {
        super(Component.translatable("nexuscore.quest.mine.title"), 320, 208);
    }

    @Override
    protected void init() {
        super.init();

        createdTab = addRenderableWidget(new QuestButton(left + 10, top + 24, 92, 18,
                        Component.translatable("nexuscore.quest.tab.created"), button -> switchTab(true)));
        acceptedTab = addRenderableWidget(new QuestButton(left + 106, top + 24, 92, 18,
                        Component.translatable("nexuscore.quest.tab.accepted"), button -> switchTab(false)));

        list = new QuestList(minecraft, left + 10, top + 46, LIST_WIDTH, 122);
        addRenderableWidget(list);

        completeButton = addRenderableWidget(new QuestButton(left + 10, top + panelHeight - 28, 92, 20,
                        Component.translatable("nexuscore.quest.button.complete"), button -> complete()));
        deleteButton = addRenderableWidget(new QuestButton(left + 106, top + panelHeight - 28, 92, 20,
                        Component.translatable("nexuscore.quest.button.delete"), button -> delete()));
        abandonButton = addRenderableWidget(new QuestButton(left + 10, top + panelHeight - 28, 188, 20,
                        Component.translatable("nexuscore.quest.button.abandon"), button -> abandonOrSubmit()));
        addRenderableWidget(new QuestButton(left + 202, top + panelHeight - 28, 108, 20,
                        Component.translatable("gui.back"), button -> minecraft.setScreen(new QuestBoardScreen())));

        // Abriu a lista: o selo de novidade no botao do livro pode apagar.
        QuestActions.seen();
        refresh();
    }

    private void switchTab(boolean created) {
        showingCreated = created;
        refresh();
    }

    private void refresh() {
        version = QuestClientCache.version();
        list.setQuests(showingCreated ? QuestClientCache.createdByMe() : QuestClientCache.acceptedByMe());
        createdTab.active = !showingCreated;
        acceptedTab.active = showingCreated;
        completeButton.visible = showingCreated;
        deleteButton.visible = showingCreated;
        abandonButton.visible = !showingCreated;
        updateButtons();
    }

    private void complete() {
        QuestView selected = list.selected();
        if (selected != null && selected.open() && !selected.accepted().isEmpty()) {
            minecraft.setScreen(new CompleteQuestScreen(selected));
        }
    }

    private void delete() {
        QuestView selected = list.selected();
        if (selected != null) {
            QuestActions.delete(selected.id());
        }
    }

    /**
     * O mesmo botao, duas coisas.
     *
     * <p>Numa quest de mural, desistir libera a vaga. Numa missao do servidor nao ha vaga nem de
     * quem desistir: o que se faz ali e dizer que o pedido foi cumprido, e quem confere e o
     * operador — o pedido costuma ser uma coisa do mundo, que nenhum codigo saberia medir.
     */
    private void abandonOrSubmit() {
        QuestView selected = list.selected();
        if (selected == null) {
            return;
        }
        if (selected.fromServer()) {
            QuestActions.submit(selected.id());
        } else {
            QuestActions.abandon(selected.id());
        }
    }

    @Override
    public void tick() {
        if (version != QuestClientCache.version()) {
            refresh();
        }
        updateButtons();
    }

    /**
     * Botao que depende de selecao nasce desligado.
     *
     * <p>O tick so roda 20 vezes por segundo e a tela desenha bem mais que isso: deixar essa conta so
     * para o tick fazia o botao aparecer acesso no primeiro quadro e apagar logo em seguida — a
     * piscada ao abrir a tela.
     */
    private void updateButtons() {
        QuestView selected = list.selected();
        completeButton.active = selected != null && selected.open() && !selected.accepted().isEmpty();
        deleteButton.active = selected != null;

        boolean server = selected != null && selected.fromServer();
        abandonButton.setMessage(Component.translatable(server
                ? "nexuscore.quest.button.submit"
                : "nexuscore.quest.button.abandon"));
        abandonButton.active = selected != null && selected.open()
                && (!server || selected.canSubmit(QuestClientCache.self()));
        // O caminho bom para tudo isso agora e o caderno do inventario; esta tela continua sendo a
        // do mural, para quem esta de pe na frente do quadro.
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        QuestTheme.text(graphics, font, title, left + 10, top + 12, QuestTheme.TITLE);
        renderDetails(graphics, list.selected());
    }

    private void renderDetails(GuiGraphics graphics, @Nullable QuestView quest) {
        int x = left + 210;
        int y = top + 46;
        int width = 100;
        if (quest == null) {
            QuestTheme.wrapped(graphics, font, Component.translatable("nexuscore.quest.board.pick"),
                    x, y, width, 3, QuestTheme.MUTED);
            return;
        }
        y += QuestTheme.wrapped(graphics, font, Component.literal(quest.title()), x, y, width, 2, QuestTheme.TITLE);
        y += 2;
        if (!quest.open()) {
            QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.details.completed", quest.completedByName()),
                    x, y, QuestTheme.ACCENT);
            y += font.lineHeight + 2;
        } else if (quest.fromServer()) {
            // Sem "vagas" e sem lista de gente: a missao do servidor e entre o servidor e uma pessoa.
            QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.entry.server"),
                    x, y, QuestTheme.ACCENT);
            y += font.lineHeight + 2;
            QuestTheme.text(graphics, font, Component.translatable(quest.pending()
                            ? "nexuscore.quest.state.waiting"
                            : "nexuscore.quest.state.doing"),
                    x, y, QuestTheme.MUTED);
            y += font.lineHeight + 2;
        }
        if (!quest.fromServer()) {
            QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.details.slots",
                    quest.accepted().size(), quest.maxSlots()), x, y, QuestTheme.MUTED);
            y += font.lineHeight + 2;
            for (QuestView.Participant participant : quest.accepted()) {
                if (y > top + panelHeight - 44) {
                    break;
                }
                QuestTheme.text(graphics, font, Component.literal("- " + participant.name()), x, y, QuestTheme.TEXT);
                y += font.lineHeight;
            }
        }
        y += 3;
        if (!quest.description().isBlank()) {
            y += QuestTheme.wrapped(graphics, font, Component.literal(quest.description()), x, y, width, 4,
                    QuestTheme.TEXT);
            y += 3;
        }
        if (!quest.reward().isBlank()) {
            QuestTheme.wrapped(graphics, font, Component.translatable("nexuscore.quest.details.reward", quest.reward()),
                    x, y, width, 3, QuestTheme.ACCENT);
        }
    }

}
