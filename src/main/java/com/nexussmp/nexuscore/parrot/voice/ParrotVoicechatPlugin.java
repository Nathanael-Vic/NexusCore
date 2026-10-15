package com.nexussmp.nexuscore.parrot.voice;

import com.nexussmp.nexuscore.NexusCore;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A ponte dos papagaios com o Simple Voice Chat: o ouvido e o bico.
 *
 * <p>Plugin separado do das backrooms de proposito: os dois escutam o mesmo microfone, mas cada um
 * guarda o que precisa do seu jeito, e mexer num nunca muda o outro.
 *
 * <p>A fala sai por um canal <b>preso a entidade</b>, nao a um ponto: o papagaio esta no ombro de
 * alguem que pode estar andando, e a voz anda junto. Ela entra numa categoria de volume propria,
 * "Papagaios", que cada pessoa pode baixar ou mutar nas configuracoes do chat de voz.
 */
@ForgeVoicechatPlugin
public final class ParrotVoicechatPlugin implements VoicechatPlugin {
    /** Id da categoria de volume: so letras minusculas e _, ate 16. */
    private static final String CATEGORY = "nexus_parrots";
    /** Quadro do Opus, em amostras: 20 ms a 48 kHz. */
    private static final int FRAME_SAMPLES = 960;
    /** Amostras por tick (48 kHz / 20 ticks). */
    private static final int SAMPLES_PER_TICK = 2400;
    /** Alcance da voz do papagaio: menor que o de gente, ele fala de cima de um ombro. */
    private static final float VOICE_RANGE = 24.0F;

    /** Falas tocando agora. Precisam de alguem segurando, senao o coletor de lixo leva o audio. */
    private static final Map<UUID, AudioPlayer> PLAYING = new ConcurrentHashMap<>();
    private static final Map<UUID, EntityAudioChannel> CHANNELS = new ConcurrentHashMap<>();
    /** Uma thread so para preparar as falas, na fila: papagaio nenhum fala por cima de outro na conta. */
    private static final ExecutorService VOICE = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "NexusCore papagaios");
        thread.setDaemon(true);
        return thread;
    });

    @Nullable
    private static volatile VoicechatServerApi api;

    @Override
    public String getPluginId() {
        return NexusCore.MOD_ID + "_parrots";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, ParrotVoicechatPlugin::onStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            api = null;
            PLAYING.values().forEach(AudioPlayer::stopPlaying);
            PLAYING.clear();
            CHANNELS.clear();
        });
        registration.registerEvent(MicrophonePacketEvent.class, ParrotVoicechatPlugin::onMicrophone);
    }

    private static void onStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi server = event.getVoicechat();
        api = server;
        VolumeCategory category = server.volumeCategoryBuilder()
                .setId(CATEGORY)
                .setName("Papagaios")
                .setNameTranslationKey("nexuscore.parrot.voice_category")
                .setDescription("Papagaios repetindo o que ouviram")
                .setDescriptionTranslationKey("nexuscore.parrot.voice_category.description")
                .setIcon(ParrotIcon.pixels())
                .build();
        server.registerVolumeCategory(category);
    }

    /**
     * Cada pacote de microfone vai para os papagaios que estao ouvindo quem falou.
     *
     * <p>Cochicho e conversa de grupo ficam de fora: e o que as pessoas em volta tambem nao ouvem.
     */
    private static void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (api == null || sender == null || sender.isInGroup() || event.getPacket().isWhispering()) {
            return;
        }
        ParrotBrain.hear(sender.getPlayer().getUuid(), event.getPacket().getOpusEncodedData());
    }

    /**
     * Toca uma frase saindo do papagaio.
     *
     * <p>O canal abre aqui, na thread do jogo, porque e ele que precisa da entidade. Descomprimir e
     * afinar a voz ({@link ParrotVoice}) leva alguns milissegundos, e isso vai para a thread propria
     * dos papagaios: o tick do servidor nunca espera o papagaio pensar no que vai dizer. A duracao,
     * que o papagaio precisa saber na hora para mexer junto com a fala, sai da conta: cada pacote
     * sao 20 ms, e so a velocidade muda o tamanho.
     *
     * @return ticks de fala; 0 sem o servidor de voz no ar, -1 se o canal nao abriu
     */
    static int play(Entity parrot, List<byte[]> frames, double pitch, double speed) {
        VoicechatServerApi server = api;
        if (server == null) {
            return 0;
        }
        UUID channelId = UUID.randomUUID();
        EntityAudioChannel channel = server.createEntityAudioChannel(channelId, server.fromEntity(parrot));
        if (channel == null) {
            return -1;
        }
        channel.setDistance(VOICE_RANGE);
        channel.setCategory(CATEGORY);
        CHANNELS.put(channelId, channel);

        VOICE.execute(() -> {
            try {
                OpusDecoder decoder = server.createDecoder();
                short[] samples;
                try {
                    samples = decode(frames, decoder);
                } finally {
                    decoder.close();
                }
                samples = ParrotVoice.transform(samples, pitch, speed);
                AudioPlayer player = server.createAudioPlayer(channel, server.createEncoder(), padToFrames(samples));
                PLAYING.put(channelId, player);
                player.setOnStopped(() -> {
                    PLAYING.remove(channelId);
                    CHANNELS.remove(channelId);
                });
                player.startPlaying();
            } catch (RuntimeException error) {
                CHANNELS.remove(channelId);
                NexusCore.LOGGER.warn("[NexusCore] o papagaio engasgou ao preparar a fala", error);
            }
        });
        return Math.max(1, (int) (frames.size() * FRAME_SAMPLES / speed) / SAMPLES_PER_TICK);
    }

    private static short[] decode(List<byte[]> frames, OpusDecoder decoder) {
        List<short[]> pieces = new ArrayList<>(frames.size());
        int total = 0;
        for (byte[] frame : frames) {
            short[] decoded = decoder.decode(frame);
            if (decoded != null && decoded.length > 0) {
                pieces.add(decoded);
                total += decoded.length;
            }
        }
        short[] samples = new short[total];
        int offset = 0;
        for (short[] piece : pieces) {
            System.arraycopy(piece, 0, samples, offset, piece.length);
            offset += piece.length;
        }
        return samples;
    }

    /** O tocador trabalha em quadros de 20 ms; a sobra do fim vira silencio completando o quadro. */
    private static short[] padToFrames(short[] samples) {
        int full = (samples.length + FRAME_SAMPLES - 1) / FRAME_SAMPLES * FRAME_SAMPLES;
        return full == samples.length ? samples : Arrays.copyOf(samples, full);
    }
}
