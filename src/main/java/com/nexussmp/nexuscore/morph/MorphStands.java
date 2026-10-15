package com.nexussmp.nexuscore.morph;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A criatura solta de cada pessoa transformada em bicho, dos dois lados.
 *
 * <p>E a quem se pergunta tudo o que depende do bicho: que som ele faz ao levar dano, ao morrer, ao
 * pisar e a toa, em que volume, se voa, se nada. No cliente, e tambem ela que e desenhada.
 *
 * <p>Uma por pessoa, criada na primeira pergunta e trocada quando a forma muda ou a pessoa troca de
 * mundo. Um tipo que nao da para criar fica guardado como "nenhuma" — sem isso, a criacao seria
 * tentada de novo a cada som e a cada quadro.
 */
public final class MorphStands {
    private static final Map<UUID, Stand> SERVER = new HashMap<>();
    private static final Map<UUID, Stand> CLIENT = new HashMap<>();

    private record Stand(MorphForm form, Level level, @Nullable LivingEntity entity) {}

    /** A criatura desta pessoa; {@code null} para quem nao esta transformado em bicho. */
    @Nullable
    public static LivingEntity of(Player player) {
        MorphForm form = MorphState.form(player);
        if (form == null || form.isPlayer()) {
            return null;
        }
        Map<UUID, Stand> side = player.level().isClientSide() ? CLIENT : SERVER;
        Stand stand = side.get(player.getUUID());
        if (stand == null || !stand.form().equals(form) || stand.level() != player.level()) {
            stand = new Stand(form, player.level(), MorphEntities.create(player.level(), form));
            side.put(player.getUUID(), stand);
        }
        return stand.entity();
    }

    static void forget(UUID player) {
        SERVER.remove(player);
    }

    static void clearServer() {
        SERVER.clear();
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    private MorphStands() {}
}
