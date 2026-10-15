package com.nexussmp.nexuscore.quest.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base das telas de quest: painel no estilo do inventario, titulo no canto e as teclas de sempre —
 * Esc e a tecla do inventario (E) fecham, menos enquanto se escreve num campo.
 */
public abstract class QuestScreen extends Screen {
    protected final int panelWidth;
    protected final int panelHeight;
    protected int left;
    protected int top;

    protected QuestScreen(Component title, int panelWidth, int panelHeight) {
        super(title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    @Override
    protected void init() {
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode) && !isTyping()) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Com o cursor num campo de texto, a tecla do inventario e so mais uma letra. */
    private boolean isTyping() {
        return getFocused() instanceof EditBox || getFocused() instanceof MultiLineEditBox;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        QuestTheme.panel(graphics, left, top, panelWidth, panelHeight);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
