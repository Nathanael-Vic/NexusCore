package com.nexussmp.nexuscore.compat.watermedia;

import com.nexussmp.nexuscore.NexusCore;
import net.neoforged.fml.ModList;
import org.watermedia.api.network.NetworkAPI;
import org.watermedia.api.network.patchs.AbstractPatch;
import org.watermedia.api.player.PlayerAPI;
import org.watermedia.api.player.videolan.MusicPlayer;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * O tocador de musica da radio: a unica classe do mod que encosta no WaterMedia.
 *
 * <p>O servidor nunca transmite audio — ele so diz <i>qual endereco</i> e <i>em que ponto</i>. Quem
 * toca e cada cliente, por conta propria, pelo VLC que o WaterMedia traz. E assim que os teloes do
 * WaterFrames funcionam, e e o unico jeito que presta: mandar musica pelo canal de voz daria o som
 * de telefone que o codec de voz produz.
 *
 * <p>Resolver o endereco (transformar um link do YouTube na faixa de audio de verdade) e uma ida a
 * rede, entao acontece fora da thread do jogo. O jogo nunca espera por isso.
 *
 * <p>Sem o WaterMedia instalado nada aqui carrega: quem pergunta e {@link #available()}, e a classe
 * de dentro — a unica que nomeia classes do mod — so e tocada depois dessa resposta.
 */
public final class MusicBox {
    private static final String MOD_ID = "watermedia";

    private static Boolean loaded;
    /** Uma falha basta para desistir: melhor programa sem musica do que erro a cada tick. */
    private static boolean broken;

    /** {@code true} quando da para tocar: mod instalado e VLC carregado. */
    public static boolean available() {
        if (broken || !loaded()) {
            return false;
        }
        try {
            return Box.ready();
        } catch (Throwable error) {
            fail(error);
            return false;
        }
    }

    /** Poe o endereco no ar a partir daquele ponto. Repetir o mesmo endereco nao reinicia nada. */
    public static void play(String url, long positionMillis) {
        run(() -> Box.play(url, positionMillis));
    }

    public static void setPaused(boolean paused) {
        run(() -> Box.setPaused(paused));
    }

    /** Volume de 0 a 100. */
    public static void setVolume(int volume) {
        run(() -> Box.setVolume(volume));
    }

    /** Corrige a posicao quando ela se afastou demais do que o programa diz. */
    public static void sync(long positionMillis) {
        run(() -> Box.sync(positionMillis));
    }

    /** Chamado a cada tick: termina o que estava pendente (buscar a posicao ao ficar pronto). */
    public static void tick() {
        run(Box::tick);
    }

    /** Para e solta o tocador. Nada de VLC vivo depois que a musica acabou. */
    public static void stop() {
        run(Box::stop);
    }

    private static void run(Runnable action) {
        if (broken || !loaded()) {
            return;
        }
        try {
            action.run();
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static void fail(Throwable error) {
        broken = true;
        NexusCore.LOGGER.warn("[NexusCore] a musica da radio falhou; seguindo sem ela", error);
    }

    private static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /** A parte que nomeia o WaterMedia. So e carregada depois da checagem la em cima. */
    private static final class Box {
        /** Diferenca a partir da qual vale a pena pular: menos que isso ninguem percebe. */
        private static final long DRIFT_TOLERANCE_MILLIS = 2500L;

        private static MusicPlayer player;
        private static String url = "";
        private static long pendingSeek = -1L;

        static boolean ready() {
            return PlayerAPI.isReady();
        }

        static synchronized void play(String wanted, long positionMillis) {
            if (wanted.equals(url) && player != null && !player.isBroken()) {
                sync(positionMillis);
                return;
            }
            stop();
            url = wanted;
            // O YouTube (e o resto) vira faixa de audio aqui, e isso e rede: vai para outra thread.
            CompletableFuture
                    .supplyAsync(() -> NetworkAPI.patch(URI.create(wanted)))
                    .whenComplete((result, error) -> start(wanted, result, positionMillis, error));
        }

        private static synchronized void start(String wanted, AbstractPatch.Result result,
                                               long positionMillis, Throwable error) {
            if (!wanted.equals(url)) {
                // A musica ja mudou enquanto este endereco era resolvido: o resultado nao serve mais.
                return;
            }
            if (error != null || result == null) {
                NexusCore.LOGGER.warn("[NexusCore] nao consegui resolver a musica: {}", wanted, error);
                url = "";
                return;
            }
            // Quando a fonte separa video e audio (o caso do YouTube), a faixa de audio e o que a
            // radio quer: nada de baixar imagem para tocar som.
            URI media = result.audioUrl != null ? result.audioUrl : result.uri;
            player = new MusicPlayer();
            player.start(media);
            pendingSeek = positionMillis > DRIFT_TOLERANCE_MILLIS ? positionMillis : -1L;
        }

        static synchronized void setPaused(boolean paused) {
            if (player == null) {
                return;
            }
            if (paused && player.isPlaying()) {
                player.pause();
            } else if (!paused && player.isPaused()) {
                player.play();
            }
        }

        static synchronized void setVolume(int volume) {
            if (player != null && player.getVolume() != volume) {
                player.setVolume(volume);
            }
        }

        static synchronized void sync(long positionMillis) {
            if (player == null || pendingSeek >= 0 || !player.isSeekAble() || player.isLive()) {
                return;
            }
            if (Math.abs(player.getTime() - positionMillis) > DRIFT_TOLERANCE_MILLIS) {
                player.seekTo(positionMillis);
            }
        }

        static synchronized void tick() {
            if (player == null || pendingSeek < 0) {
                return;
            }
            // Pular so funciona depois que o VLC abriu a faixa; ate la o pedido fica guardado.
            if (player.isSeekAble() && !player.isLive()) {
                player.seekTo(pendingSeek);
                pendingSeek = -1L;
            } else if (player.isBroken()) {
                pendingSeek = -1L;
            }
        }

        static synchronized void stop() {
            url = "";
            pendingSeek = -1L;
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
        }
    }

    private MusicBox() {}
}
