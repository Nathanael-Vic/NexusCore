package com.nexussmp.nexuscore.quest.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Uma aba na lateral do diario, no mesmo couro do painel.
 *
 * <p>Comecou usando a imagem das abas do livro de receitas — a forma estava certa, a cor nao: cinza
 * do jogo grudado num painel de couro. Agora ela e desenhada com a mesma paleta do resto do diario,
 * e com a mesma anatomia dos botoes: contorno de tinta, luz em cima e a esquerda, sombra embaixo.
 *
 * <p>Duas coisas fazem a aba parecer aba, e as duas sao de proposito. Ela <b>nao tem borda direita</b>,
 * entao a que esta aberta se funde ao painel como se fosse a mesma folha; e a fechada e mais escura
 * e recua alguns pixels, de modo que a aberta pareca estar por cima das outras.
 */
final class QuestPanelTab extends Button {
    /** Medida da aba do livro de receitas, para a fileira encaixar do mesmo jeito. */
    static final int WIDTH = 35;
    static final int HEIGHT = 27;
    /** Os ultimos pixels ficam por baixo do painel: e o que faz a aba aberta se fundir a folha. */
    private static final int OVERLAP = 4;
    /** Quanto a aba fechada recua, para a aberta parecer estar por cima. */
    private static final int CLOSED_STEP = 3;

    private static final int EDGE = 0xFF1B1410;
    /** Couro do painel: a aba aberta e feita do mesmo material, e por isso some dentro dele. */
    private static final int FACE_OPEN = 0xFFC3A87C;
    private static final int FACE_CLOSED = 0xFF9E8156;
    private static final int FACE_CLOSED_HOVER = 0xFFB59670;
    private static final int LIGHT = 0xFFF0E4C8;
    private static final int SHADOW = 0xFF8A6F49;

    private final ItemStack icon;
    private final boolean selected;

    QuestPanelTab(int x, int y, ItemStack icon, Component name, boolean selected, OnPress onPress) {
        super(x, y, WIDTH, HEIGHT, name, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.selected = selected;
        setTooltip(Tooltip.create(name));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX() + (selected ? 0 : CLOSED_STEP);
        int y = getY();
        int right = getX() + WIDTH;
        int bottom = y + HEIGHT;
        int face = selected ? FACE_OPEN : isHoveredOrFocused() ? FACE_CLOSED_HOVER : FACE_CLOSED;

        // Sem contorno a direita: aquele lado encosta no painel e tem de sumir dentro dele.
        graphics.fill(x + 1, y, right, y + 1, EDGE);
        graphics.fill(x + 1, bottom - 1, right, bottom, EDGE);
        graphics.fill(x, y + 1, x + 1, bottom - 1, EDGE);

        graphics.fill(x + 1, y + 1, right, bottom - 1, face);
        graphics.fill(x + 1, y + 1, right, y + 2, LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 2, LIGHT);
        graphics.fill(x + 1, bottom - 2, right, bottom - 1, SHADOW);

        // O icone se centra no pedaco que aparece, e nao na largura cheia: o resto esta escondido.
        int visible = right - OVERLAP - x;
        graphics.renderFakeItem(icon, x + (visible - 16) / 2, y + (HEIGHT - 16) / 2);
    }
}
