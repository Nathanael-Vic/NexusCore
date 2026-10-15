package com.nexussmp.nexuscore.stats.event;

import com.nexussmp.nexuscore.stats.data.StructureInstanceId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Um jogador entrou numa estrutura que ainda nao tinha contado para ele.
 * Disparado no {@code NeoForge.EVENT_BUS}, so no servidor.
 */
public final class StructureDiscoveredEvent extends Event {
    private final ServerPlayer player;
    private final ResourceKey<Level> dimension;
    private final StructureInstanceId instance;
    private final boolean firstTypeInServer;
    private final boolean firstInstanceInServer;
    private final boolean firstInstanceForPlayer;

    public StructureDiscoveredEvent(ServerPlayer player, ResourceKey<Level> dimension, StructureInstanceId instance,
                                    boolean firstTypeInServer, boolean firstInstanceInServer, boolean firstInstanceForPlayer) {
        this.player = player;
        this.dimension = dimension;
        this.instance = instance;
        this.firstTypeInServer = firstTypeInServer;
        this.firstInstanceInServer = firstInstanceInServer;
        this.firstInstanceForPlayer = firstInstanceForPlayer;
    }

    public ServerPlayer player() {
        return player;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public ResourceLocation structureType() {
        return instance.type();
    }

    public StructureInstanceId instance() {
        return instance;
    }

    /** Ninguem no servidor tinha achado este tipo de estrutura (pioneiro do tipo). */
    public boolean firstTypeInServer() {
        return firstTypeInServer;
    }

    /** Ninguem no servidor tinha entrado nesta estrutura especifica. */
    public boolean firstInstanceInServer() {
        return firstInstanceInServer;
    }

    /** O jogador nunca tinha entrado nesta estrutura especifica (ja pode ter visto outras do mesmo tipo). */
    public boolean firstInstanceForPlayer() {
        return firstInstanceForPlayer;
    }
}
