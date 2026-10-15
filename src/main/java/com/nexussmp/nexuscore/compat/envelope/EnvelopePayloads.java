package com.nexussmp.nexuscore.compat.envelope;

import com.nexussmp.nexuscore.NexusCore;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** O unico pacote do correio. */
public final class EnvelopePayloads {

    /** Servidor -> cliente: este pombo aparece como morcego (ou deixou de aparecer). */
    public record BatCourier(int entity, boolean bat) implements CustomPacketPayload {
        public static final Type<BatCourier> TYPE = new Type<>(NexusCore.id("envelope_bat_courier"));

        public static final StreamCodec<ByteBuf, BatCourier> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, BatCourier::entity,
                ByteBufCodecs.BOOL, BatCourier::bat,
                BatCourier::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private EnvelopePayloads() {}
}
