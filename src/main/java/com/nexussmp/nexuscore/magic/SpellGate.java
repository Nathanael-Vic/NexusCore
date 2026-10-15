package com.nexussmp.nexuscore.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A porta que os mixins do Iron's Spells ({@code mixin/magic}) consultam.
 *
 * <p>O Iron's Spells ja tem, pronto, o comportamento que o servidor quer para todas as magias: as
 * magias eldritch comecam <b>desconhecidas</b> — o nome embaralhado, os numeros escondidos, e quem
 * tenta lancar ouve que nao entende aquilo. Aqui a mesma regra passa a valer para <b>todas</b> as
 * magias, para quem nao e mago. Para o mago, nada muda: as eldritch continuam pedindo o manuscrito,
 * como no mod.
 */
public final class SpellGate {
    public static final String CANNOT_CAST = "nexuscore.magic.cannot_cast";

    /** Nome e numeros da magia embaralhados para esta pessoa. */
    public static boolean hidesSpells(Player player) {
        return !Mages.isMage(player);
    }

    public static boolean mayCast(Player player) {
        return Mages.isMage(player);
    }

    /**
     * A frase de recusa — o Iron's Spells a mostra na barra de acao. No servidor, junto vem o chiado
     * ({@link MagicFx}).
     */
    public static Component refuse(Player player) {
        if (player instanceof ServerPlayer server) {
            MagicFx.fizzle(server);
        }
        return Component.translatable(CANNOT_CAST).withStyle(ChatFormatting.DARK_PURPLE);
    }

    private SpellGate() {}
}
