package com.nexussmp.nexuscore.radio.client;

import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.radio.Membership;
import com.nexussmp.nexuscore.radio.client.screen.RadioMixerScreen;
import com.nexussmp.nexuscore.radio.client.screen.RadioScreen;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * O que o cliente sabe da radio: a transmissao no ar e o que esta pessoa e dentro dela.
 *
 * <p>Nada e adivinhado aqui. O servidor manda o estado inteiro a cada mudanca, e este lado so
 * guarda o ultimo que chegou — e por isso a tela, o aviso e o indicador nunca discordam entre si.
 *
 * <p>O tempo no ar continua correndo sozinho a partir do numero que veio junto, sem comparar
 * relogios de maquinas diferentes.
 */
public final class RadioClient {
    /**
     * A transmissao como este cliente a conhece.
     *
     * <p>{@code volatile} porque ela tambem e lida pela thread de audio do chat de voz, que aplica o
     * volume da radio em cada pedaco de voz que chega. Sem isto, aquela thread poderia continuar
     * vendo uma transmissao velha — com o volume de antes, ou uma voz que ja saiu do ar.
     */
    @Nullable
    private static volatile RadioPayloads.Live live;
    private static long receivedAt;
    /** Se esta pessoa e locutor: e o que decide o botao de abrir o ar aparecer. */
    private static boolean mayBroadcast;

    /** Chegou estado novo do servidor. */
    public static void apply(RadioPayloads.State payload) {
        RadioPayloads.Live incoming = payload.live().orElse(null);
        live = incoming;
        mayBroadcast = payload.mayBroadcast();
        receivedAt = Util.getMillis();
        if (incoming != null && payload.announce()) {
            announce(incoming);
        }
        if (incoming == null || !incoming.music().playing() || !incoming.me().listening()) {
            // Fora do ar, sem musica ou fora da transmissao: o tocador para na mesma hora.
            RadioMusic.stop();
        }
        refreshOpenScreen();
    }

    /**
     * A tela aberta acompanha a mudanca.
     *
     * <p>A da radio troca os botoes; a de pausa se remonta para o botao da radio aparecer (ou
     * sumir) no mesmo instante em que a transmissao comeca ou acaba, sem a pessoa ter que fechar e
     * abrir o menu para ver.
     */
    private static void refreshOpenScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen instanceof RadioScreen radio) {
            radio.refresh();
        } else if (screen instanceof RadioMixerScreen mixer) {
            mixer.refresh();
        } else if (screen instanceof PauseScreen) {
            screen.resize(minecraft, screen.width, screen.height);
        }
    }

    /**
     * O aviso na tela, igual ao de uma conquista.
     *
     * <p>Se ja houver um aviso desta mesma transmissao na fila, nao entra outro: quem acabou de
     * entrar no servidor no segundo em que a radio abriu veria o mesmo recado duas vezes.
     */
    private static void announce(RadioPayloads.Live incoming) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getToasts().getToast(BroadcastToast.class, incoming.id()) == null) {
            minecraft.getToasts().addToast(new BroadcastToast(incoming));
        }
    }

    @Nullable
    public static RadioPayloads.Live live() {
        return live;
    }

    public static boolean mayBroadcast() {
        return mayBroadcast;
    }

    public static Membership membership() {
        return live == null ? Membership.NONE : live.me();
    }

    public static boolean listening() {
        return membership().listening();
    }

    /** Quem apresenta ve o botao de encerrar; o servidor confere de novo do outro lado. */
    public static boolean isPresenter() {
        LocalPlayer player = Minecraft.getInstance().player;
        return live != null && player != null && player.getUUID().equals(live.presenterId());
    }

    /**
     * Quem pode encerrar o que esta no ar: o dono do programa e a moderacao.
     *
     * <p>O nivel de permissao que o cliente conhece e o mesmo que o servidor mandou no login, e de
     * qualquer forma quem decide de verdade e o servidor — aqui isso so serve para o botao existir.
     */
    public static boolean mayEnd() {
        LocalPlayer player = Minecraft.getInstance().player;
        return live != null && player != null
                && (isPresenter() || player.hasPermissions(2));
    }

    /** A musica do programa, ou o silencio. */
    public static RadioPayloads.Music music() {
        return live == null ? RadioPayloads.Music.NONE : live.music();
    }

    /** Quem esta no ar agora — a lista que a mesa de som mostra. */
    public static List<RadioPayloads.Voice> voices() {
        return live == null ? List.of() : live.voices();
    }

    /**
     * Em que ponto da musica o programa esta.
     *
     * <p>O servidor manda a posicao do momento em que o pacote saiu; daqui em diante o relogio
     * daqui continua a conta sozinho, sem comparar relogio de maquina com relogio de servidor.
     */
    public static long musicPositionMillis() {
        RadioPayloads.Music music = music();
        if (!music.playing()) {
            return 0L;
        }
        return music.paused()
                ? music.positionMillis()
                : music.positionMillis() + (Util.getMillis() - receivedAt);
    }

    /** Quem mexe na mesa de som: quem apresenta e a moderacao. */
    public static boolean mayMix() {
        LocalPlayer player = Minecraft.getInstance().player;
        return live != null && player != null && (isPresenter() || player.hasPermissions(2));
    }

    /** O volume da radio para esta pessoa. E so dela: nao encosta no jogo nem no chat de voz. */
    public static int personalVolume() {
        return ClientConfig.get(ClientConfig.RADIO_VOLUME);
    }

    public static void setPersonalVolume(int volume) {
        ClientConfig.RADIO_VOLUME.set(Membership.clampVolume(volume));
        ClientConfig.RADIO_VOLUME.save();
    }

    // ---- a mesa de som ----

    public static void playMusic(String url) {
        PacketDistributor.sendToServer(RadioPayloads.Mix.play(url));
    }

    public static void pauseMusic(boolean paused) {
        PacketDistributor.sendToServer(RadioPayloads.Mix.music(paused
                ? RadioPayloads.Mix.Kind.MUSIC_PAUSE
                : RadioPayloads.Mix.Kind.MUSIC_RESUME));
    }

    public static void stopMusic() {
        PacketDistributor.sendToServer(RadioPayloads.Mix.music(RadioPayloads.Mix.Kind.MUSIC_STOP));
    }

    public static void setMusicVolume(int volume) {
        PacketDistributor.sendToServer(RadioPayloads.Mix.musicVolume(volume));
    }

    public static void setVoiceVolume(UUID voice, int volume) {
        PacketDistributor.sendToServer(RadioPayloads.Mix.voiceVolume(voice, volume));
    }

    /** No ar com o microfone fechado. */
    public static boolean micMuted() {
        return membership().onAir() && membership().micMuted();
    }

    public static void setMicMuted(boolean muted) {
        send(muted ? RadioPayloads.Action.Kind.MIC_OFF : RadioPayloads.Action.Kind.MIC_ON);
    }

    /** Ha quantos segundos a transmissao esta no ar. */
    public static int elapsedSeconds() {
        if (live == null) {
            return 0;
        }
        return live.elapsedSeconds() + (int) ((Util.getMillis() - receivedAt) / 1000L);
    }

    // ---- pedidos ----

    /** Pede para abrir o ar. O nome vazio significa "use o meu nick". */
    public static void start(String title, String presenterName) {
        PacketDistributor.sendToServer(new RadioPayloads.Start(title, presenterName));
    }

    public static void listen() {
        send(RadioPayloads.Action.Kind.LISTEN);
    }

    public static void leave() {
        send(RadioPayloads.Action.Kind.LEAVE);
    }

    public static void end() {
        send(RadioPayloads.Action.Kind.END);
    }

    private static void send(RadioPayloads.Action.Kind kind) {
        if (live != null) {
            PacketDistributor.sendToServer(new RadioPayloads.Action(kind, live.id()));
        }
    }

    /** Sair do servidor apaga o que se sabia: o proximo mundo comeca com o ar vazio. */
    public static void clear() {
        live = null;
        mayBroadcast = false;
        RadioMusic.stop();
    }

    private RadioClient() {}
}
