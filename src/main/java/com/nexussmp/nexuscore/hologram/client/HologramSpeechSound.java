package com.nexussmp.nexuscore.hologram.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/**
 * A fala do holograma, presa a projecao: vem da direcao certa, tem volume por distancia e para
 * sozinha quando a apresentacao acaba.
 *
 * <p>Existe em vez do {@code EntityBoundSoundInstance} do jogo por um motivo so: aquele recusa tocar
 * em entidade marcada como silenciosa, e o holograma e silencioso de proposito — e assim que ele nao
 * solta os grunhidos e passos que o jogo daria em qualquer criatura. A marca vale para os sons do
 * jogo; o discurso e outra coisa.
 */
public final class HologramSpeechSound extends AbstractTickableSoundInstance {
    private final Entity projection;

    HologramSpeechSound(SoundEvent sound, float volume, float pitch, Entity projection, RandomSource random) {
        super(sound, SoundSource.VOICE, random);
        this.volume = volume;
        this.pitch = pitch;
        this.projection = projection;
        follow();
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    @Override
    public void tick() {
        if (projection.isRemoved()) {
            stop();
        } else {
            follow();
        }
    }

    /** Na altura dos olhos: a voz sai de onde estaria a boca, nao dos pes. */
    private void follow() {
        this.x = projection.getX();
        this.y = projection.getEyeY();
        this.z = projection.getZ();
    }
}
