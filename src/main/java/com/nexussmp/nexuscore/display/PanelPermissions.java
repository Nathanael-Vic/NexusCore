package com.nexussmp.nexuscore.display;

import com.nexussmp.nexuscore.core.config.ServerConfig;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Quem pode colocar e configurar paineis. Funciona nos dois lados: o NeoForge manda a config do servidor
 * ao cliente, e o cliente conhece o proprio nivel de operador.
 */
public final class PanelPermissions {
    private static final int OPERATOR_LEVEL = 2;

    public static boolean canPlace(@Nullable Player player) {
        return player == null || allowed(player);
    }

    public static boolean canConfigure(Player player) {
        return allowed(player);
    }

    /** Criativo tambem pode: no servidor so um operador coloca alguem no criativo, e o singleplayer sem cheats continua funcionando. */
    private static boolean allowed(Player player) {
        return !ServerConfig.get(ServerConfig.PANEL_REQUIRE_OPERATOR)
                || player.hasPermissions(OPERATOR_LEVEL)
                || player.getAbilities().instabuild;
    }

    private PanelPermissions() {}
}
