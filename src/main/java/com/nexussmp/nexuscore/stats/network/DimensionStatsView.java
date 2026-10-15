package com.nexussmp.nexuscore.stats.network;

import com.nexussmp.nexuscore.stats.data.RecentDiscovery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * O que o cliente precisa saber de uma dimensao para desenhar o painel: so o que ja foi descoberto.
 *
 * <p>Nada de totais. A dimensao e desconhecida de proposito: os numeros so sobem, e o total nao pode
 * nem chegar ao cliente, senao daria para ler com um mod de debug.
 * Lido e escrito com limites, para um pacote malformado nao alocar sem fim.
 */
public record DimensionStatsView(
        ResourceLocation dimension,
        int areaCenterX,
        int areaCenterZ,
        int areaRadius,
        List<Entry> biomes,
        List<Entry> structureTypes,
        int structureCount,
        List<Recent> recent) {

    static final int MAX_IDS = 4096;
    static final int MAX_RECENT = 64;
    private static final int MAX_NAME = 64;

    /** Algo descoberto: id, pioneiro, quando, e quantas instancias (so estruturas; 0 para biomas). */
    public record Entry(ResourceLocation id, String pioneer, long epochMillis, int count) {}

    public record Recent(RecentDiscovery.Kind kind, ResourceLocation id, String playerName, long epochMillis) {}

    void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension);
        buf.writeVarInt(areaCenterX);
        buf.writeVarInt(areaCenterZ);
        buf.writeVarInt(areaRadius);
        writeList(buf, biomes, MAX_IDS, DimensionStatsView::writeEntry);
        writeList(buf, structureTypes, MAX_IDS, DimensionStatsView::writeEntry);
        buf.writeVarInt(structureCount);
        writeList(buf, recent, MAX_RECENT, (out, entry) -> {
            out.writeByte(entry.kind().ordinal());
            out.writeResourceLocation(entry.id());
            out.writeUtf(entry.playerName(), MAX_NAME);
            out.writeLong(entry.epochMillis());
        });
    }

    static DimensionStatsView read(FriendlyByteBuf buf) {
        return new DimensionStatsView(
                buf.readResourceLocation(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                readList(buf, MAX_IDS, DimensionStatsView::readEntry),
                readList(buf, MAX_IDS, DimensionStatsView::readEntry),
                buf.readVarInt(),
                readList(buf, MAX_RECENT, in -> new Recent(RecentDiscovery.Kind.byOrdinal(in.readByte()),
                        in.readResourceLocation(), in.readUtf(MAX_NAME), in.readLong())));
    }

    private static void writeEntry(FriendlyByteBuf buf, Entry entry) {
        buf.writeResourceLocation(entry.id());
        buf.writeUtf(entry.pioneer(), MAX_NAME);
        buf.writeLong(entry.epochMillis());
        buf.writeVarInt(entry.count());
    }

    private static Entry readEntry(FriendlyByteBuf buf) {
        return new Entry(buf.readResourceLocation(), buf.readUtf(MAX_NAME), buf.readLong(), buf.readVarInt());
    }

    static <T> void writeList(FriendlyByteBuf buf, List<T> list, int max, BiConsumer<FriendlyByteBuf, T> writer) {
        int size = Math.min(list.size(), max);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            writer.accept(buf, list.get(i));
        }
    }

    static <T> List<T> readList(FriendlyByteBuf buf, int max, Function<FriendlyByteBuf, T> reader) {
        int size = buf.readVarInt();
        if (size < 0 || size > max) {
            throw new IllegalArgumentException("lista com " + size + " itens (maximo " + max + ")");
        }
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(reader.apply(buf));
        }
        return List.copyOf(list);
    }
}
