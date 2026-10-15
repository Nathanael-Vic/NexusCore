package com.nexussmp.nexuscore.hologram.client;

import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import com.nexussmp.nexuscore.hologram.network.HologramAudioPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;

/**
 * O audio do discurso.
 *
 * <p>O som e preso a entidade: vem da direcao do holograma, com volume conforme a distancia, e para
 * sozinho quando a apresentacao acaba. O id vem pela rede, entao basta por o arquivo no resource pack —
 * nao precisa registrar som nenhum no mod nem recompilar para trocar a fala.
 */
public final class HologramClientAudio {
    /** Ids de holograma que ja comecaram a falar, para o mesmo discurso nao tocar duas vezes. */
    private static final Set<Integer> PLAYING = new HashSet<>();

    public static void play(HologramAudioPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !HologramClientSettings.enabled()) {
            return;
        }
        // Hologramas que sumiram nao precisam mais de controle.
        PLAYING.removeIf(id -> minecraft.level.getEntity(id) == null);

        Entity entity = minecraft.level.getEntity(payload.entityId());
        if (!(entity instanceof HologramEntity) || !PLAYING.add(payload.entityId())) {
            return;
        }
        SoundEvent sound = SoundEvent.createVariableRangeEvent(payload.sound());
        minecraft.getSoundManager().play(new HologramSpeechSound(sound, payload.volume(), payload.pitch(),
                entity, minecraft.level.random));
    }

    public static void clear() {
        PLAYING.clear();
    }

    private HologramClientAudio() {}
}
