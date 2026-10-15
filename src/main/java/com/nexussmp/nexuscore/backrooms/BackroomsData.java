package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que precisa sobreviver ao desligar do servidor: os andares gerados e, para cada pessoa que esta
 * la dentro, o lugar exato de onde ela caiu.
 *
 * <p>Fica no mundo principal (e nao na dimensao das backrooms) de proposito: o ponto de volta e uma
 * informacao do jogador, e continua valendo mesmo se o andar for apagado.
 */
public final class BackroomsData extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_backrooms";
    private static final Factory<BackroomsData> FACTORY = new Factory<>(BackroomsData::new, BackroomsData::load);
    /** Distancia entre andares. Longe o bastante para um nunca encostar no outro. */
    private static final int AREA_SPACING = 4096;
    /** A "ultima queda" de quem nunca caiu. */
    public static final long NEVER = Long.MIN_VALUE;

    private final List<BackroomsArea> areas = new ArrayList<>();
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();
    /** Quando cada pessoa caiu pela ultima vez, em tempo de mundo: e o tempo de espera do evento. */
    private final Map<UUID, Long> lastEvent = new HashMap<>();
    /** Onde ficaram as cabecas de quem foi pego. Guardadas para dar para limpar depois. */
    private final List<BlockPos> heads = new ArrayList<>();

    /** De onde a pessoa caiu: dimensao, posicao e para onde estava olhando. */
    public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimension.location().toString());
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putFloat("yaw", yaw);
            tag.putFloat("pitch", pitch);
            return tag;
        }

        static ReturnPoint load(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
            return new ReturnPoint(
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            id == null ? Level.OVERWORLD.location() : id),
                    tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                    tag.getFloat("yaw"), tag.getFloat("pitch"));
        }
    }

    public static BackroomsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public List<BackroomsArea> areas() {
        return areas;
    }

    /** O andar mais novo: e nele que as pessoas caem. */
    @Nullable
    public BackroomsArea newest() {
        return areas.isEmpty() ? null : areas.get(areas.size() - 1);
    }

    @Nullable
    public BackroomsArea areaAt(int x, int z) {
        for (BackroomsArea area : areas) {
            if (area.contains(x, z)) {
                return area;
            }
        }
        return null;
    }

    /** Proximo canto livre: os andares ficam numa fila, bem separados um do outro. */
    public int nextOriginX() {
        return areas.size() * AREA_SPACING;
    }

    public void addArea(BackroomsArea area) {
        areas.add(area);
        setDirty();
    }

    public boolean removeArea(BackroomsArea area) {
        boolean removed = areas.remove(area);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void setReturn(UUID player, ReturnPoint point) {
        returns.put(player, point);
        setDirty();
    }

    @Nullable
    public ReturnPoint takeReturn(UUID player) {
        ReturnPoint point = returns.remove(player);
        if (point != null) {
            setDirty();
        }
        return point;
    }

    @Nullable
    public ReturnPoint peekReturn(UUID player) {
        return returns.get(player);
    }

    public void rememberHead(BlockPos pos) {
        heads.add(pos);
        setDirty();
    }

    public List<BlockPos> heads() {
        return heads;
    }

    public void forgetHeads() {
        if (!heads.isEmpty()) {
            heads.clear();
            setDirty();
        }
    }

    /** Esquece so as cabecas que ficaram dentro deste andar. */
    public void forgetHeadsIn(BackroomsArea area) {
        if (heads.removeIf(head -> area.contains(head.getX(), head.getZ()))) {
            setDirty();
        }
    }

    public long lastEvent(UUID player) {
        return lastEvent.getOrDefault(player, NEVER);
    }

    public void markEvent(UUID player, long gameTime) {
        lastEvent.put(player, gameTime);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag areaList = new ListTag();
        for (BackroomsArea area : areas) {
            areaList.add(area.save());
        }
        tag.put("areas", areaList);

        ListTag returnList = new ListTag();
        returns.forEach((player, point) -> {
            CompoundTag entry = point.save();
            entry.putUUID("player", player);
            returnList.add(entry);
        });
        tag.put("returns", returnList);

        ListTag eventList = new ListTag();
        lastEvent.forEach((player, time) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putLong("time", time);
            eventList.add(entry);
        });
        tag.put("lastEvents", eventList);

        long[] headPositions = new long[heads.size()];
        for (int i = 0; i < heads.size(); i++) {
            headPositions[i] = heads.get(i).asLong();
        }
        tag.putLongArray("heads", headPositions);
        return tag;
    }

    private static BackroomsData load(CompoundTag tag, HolderLookup.Provider registries) {
        BackroomsData data = new BackroomsData();
        for (Tag entry : tag.getList("areas", Tag.TAG_COMPOUND)) {
            data.areas.add(BackroomsArea.load((CompoundTag) entry));
        }
        for (Tag entry : tag.getList("returns", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            data.returns.put(compound.getUUID("player"), ReturnPoint.load(compound));
        }
        for (Tag entry : tag.getList("lastEvents", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            data.lastEvent.put(compound.getUUID("player"), compound.getLong("time"));
        }
        for (long head : tag.getLongArray("heads")) {
            data.heads.add(BlockPos.of(head));
        }
        return data;
    }
}
