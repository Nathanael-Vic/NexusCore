package com.nexussmp.nexuscore.relic.astral;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Quem esta fora do corpo agora, e onde o corpo ficou.
 *
 * <p>Vai para o arquivo do mundo de proposito. Fora do corpo a pessoa esta em modo espectador; se o
 * servidor cair nesse meio-tempo e isto morasse so na memoria, ela voltaria espectadora para sempre,
 * longe de onde estava. Assim, ao entrar de novo, ela e devolvida ao corpo e ao modo de antes.
 */
public final class AstralSessions extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_astral";
    private static final Factory<AstralSessions> FACTORY = new Factory<>(AstralSessions::new, AstralSessions::load);

    /** Onde o corpo ficou, o modo de jogo de antes, quando saiu e qual entidade e o corpo. */
    public record Session(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch,
                          GameType previous, long startedAt, UUID body) {}

    private final Map<UUID, Session> sessions = new HashMap<>();

    public static AstralSessions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    @Nullable
    public Session get(UUID player) {
        return sessions.get(player);
    }

    public void put(UUID player, Session session) {
        sessions.put(player, session);
        setDirty();
    }

    @Nullable
    public Session remove(UUID player) {
        Session session = sessions.remove(player);
        if (session != null) {
            setDirty();
        }
        return session;
    }

    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    public void forEach(BiConsumer<UUID, Session> action) {
        Map.copyOf(sessions).forEach(action);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        sessions.forEach((player, session) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putString("dimension", session.dimension().location().toString());
            entry.putDouble("x", session.x());
            entry.putDouble("y", session.y());
            entry.putDouble("z", session.z());
            entry.putFloat("yaw", session.yaw());
            entry.putFloat("pitch", session.pitch());
            entry.putString("previous", session.previous().getName());
            entry.putLong("startedAt", session.startedAt());
            entry.putUUID("body", session.body());
            list.add(entry);
        });
        tag.put("sessions", list);
        return tag;
    }

    private static AstralSessions load(CompoundTag tag, HolderLookup.Provider registries) {
        AstralSessions data = new AstralSessions();
        for (Tag raw : tag.getList("sessions", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (!entry.hasUUID("player") || !entry.hasUUID("body") || dimension == null) {
                continue;
            }
            data.sessions.put(entry.getUUID("player"), new Session(ResourceKey.create(Registries.DIMENSION, dimension),
                    entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                    entry.getFloat("yaw"), entry.getFloat("pitch"),
                    GameType.byName(entry.getString("previous"), GameType.SURVIVAL),
                    entry.getLong("startedAt"), entry.getUUID("body")));
        }
        return data;
    }
}
