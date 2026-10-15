package com.nexussmp.nexuscore.radio.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * O desenho das telas da radio, montado so com peca do proprio jogo.
 *
 * <p>Tres sprites vanilla fazem tudo: o fundo de janela ({@code popup/background}), a faixa de
 * titulo das conquistas ({@code advancements/title_box}) para o letreiro de "no ar", e a moldura de
 * vaga do inventario ({@code container/slot}) em volta da cara de quem apresenta. Todas sao
 * nine-slice, entao esticam em qualquer tamanho sem borrar canto nenhum.
 *
 * <p>O unico enfeite proprio e o medidor de som: barras que sobem e descem como as de um aparelho
 * de radio. Elas sao desenhadas com retangulos, do mesmo jeito que o jogo desenha a barra de
 * experiencia — e ficam apagadas para quem nao esta ouvindo, que e a forma mais curta de dizer
 * "tem som rolando, e voce nao esta nele".
 */
public final class RadioPanel {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("popup/background");
    private static final ResourceLocation TITLE_BOX = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");

    public static final int SLOT_SIZE = 18;
    /** Altura da faixa de "no ar". */
    public static final int HEADER_HEIGHT = 20;
    /** Altura do medidor de som. */
    public static final int WAVE_HEIGHT = 10;

    public static final int TEXT = 0xFFFFFF;
    public static final int MUTED_TEXT = 0xA0A0A0;
    public static final int LIVE = 0xFFFF5555;
    private static final int WAVE_ON = 0xFF55FF55;
    private static final int WAVE_OFF = 0xFF5A5A5A;

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitSprite(BACKGROUND, x, y, width, height);
    }

    /** A faixa do alto, com o ponto vermelho pulsando e o tempo no ar a direita. */
    public static void header(GuiGraphics graphics, Font font, int x, int y, int width,
                              Component label, Component right) {
        graphics.blitSprite(TITLE_BOX, x, y, width, HEADER_HEIGHT);
        int dotSize = 4;
        int textY = y + (HEADER_HEIGHT - font.lineHeight) / 2 + 1;
        int dotX = x + 8;
        graphics.fill(dotX, textY + 2, dotX + dotSize, textY + 2 + dotSize, pulse());
        graphics.drawString(font, label, dotX + dotSize + 5, textY, LIVE, false);
        if (right != null) {
            graphics.drawString(font, right, x + width - 8 - font.width(right), textY, MUTED_TEXT, false);
        }
    }

    /** A moldura de vaga do inventario, para a cara caber dentro de alguma coisa. */
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.blitSprite(SLOT, x, y, SLOT_SIZE, SLOT_SIZE);
    }

    /** O medidor de som. Aceso para quem esta ouvindo, apagado para quem so esta olhando. */
    public static void waveform(GuiGraphics graphics, int x, int y, int width, boolean active) {
        int barWidth = 3;
        int gap = 2;
        int bars = Mth.clamp((width + gap) / (barWidth + gap), 4, 15);
        int total = bars * barWidth + (bars - 1) * gap;
        int startX = x + (width - total) / 2;
        long time = System.currentTimeMillis();
        for (int i = 0; i < bars; i++) {
            float phase = time / 260.0F + i * 0.8F;
            float amplitude = (0.35F + 0.65F * Math.abs(Mth.sin(phase))) * (active ? 1.0F : 0.3F);
            int barHeight = Math.max(1, Math.round(WAVE_HEIGHT * amplitude));
            int barX = startX + i * (barWidth + gap);
            graphics.fill(barX, y + WAVE_HEIGHT - barHeight, barX + barWidth, y + WAVE_HEIGHT,
                    active ? WAVE_ON : WAVE_OFF);
        }
    }

    /** O vermelho do "no ar" indo e voltando, como a luz de uma camera ligada. */
    private static int pulse() {
        float phase = (System.currentTimeMillis() % 1500L) / 1500.0F;
        float wave = 0.55F + 0.45F * Mth.cos(phase * Mth.TWO_PI);
        return (Mth.floor(wave * 255.0F) << 24) | (LIVE & 0x00FFFFFF);
    }

    private RadioPanel() {}
}
