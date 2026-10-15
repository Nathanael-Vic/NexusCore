package com.nexussmp.nexuscore.mixin.envelope;

import com.nexussmp.nexuscore.compat.envelope.EnvelopeHooks;
import io.github.mortuusars.envelope.world.entity.spawner.BackgroundCourierSpawner;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No meio da viagem, o Envelope as vezes faz o pombo aparecer voando perto de alguem, so de enfeite.
 * O morcego do lorde nao aparece de dia: continua a viagem sem ser visto.
 */
@Mixin(BackgroundCourierSpawner.class)
public abstract class BackgroundCourierSpawnerMixin {

    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void nexus$noBatsByDay(BackgroundCourier courier, CallbackInfoReturnable<Boolean> result) {
        if (EnvelopeHooks.hideBackgroundCourier(courier)) {
            result.setReturnValue(false);
        }
    }
}
