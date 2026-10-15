package com.nexussmp.nexuscore.radio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * A plaquinha de "no ar" enquanto existir transmissao.
 *
 * <p>Fica no canto de baixo a direita, que e o unico canto que o jogo nao usa para nada: o chat
 * mora embaixo a esquerda, os avisos nascem em cima a direita, e os efeitos e o minimapa de quem
 * usa um disputam o alto da tela. E pequena de proposito — ela e um lembrete, nao um painel.
 *
 * <p>O ponto vermelho pisca devagar, como a luz de uma camera ligada. E so isso: quem quiser entrar
 * ou sair abre a pausa, porque durante o jogo o mouse pertence a camera e nao ha clique nenhum
 * para dar aqui.
 *
 * <p>A palavra diz <b>onde voce esta</b> na transmissao, nessa ordem: no ar, mudo, ouvindo, ou so
 * sabendo que tem alguem ao vivo. Antes ela so perguntava "voce esta ouvindo?" — e quem apresentava
 * lia OUVINDO no proprio canto da tela, com microfone aberto ou fechado. Quando o microfone fecha, a
 * luz vermelha tambem apaga e fica cinza, parada: e o sinal universal de "o seu audio nao esta indo".
 */
public final class RadioHud {
    private static final int MARGIN = 4;
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0x66000000;
    private static final int DOT_SIZE = 3;
    private static final int DOT_COLOR = 0xFFFF5555;
    /** A luz de no ar, apagada: microfone fechado. */
    private static final int DOT_MUTED = 0xFF8A8A8A;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    /** Um ciclo de pisca por segundo e meio. */
    private static final float BLINK_PERIOD_MILLIS = 1500.0F;

    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (RadioClient.live() == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        boolean muted = RadioClient.micMuted();
        Component label = Component.translatable(labelKey());

        GuiGraphics graphics = event.getGuiGraphics();
        int textWidth = font.width(label);
        int chipWidth = PADDING + DOT_SIZE + 4 + textWidth + PADDING;
        int chipHeight = PADDING + font.lineHeight + PADDING - 1;
        int x = graphics.guiWidth() - MARGIN - chipWidth;
        int y = graphics.guiHeight() - MARGIN - chipHeight;

        graphics.fill(x, y, x + chipWidth, y + chipHeight, BACKGROUND);
        graphics.fill(x + PADDING, y + PADDING + 2, x + PADDING + DOT_SIZE, y + PADDING + 2 + DOT_SIZE,
                muted ? DOT_MUTED : blink());
        graphics.drawString(font, label, x + PADDING + DOT_SIZE + 4, y + PADDING, TEXT_COLOR, false);
    }

    /** Quem esta no ar ve o proprio estado; quem nao esta ve se esta ouvindo ou nao. */
    private static String labelKey() {
        if (RadioClient.membership().onAir()) {
            return RadioClient.micMuted() ? "nexuscore.radio.hud.muted" : "nexuscore.radio.hud.on_air";
        }
        return RadioClient.listening() ? "nexuscore.radio.hud.listening" : "nexuscore.radio.hud.live";
    }

    /** O vermelho vai e volta em vez de acender e apagar: piscar duro cansa a vista. */
    private static int blink() {
        float phase = (System.currentTimeMillis() % (long) BLINK_PERIOD_MILLIS) / BLINK_PERIOD_MILLIS;
        float wave = 0.55F + 0.45F * Mth.cos(phase * Mth.TWO_PI);
        int alpha = Mth.floor(wave * 255.0F) << 24;
        return alpha | (DOT_COLOR & 0x00FFFFFF);
    }

    private RadioHud() {}
}
