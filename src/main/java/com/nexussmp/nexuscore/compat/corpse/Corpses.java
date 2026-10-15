package com.nexussmp.nexuscore.compat.corpse;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Reconhece o corpo do mod Corpse.
 *
 * <p>Nas backrooms o rastro de quem foi pego e a cabeca no chao, e so ela. Um corpo caido no
 * corredor contaria outra historia — a de alguem que morreu — e ainda guardaria um inventario que
 * nem esta com a pessoa, porque na entrada tudo foi para o guarda-volumes.
 *
 * <p>Na pratica o corpo nem chega a nascer: la dentro a morte e cancelada antes de qualquer mod
 * ouvi-la. Isto aqui e a tranca da porta — reconhecer a entidade pelo id, sem dependencia nenhuma,
 * para o caso de algum outro caminho tentar colocar um corpo ali.
 */
public final class Corpses {
    private static final ResourceLocation ENTITY_ID =
            ResourceLocation.fromNamespaceAndPath("corpse", "corpse");

    public static boolean isCorpse(Entity entity) {
        return ENTITY_ID.equals(EntityType.getKey(entity.getType()));
    }

    private Corpses() {}
}
