package com.nexussmp.nexuscore.quest.client.screen;

import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/** A lista de quests das telas: titulo em cima, autor, vagas e recompensa embaixo. */
public final class QuestList extends ObjectSelectionList<QuestList.QuestEntry> {
    private static final int ROW_HEIGHT = 26;

    public QuestList(Minecraft minecraft, int x, int y, int width, int height) {
        super(minecraft, width, height, y, ROW_HEIGHT);
        setX(x);
    }

    /** Troca o conteudo tentando manter selecionada a mesma quest de antes. */
    public void setQuests(List<QuestView> quests) {
        UUID previous = selectedId();
        clearEntries();
        QuestEntry toSelect = null;
        for (QuestView quest : quests) {
            QuestEntry entry = new QuestEntry(quest);
            addEntry(entry);
            if (quest.id().equals(previous)) {
                toSelect = entry;
            }
        }
        setSelected(toSelect);
    }

    @Nullable
    public QuestView selected() {
        QuestEntry entry = getSelected();
        return entry == null ? null : entry.quest;
    }

    @Nullable
    private UUID selectedId() {
        QuestView quest = selected();
        return quest == null ? null : quest.id();
    }

    @Override
    public int getRowWidth() {
        return getWidth() - 14;
    }

    @Override
    protected int getScrollbarPosition() {
        return getX() + getWidth() - 7;
    }

    /** O fundo da lista e a mesma area afundada das telas do jogo. */
    @Override
    protected void renderListBackground(GuiGraphics graphics) {
        QuestTheme.inset(graphics, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    protected void renderListSeparators(GuiGraphics graphics) {
        // O painel ja delimita a lista; linhas extras sujariam o visual.
    }

    public final class QuestEntry extends ObjectSelectionList.Entry<QuestEntry> {
        private final QuestView quest;

        private QuestEntry(QuestView quest) {
            this.quest = quest;
        }

        public QuestView quest() {
            return quest;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            var font = Minecraft.getInstance().font;
            Component title = Component.literal(quest.title());
            graphics.drawString(font, QuestTheme.fit(font, title, width - 40), left + 2, top + 3, QuestTheme.TITLE, false);

            // Missao do servidor tem sempre uma vaga so, e ela e de quem esta olhando: contar
            // "1/1" nao diz nada. No lugar vai a marca de que o pedido veio do servidor.
            Component corner = quest.fromServer()
                    ? Component.translatable("nexuscore.quest.entry.server")
                    : Component.literal(quest.accepted().size() + "/" + quest.maxSlots());
            graphics.drawString(font, corner, left + width - font.width(corner) - 2, top + 3,
                    quest.fromServer() ? QuestTheme.ACCENT : quest.isFull() ? QuestTheme.WARNING : QuestTheme.MUTED,
                    false);

            Component line;
            if (!quest.open()) {
                line = Component.translatable("nexuscore.quest.entry.completed", quest.completedByName());
            } else if (quest.fromServer()) {
                line = Component.translatable(quest.pending()
                        ? "nexuscore.quest.state.waiting"
                        : "nexuscore.quest.state.doing");
            } else {
                line = Component.translatable("nexuscore.quest.entry.line", quest.authorName(), quest.reward());
            }
            graphics.drawString(font, QuestTheme.fit(font, line, width - 4), left + 2, top + 14, QuestTheme.MUTED, false);
        }

        @Override
        public Component getNarration() {
            return Component.literal(quest.title());
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            setSelected(this);
            return true;
        }
    }
}
