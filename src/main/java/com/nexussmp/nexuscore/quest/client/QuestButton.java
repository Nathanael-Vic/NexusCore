package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.quest.client.screen.QuestTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Um botao do diario, pintado no mesmo couro do painel.
 *
 * <p>O botao do jogo e cinza-azulado, e ao lado do papel do diario ele denunciava que veio de outro
 * lugar. Este tem a <b>mesma anatomia</b> — contorno de um pixel com os cantos cortados, luz em cima
 * e a esquerda, sombra embaixo e a direita —, so que nas cores do mural: face de papel, contorno de
 * tinta, e a face clareando quando o mouse passa por cima.
 *
 * <p>E desenhado a mao em vez de sair de uma imagem porque assim ele estica para qualquer largura
 * sem ninguem precisar gerar textura nova, e porque as cores saem do mesmo lugar que as do painel —
 * mudou o painel, mudou o botao junto.
 *
 * <p>Vale para o diario do inventario e para as telas do mural: e o mesmo mural nos dois lugares,
 * entao nao ha razao para o botao mudar de cara conforme por onde se chega nele.
 */
public final class QuestButton extends Button {
    /** Contorno de tinta, o mesmo do painel. */
    private static final int EDGE = 0xFF1B1410;
    /** Face de papel; com o mouse em cima ela clareia. */
    private static final int FACE = 0xFFD9CBA6;
    private static final int FACE_HOVER = 0xFFEFE3C0;
    private static final int FACE_OFF = 0xFFC3A87C;
    /** Chanfro claro em cima e a esquerda. */
    private static final int LIGHT = 0xFFF0E4C8;
    /** Chanfro escuro embaixo e a direita. */
    private static final int SHADOW = 0xFF8A6F49;

    public QuestButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int right = x + width;
        int bottom = y + height;
        int face = !active ? FACE_OFF : isHoveredOrFocused() ? FACE_HOVER : FACE;

        // Contorno com os cantos cortados: quatro tracos, e nao um retangulo, para os cantos
        // ficarem vazados como nos botoes do jogo.
        graphics.fill(x + 1, y, right - 1, y + 1, EDGE);
        graphics.fill(x + 1, bottom - 1, right - 1, bottom, EDGE);
        graphics.fill(x, y + 1, x + 1, bottom - 1, EDGE);
        graphics.fill(right - 1, y + 1, right, bottom - 1, EDGE);

        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, face);
        graphics.fill(x + 1, y + 1, right - 1, y + 2, LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 2, LIGHT);
        graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, SHADOW);
        graphics.fill(right - 2, y + 2, right - 1, bottom - 1, SHADOW);

        // Texto sem sombra, como todo o resto do diario: e tinta sobre papel, nao letreiro.
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, QuestTheme.fit(font, getMessage(), width - 8),
                x + (width - Math.min(font.width(getMessage()), width - 8)) / 2,
                y + (height - font.lineHeight) / 2 + 1,
                active ? QuestTheme.TITLE : QuestTheme.MUTED, false);
    }
}
