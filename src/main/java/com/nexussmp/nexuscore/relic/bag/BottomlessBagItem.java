package com.nexussmp.nexuscore.relic.bag;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A bolsa sem fundo: o espaco de um bau duplo, numa bolsa que nao sai de voce.
 *
 * <p>Clique direito abre. O que esta dentro mora no proprio item — por isso, como toda reliquia, ela
 * fica com a pessoa na morte, e tudo o que estava dentro vai junto. E o lugar seguro do inventario.
 * As regras de dentro estao em {@link BagMenu}.
 */
public final class BottomlessBagItem extends RelicItem {

    public BottomlessBagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            Component title = bag.has(DataComponents.CUSTOM_NAME) ? bag.getHoverName() : getDescription();
            server.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> BagMenu.open(id, inventory, bag, hand), title));
            level.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.9F, 0.8F);
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(bag, level.isClientSide());
    }

    /** Bolsa nao entra em bolsa (nem em caixa de shulker): seria espaco infinito. */
    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.bottomless_bag.tooltip").withStyle(ChatFormatting.GRAY));
        long used = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().count();
        lines.add(Component.translatable("nexuscore.relic.bottomless_bag.used", used, BagMenu.SIZE)
                .withStyle(ChatFormatting.DARK_GRAY));
        RelicTooltips.relic(lines);
    }
}
