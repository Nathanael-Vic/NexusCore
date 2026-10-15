package com.nexussmp.nexuscore.radio.client.screen;

import com.nexussmp.nexuscore.radio.client.RadioClient;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Abrir o ar: o titulo do programa e, se a pessoa quiser, o nome que vai aparecer no lugar do nick.
 *
 * <p>O campo do nome e opcional de proposito, e o vazio nao e um erro — e a resposta "use o meu
 * nick". Quem apresenta como "Rádio Nexus" continua sendo visto com a propria skin: o nome muda, a
 * cara nao, porque ela vem de quem esta falando e nao do que foi digitado.
 */
public final class BroadcastStartScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 240;
    private static final int PADDING = 8;
    private static final int FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int LINE = 11;

    @Nullable
    private final Screen parent;

    private EditBox titleField;
    private EditBox nameField;
    private Button goLive;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public BroadcastStartScreen(@Nullable Screen parent) {
        super(Component.translatable("nexuscore.radio.start.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, width - 2 * PADDING);
        panelX = (width - panelWidth) / 2;
        int fieldWidth = panelWidth - 2 * PADDING;

        // Altura: faixa + (rotulo + campo) duas vezes + respiro.
        panelHeight = PADDING + RadioPanel.HEADER_HEIGHT + PADDING
                + 2 * (LINE + FIELD_HEIGHT + GAP) + PADDING;
        int buttonsHeight = 2 * (BUTTON_HEIGHT + GAP) - GAP;
        panelY = Math.max(PADDING, (height - (panelHeight + PADDING + buttonsHeight)) / 2);

        int buttonWidth = Math.min(200, panelWidth);
        int buttonX = (width - buttonWidth) / 2;
        int buttonY = panelY + panelHeight + PADDING;
        // O botao nasce antes dos campos porque o campo de titulo o liga e desliga enquanto digita.
        goLive = Button.builder(Component.translatable("nexuscore.radio.start.go"), button -> goLive())
                .bounds(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT).build();
        goLive.active = false;

        int y = panelY + PADDING / 2 + RadioPanel.HEADER_HEIGHT + PADDING + LINE;
        titleField = new EditBox(font, panelX + PADDING, y, fieldWidth, FIELD_HEIGHT,
                Component.translatable("nexuscore.radio.start.field_title"));
        titleField.setMaxLength(RadioPayloads.MAX_TITLE);
        titleField.setResponder(text -> goLive.active = !text.isBlank());
        addRenderableWidget(titleField);

        y += FIELD_HEIGHT + GAP + LINE;
        nameField = new EditBox(font, panelX + PADDING, y, fieldWidth, FIELD_HEIGHT,
                Component.translatable("nexuscore.radio.start.field_name"));
        nameField.setMaxLength(RadioPayloads.MAX_NAME);
        // Vazio mostra o nick em cinza: o campo ja diz o que acontece se ninguem digitar nada.
        nameField.setHint(Component.literal(
                minecraft == null || minecraft.player == null ? "" : minecraft.player.getGameProfile().getName()));
        addRenderableWidget(nameField);

        addRenderableWidget(goLive);

        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(buttonX, buttonY + BUTTON_HEIGHT + GAP, buttonWidth, BUTTON_HEIGHT).build());

        setInitialFocus(titleField);
    }

    private void goLive() {
        RadioClient.start(titleField.getValue(), nameField.getValue());
        if (minecraft != null) {
            // Volta para a tela da radio: o estado novo chega em seguida e ela ja se monta com ele.
            minecraft.setScreen(new RadioScreen(parent));
        }
    }

    /** Painel e rotulos sao fundo: os campos de texto ficam por cima deles. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        RadioPanel.panel(graphics, panelX, panelY, panelWidth, panelHeight);
        RadioPanel.header(graphics, font, panelX + PADDING / 2, panelY + PADDING / 2, panelWidth - PADDING,
                Component.translatable("nexuscore.radio.start.header"), null);

        int labelY = panelY + PADDING / 2 + RadioPanel.HEADER_HEIGHT + PADDING;
        graphics.drawString(font, Component.translatable("nexuscore.radio.start.field_title"),
                panelX + PADDING, labelY, RadioPanel.TEXT, false);
        graphics.drawString(font, Component.translatable("nexuscore.radio.start.field_name"),
                panelX + PADDING, labelY + LINE + FIELD_HEIGHT + GAP, RadioPanel.MUTED_TEXT, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
