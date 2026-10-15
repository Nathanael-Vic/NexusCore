package com.nexussmp.nexuscore.radio.network;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.radio.Membership;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Os pacotes da radio. O cliente pede e desenha; quem decide e o servidor. */
public final class RadioPayloads {
    /** Teto do titulo, conferido aqui na porta: nada de texto gigante vindo de fora. */
    public static final int MAX_TITLE = 64;
    /** Teto do nome no ar: cabe um nick (16) ou um nome de programa curto. */
    public static final int MAX_NAME = 24;
    /** Teto do endereco da musica. */
    public static final int MAX_URL = 256;
    /** Teto de vozes no ar ao mesmo tempo — a mesa de som nao precisa de mais que isso. */
    private static final int MAX_VOICES = 16;

    /**
     * Servidor → cliente: o que esta no ar agora, e o que <i>esta pessoa</i> e dentro disso.
     *
     * <p>Vazio significa "nao ha nada no ar" — e o mesmo pacote serve para comecar, terminar,
     * atualizar depois de um clique e para contar a novidade a quem acabou de entrar no servidor.
     * Uma fonte de verdade so, e o cliente nunca precisa adivinhar estado nenhum.
     */
    public record State(Optional<Live> live, boolean announce, boolean mayBroadcast)
            implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(NexusCore.id("radio_state"));

        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeBoolean(payload.live().isPresent());
                    payload.live().ifPresent(live -> live.write(buf));
                    buf.writeBoolean(payload.announce());
                    buf.writeBoolean(payload.mayBroadcast());
                },
                buf -> {
                    Optional<Live> live = buf.readBoolean() ? Optional.of(Live.read(buf)) : Optional.empty();
                    return new State(live, buf.readBoolean(), buf.readBoolean());
                });

        public static State off(boolean mayBroadcast) {
            return new State(Optional.empty(), false, mayBroadcast);
        }

        public static State of(Live live, boolean announce, boolean mayBroadcast) {
            return new State(Optional.of(live), announce, mayBroadcast);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * A transmissao como o cliente precisa dela.
     *
     * <p>O tempo vai como <b>segundos ja decorridos</b>, e nao como horario de inicio: relogio de
     * maquina nao bate com relogio de servidor, e "no ar ha 3 min" nao pode depender disso. O
     * cliente recebe o numero e continua contando sozinho.
     */
    public record Live(UUID id, UUID presenterId, String presenterName, String title,
                       int elapsedSeconds, Membership me, Music music, List<Voice> voices) {

        void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUUID(presenterId);
            buf.writeUtf(presenterName, MAX_NAME);
            buf.writeUtf(title, MAX_TITLE);
            buf.writeVarInt(Math.max(0, elapsedSeconds));
            me.write(buf);
            music.write(buf);
            int count = Math.min(voices.size(), MAX_VOICES);
            buf.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                voices.get(i).write(buf);
            }
        }

        static Live read(FriendlyByteBuf buf) {
            UUID id = buf.readUUID();
            UUID presenterId = buf.readUUID();
            String presenterName = buf.readUtf(MAX_NAME);
            String title = buf.readUtf(MAX_TITLE);
            int elapsed = buf.readVarInt();
            Membership me = Membership.read(buf);
            Music music = Music.read(buf);
            int count = buf.readVarInt();
            if (count < 0 || count > MAX_VOICES) {
                throw new IllegalArgumentException("mesa de som com " + count + " vozes");
            }
            List<Voice> voices = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                voices.add(Voice.read(buf));
            }
            return new Live(id, presenterId, presenterName, title, elapsed, me, music, List.copyOf(voices));
        }
    }

    /**
     * A musica do programa, como o cliente precisa dela.
     *
     * <p>A posicao vai em milissegundos <b>ja decorridos</b>, e nao como horario de inicio: relogio
     * de maquina nao bate com relogio de servidor. O cliente recebe o ponto e segue tocando dali.
     */
    public record Music(String url, boolean paused, long positionMillis, int volume) {
        public static final Music NONE = new Music("", false, 0L, Membership.FULL_VOLUME);

        public boolean playing() {
            return !url.isEmpty();
        }

        void write(FriendlyByteBuf buf) {
            buf.writeUtf(url, MAX_URL);
            buf.writeBoolean(paused);
            buf.writeVarLong(Math.max(0L, positionMillis));
            buf.writeByte(Membership.clampVolume(volume));
        }

        static Music read(FriendlyByteBuf buf) {
            return new Music(buf.readUtf(MAX_URL), buf.readBoolean(), buf.readVarLong(),
                    Membership.clampVolume(buf.readByte()));
        }
    }

    /** Uma voz no ar, para a mesa de som: quem e, e em que volume ela entra. */
    public record Voice(UUID id, String name, int volume, boolean micMuted) {
        void write(FriendlyByteBuf buf) {
            buf.writeUUID(id);
            buf.writeUtf(name, MAX_NAME);
            buf.writeByte(Membership.clampVolume(volume));
            buf.writeBoolean(micMuted);
        }

        static Voice read(FriendlyByteBuf buf) {
            return new Voice(buf.readUUID(), buf.readUtf(MAX_NAME), Membership.clampVolume(buf.readByte()),
                    buf.readBoolean());
        }
    }

    /**
     * Cliente → servidor: entrar ou sair da transmissao.
     *
     * <p>Vai com o id da sessao de proposito. Sem ele, um clique atrasado (a transmissao acabou e
     * outra comecou no meio do caminho) colocaria a pessoa na transmissao errada.
     */
    public record Action(Kind kind, UUID sessionId) implements CustomPacketPayload {
        public enum Kind {
            /** Entrar como ouvinte. */
            LISTEN,
            /** Sair da transmissao (continua convidado, se era). */
            LEAVE,
            /** Encerrar a transmissao. So o apresentador e a moderacao conseguem. */
            END,
            /** Abrir o microfone de quem esta no ar. */
            MIC_ON,
            /** Fechar o microfone de quem esta no ar, sem sair do programa. */
            MIC_OFF;

            private static final Kind[] VALUES = values();

            static Kind byOrdinal(int ordinal) {
                return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : LEAVE;
            }
        }

        public static final Type<Action> TYPE = new Type<>(NexusCore.id("radio_action"));

        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.kind().ordinal());
                    buf.writeUUID(payload.sessionId());
                },
                buf -> new Action(Kind.byOrdinal(buf.readByte()), buf.readUUID()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Cliente → servidor: abrir o ar.
     *
     * <p>O nome vai junto porque e escolhido na hora: vazio significa "use o meu nick". Os dois
     * campos sao conferidos de novo do outro lado — o que chega daqui e digitado por alguem.
     */
    public record Start(String title, String presenterName) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(NexusCore.id("radio_start"));

        public static final StreamCodec<FriendlyByteBuf, Start> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.title(), MAX_TITLE);
                    buf.writeUtf(payload.presenterName(), MAX_NAME);
                },
                buf -> new Start(buf.readUtf(MAX_TITLE), buf.readUtf(MAX_NAME)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Cliente → servidor: a mesa de som de quem apresenta.
     *
     * <p>Tocar, pausar, parar e os volumes — tudo no mesmo pacote porque e tudo a mesma coisa: o
     * ajuste que vale para o programa inteiro. Quem pode mexer e conferido do outro lado.
     */
    public record Mix(Kind kind, String url, int value, Optional<UUID> target) implements CustomPacketPayload {
        public enum Kind {
            MUSIC_PLAY,
            MUSIC_PAUSE,
            MUSIC_RESUME,
            MUSIC_STOP,
            MUSIC_VOLUME,
            VOICE_VOLUME;

            private static final Kind[] VALUES = values();

            static Kind byOrdinal(int ordinal) {
                return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : MUSIC_STOP;
            }
        }

        public static final Type<Mix> TYPE = new Type<>(NexusCore.id("radio_mix"));

        public static final StreamCodec<FriendlyByteBuf, Mix> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.kind().ordinal());
                    buf.writeUtf(payload.url(), MAX_URL);
                    buf.writeByte(Membership.clampVolume(payload.value()));
                    buf.writeBoolean(payload.target().isPresent());
                    payload.target().ifPresent(buf::writeUUID);
                },
                buf -> new Mix(Kind.byOrdinal(buf.readByte()), buf.readUtf(MAX_URL),
                        Membership.clampVolume(buf.readByte()),
                        buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty()));

        public static Mix music(Kind kind) {
            return new Mix(kind, "", 0, Optional.empty());
        }

        public static Mix play(String url) {
            return new Mix(Kind.MUSIC_PLAY, url, 0, Optional.empty());
        }

        public static Mix musicVolume(int value) {
            return new Mix(Kind.MUSIC_VOLUME, "", value, Optional.empty());
        }

        public static Mix voiceVolume(UUID voice, int value) {
            return new Mix(Kind.VOICE_VOLUME, "", value, Optional.of(voice));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private RadioPayloads() {}
}
