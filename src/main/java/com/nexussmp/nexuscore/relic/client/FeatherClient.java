package com.nexussmp.nexuscore.relic.client;

import com.nexussmp.nexuscore.relic.flight.WindFeather;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * O voo pela pena, do lado de quem voa: o movimento de quem joga e calculado no cliente dele, entao
 * e aqui que a pena segura a pessoa no ar um pouco mais que uma elytra. E o rastro de vento atras.
 */
final class FeatherClient {

    static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() || !WindFeather.glidingByFeather(player)) {
            return;
        }
        if (player == Minecraft.getInstance().player) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, motion.y + WindFeather.LIFT, motion.z);
        }
        if (player.tickCount % 3 == 0) {
            player.level().addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.6, player.getZ(), 0.0, 0.0, 0.0);
        }
    }

    private FeatherClient() {}
}
