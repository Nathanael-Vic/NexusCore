package com.nexussmp.nexuscore.mixin.morph;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Para perguntar a criatura do anel os sons dela. Os tres metodos sao protegidos no jogo; a chamada
 * por aqui continua indo para a versao de cada bicho, entao a vaca responde com o som da vaca.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {

    @Invoker("getHurtSound")
    SoundEvent nexus$getHurtSound(DamageSource source);

    @Invoker("getDeathSound")
    SoundEvent nexus$getDeathSound();

    @Invoker("getSoundVolume")
    float nexus$getSoundVolume();
}
