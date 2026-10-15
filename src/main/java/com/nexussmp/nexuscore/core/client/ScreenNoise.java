package com.nexussmp.nexuscore.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;

/**
 * Chiado de TV fora do ar desenhado por cima da tela: quadradinhos cinza sorteados a cada quadro,
 * uma faixa mais forte descendo e linhas rasgadas.
 *
 * <p>Usado pela tela de quando o skinwalker pega alguem e pela loucura da mascara astral. E barato:
 * tudo vai numa leva so de desenho, e o sorteio e por quadro de {@value #FRAME_MS} ms, igual em
 * qualquer FPS.
 */
public final class ScreenNoise {
    /** O chiado troca de quadro a cada tantos milissegundos. */
    public static final long FRAME_MS = 45;
    /** Quantos quadradinhos cabem na largura da tela. */
    private static final int COLUMNS = 240;
    /** Quantos acendem por quadro. */
    private static final float DENSITY = 0.55F;

    /** O numero do quadro de chiado agora. */
    public static long frame(long millis) {
        return millis / FRAME_MS;
    }

    /** O sorteio deste quadro: o mesmo durante os 45 ms, diferente no seguinte. */
    public static RandomSource random(long frame) {
        return RandomSource.create(frame * 0x9E3779B97F4A7C15L);
    }

    /** Desenha o chiado com forca de 0 (nada) a 1 (quase so chiado). */
    public static void draw(GuiGraphics graphics, int width, int height, float strength, long frame, RandomSource random) {
        if (strength <= 0.01F) {
            return;
        }
        int cell = Math.max(2, width / COLUMNS);
        int band = (int) (frame * 7 % (height + 60)) - 30;
        graphics.drawManaged(() -> {
            for (int y = 0; y < height; y += cell) {
                boolean inBand = y >= band && y < band + 24;
                // Uma linha ou outra sai do lugar, como sinal rasgado.
                int shift = random.nextInt(18) == 0 ? random.nextInt(11) - 5 : 0;
                for (int x = 0; x < width; x += cell) {
                    if (random.nextFloat() > DENSITY) {
                        continue;
                    }
                    int gray = 40 + random.nextInt(216);
                    float alpha = strength * (inBand ? 0.85F : 0.45F) * (0.35F + 0.65F * random.nextFloat());
                    graphics.fill(x + shift, y, x + shift + cell, y + cell,
                            ((int) (alpha * 255) << 24) | gray * 0x010101);
                }
            }
            int lines = 1 + random.nextInt(3);
            for (int i = 0; i < lines; i++) {
                int y = random.nextInt(height);
                graphics.fill(0, y, width, y + 1, ((int) (strength * 140) << 24) | 0xDDDDDD);
            }
        });
    }

    private ScreenNoise() {}
}
