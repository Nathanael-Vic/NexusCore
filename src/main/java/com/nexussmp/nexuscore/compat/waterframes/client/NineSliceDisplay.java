package com.nexussmp.nexuscore.compat.waterframes.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Desenha um sprite de GUI; se o sprite tiver {@code nine_slice} no {@code .png.mcmeta}, o jogo estica
 * so o miolo e preserva as bordas.
 *
 * <p>O CreativeCore passa coordenadas em {@code double}. Nine-slice em posicao quebrada deixa a borda
 * de 1px borrada ou irregular, entao as bordas sao arredondadas para a grade de pixels antes de desenhar.
 */
public final class NineSliceDisplay extends StyleDisplay {
    private final ResourceLocation sprite;

    public NineSliceDisplay(ResourceLocation sprite) {
        this.sprite = sprite;
    }

    @Override
    public void render(GuiGraphics graphics, double x, double y, double width, double height) {
        int left = (int) Math.round(x);
        int top = (int) Math.round(y);
        int right = (int) Math.round(x + width);
        int bottom = (int) Math.round(y + height);
        if (right <= left || bottom <= top) {
            return;
        }
        graphics.blitSprite(sprite, left, top, right - left, bottom - top);
    }
}
