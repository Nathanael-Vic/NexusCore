package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.vampire.network.VampirePayloads;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A porta de entrada para o Vampirism — e a unica daqui para fora.
 *
 * <p>Nenhuma assinatura aqui menciona uma classe do Vampirism, de proposito: assim esta classe
 * carrega mesmo sem o mod instalado, e quem so quer perguntar "essa pessoa e vampira?" nao precisa
 * saber se o mod existe. O contato de verdade fica no {@code VampirismBridge}, que so e tocado
 * depois de {@link #installed()} dizer que sim — e por isso a maquina nunca tenta carregar uma
 * classe que nao esta la.
 */
public final class Vampires {
    public static final String MOD_ID = "vampirism";
    /** Nivel de operador para mexer em lorde, tornar e curar. */
    public static final int MODERATOR_LEVEL = 2;
    /** O teto de quem nao e lorde: vampiro, e so. */
    public static final int CAPPED_LEVEL = 1;

    /** Resolvido uma vez: a lista de mods nao muda com o jogo em pe. */
    private static Boolean installed;

    public static boolean installed() {
        if (installed == null) {
            installed = ModList.get().isLoaded(MOD_ID);
        }
        return installed;
    }

    /** Se esta pessoa pode progredir como vampiro. No cliente, quem sabe disso e o VampireClient. */
    public static boolean isLord(MinecraftServer server, Player player) {
        return VampireLords.get(server).isLord(player.getUUID());
    }

    public static boolean isVampire(Player player) {
        return installed() && VampirismBridge.isVampire(player);
    }

    /** Nivel de vampiro, ou 0 para quem nao e. */
    public static int level(Player player) {
        return installed() ? VampirismBridge.level(player) : 0;
    }

    /** Torna a pessoa vampira de nivel 1. {@code false} se ja era, ou se o mod nao esta la. */
    public static boolean turn(ServerPlayer player) {
        if (!installed() || VampirismBridge.isVampire(player)) {
            return false;
        }
        return VampirismBridge.setLevel(player, CAPPED_LEVEL);
    }

    /** Devolve a pessoa a condicao humana. {@code false} se ja era humana. */
    public static boolean cure(ServerPlayer player) {
        if (!installed() || !VampirismBridge.isVampire(player)) {
            return false;
        }
        VampirismBridge.forget(player);
        return VampirismBridge.setLevel(player, 0);
    }

    /**
     * Puxa a pessoa de volta ao nivel 1 e apaga o que ela tinha aprendido.
     *
     * <p>E o que acontece com um lorde rebaixado: o poder que veio do posto vai junto com o posto.
     * Quem nao e vampiro nao sente nada.
     */
    public static void demote(ServerPlayer player) {
        if (!installed() || !VampirismBridge.isVampire(player)) {
            return;
        }
        VampirismBridge.forget(player);
        if (VampirismBridge.level(player) > CAPPED_LEVEL) {
            VampirismBridge.setLevel(player, CAPPED_LEVEL);
        }
    }

    /** Conta ao cliente daquela pessoa se ela e lorde — e o que decide o botao no inventario. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new VampirePayloads.Lord(isLord(player.server, player)));
    }

    private Vampires() {}
}
