package com.nexussmp.nexuscore.mixin.envelope;

import com.nexussmp.nexuscore.compat.envelope.EnvelopeHooks;
import io.github.mortuusars.envelope.world.Position;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Onde o pombo some ao sair com a carta, e de onde surge ao chegar: o Envelope poe esse ponto 16
 * blocos para o lado e 16 para cima da caixa — perto o bastante para ver a fumacinha no ar. Aqui ele
 * vai para mais longe e mais alto ({@code ascendDistance} na configuracao do servidor).
 */
@Mixin(Position.class)
public abstract class PositionMixin {

    @ModifyVariable(method = "ascendTowards(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int nexus$fartherAscend(int distance) {
        return EnvelopeHooks.ascendDistance(distance);
    }

    @ModifyVariable(method = "ascendTowards(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/Optional;II)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int nexus$fartherAscendNearGround(int distance) {
        return EnvelopeHooks.ascendDistance(distance);
    }
}
