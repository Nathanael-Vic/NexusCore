package com.nexussmp.nexuscore.quest.client.screen;

import com.nexussmp.nexuscore.quest.client.QuestActions;
import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.nexussmp.nexuscore.quest.client.QuestButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Quem cumpriu a quest? O autor escolhe na lista de quem assumiu e confirma. */
public final class CompleteQuestScreen extends QuestScreen {

    private final QuestView quest;
    private ParticipantList list;
    private Button confirmButton;

    public CompleteQuestScreen(QuestView quest) {
        super(Component.translatable("nexuscore.quest.complete.title"), 220, 180);
        this.quest = quest;
    }

    @Override
    protected void init() {
        super.init();

        list = new ParticipantList(minecraft, left + 10, top + 40, panelWidth - 20, 96);
        for (QuestView.Participant participant : quest.accepted()) {
            list.addParticipant(participant);
        }
        addRenderableWidget(list);

        confirmButton = addRenderableWidget(new QuestButton(left + 10, top + panelHeight - 28, 98, 20,
                        Component.translatable("nexuscore.quest.button.confirm"), button -> confirm()));
        addRenderableWidget(new QuestButton(left + 112, top + panelHeight - 28, 98, 20,
                        Component.translatable("gui.back"), button -> minecraft.setScreen(new MyQuestsScreen())));
        updateButtons();
    }

    /**
     * Confirmar depende de ter alguem escolhido na lista, entao nasce desligado.
     *
     * <p>Mesma piscada do {@code QuestBoardScreen}: o tick roda 20 vezes por segundo e a tela
     * desenha bem mais, entao deixar a conta so para o tick acendia o botao no primeiro quadro.
     */
    private void updateButtons() {
        confirmButton.active = list.selectedParticipant() != null;
    }

    private void confirm() {
        QuestView.Participant selected = list.selectedParticipant();
        if (selected != null) {
            QuestActions.complete(quest.id(), selected.id());
            minecraft.setScreen(new MyQuestsScreen());
        }
    }

    @Override
    public void tick() {
        updateButtons();
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        QuestTheme.text(graphics, font, title, left + 10, top + 12, QuestTheme.TITLE);
        graphics.drawString(font, QuestTheme.fit(font, Component.literal(quest.title()), panelWidth - 20),
                left + 10, top + 26, QuestTheme.MUTED, false);
    }


    private static final class ParticipantList extends ObjectSelectionList<ParticipantList.ParticipantEntry> {
        private ParticipantList(Minecraft minecraft, int x, int y, int width, int height) {
            super(minecraft, width, height, y, 14);
            setX(x);
        }

        private void addParticipant(QuestView.Participant participant) {
            addEntry(new ParticipantEntry(participant));
        }

        @Nullable
        private QuestView.Participant selectedParticipant() {
            ParticipantEntry entry = getSelected();
            return entry == null ? null : entry.participant;
        }

        @Override
        public int getRowWidth() {
            return getWidth() - 14;
        }

        @Override
        protected int getScrollbarPosition() {
            return getX() + getWidth() - 7;
        }

        @Override
        protected void renderListBackground(GuiGraphics graphics) {
            QuestTheme.inset(graphics, getX(), getY(), getWidth(), getHeight());
        }

        @Override
        protected void renderListSeparators(GuiGraphics graphics) {}

        private final class ParticipantEntry extends ObjectSelectionList.Entry<ParticipantEntry> {
            private final QuestView.Participant participant;

            private ParticipantEntry(QuestView.Participant participant) {
                this.participant = participant;
            }

            @Override
            public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                               int mouseX, int mouseY, boolean hovering, float partialTick) {
                graphics.drawString(Minecraft.getInstance().font, participant.name(), left + 2, top + 2,
                        QuestTheme.TITLE, false);
            }

            @Override
            public Component getNarration() {
                return Component.literal(participant.name());
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                setSelected(this);
                return true;
            }
        }
    }
}
