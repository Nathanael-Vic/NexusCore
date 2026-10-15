package com.nexussmp.nexuscore.quest.client.screen;

import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.quest.client.QuestActions;
import net.minecraft.client.gui.GuiGraphics;
import com.nexussmp.nexuscore.quest.client.QuestButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Criar quest: titulo, descricao, recompensa (texto livre, para as pessoas negociarem) e quantas
 * pessoas podem assumir. O servidor corta texto grande e confere o limite de quests por jogador.
 */
public final class QuestCreateScreen extends QuestScreen {
    private static final int MAX_TITLE = 64;
    private static final int MAX_DESCRIPTION = 400;
    private static final int MAX_REWARD = 96;

    private EditBox titleBox;
    private MultiLineEditBox descriptionBox;
    private EditBox rewardBox;
    private Button slotsButton;
    private Button publishButton;
    private int slots = 1;

    public QuestCreateScreen() {
        super(Component.translatable("nexuscore.quest.create.title"), 280, 208);
    }

    @Override
    protected void init() {
        super.init();
        int fieldWidth = panelWidth - 20;

        titleBox = new EditBox(font, left + 10, top + 34, fieldWidth, 18, Component.translatable("nexuscore.quest.field.title"));
        titleBox.setMaxLength(MAX_TITLE);
        titleBox.setHint(Component.translatable("nexuscore.quest.field.title.hint"));
        addRenderableWidget(titleBox);

        descriptionBox = new MultiLineEditBox(font, left + 10, top + 68, fieldWidth, 64,
                Component.translatable("nexuscore.quest.field.description.hint"),
                Component.translatable("nexuscore.quest.field.description"));
        descriptionBox.setCharacterLimit(MAX_DESCRIPTION);
        addRenderableWidget(descriptionBox);

        rewardBox = new EditBox(font, left + 10, top + 146, fieldWidth, 18, Component.translatable("nexuscore.quest.field.reward"));
        rewardBox.setMaxLength(MAX_REWARD);
        rewardBox.setHint(Component.translatable("nexuscore.quest.field.reward.hint"));
        addRenderableWidget(rewardBox);

        slotsButton = addRenderableWidget(new QuestButton(left + 10, top + panelHeight - 28, 88, 20,
                        slotsLabel(), button -> cycleSlots()));
        publishButton = addRenderableWidget(new QuestButton(left + 102, top + panelHeight - 28, 88, 20,
                        Component.translatable("nexuscore.quest.button.publish"), button -> publish()));
        addRenderableWidget(new QuestButton(left + 194, top + panelHeight - 28, 76, 20,
                        Component.translatable("gui.cancel"), button -> minecraft.setScreen(new QuestBoardScreen())));

        setInitialFocus(titleBox);
        updateButtons();
    }

    /**
     * Publicar depende dos campos preenchidos, entao nasce desligado.
     *
     * <p>O tick roda 20 vezes por segundo e a tela desenha bem mais: deixar isso so para o tick
     * fazia o botao aparecer aceso no primeiro quadro e apagar em seguida — a piscada ao trocar de
     * pagina. Mesmo motivo do {@code QuestBoardScreen}.
     */
    private void updateButtons() {
        publishButton.active = !titleBox.getValue().isBlank() && !rewardBox.getValue().isBlank();
    }

    private Component slotsLabel() {
        return Component.translatable("nexuscore.quest.button.slots", slots);
    }

    /**
     * Quantas vagas o servidor aceita. A config do servidor chega ao cliente na entrada, entao a
     * tela oferece exatamente o que vale la — antes ela parava em 10 fosse qual fosse a config, e o
     * servidor cortava calado o que passasse do limite dele.
     */
    private static int maxSlots() {
        return Math.max(1, ServerConfig.get(ServerConfig.QUEST_MAX_SLOTS));
    }

    private void cycleSlots() {
        int max = maxSlots();
        slots = Mth.clamp(slots % max + 1, 1, max);
        slotsButton.setMessage(slotsLabel());
    }

    private void publish() {
        String title = titleBox.getValue().trim();
        String reward = rewardBox.getValue().trim();
        if (title.isEmpty() || reward.isEmpty()) {
            return;
        }
        QuestActions.create(title, descriptionBox.getValue().trim(), reward, slots);
        minecraft.setScreen(new QuestBoardScreen());
    }

    @Override
    public void tick() {
        updateButtons();
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        QuestTheme.text(graphics, font, title, left + 10, top + 12, QuestTheme.TITLE);
        QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.field.description"),
                left + 10, top + 58, QuestTheme.MUTED);
        QuestTheme.text(graphics, font, Component.translatable("nexuscore.quest.field.reward"),
                left + 10, top + 136, QuestTheme.MUTED);
    }

}
