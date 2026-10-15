package com.nexussmp.nexuscore.mixin.morph;

import com.nexussmp.nexuscore.morph.MorphStands;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Quem virou bicho faz o som do bicho: ao levar dano, ao morrer e ao pisar.
 *
 * <p>Dano e morte so trocam a <i>resposta</i> — o jogo continua tocando do jeito dele, em cada lado:
 * o servidor para quem esta em volta, o cliente para a propria pessoa. Por isso basta responder com
 * o som do bicho nos dois lados.
 *
 * <p>O passo e diferente: cada bicho toca o proprio passo, com volume e ritmo dele (o da vaca e
 * baixinho, o do golem e pesado). Entao o passo e tocado pelo bicho, no servidor, para todo mundo em
 * volta — inclusive a pessoa. O cliente dela fica quieto, senao ela ouviria dois passos.
 */
@Mixin(Player.class)
public abstract class PlayerMorphSoundMixin {

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void nexus$morphHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> result) {
        LivingEntity stand = MorphStands.of((Player) (Object) this);
        if (stand != null) {
            result.setReturnValue(((LivingEntityInvoker) stand).nexus$getHurtSound(source));
        }
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void nexus$morphDeathSound(CallbackInfoReturnable<SoundEvent> result) {
        LivingEntity stand = MorphStands.of((Player) (Object) this);
        if (stand != null) {
            result.setReturnValue(((LivingEntityInvoker) stand).nexus$getDeathSound());
        }
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void nexus$morphStepSound(BlockPos pos, BlockState state, CallbackInfo callback) {
        Player self = (Player) (Object) this;
        LivingEntity stand = MorphStands.of(self);
        if (stand == null) {
            return;
        }
        callback.cancel();
        if (self.level().isClientSide()) {
            return;
        }
        stand.setPos(self.getX(), self.getY(), self.getZ());
        ((EntityInvoker) stand).nexus$playStepSound(pos, state);
    }
}
