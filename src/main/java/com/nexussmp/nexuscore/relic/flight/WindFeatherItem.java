package com.nexussmp.nexuscore.relic.flight;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A pena dos ventos. O que ela faz esta em {@link WindFeather}. */
public final class WindFeatherItem extends RelicItem {

    public WindFeatherItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.wind_feather.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("nexuscore.relic.wind_feather.tooltip2", WindFeather.COOLDOWN_TICKS / 20)
                .withStyle(ChatFormatting.DARK_GRAY));
        RelicTooltips.relic(lines);
    }
}
