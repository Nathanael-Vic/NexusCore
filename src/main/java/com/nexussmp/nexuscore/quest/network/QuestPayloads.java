package com.nexussmp.nexuscore.quest.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Os pacotes do mural. O cliente pede; quem decide e o servidor. */
public final class QuestPayloads {
    private static final int MAX_QUESTS = 256;
    private static final int MAX_TEXT = 512;
    private static final int MAX_NAME = 64;

    /**
     * Servidor → cliente: a lista que este jogador pode ver, e se ele manda no mural.
     *
     * <p>O {@code admin} nao e permissao — quem valida tudo continua sendo o servidor. Ele so diz a
     * tela se vale desenhar as abas de administracao, que para quem nao e operador nao existiriam.
     */
    public record Sync(List<QuestView> quests, boolean admin) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(NexusCore.id("quest_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    int size = Math.min(payload.quests().size(), MAX_QUESTS);
                    buf.writeVarInt(size);
                    for (int i = 0; i < size; i++) {
                        payload.quests().get(i).write(buf);
                    }
                    buf.writeBoolean(payload.admin());
                },
                buf -> {
                    int size = buf.readVarInt();
                    if (size < 0 || size > MAX_QUESTS) {
                        throw new IllegalArgumentException("lista com " + size + " quests");
                    }
                    List<QuestView> quests = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        quests.add(QuestView.read(buf));
                    }
                    return new Sync(List.copyOf(quests), buf.readBoolean());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor → cliente: pode abrir a tela do mural (o jogador clicou num quest board valido). */
    public record OpenBoard() implements CustomPacketPayload {
        public static final Type<OpenBoard> TYPE = new Type<>(NexusCore.id("quest_open_board"));
        public static final StreamCodec<FriendlyByteBuf, OpenBoard> STREAM_CODEC =
                StreamCodec.unit(new OpenBoard());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente → servidor: assumir, desistir, apagar, concluir, entregar ou dizer que leu. */
    public record Action(Kind kind, UUID questId, Optional<UUID> target) implements CustomPacketPayload {
        public enum Kind {
            ACCEPT,
            ABANDON,
            DELETE,
            COMPLETE,
            /** "Fiz o que foi pedido." Quem confere e o autor, ou um operador. */
            SUBMIT,
            /** Voltar atras no "fiz". */
            UNSUBMIT,
            /** O livro foi aberto: apaga o selo de novidade. Nao usa {@code questId}. */
            SEEN;

            private static final Kind[] VALUES = values();

            static Kind byOrdinal(int ordinal) {
                return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ACCEPT;
            }
        }

        public static final Type<Action> TYPE = new Type<>(NexusCore.id("quest_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeByte(payload.kind().ordinal());
                    buf.writeUUID(payload.questId());
                    buf.writeBoolean(payload.target().isPresent());
                    payload.target().ifPresent(buf::writeUUID);
                },
                buf -> {
                    Kind kind = Kind.byOrdinal(buf.readByte());
                    UUID questId = buf.readUUID();
                    Optional<UUID> target = buf.readBoolean() ? Optional.of(buf.readUUID()) : Optional.empty();
                    return new Action(kind, questId, target);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente → servidor: publicar uma quest nova. O servidor corta textos grandes e valida o resto. */
    public record Create(String title, String description, String reward, int slots) implements CustomPacketPayload {
        public static final Type<Create> TYPE = new Type<>(NexusCore.id("quest_create"));
        public static final StreamCodec<FriendlyByteBuf, Create> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.title(), MAX_TEXT);
                    buf.writeUtf(payload.description(), MAX_TEXT);
                    buf.writeUtf(payload.reward(), MAX_TEXT);
                    buf.writeVarInt(payload.slots());
                },
                buf -> new Create(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Cliente → servidor: um operador entrega uma missao a alguem, pela tela.
     *
     * <p>O mesmo que {@code /nexus quests dar} faz, sem a barra de comando. O jogador vai pelo nome,
     * e nao pelo id, porque e o que o operador tem na mao ao olhar a lista de quem esta online.
     */
    public record Give(String player, String title, String description, String reward)
            implements CustomPacketPayload {
        public static final Type<Give> TYPE = new Type<>(NexusCore.id("quest_give"));
        public static final StreamCodec<FriendlyByteBuf, Give> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeUtf(payload.player(), MAX_NAME);
                    buf.writeUtf(payload.title(), MAX_TEXT);
                    buf.writeUtf(payload.description(), MAX_TEXT);
                    buf.writeUtf(payload.reward(), MAX_TEXT);
                },
                buf -> new Give(buf.readUtf(MAX_NAME), buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT),
                        buf.readUtf(MAX_TEXT)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private QuestPayloads() {}
}
