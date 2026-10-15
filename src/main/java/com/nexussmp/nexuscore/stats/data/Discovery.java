package com.nexussmp.nexuscore.stats.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Quem descobriu e quando. O nome e guardado junto para o painel e o chat nao precisarem
 * procurar jogadores offline.
 */
public record Discovery(UUID player, String playerName, long gameTime, long epochMillis) {

    public static Discovery of(ServerPlayer player) {
        return new Discovery(player.getUUID(), player.getGameProfile().getName(),
                player.serverLevel().getGameTime(), System.currentTimeMillis());
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", player);
        tag.putString("name", playerName);
        tag.putLong("gameTime", gameTime);
        tag.putLong("time", epochMillis);
        return tag;
    }

    static Discovery load(CompoundTag tag) {
        UUID uuid = tag.hasUUID("player") ? tag.getUUID("player") : new UUID(0, 0);
        return new Discovery(uuid, tag.getString("name"), tag.getLong("gameTime"), tag.getLong("time"));
    }
}
