package com.nexussmp.nexuscore.monolith;

import com.nexussmp.nexuscore.NexusCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Os pacotes dos monolitos. O cliente nunca fica sabendo onde eles estao: so o quanto o mundo esta
 * errado onde a pessoa pisa, e quais criaturas sao "sem textura".
 */
public final class MonolithPayloads {

    /** Servidor -> cliente: a intensidade do eco onde o jogador esta, de 0 a 1. */
    public record Intensity(float value) implements CustomPacketPayload {
        public static final Type<Intensity> TYPE = new Type<>(NexusCore.id("monolith_intensity"));
        public static final StreamCodec<ByteBuf, Intensity> STREAM_CODEC =
                ByteBufCodecs.FLOAT.map(Intensity::new, Intensity::value);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Servidor -> cliente: esta criatura se desenha com a textura faltando. */
    public record Glitched(int entityId) implements CustomPacketPayload {
        public static final Type<Glitched> TYPE = new Type<>(NexusCore.id("monolith_glitched"));
        public static final StreamCodec<ByteBuf, Glitched> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(Glitched::new, Glitched::entityId);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private MonolithPayloads() {}
}
