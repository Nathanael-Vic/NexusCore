package com.nexussmp.nexuscore.display.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Tudo que a tela desenha, ja medido e posicionado. Montado so quando os dados mudam ({@link PanelLayoutBuilder});
 * a cada frame o renderer so percorre estes arrays.
 *
 * <p>Coordenadas em "unidades de fonte", origem no canto superior esquerdo da tela, y para baixo.
 */
final class PanelLayout {
    private final float unitsPerBlock;
    private final Page[] pages;

    PanelLayout(float unitsPerBlock, Page[] pages) {
        this.unitsPerBlock = unitsPerBlock;
        this.pages = pages;
    }

    float unitsPerBlock() {
        return unitsPerBlock;
    }

    /** Quantas telas esta pagina ocupa (listas longas viram varias). */
    int slideCount() {
        return pages.length;
    }

    Page slide(int index) {
        return pages[Math.floorMod(index, pages.length)];
    }

    record Page(float[] rects, int[] rectColors, FormattedCharSequence[] texts, float[] textX, float[] textY, int[] textColors) {

        void drawRects(VertexConsumer quads, Matrix4f matrix) {
            for (int i = 0; i < rectColors.length; i++) {
                quad(quads, matrix, rects, i, rectColors[i], LightTexture.FULL_BRIGHT);
            }
        }

        void drawTexts(Font font, Matrix4f matrix, MultiBufferSource buffers) {
            for (int i = 0; i < texts.length; i++) {
                font.drawInBatch(texts[i], textX[i], textY[i], textColors[i], false, matrix, buffers,
                        Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
            }
        }
    }

    /** Mesma ordem de vertices do fundo de texto do jogo, para a face ficar virada para quem olha. */
    private static void quad(VertexConsumer quads, Matrix4f matrix, float[] rects, int index, int color, int light) {
        int o = index * 4;
        float x0 = rects[o];
        float y0 = rects[o + 1];
        float x1 = rects[o + 2];
        float y1 = rects[o + 3];
        quads.addVertex(matrix, x0, y0, 0F).setColor(color).setLight(light);
        quads.addVertex(matrix, x1, y0, 0F).setColor(color).setLight(light);
        quads.addVertex(matrix, x1, y1, 0F).setColor(color).setLight(light);
        quads.addVertex(matrix, x0, y1, 0F).setColor(color).setLight(light);
    }
}
