package com.nexussmp.nexuscore.backrooms.client;

import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * O som que fica: o ar do andar, o zumbido das lampadas e a musica da perseguicao.
 *
 * <p>Som tocado pelo servidor nasce num ponto do mundo e fica la — serve para passo e estalo, nao
 * para trilha. Uma trilha precisa acompanhar a pessoa e se repetir sozinha, e isso so o cliente faz:
 * estes sao instancias em loop, sem posicao, iguais a musica do jogo.
 */
public final class BackroomsAudio {
    @Nullable
    private static SoundInstance ambience;
    @Nullable
    private static BackroomsHumSound hum;
    @Nullable
    private static SoundInstance chase;

    /** O zumbido comeca ao entrar e para ao sair — inclusive se a pessoa sair pela porta correndo. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player) {
            return;
        }
        boolean inside = minecraft.player.level().dimension().equals(BackroomsDimensions.BACKROOMS);
        if (inside && ambience == null) {
            ambience = loop(NexusSounds.AMBIENCE.get(), 0.55F, SoundSource.AMBIENT);
            // O zumbido do forro anda junto e se mede pelas lampadas em volta.
            hum = new BackroomsHumSound();
            minecraft.getSoundManager().play(hum);
        } else if (!inside && ambience != null) {
            stop();
        }
    }

    /** A musica da cacada: entra quando ele se transforma e so para quando acaba. */
    public static void startChase() {
        if (chase == null) {
            chase = loop(NexusSounds.SKINWALKER_CHASE.get(), 0.9F, SoundSource.HOSTILE);
        }
    }

    public static void stopChase() {
        if (chase != null) {
            Minecraft.getInstance().getSoundManager().stop(chase);
            chase = null;
        }
    }

    /** Fim de tudo: nem zumbido, nem musica. */
    public static void stop() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ambience != null) {
            minecraft.getSoundManager().stop(ambience);
            ambience = null;
        }
        if (hum != null) {
            minecraft.getSoundManager().stop(hum);
            hum = null;
        }
        stopChase();
    }

    /**
     * Um som que toca dentro da cabeca de quem esta vendo a cena.
     *
     * <p>Vai no canal de <b>criatura hostil</b>, que e onde moram todos os outros sons do bicho.
     * Antes ia pelo caminho do jogador, e quem tivesse aquele volume baixo simplesmente nao ouvia o
     * susto — um susto que depende de configuracao nao e um susto.
     */
    public static void scare(SoundEvent sound, float volume, float pitch) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound.getLocation(),
                SoundSource.HOSTILE, volume, pitch, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    private static SoundInstance loop(SoundEvent sound, float volume, SoundSource source) {
        // Sem posicao e sem atenuacao: e trilha, vem de todo lado e acompanha a pessoa.
        SoundInstance instance = new SimpleSoundInstance(sound.getLocation(), source,
                volume, 1.0F, RandomSource.create(), true, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        Minecraft.getInstance().getSoundManager().play(instance);
        return instance;
    }

    private BackroomsAudio() {}
}
