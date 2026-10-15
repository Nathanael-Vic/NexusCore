package com.nexussmp.nexuscore.mixin.envelope;

import com.nexussmp.nexuscore.compat.envelope.EnvelopeHooks;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.goal.PigeonLocateMailboxGoal;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** O pombo comum procurando caixa com carta nem enxerga a caixa de um lorde. */
@Mixin(PigeonLocateMailboxGoal.class)
public abstract class PigeonLocateMailboxGoalMixin {
    @Shadow
    @Final
    protected Pigeon pigeon;

    @Inject(method = "findNearbyAvailableMailboxes", at = @At("RETURN"), cancellable = true)
    private void nexus$skipLordMail(CallbackInfoReturnable<List<BlockPos>> result) {
        result.setReturnValue(EnvelopeHooks.reachableMailboxes(pigeon, result.getReturnValue()));
    }
}
