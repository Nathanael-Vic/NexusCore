package com.nexussmp.nexuscore.stats.client;

import com.nexussmp.nexuscore.stats.network.DimensionStatsView;
import com.nexussmp.nexuscore.stats.network.StatsSnapshotPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ultimo snapshot recebido do servidor. Usado so no cliente, mas sem tipos de cliente,
 * para o handler do pacote poder ser registrado em codigo comum.
 */
public final class ClientStatsCache {
    private static List<DimensionStatsView> dimensions = List.of();
    private static boolean received;
    /** Sobe a cada snapshot; o painel refaz o layout so quando isto muda. */
    private static int version;

    public static void accept(StatsSnapshotPayload payload) {
        dimensions = payload.dimensions();
        received = true;
        version++;
    }

    public static void clear() {
        dimensions = List.of();
        received = false;
        version++;
    }

    public static int version() {
        return version;
    }

    public static boolean received() {
        return received;
    }

    /** A dimensao pedida, ou a primeira rastreada quando {@code id} e {@code null}. */
    @Nullable
    public static DimensionStatsView get(@Nullable ResourceLocation id) {
        if (id == null) {
            return dimensions.isEmpty() ? null : dimensions.get(0);
        }
        for (DimensionStatsView view : dimensions) {
            if (view.dimension().equals(id)) {
                return view;
            }
        }
        return null;
    }

    private ClientStatsCache() {}
}
