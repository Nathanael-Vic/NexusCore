package com.nexussmp.nexuscore.hologram.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Servidor → cliente: comeca o audio do holograma, preso a entidade (o som segue e para junto com ela).
 *
 * <p>O som e identificado por id de recurso e tocado direto pelo cliente, entao qualquer arquivo novo
 * no resource pack funciona sem precisar registrar nada no mod nem atualizar o jar.
 *
 * <p>{@code elapsedTicks} diz quanto tempo de apresentacao ja passou: quem chega no meio nao toca o audio
 * do comeco por cima do que os outros ja estao ouvindo.
 */
public record HologramAudioPayload(int entityId, ResourceLocation sound, float volume, float pitch, int elapsedTicks)
        implements CustomPacketPayload {

    public static final Type<HologramAudioPayload> TYPE = new Type<>(NexusCore.id("hologram_audio"));

    public static final StreamCodec<FriendlyByteBuf, HologramAudioPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeResourceLocation(payload.sound());
                buf.writeFloat(payload.volume());
                buf.writeFloat(payload.pitch());
                buf.writeVarInt(payload.elapsedTicks());
            },
            buf -> new HologramAudioPayload(buf.readVarInt(), buf.readResourceLocation(),
                    buf.readFloat(), buf.readFloat(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
