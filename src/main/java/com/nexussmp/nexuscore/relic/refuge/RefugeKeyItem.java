package com.nexussmp.nexuscore.relic.refuge;

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

/** A chave do refugio: clique direito abre a porta da sua sala na sua frente ({@link Refuges}). */
public final class RefugeKeyItem extends RelicItem {

    public RefugeKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            Refuges.useKey(server, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.refuge_key.tooltip", Refuges.DOOR_SECONDS).withStyle(ChatFormatting.GRAY));
        RelicTooltips.resting(stack, context, lines);
    }
}
