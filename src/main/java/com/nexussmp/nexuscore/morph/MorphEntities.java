package com.nexussmp.nexuscore.morph;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Uma copia solta de uma criatura, que nunca entra no mundo.
 *
 * <p>O servidor usa uma para saber o tamanho da forma; o cliente, para desenhar. Nos dois casos a
 * criatura e criada pelo proprio tipo dela e carrega o NBT guardado no anel — e assim que um filhote
 * sai pequeno, um slime sai do tamanho certo e a ovelha sai da cor que era.
 */
public final class MorphEntities {
    /**
     * A marca das criaturas soltas. E por ela que se reconhece uma copia em qualquer lugar do jogo —
     * por exemplo, para ela nao empurrar ninguem quando vive no cliente (ver o mixin de morph).
     */
    public static final String STAND_TAG = "nexuscore_morph_stand";

    public static boolean isStand(Entity entity) {
        return entity.getTags().contains(STAND_TAG);
    }

    /** {@code null} para forma de jogador, tipo que nao existe mais, ou criatura que nao e viva. */
    @Nullable
    public static LivingEntity create(Level level, MorphForm form) {
        if (form.isPlayer()) {
            return null;
        }
        EntityType<?> type = form.entityType().orElse(null);
        if (type == null) {
            return null;
        }
        Entity entity;
        try {
            entity = type.create(level);
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
        if (!(entity instanceof LivingEntity living)) {
            return null;
        }
        if (!form.data().isEmpty()) {
            try {
                living.load(form.data().copy());
            } catch (RuntimeException error) {
                // O mod nao aceitou o NBT fora do mundo: a criatura fica com a cara padrao do tipo.
            }
        }
        // Depois do NBT: carregar a lista de marcas dele apagaria esta.
        living.addTag(STAND_TAG);
        return living;
    }

    private MorphEntities() {}
}
