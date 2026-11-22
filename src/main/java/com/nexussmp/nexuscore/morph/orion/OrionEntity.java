package com.nexussmp.nexuscore.morph.orion;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * O ORION H-01: um robozinho que so existe como forma do anel da metamorfose. Nunca aparece no mundo
 * sozinho e nao tem cabeca propria (nem IA, nem objetivos): e o corpo que um admin veste.
 *
 * <p>Como forma, o que vale e o que o anel pergunta a ele: o tamanho (pequeno, uns 0,8 bloco) e os
 * sons de dano e morte (de lata, mais finos). O bip de quando ele "fala" no chat fica em
 * {@link OrionMorph}.
 */
public class OrionEntity extends PathfinderMob {

    public OrionEntity(EntityType<? extends OrionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    /** Lata pequena: o som do golem de ferro, bem mais fino. */
    @Override
    public float getVoicePitch() {
        return 1.7F + (random.nextFloat() - 0.5F) * 0.15F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }
}
