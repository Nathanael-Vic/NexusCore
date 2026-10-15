package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.NexusCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Optional;
import java.util.UUID;

/** O unico pacote do modulo. */
public final class MorphPayloads {

    /**
     * Servidor -> cliente: a forma de um jogador agora, ou nenhuma.
     *
     * <p>Vai para quem se transformou e para todo mundo que esta vendo essa pessoa, e de novo para
     * cada um que passa a ver — inclusive "nenhuma", que e o que apaga do cliente a forma velha de
     * quem saiu e voltou sem o anel.
     */
    public record Sync(UUID player, Optional<MorphForm> form) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(NexusCore.id("morph_sync"));

        public static final StreamCodec<ByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Sync::player,
                ByteBufCodecs.optional(MorphForm.STREAM_CODEC), Sync::form,
                Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private MorphPayloads() {}
}
