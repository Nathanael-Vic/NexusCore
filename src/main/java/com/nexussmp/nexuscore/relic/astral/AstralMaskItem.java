package com.nexussmp.nexuscore.relic.astral;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A mascara astral: clique direito sai do corpo. O resto esta em {@link AstralProjection}. */
public final class AstralMaskItem extends RelicItem {

    public AstralMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            AstralProjection.start(server, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.astral_mask.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("nexuscore.relic.astral_mask.tooltip2", AstralProjection.MAX_TICKS / 20)
                .withStyle(ChatFormatting.DARK_RED));
        RelicTooltips.resting(stack, context, lines);
    }
}
