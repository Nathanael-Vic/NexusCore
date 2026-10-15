package com.nexussmp.nexuscore.mixin.envelope;

import com.nexussmp.nexuscore.compat.envelope.EnvelopeHooks;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A ultima porta antes de a carta sair da caixa: carta de lorde so sai com o morcego chamado para
 * ela. Um pombo comum que chegue ate ali volta sem nada e risca a caixa da lista dele.
 */
@Mixin(MailboxBlockEntity.class)
public abstract class MailboxBlockEntityMixin {

    @Inject(method = "tryStartDelivery", at = @At("HEAD"), cancellable = true)
    private void nexus$lordMail(Pigeon pigeon, CallbackInfoReturnable<Boolean> result) {
        if (!EnvelopeHooks.mayPickUp((MailboxBlockEntity) (Object) this, pigeon)) {
            pigeon.getMailboxHandler().dropAndBlacklistMailbox();
            result.setReturnValue(false);
        }
    }
}
