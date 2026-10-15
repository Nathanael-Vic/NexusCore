package com.nexussmp.nexuscore.space.client;

import com.nexussmp.nexuscore.space.SpaceDimensions;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Garantia contra qualquer degrade no Space.
 *
 * <p>O jogo escurece ou clareia a cor da nevoa em varias situacoes (camera abaixo da altura do horizonte,
 * chuva no overworld, cegueira, visao noturna). Aqui a cor final e presa na cor do espaco, entao o fundo
 * fica igual em todas as direcoes. Dentro d'agua ou lava a nevoa do jogo continua valendo.
 */
final class SpaceSkyRenderer {
    private static final float RED = ((SpaceDimensions.SKY_COLOR >> 16) & 0xFF) / 255.0F;
    private static final float GREEN = ((SpaceDimensions.SKY_COLOR >> 8) & 0xFF) / 255.0F;
    private static final float BLUE = (SpaceDimensions.SKY_COLOR & 0xFF) / 255.0F;

    static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!SpaceClientSettings.enabled()) {
            return;
        }
        Camera camera = event.getCamera();
        if (camera.getFluidInCamera() != FogType.NONE) {
            return;
        }
        Entity entity = camera.getEntity();
        if (entity == null || entity.level().dimension() != SpaceDimensions.SPACE) {
            return;
        }
        event.setRed(RED);
        event.setGreen(GREEN);
        event.setBlue(BLUE);
    }

    private SpaceSkyRenderer() {}
}
