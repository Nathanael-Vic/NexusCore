package com.nexussmp.nexuscore.mixin.envelope;

import com.nexussmp.nexuscore.compat.envelope.EnvelopeHooks;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.Delivery;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dois ajustes no pombo do Envelope.
 *
 * <p><b>A voz.</b> Todo som do pombo — arrulho, dano, morte, passo, bater de asa — passa pelo
 * {@code playSound} dele. O pombo que leva carta de lorde toca ali o som de morcego, ou nenhum.
 *
 * <p><b>O fim da entrega.</b> O pombo carteiro de servico, ao terminar, vira fumaca onde estiver —
 * as vezes na frente de quem acabou de receber a carta. Aqui, em vez disso, ele vai embora voando e
 * some longe, fora da vista.
 */
@Mixin(Pigeon.class)
public abstract class PigeonMixin {

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void nexus$courierVoice(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
        Pigeon self = (Pigeon) (Object) this;
        SoundEvent voice = EnvelopeHooks.courierSound(self, sound);
        if (voice == sound) {
            return;
        }
        callback.cancel();
        if (voice != null && !self.isSilent()) {
            self.level().playSound(null, self, voice, self.getSoundSource(), volume, pitch);
        }
    }

    @Inject(method = "endDelivery",
            at = @At(value = "INVOKE",
                    target = "Lio/github/mortuusars/envelope/world/entity/Pigeon;onVanished(Lnet/minecraft/server/level/ServerLevel;)V"),
            cancellable = true)
    private void nexus$flyAway(ServerLevel level, Delivery delivery, CallbackInfo callback) {
        if (EnvelopeHooks.flyAway((Pigeon) (Object) this)) {
            callback.cancel();
        }
    }
}
