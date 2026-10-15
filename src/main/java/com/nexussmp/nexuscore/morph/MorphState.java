package com.nexussmp.nexuscore.morph;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quem esta transformado em que, de cada lado.
 *
 * <p>Sao dois mapas, e nao um, por causa do mundo de um jogador so: la o servidor e o cliente rodam
 * no mesmo programa, e cada um precisa da sua visao. O do servidor e a verdade — sai do anel que a
 * pessoa esta vestindo ({@link MorphEquip}). O do cliente e o que o servidor contou.
 *
 * <p><b>O tamanho.</b> A hitbox e a altura dos olhos sao as da criatura, e os dois lados precisam
 * concordar exatamente, senao o servidor corrige o movimento a cada passo. Por isso os dois fazem a
 * mesma conta, pelo mesmo caminho: criam a criatura com o NBT do anel ({@link MorphEntities}) e
 * perguntam o tamanho a ela. Filhote sai pequeno, slime sai do tamanho que era.
 */
public final class MorphState {
    private static final Map<UUID, Active> SERVER = new HashMap<>();
    private static final Map<UUID, Active> CLIENT = new HashMap<>();

    /** A forma e o tamanho dela; sem tamanho quando a forma e de jogador (fica o de jogador). */
    private record Active(MorphForm form, @Nullable EntityDimensions dimensions) {}

    @Nullable
    public static MorphForm form(Player player) {
        Active active = side(player.level()).get(player.getUUID());
        return active == null ? null : active.form();
    }

    /** O tamanho que aquela forma teria. {@code null} para forma de jogador. */
    @Nullable
    static EntityDimensions dimensionsOf(Level level, MorphForm form) {
        LivingEntity creature = MorphEntities.create(level, form);
        return creature == null ? null : creature.getDimensions(Pose.STANDING);
    }

    /** Troca a forma de alguem do lado em que esta entidade vive, e ajusta o tamanho na hora. */
    static void set(Player player, @Nullable MorphForm form) {
        put(side(player.level()), player.level(), player.getUUID(), form);
        player.refreshDimensions();
    }

    /**
     * O cliente pode saber a forma de alguem antes de a entidade dele existir aqui — ou depois de ela
     * sumir e voltar. Guarda por id; quem estiver no mundo ajusta o tamanho agora.
     */
    public static void setClient(Level level, UUID player, @Nullable MorphForm form) {
        put(CLIENT, level, player, form);
        Player entity = level.getPlayerByUUID(player);
        if (entity != null) {
            entity.refreshDimensions();
        }
    }

    private static void put(Map<UUID, Active> map, Level level, UUID player, @Nullable MorphForm form) {
        if (form == null) {
            map.remove(player);
        } else {
            map.put(player, new Active(form, dimensionsOf(level, form)));
        }
    }

    /**
     * O jogo pergunta o tamanho de uma entidade a cada mudanca de pose. Para quem esta transformado,
     * a resposta e o tamanho da criatura em qualquer pose — agachado, nadando ou planando, um lobo
     * continua do tamanho de um lobo. Dormir fica de fora: a cama precisa do tamanho de jogador.
     */
    static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player) || event.getPose() == Pose.SLEEPING) {
            return;
        }
        Active active = side(player.level()).get(player.getUUID());
        if (active != null && active.dimensions() != null) {
            event.setNewSize(active.dimensions());
        }
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

    private static Map<UUID, Active> side(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    private MorphState() {}
}
