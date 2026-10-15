package com.nexussmp.nexuscore.compat.waterframes.client;

import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.DisplayColor;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;
import team.creative.creativecore.common.util.type.Color;

/**
 * O {@link GuiStyle} das telas do WaterFrames.
 *
 * <p>Como o CreativeCore desenha cada controle ({@code GuiControl.render}): primeiro a borda no retangulo
 * inteiro, depois o fundo no miolo encolhido pela largura da borda. Por isso o sprite do painel, que ja traz
 * o chanfro, entra como <b>borda</b> (BIG) com fundo vazio; e a borda fina (SMALL) fica vazia com largura 0,
 * para botoes, secoes e campos desenharem o sprite deles no retangulo inteiro sem contorno sobrando.
 */
public final class NexusWaterFramesStyle {
    /** Chanfro do painel: 1px de contorno + 2px de luz/sombra + 1px de folga. */
    private static final int PANEL_BORDER = 4;

    private static GuiStyle style;

    public static GuiStyle get() {
        if (style == null) {
            style = build();
        }
        return style;
    }

    public static boolean isOurs(GuiStyle candidate) {
        return candidate != null && candidate == style;
    }

    private static GuiStyle build() {
        GuiStyle s = new GuiStyle();

        s.borderThickWidth = PANEL_BORDER;
        s.borderThick = WaterFramesSprites.PANEL;
        s.background = StyleDisplay.NONE;

        s.borderWidth = 0;
        s.border = StyleDisplay.NONE;

        s.secondaryBackground = WaterFramesSprites.INSET;
        s.headerBackground = WaterFramesSprites.INSET;
        s.bar = WaterFramesSprites.INSET;
        s.slot = WaterFramesSprites.INSET;
        s.disabledBackground = WaterFramesSprites.INSET;

        s.clickable = WaterFramesSprites.BUTTON;
        s.clickableHighlight = WaterFramesSprites.BUTTON_HIGHLIGHTED;
        s.clickableInactive = WaterFramesSprites.BUTTON_DISABLED;
        s.clickableInactiveHighlight = WaterFramesSprites.BUTTON_DISABLED;

        s.disabled = new DisplayColor(color(NexusPalette.DISABLED_OVERLAY));

        s.fontColor = color(NexusPalette.FIELD_TEXT);
        s.fontColorHighlight = color(NexusPalette.HIGHLIGHT_TEXT);
        s.fontColorDisabled = color(NexusPalette.FIELD_TEXT_DISABLED);
        return s;
    }

    private static Color color(int argb) {
        return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }

    private NexusWaterFramesStyle() {}
}
