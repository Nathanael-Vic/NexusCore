package com.nexussmp.nexuscore.magic.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** O pacote do modulo: se esta pessoa sabe magia. */
public final class MagicPayloads {

    /**
     * Servidor -> cliente: "voce sabe magia" ou "voce nao sabe".
     *
     * <p>O cliente nao decide nada com isso — quem barra a magia e o servidor. E o que faz os nomes
     * das magias aparecerem legiveis ou embaralhados, e a barra de mana aparecer ou nao.
     */
    public record Mage(boolean mage) implements CustomPacketPayload {
        public static final Type<Mage> TYPE = new Type<>(NexusCore.id("magic_mage"));

        public static final StreamCodec<FriendlyByteBuf, Mage> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.mage()),
                buf -> new Mage(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private MagicPayloads() {}
}
