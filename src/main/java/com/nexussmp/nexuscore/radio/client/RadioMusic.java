package com.nexussmp.nexuscore.radio.client;

import com.nexussmp.nexuscore.compat.watermedia.MusicBox;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A musica do programa tocando aqui, no ritmo do que o servidor diz.
 *
 * <p>Este lado nao decide nada: ele olha o estado que chegou — endereco, pausa, posicao e volume —
 * e faz o tocador obedecer. Quem entra no meio do programa entra no meio da musica, porque a posicao
 * veio junto.
 *
 * <p>So quem esta <b>ouvindo</b> escuta. Sair da transmissao, a transmissao acabar ou o programa
 * parar a musica dao todos no mesmo lugar: o tocador para e some.
 */
public final class RadioMusic {
    /** De quanto em quanto tempo a posicao e conferida contra a do servidor. */
    private static final int SYNC_INTERVAL_TICKS = 40;

    private static String playing = "";
    private static int ticks;

    public static void onClientTick(ClientTickEvent.Post event) {
        RadioPayloads.Live live = RadioClient.live();
        RadioPayloads.Music music = live == null ? RadioPayloads.Music.NONE : live.music();
        boolean shouldPlay = live != null && RadioClient.listening() && music.playing() && MusicBox.available();

        if (!shouldPlay) {
            stop();
            return;
        }

        if (!music.url().equals(playing)) {
            playing = music.url();
            MusicBox.play(playing, RadioClient.musicPositionMillis());
        }
        MusicBox.setPaused(music.paused());
        // O volume final e a conta das duas pontas: o da mesa (vale para todos) e o de quem ouve.
        MusicBox.setVolume(Math.round(music.volume() * RadioClient.personalVolume() / 100.0F));
        MusicBox.tick();

        if (++ticks % SYNC_INTERVAL_TICKS == 0 && !music.paused()) {
            MusicBox.sync(RadioClient.musicPositionMillis());
        }
    }

    /** Silencio, e sem deixar tocador nenhum aberto por tras. */
    public static void stop() {
        if (!playing.isEmpty()) {
            playing = "";
            MusicBox.stop();
        }
    }

    private RadioMusic() {}
}
