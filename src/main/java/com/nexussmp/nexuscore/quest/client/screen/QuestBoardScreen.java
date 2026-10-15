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
 * O mural: lista das quests que da para assumir, com os detalhes ao lado.
 *
 * <p>Quest cheia some daqui (o servidor nem manda), e as suas ficam em "Minhas quests".
 */
public final class QuestBoardScreen extends QuestScreen {
    private static final int LIST_WIDTH = 190;

    private int version = -1;
    private QuestList list;
    private Button acceptButton;

    public QuestBoardScreen() {
        super(Component.translatable("nexuscore.quest.board.title"), 320, 208);
    }

    @Override
    protected void init() {
        super.init();

        list = new QuestList(minecraft, left + 10, top + 26, LIST_WIDTH, 142);
        addRenderableWidget(list);

        acceptButton = addRenderableWidget(new QuestButton(left + 10, top + panelHeight - 28, 92, 20,
                        Component.translatable("nexuscore.quest.button.accept"), button -> accept()));
        addRenderableWidget(new QuestButton(left + 106, top + panelHeight - 28, 92, 20,
                        Component.translatable("nexuscore.quest.button.create"), button -> minecraft.setScreen(new QuestCreateScreen())));
        addRenderableWidget(new QuestButton(left + 202, top + panelHeight - 28, 108, 20,
                        Component.translatable("nexuscore.quest.button.mine"), button -> minecraft.setScreen(new MyQuestsScreen())));

        refresh();
    }

    private void refresh() {
        version = QuestClientCache.version();
        list.setQuests(QuestClientCache.board());
        updateButtons();
    }

    /**
     * Assumir depende de ter uma quest selecionada, entao nasce desligado.
     *
     * <p>O tick roda 20 vezes por segundo e a tela desenha bem mais: deixar isso so para o tick fazia
     * o botao aparecer aceso no primeiro quadro e apagar em seguida — a piscada ao abrir a tela.
     */
    private void updateButtons() {
        QuestView selected = list.selected();
        acceptButton.active = selected != null && selected.canAccept(QuestClientCache.self());
    }

    private void accept() {
        QuestView selected = list.selected();
        if (selected != null) {
            QuestActions.accept(selected.id());
        }
    }

    @Override
    public void tick() {
        // O servidor pode mandar a lista nova enquanto a tela esta aberta.
        if (version != QuestClientCache.version()) {
            refresh();
        }
        updateButtons();
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        QuestTheme.text(graphics, font, title, left + 10, top + 12, QuestTheme.TITLE);
        renderDetails(graphics, list.selected());
    }

    private void renderDetails(GuiGraphics graphics, @Nullable QuestView quest) {
        int x = left + 210;
        int y = top + 26;
        int width = 100;
        if (quest == null) {
            QuestTheme.wrapped(graphics, font, Component.translatable("nexuscore.quest.board.pick"),
                    x, y, width, 3, QuestTheme.MUTED);
            return;
        }
        y += QuestTheme.wrapped(graphics, font, Component.literal(quest.title()), x, y, width, 2, QuestTheme.TITLE);
        y += 2;
        QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.details.author", quest.authorName()),
                x, y, QuestTheme.MUTED);
        y += font.lineHeight + 1;
        QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.details.slots",
                quest.accepted().size(), quest.maxSlots()), x, y, QuestTheme.MUTED);
        y += font.lineHeight + 4;
        y += QuestTheme.wrapped(graphics, font, Component.translatable("nexuscore.quest.details.reward", quest.reward()),
                x, y, width, 3, QuestTheme.ACCENT);
        y += 4;
        QuestTheme.wrapped(graphics, font, Component.literal(quest.description()), x, y, width, 8, QuestTheme.TEXT);
    }

}
