package com.nexussmp.nexuscore.compat.waterframes.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.nexussmp.nexuscore.mixin.waterframes.creativecore.CompiledTextAccessor;
import net.minecraft.client.gui.GuiGraphics;
import team.creative.creativecore.client.render.text.CompiledText;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.style.ControlFormatting;

/**
 * Rotulos que ficam direto sobre o painel claro. O {@code CompiledText} do CreativeCore guarda cor e sombra
 * proprios (branco com sombra), sem olhar o {@code GuiStyle}; branco com sombra some no cinza claro.
 * Aqui, so durante o desenho, o texto vira {@code #404040} sem sombra, como o titulo do inventario.
 * Texto dentro de botoes e campos continua branco, igual ao vanilla.
 */
public final class PanelText {
    /** Cor padrao do CompiledText (branco). Cor diferente foi escolhida pelo WaterFrames e e respeitada. */
    private static final int DEFAULT_TEXT_COLOR = -1;

    /** So controles sem fundo proprio (face NONE) nas telas com o nosso estilo. */
    public static boolean appliesTo(GuiControl control) {
        return WaterFramesSettings.vanillaStyle()
                && control.getControlFormatting().face == ControlFormatting.ControlStyleFace.NONE
                && NexusWaterFramesStyle.isOurs(control.getStyle());
    }

    public static void render(CompiledText text, GuiGraphics graphics, Operation<Void> original) {
        int color = text.getDefaultColor();
        if (color != DEFAULT_TEXT_COLOR) {
            original.call(text, graphics);
            return;
        }
        boolean shadow = ((CompiledTextAccessor) text).nexus$hasShadow();
        text.setDefaultColor(NexusPalette.LABEL);
        text.setShadow(false);
        try {
            original.call(text, graphics);
        } finally {
            text.setDefaultColor(color);
            text.setShadow(shadow);
        }
    }

    private PanelText() {}
}
