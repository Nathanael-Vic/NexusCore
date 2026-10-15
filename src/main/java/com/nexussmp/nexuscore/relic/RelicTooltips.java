package com.nexussmp.nexuscore.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** As linhas que toda reliquia poe na dica: o "reliquia" embaixo e quanto falta para descansar. */
public final class RelicTooltips {

    /** "Descansando: 2 min." enquanto a reliquia espera; nada quando esta pronta. */
    public static void resting(ItemStack stack, Item.TooltipContext context, List<Component> lines) {
        Level level = context.level();
        if (level != null && !Relics.ready(level, stack)) {
            lines.add(Relics.restingFor(level, stack).copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        relic(lines);
    }

    /** A marca de reliquia, a ultima linha: lembra que ela nao cai na morte. */
    public static void relic(List<Component> lines) {
        lines.add(Component.translatable("nexuscore.relic.tooltip").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    private RelicTooltips() {}
}
