package com.nexussmp.nexuscore.relic.astral;

import com.nexussmp.nexuscore.NexusCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Os pacotes da mascara astral. */
public final class AstralPayloads {

    /**
     * Servidor -> cliente: saiu do corpo (e quanto tempo aguenta la fora), ou voltou.
     *
     * <p>O cliente conta o tempo sozinho a partir daqui: e o relogio da loucura que vai tomando a tela.
     */
    public record State(boolean active, int maxTicks) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(NexusCore.id("astral_state"));

        public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, State::active,
                ByteBufCodecs.VAR_INT, State::maxTicks,
                State::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cliente -> servidor: apertou a tecla de voltar ao corpo. */
    public record Return() implements CustomPacketPayload {
        public static final Type<Return> TYPE = new Type<>(NexusCore.id("astral_return"));
        public static final StreamCodec<ByteBuf, Return> STREAM_CODEC = StreamCodec.unit(new Return());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private AstralPayloads() {}
}
