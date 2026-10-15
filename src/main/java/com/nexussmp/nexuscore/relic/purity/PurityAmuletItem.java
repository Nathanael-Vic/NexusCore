package com.nexussmp.nexuscore.relic.purity;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** O amuleto da pureza. O que ele faz esta em {@link PurityAmulet}. */
public final class PurityAmuletItem extends RelicItem {

    public PurityAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.purity_amulet.tooltip").withStyle(ChatFormatting.GRAY));
        int drops = PurityAmulet.drops(stack);
        lines.add(Component.translatable("nexuscore.relic.purity_amulet.drops", drops, PurityAmulet.MAX_DROPS)
                .withStyle(drops > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
        RelicTooltips.relic(lines);
    }
}
