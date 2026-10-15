package com.nexussmp.nexuscore.morph;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O som de fundo do bicho: o mugido da vaca, o grunhido do zumbi, o chiado da aranha.
 *
 * <p>E o mesmo sorteio que o proprio bicho faz a cada tick, com o intervalo que ele pede: um contador
 * que cresce, e cada tick tem uma chance cada vez maior de tocar; tocou, o contador volta para
 * menos o intervalo do bicho. Quem toca e a criatura do anel, posta no lugar da pessoa — entao sai
 * com o volume, o tom e a categoria de som dela, e todo mundo em volta ouve, inclusive a pessoa.
 */
final class MorphSounds {
    private static final Map<UUID, Integer> AMBIENT = new HashMap<>();

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || !(MorphStands.of(player) instanceof Mob creature)) {
            return;
        }
        int time = AMBIENT.merge(player.getUUID(), 1, Integer::sum);
        if (player.getRandom().nextInt(1000) < time) {
            AMBIENT.put(player.getUUID(), -creature.getAmbientSoundInterval());
            creature.setPos(player.getX(), player.getY(), player.getZ());
            creature.playAmbientSound();
        }
    }

    static void forget(UUID player) {
        AMBIENT.remove(player);
    }

    static void clear() {
        AMBIENT.clear();
    }

    private MorphSounds() {}
}
