package com.nexussmp.nexuscore.relic.refuge;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O que o mundo lembra dos refugios: a sala de cada dono, quais salas ja foram erguidas, e de onde
 * cada pessoa entrou (para a porta de saida saber para onde mandar).
 */
public final class RefugeData extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_refuges";
    private static final Factory<RefugeData> FACTORY = new Factory<>(RefugeData::new, RefugeData::load);

    /** De onde alguem entrou: o mundo, o lugar e para onde olhava. */
    public record ReturnPoint(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {}

    /** dono -> numero da sala. A sala n fica em x = n * espacamento. */
    private final Map<UUID, Integer> rooms = new HashMap<>();
    private final Set<Integer> built = new HashSet<>();
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();
    private int nextRoom;

    public static RefugeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    /** A sala deste dono; a primeira vez, ganha a proxima livre. */
    public int roomOf(UUID owner) {
        Integer room = rooms.get(owner);
        if (room == null) {
            room = nextRoom++;
            rooms.put(owner, room);
            setDirty();
        }
        return room;
    }

    public boolean isBuilt(int room) {
        return built.contains(room);
    }

    public void markBuilt(int room) {
        if (built.add(room)) {
            setDirty();
        }
    }

    /** Guarda de onde a pessoa entrou — so a primeira entrada vale, ate ela sair. */
    public void rememberReturn(UUID player, ReturnPoint point) {
        if (!returns.containsKey(player)) {
            returns.put(player, point);
            setDirty();
        }
    }

    @Nullable
    public ReturnPoint takeReturn(UUID player) {
        ReturnPoint point = returns.remove(player);
        if (point != null) {
            setDirty();
        }
        return point;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag roomList = new ListTag();
        rooms.forEach((owner, room) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putInt("room", room);
            roomList.add(entry);
        });
        tag.put("rooms", roomList);
        tag.putIntArray("built", built.stream().mapToInt(Integer::intValue).toArray());
        tag.putInt("next", nextRoom);
        ListTag returnList = new ListTag();
        returns.forEach((player, point) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putString("dimension", point.dimension().location().toString());
            entry.putDouble("x", point.x());
            entry.putDouble("y", point.y());
            entry.putDouble("z", point.z());
            entry.putFloat("yaw", point.yaw());
            entry.putFloat("pitch", point.pitch());
            returnList.add(entry);
        });
        tag.put("returns", returnList);
        return tag;
    }

    private static RefugeData load(CompoundTag tag, HolderLookup.Provider registries) {
        RefugeData data = new RefugeData();
        for (Tag raw : tag.getList("rooms", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID("owner")) {
                data.rooms.put(entry.getUUID("owner"), entry.getInt("room"));
            }
        }
        for (int room : tag.getIntArray("built")) {
            data.built.add(room);
        }
        data.nextRoom = tag.getInt("next");
        for (Tag raw : tag.getList("returns", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (entry.hasUUID("player") && dimension != null) {
                data.returns.put(entry.getUUID("player"), new ReturnPoint(ResourceKey.create(Registries.DIMENSION, dimension),
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                        entry.getFloat("yaw"), entry.getFloat("pitch")));
            }
        }
        return data;
    }
}
