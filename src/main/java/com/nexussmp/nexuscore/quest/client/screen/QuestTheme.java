package com.nexussmp.nexuscore.quest.client.screen;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * O visual das telas de quest: a anatomia de painel do inventario (contorno, chanfro claro em cima
 * e escuro embaixo, texto sem sombra), pintada em couro e papel -- um mural de recados, nao a tela
 * de opcoes. As cores saem daqui e do gerador tools/textures/generate-quest-textures.cjs.
 */
public final class QuestTheme {
    public static final ResourceLocation PANEL = NexusCore.id("quest/panel");
    public static final ResourceLocation INSET = NexusCore.id("quest/inset");

    /** Tinta sobre papel: escura o bastante para ler no couro e no papel do painel. */
    public static final int TITLE = 0x36281A;
    public static final int TEXT = 0x40301E;
    public static final int MUTED = 0x6B5233;
    public static final int ACCENT = 0x1F6B3A;
    public static final int WARNING = 0x8B2E2E;

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitSprite(PANEL, x, y, width, height);
    }

    public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitSprite(INSET, x, y, width, height);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, false);
    }

    /** Escreve quebrando em varias linhas e para quando acabar o espaco. */
    public static int wrapped(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int maxLines, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int drawn = 0;
        for (FormattedCharSequence line : lines) {
            if (drawn >= maxLines) {
                break;
            }
            graphics.drawString(font, line, x, y + drawn * (font.lineHeight + 1), color, false);
            drawn++;
        }
        return drawn * (font.lineHeight + 1);
    }

    /** Corta com "..." o que passar da largura. */
    public static FormattedCharSequence fit(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text.getVisualOrderText();
        }
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width("...")));
        return net.minecraft.locale.Language.getInstance()
                .getVisualOrder(FormattedText.composite(cut, FormattedText.of("...")));
    }

    private QuestTheme() {}
}
