package com.nexussmp.nexuscore.backrooms.voice;

import com.nexussmp.nexuscore.NexusCore;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A ponte com o Simple Voice Chat: escuta os microfones e faz o skinwalker falar.
 *
 * <p>Guardar e trabalho do {@link VoiceVault}; aqui ficam os eventos do chat de voz e a reproducao.
 */
@ForgeVoicechatPlugin
public final class BackroomsVoicechatPlugin implements VoicechatPlugin {
    /** Tom da voz enquanto ele finge ser gente: nenhum retoque. E a voz da pessoa, igualzinha. */
    public static final double PITCH_HUMAN = 1.0;
    /** Depois que ele se revela: mais fundo, sem virar rosnado. */
    public static final double PITCH_BEAST = 0.78;
    /** Quadro do Opus, em amostras: 20 ms a 48 kHz. */
    private static final int FRAME_SAMPLES = 960;
    /** Alcance da voz dele, em blocos. */
    private static final float VOICE_RANGE = 48.0F;

    private static final VoiceVault VAULT = new VoiceVault();
    /**
     * Falas tocando agora.
     *
     * <p>Canal e tocador precisam de alguem segurando: como variaveis locais, nada impedia o coletor
     * de lixo de levar embora audio ainda em reproducao.
     */
    private static final Map<UUID, AudioPlayer> PLAYING = new ConcurrentHashMap<>();
    private static final Map<UUID, LocationalAudioChannel> CHANNELS = new ConcurrentHashMap<>();

    @Nullable
    private static volatile VoicechatServerApi api;

    /** Em que passo a fala parou — falha calada foi o que custou uma noite de teste. */
    public enum Result {
        SEM_API,
        SEM_TRECHO,
        SEM_CANAL,
        TOCANDO
    }

    @Override
    public String getPluginId() {
        return NexusCore.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> api = event.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            api = null;
            stopAll();
            VAULT.clear();
        });
        registration.registerEvent(PlayerDisconnectedEvent.class, event -> VAULT.forget(event.getPlayerUuid()));
        registration.registerEvent(MicrophonePacketEvent.class, BackroomsVoicechatPlugin::onMicrophone);
    }

    /**
     * Cada pacote de microfone entra no cofre do jeito que chegou, comprimido.
     *
     * <p>Antes eu parava de guardar quando o cofre enchia, para poupar a decodificacao — so que assim
     * o cofre nunca se renovava e ele repetia a mesma frase para sempre. Guardando comprimido nao ha
     * o que poupar: e barato o bastante para aceitar tudo e deixar a fila circular fazer o resto.
     */
    private static void onMicrophone(MicrophonePacketEvent event) {
        if (api == null || event.getSenderConnection() == null) {
            return;
        }
        VAULT.accept(event.getSenderConnection().getPlayer().getUuid(),
                event.getPacket().getOpusEncodedData(), System.currentTimeMillis());
    }

    /** Solta uma fala de {@code speaker} saindo de onde a entidade esta. */
    static Result speak(Entity source, UUID speaker, double pitch) {
        VoicechatServerApi server = api;
        if (server == null || !(source.level() instanceof ServerLevel level)) {
            return Result.SEM_API;
        }
        VoiceVault.Clip clip = VAULT.random(speaker, source.getRandom(), System.currentTimeMillis());
        if (clip == null) {
            return Result.SEM_TRECHO;
        }

        Position position = server.createPosition(source.getX(), source.getEyeY(), source.getZ());
        UUID channelId = UUID.randomUUID();
        LocationalAudioChannel channel = server.createLocationalAudioChannel(
                channelId, server.fromServerLevel(level), position);
        if (channel == null) {
            return Result.SEM_CANAL;
        }
        channel.setDistance(VOICE_RANGE);

        OpusDecoder decoder = server.createDecoder();
        short[] samples;
        try {
            samples = VoiceVault.decode(clip, decoder);
        } finally {
            decoder.close();
        }

        AudioPlayer player = server.createAudioPlayer(channel, server.createEncoder(),
                frames(pitch == 1.0 ? samples : deepen(samples, pitch)));
        CHANNELS.put(channelId, channel);
        PLAYING.put(channelId, player);
        player.setOnStopped(() -> {
            PLAYING.remove(channelId);
            CHANNELS.remove(channelId);
        });
        player.startPlaying();
        return Result.TOCANDO;
    }

    /**
     * Desce o tom da voz.
     *
     * <p>Tocar a mesma onda mais devagar deixa o som mais grave e mais lento — o velho truque do
     * disco na rotacao errada. E o que transforma "o fulano falando" em "algo imitando o fulano".
     */
    private static short[] deepen(short[] samples, double pitch) {
        int length = (int) (samples.length / pitch);
        short[] out = new short[length];
        for (int i = 0; i < length; i++) {
            double source = i * pitch;
            int index = (int) source;
            double blend = source - index;
            short first = samples[Math.min(index, samples.length - 1)];
            short second = samples[Math.min(index + 1, samples.length - 1)];
            out[i] = (short) (first + (second - first) * blend);
        }
        return out;
    }

    /** O tocador trabalha em quadros de 20 ms; a sobra do fim vira silencio completando o quadro. */
    private static short[] frames(short[] samples) {
        int full = (samples.length + FRAME_SAMPLES - 1) / FRAME_SAMPLES * FRAME_SAMPLES;
        return full == samples.length ? samples : Arrays.copyOf(samples, full);
    }

    /** Quem tem voz guardada agora: e por aqui que o skinwalker escolhe quem imitar. */
    static List<UUID> voices() {
        return VAULT.speakers(System.currentTimeMillis());
    }

    private static void stopAll() {
        PLAYING.values().forEach(AudioPlayer::stopPlaying);
        PLAYING.clear();
        CHANNELS.clear();
    }
}
