package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.astral.AstralProjection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No espectador comum, clicar numa criatura poe a camera dentro dela. Fora do corpo pela mascara,
 * isso nao existe: a alma so olha, nao possui ninguem.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerAstralMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void nexus$astralCannotPossess(Entity target, CallbackInfo callback) {
        if (AstralProjection.isProjecting((ServerPlayer) (Object) this)) {
            callback.cancel();
        }
    }
}
