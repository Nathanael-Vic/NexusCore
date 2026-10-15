package com.nexussmp.nexuscore.relic;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * O que toda reliquia tem em comum: uma so por vaga, e impossivel de perder.
 *
 * <p>Jogada no chao, ela nao queima em lava nem fogo, nao quebra com explosao nem cacto, nao some
 * depois de cinco minutos — e se cair no vazio, volta para quem a jogou. Na morte ela fica com a
 * pessoa ({@link RelicKeeper}). O unico jeito de uma reliquia mudar de mao e alguem entregar.
 */
public class RelicItem extends Item {

    public RelicItem(Properties properties) {
        super(properties);
    }

    /** Uma por vaga, raridade epica (o nome em roxo) e a prova de fogo. */
    public static Properties properties() {
        return new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant();
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Relics.GROUND_LIFESPAN;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Relics.rescueFromVoid(entity);
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return true;
    }
}
