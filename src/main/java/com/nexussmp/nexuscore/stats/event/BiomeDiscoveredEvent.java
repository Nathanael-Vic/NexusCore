package com.nexussmp.nexuscore.stats.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Um jogador entrou num bioma pela primeira vez (dele). Disparado no {@code NeoForge.EVENT_BUS}, so no servidor.
 * Anuncios, recompensas e leaderboards escutam isto; o rastreador nao sabe que eles existem.
 */
public final class BiomeDiscoveredEvent extends Event {
    private final ServerPlayer player;
    private final ResourceKey<Level> dimension;
    private final ResourceLocation biome;
    private final boolean firstInServer;

    public BiomeDiscoveredEvent(ServerPlayer player, ResourceKey<Level> dimension, ResourceLocation biome, boolean firstInServer) {
        this.player = player;
        this.dimension = dimension;
        this.biome = biome;
        this.firstInServer = firstInServer;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public ResourceLocation biome() {
        return biome;
    }

    /** {@code true} se ninguem no servidor tinha descoberto este bioma antes (pioneiro). */
    public boolean firstInServer() {
        return firstInServer;
    }
}
