package com.nexussmp.nexuscore.relic.stone;

import com.nexussmp.nexuscore.relic.RelicItem;
import com.nexussmp.nexuscore.relic.RelicTooltips;
import com.nexussmp.nexuscore.relic.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A pedra inabalavel na mao. E um bloco, entao nao herda do {@link RelicItem} — mas se porta como
 * reliquia do mesmo jeito: nao queima, nao some, volta do vazio.
 */
public final class UnyieldingStoneItem extends BlockItem {

    public UnyieldingStoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Relics.rescueFromVoid(entity);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nexuscore.relic.unyielding_stone.tooltip").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("nexuscore.relic.unyielding_stone.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
        RelicTooltips.relic(lines);
    }
}
