package com.nexussmp.nexuscore.mixin.morph;

import com.nexussmp.nexuscore.morph.MorphEntities;
import com.nexussmp.nexuscore.morph.MorphStands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Duas coisas do anel que moram no {@link LivingEntity}.
 *
 * <p><b>Volume e tom de voz.</b> O som do bicho sai com o volume e o tom dele: o ghast se ouve de
 * longe, o filhote tem a voz mais fina. O jogador nao tem versao propria desses dois metodos, por
 * isso a troca e feita aqui, so para jogador transformado.
 *
 * <p><b>A criatura solta nao empurra.</b> No cliente, a criatura do anel vive a cada tick — e dali que
 * saem as particulas e as animacoes que dependem do tempo. Viver inclui empurrar quem esta encostado,
 * e ela esta exatamente em cima da pessoa: sem isto, a pessoa seria empurrada pelo proprio disfarce.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMorphMixin {

    @Inject(method = "getSoundVolume", at = @At("HEAD"), cancellable = true)
    private void nexus$morphSoundVolume(CallbackInfoReturnable<Float> result) {
        if ((Object) this instanceof Player player) {
            LivingEntity stand = MorphStands.of(player);
            if (stand != null) {
                result.setReturnValue(((LivingEntityInvoker) stand).nexus$getSoundVolume());
            }
        }
    }

    @Inject(method = "getVoicePitch", at = @At("HEAD"), cancellable = true)
    private void nexus$morphVoicePitch(CallbackInfoReturnable<Float> result) {
        if ((Object) this instanceof Player player) {
            LivingEntity stand = MorphStands.of(player);
            if (stand != null) {
                result.setReturnValue(stand.getVoicePitch());
            }
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void nexus$standsDoNotPush(CallbackInfo callback) {
        if (MorphEntities.isStand((Entity) (Object) this)) {
            callback.cancel();
        }
    }
}
