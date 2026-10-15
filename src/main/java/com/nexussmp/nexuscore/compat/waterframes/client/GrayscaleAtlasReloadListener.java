package com.nexussmp.nexuscore.compat.waterframes.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Icones do WaterFrames em cinza, sem redistribuir a arte dele (WaterFrames e All-Rights-Reserved):
 * a cada recarga de recursos le o atlas original, converte para cinza e registra o resultado no mesmo
 * {@link ResourceLocation}. Roda uma vez por F3+T, nunca por frame. Se um resource pack trocar o atlas,
 * a conversao usa o do pack.
 */
public final class GrayscaleAtlasReloadListener implements ResourceManagerReloadListener {
    public static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("waterframes", "textures/screen_atlas.png");

    /** Grade do atlas (IconStyles.create multiplica coluna e linha por 16). */
    private static final int CELL = 16;

    /** Celulas (coluna, linha) que indicam erro e mantem a cor: STATUS_ERROR, INTERNAL_ERROR, INTERNAL_ERROR_2. */
    private static final int[][] ERROR_CELLS = {{4, 1}, {3, 4}, {4, 4}};

    private boolean applied;
    private boolean warnedMissing;

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        TextureManager textures = Minecraft.getInstance().getTextureManager();
        if (!WaterFramesSettings.grayscaleIcons()) {
            restoreOriginal(textures);
            return;
        }

        Optional<Resource> resource = resourceManager.getResource(ATLAS);
        if (resource.isEmpty()) {
            if (!warnedMissing) {
                NexusCore.LOGGER.warn("[NexusCore] atlas de icones do WaterFrames nao encontrado: {}", ATLAS);
                warnedMissing = true;
            }
            restoreOriginal(textures);
            return;
        }

        boolean keepErrorRed = WaterFramesSettings.keepErrorRed();
        try (InputStream in = resource.get().open(); NativeImage source = NativeImage.read(in)) {
            NativeImage gray = new NativeImage(source.getWidth(), source.getHeight(), false);
            try {
                desaturate(source, gray, keepErrorRed);
            } catch (RuntimeException e) {
                gray.close();
                throw e;
            }
            // DynamicTexture assume a posse da imagem; o register fecha a textura anterior.
            textures.register(ATLAS, new DynamicTexture(gray));
            applied = true;
        } catch (IOException | RuntimeException e) {
            NexusCore.LOGGER.warn("[NexusCore] nao consegui gerar os icones cinza do WaterFrames: {}", e.toString());
            restoreOriginal(textures);
        }
    }

    private static void desaturate(NativeImage source, NativeImage target, boolean keepErrorRed) {
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                // NativeImage guarda ABGR
                int abgr = source.getPixelRGBA(x, y);
                if (keepErrorRed && isErrorCell(x, y)) {
                    target.setPixelRGBA(x, y, abgr);
                    continue;
                }
                int r = abgr & 0xFF;
                int g = (abgr >> 8) & 0xFF;
                int b = (abgr >> 16) & 0xFF;
                int luminance = Math.min(255, (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b));
                target.setPixelRGBA(x, y, (abgr & 0xFF000000) | (luminance << 16) | (luminance << 8) | luminance);
            }
        }
    }

    private static boolean isErrorCell(int x, int y) {
        int column = x / CELL;
        int row = y / CELL;
        for (int[] cell : ERROR_CELLS) {
            if (cell[0] == column && cell[1] == row) {
                return true;
            }
        }
        return false;
    }

    /** Solta a textura cinza; na proxima vez que o WaterFrames pedir, o jogo carrega o atlas original. */
    private void restoreOriginal(TextureManager textures) {
        if (applied) {
            textures.release(ATLAS);
            applied = false;
        }
    }
}
