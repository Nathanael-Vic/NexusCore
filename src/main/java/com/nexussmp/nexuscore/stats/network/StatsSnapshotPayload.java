package com.nexussmp.nexuscore.stats.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Servidor → cliente: estado atual das estatisticas de todas as dimensoes rastreadas. */
public record StatsSnapshotPayload(List<DimensionStatsView> dimensions) implements CustomPacketPayload {
    private static final int MAX_DIMENSIONS = 16;

    public static final Type<StatsSnapshotPayload> TYPE = new Type<>(NexusCore.id("stats_snapshot"));

    public static final StreamCodec<FriendlyByteBuf, StatsSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> DimensionStatsView.writeList(buf, payload.dimensions(), MAX_DIMENSIONS, (out, view) -> view.write(out)),
            buf -> new StatsSnapshotPayload(DimensionStatsView.readList(buf, MAX_DIMENSIONS, DimensionStatsView::read)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
