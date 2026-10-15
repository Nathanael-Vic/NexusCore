package com.nexussmp.nexuscore.backrooms;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.backrooms.entity.SkinwalkerEntity;
import com.nexussmp.nexuscore.backrooms.voice.BackroomsVoices;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem decide quando o skinwalker aparece, com que cara e de onde.
 *
 * <p>Ele nao nasce junto com a pessoa: os primeiros minutos sao so o corredor, o zumbido e a
 * sensacao de estar sozinho. E depois que essa sensacao assenta que alguem aparece ao longe.
 */
public final class SkinwalkerDirector {
    /** Distancia em que ele nasce: fora de vista, perto o bastante para achar a pessoa. */
    private static final int SPAWN_MIN = 22;
    private static final int SPAWN_MAX = 34;
    private static final int SPAWN_TRIES = 60;
    private static final int CHECK_INTERVAL_TICKS = 40;

    /** Ha quantos ticks cada pessoa esta la dentro. */
    private static final Map<UUID, Integer> TIME_INSIDE = new HashMap<>();

    public static void clear() {
        TIME_INSIDE.clear();
    }

    public static void forget(UUID player) {
        TIME_INSIDE.remove(player);
    }

    public static void tick(MinecraftServer server) {
        if (!BackroomsSettings.enabled() || !BackroomsSettings.skinwalkerEnabled()) {
            return;
        }
        ServerLevel level = server.getLevel(BackroomsDimensions.BACKROOMS);
        if (level == null || level.players().isEmpty()) {
            if (!TIME_INSIDE.isEmpty()) {
                TIME_INSIDE.clear();
            }
            return;
        }
        for (ServerPlayer player : List.copyOf(level.players())) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            int inside = TIME_INSIDE.merge(player.getUUID(), 1, Integer::sum);
            if (inside % CHECK_INTERVAL_TICKS != 0
                    || inside < BackroomsSettings.skinwalkerDelayTicks()
                    || hasSkinwalker(level, player)) {
                continue;
            }
            spawnFor(level, player);
        }
        // Faxina no mesmo ritmo da checagem: quem saiu perde o tempo acumulado e, se voltar,
        // recomeca a contagem. Fazer isso a cada tick seria uma busca de jogador por tick a toa.
        if (server.getTickCount() % CHECK_INTERVAL_TICKS == 0) {
            TIME_INSIDE.keySet().removeIf(id -> {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                return player == null || !BackroomsRules.inBackrooms(player.level());
            });
        }
    }

    private static boolean hasSkinwalker(ServerLevel level, ServerPlayer player) {
        AABB around = player.getBoundingBox().inflate(128.0);
        return !level.getEntitiesOfClass(SkinwalkerEntity.class, around).isEmpty();
    }

    private static void spawnFor(ServerLevel level, ServerPlayer victim) {
        summon(level, victim);
    }

    /** Chama um agora para esta pessoa, trocando o que ja estivesse rondando. */
    public static boolean summon(ServerLevel level, ServerPlayer victim) {
        BlockPos where = findSpot(level, victim);
        if (where == null) {
            return false;
        }
        removeFor(level, victim);
        return SkinwalkerEntity.spawn(level, victim, where, pickSkin(level, victim)) != null;
    }

    /**
     * De quem ele vai copiar a cara.
     *
     * <p>A primeira escolha e alguem cuja voz esta gravada — assim a imitacao vem completa, cara e
     * voz da mesma pessoa. Depois, qualquer outro que esteja online. Por ultimo, a propria vitima:
     * ver a si mesmo no fim do corredor tambem funciona muito bem.
     *
     * <p>So gente online entra no sorteio, inclusive entre as vozes gravadas. O perfil de quem
     * esta jogando e o unico que o servidor tem por inteiro — com nome e textura de verdade, ja
     * passados por mods de skin. Copiar alguem que saiu daria nome de um e cara de outro.
     */
    private static GameProfile pickSkin(ServerLevel level, ServerPlayer victim) {
        List<ServerPlayer> others = new ArrayList<>(level.getServer().getPlayerList().getPlayers());
        others.removeIf(player -> player.getUUID().equals(victim.getUUID()));

        List<UUID> voices = BackroomsVoices.voices();
        List<ServerPlayer> withVoice = new ArrayList<>(others.size());
        for (ServerPlayer other : others) {
            if (voices.contains(other.getUUID())) {
                withVoice.add(other);
            }
        }

        List<ServerPlayer> pool = withVoice.isEmpty() ? others : withVoice;
        ServerPlayer chosen = pool.isEmpty() ? victim : pool.get(level.getRandom().nextInt(pool.size()));
        return chosen.getGameProfile();
    }

    /** Um canto de chao livre a uma boa distancia, com espaco de pe. */
    @Nullable
    private static BlockPos findSpot(ServerLevel level, ServerPlayer victim) {
        for (int attempt = 0; attempt < SPAWN_TRIES; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0;
            double distance = Mth.nextInt(level.getRandom(), SPAWN_MIN, SPAWN_MAX);
            BlockPos spot = BlockPos.containing(
                    victim.getX() + Math.cos(angle) * distance,
                    BackroomsDimensions.FLOOR_Y + 1,
                    victim.getZ() + Math.sin(angle) * distance);
            if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()
                    && !level.getBlockState(spot.below()).isAir()) {
                return spot;
            }
        }
        return null;
    }

    /** Some com quem estiver rondando um jogador que saiu. */
    public static void removeFor(ServerLevel level, Entity player) {
        for (SkinwalkerEntity skinwalker : level.getEntitiesOfClass(SkinwalkerEntity.class,
                player.getBoundingBox().inflate(128.0))) {
            skinwalker.discard();
        }
    }

    private SkinwalkerDirector() {}
}
