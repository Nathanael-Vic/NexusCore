package com.nexussmp.nexuscore.relic.heart;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import com.nexussmp.nexuscore.relic.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** O coracao de brasa. O que ele faz esta em {@link EmberHeart}; aqui, a dica e as cinzas. */
public final class EmberHeartItem extends RelicItem {

    public EmberHeartItem(Properties properties) {
        super(properties);
    }

    /** Em cinzas: a textura cinza sai daqui (propriedade {@code nexuscore:dormant} do modelo). */
    public static boolean dormant(Level level, ItemStack stack) {
        return !Relics.ready(level, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Level level = context.level();
        if (level != null && dormant(level, stack)) {
            lines.add(Component.translatable("nexuscore.relic.ember_heart.dormant").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Relics.restingFor(level, stack).copy().withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable("nexuscore.relic.ember_heart.tooltip").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("nexuscore.relic.ember_heart.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
        }
        RelicTooltips.relic(lines);
    }
}
