package com.nexussmp.nexuscore.hologram;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import com.nexussmp.nexuscore.hologram.network.HologramAudioPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Comeca, para e acompanha as apresentacoes.
 *
 * <p>O visual nao precisa de pacote nenhum: a entidade e sincronizada pelo jogo, entao quem chega no meio
 * ve o holograma no ponto exato em que ele esta. O unico pacote e o do audio, porque som o jogo nao
 * sabe comecar pelo meio.
 */
public final class HologramPlayback {
    private static final float DEFAULT_PITCH = 1.0F;

    public record Result(boolean started, String message) {}

    public static List<HologramEntity> active(ServerLevel level) {
        List<HologramEntity> holograms = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof HologramEntity hologram && hologram.isAlive()) {
                holograms.add(hologram);
            }
        }
        return holograms;
    }

    public static int activeCount(ServerLevel level) {
        return active(level).size();
    }

    @Nullable
    public static HologramEntity start(ServerLevel level, HologramRecording recording, Vec3 position,
                                       float yawOffset, float scale, boolean loop) {
        HologramEntity hologram = NexusRegistries.HOLOGRAM.get().create(level);
        if (hologram == null) {
            return null;
        }
        hologram.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        hologram.startPlayback(recording, position, yawOffset, scale, loop);
        level.addFreshEntity(hologram);
        // O audio sai em onStartTracking, que vale tanto para quem ja estava perto quanto para quem chega
        // depois. Mandar aqui tambem faria o som tocar duas vezes para quem estava por perto.
        return hologram;
    }

    public static int stop(ServerLevel level, @Nullable String name) {
        int stopped = 0;
        for (HologramEntity hologram : active(level)) {
            if (name == null || name.equalsIgnoreCase(hologram.recordingName())) {
                hologram.discard();
                stopped++;
            }
        }
        return stopped;
    }

    /** Quem entra no alcance no meio da apresentacao: ve sempre, e ouve se ainda estiver no comeco. */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof HologramEntity hologram)
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(hologram.level() instanceof ServerLevel)) {
            return;
        }
        String name = hologram.recordingName();
        if (name == null) {
            return;
        }
        HologramRecording recording = HologramStorage.load(player.server, name);
        ResourceLocation sound = recording == null ? null : recording.sound();
        HologramSettings settings = HologramSettings.current();
        if (sound != null && hologram.frame() <= settings.audioJoinWindowTicks()) {
            PacketDistributor.sendToPlayer(player, new HologramAudioPayload(
                    hologram.getId(), sound, settings.audioVolume(), DEFAULT_PITCH, hologram.frame()));
        }
    }

    private HologramPlayback() {}
}
