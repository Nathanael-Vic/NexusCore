package com.nexussmp.nexuscore.mixin.parrot;

import com.nexussmp.nexuscore.parrot.ShoulderParrot;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Levar um papagaio no ombro nao faz de ninguem uma montaria.
 *
 * <p>O papagaio de ombro e passageiro da pessoa (o porque esta em {@link ShoulderParrot}). Para o
 * jogo, quem leva passageiro e montaria, e montaria tem duas regras que aqui seriam bugs: o nome em
 * cima da cabeca some (como o de um cavalo com alguem em cima) e ninguem consegue empurrar. Para
 * essas perguntas, um passageiro que e so papagaio de ombro nao conta.
 *
 * <p>Quem precisa da lista de verdade — o desenho, a rede, o reposicionamento a cada tick — le os
 * passageiros direto e continua vendo o papagaio.
 */
@Mixin(Entity.class)
public abstract class EntityShoulderParrotMixin {

    @Shadow
    public abstract List<Entity> getPassengers();

    @Inject(method = "isVehicle", at = @At("HEAD"), cancellable = true)
    private void nexus$shoulderParrotIsNotARide(CallbackInfoReturnable<Boolean> result) {
        List<Entity> passengers = getPassengers();
        if (passengers.isEmpty()) {
            return;
        }
        for (Entity passenger : passengers) {
            if (!(passenger instanceof ShoulderParrot)) {
                return;
            }
        }
        result.setReturnValue(false);
    }
}
