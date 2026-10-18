package com.nexussmp.nexuscore.monolith;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que o eco faz com quem anda perto de um monolito, do lado do servidor.
 *
 * <ul>
 *   <li><b>A intensidade</b> vai para o cliente, que desenha o resto (o glitch na tela, o F3, os
 *       mapas). Sem coordenada nenhuma: so o numero.</li>
 *   <li><b>O vazio.</b> Quem cai num chunk vazio nao morre la embaixo: e jogado de volta para o ultimo
 *       chao firme em que pisou, e leva um bom golpe ({@value #FALL_SHARE} da vida maxima, que
 *       atravessa armadura), mas nunca o golpe que mata.</li>
 * </ul>
 */
public final class EchoPlayers {
    public static final ResourceKey<DamageType> FELL = ResourceKey.create(Registries.DAMAGE_TYPE, NexusCore.id("fell_through_world"));
    private static final float FALL_SHARE = 0.6F;
    /** Quanto abaixo do nivel do mar, dentro do vazio, a queda e interrompida. */
    private static final int FALL_DEPTH = 24;

    private static final Map<UUID, Float> SENT = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_GROUND = new HashMap<>();

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        List<MonolithSites.Site> sites = level.dimension() == Level.OVERWORLD ? MonolithSites.of(level) : List.of();
        float intensity = sites.isEmpty() ? 0.0F : Echo.intensity(sites, player.getX(), player.getZ());
        if (intensity > 0.0F && !player.isSpectator()) {
            catchFall(player, level, sites);
        }
        if (player.tickCount % 10 == 0) {
            if (intensity > 0.0F && MonolithSites.insideStructure(level, player.blockPosition())) {
                intensity = 0.0F;
            }
            send(player, intensity);
        }
    }

    private static void catchFall(ServerPlayer player, ServerLevel level, List<MonolithSites.Site> sites) {
        BlockPos at = player.blockPosition();
        boolean hollow = level.getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ()) <= level.getMinBuildHeight();
        if (!hollow) {
            if (player.onGround()) {
                LAST_GROUND.put(player.getUUID(), player.position());
            }
            return;
        }
        if (player.getY() > level.getSeaLevel() - FALL_DEPTH || player.getAbilities().flying) {
            return;
        }
        Vec3 back = LAST_GROUND.get(player.getUUID());
        if (back == null) {
            MonolithSites.Site site = MonolithSites.nearest(sites, player.getX(), player.getZ());
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, site.center());
            back = Vec3.atBottomCenterOf(top);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        player.teleportTo(level, back.x, back.y, back.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, back.x, back.y + 1.0, back.z, 40, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, back.x, back.y, back.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        float damage = Math.min(player.getHealth() - 1.0F, player.getMaxHealth() * FALL_SHARE);
        if (damage > 0.0F) {
            player.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FELL)), damage);
        }
    }

    private static void send(ServerPlayer player, float intensity) {
        Float last = SENT.get(player.getUUID());
        if (last != null && Math.abs(last - intensity) < 0.02F && (last == 0.0F) == (intensity == 0.0F)) {
            return;
        }
        SENT.put(player.getUUID(), intensity);
        PacketDistributor.sendToPlayer(player, new MonolithPayloads.Intensity(intensity));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
        LAST_GROUND.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        SENT.clear();
        LAST_GROUND.clear();
    }

    private EchoPlayers() {}
}
