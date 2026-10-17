package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.astral.AstralProjection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * O espectador comum atravessa tudo: o jogo liga {@code noPhysics} no comeco de cada tick de quem
 * esta no espectador. Fora do corpo pela mascara, desligamos de novo logo em seguida: a alma volta a
 * ter colisao, e o {@code BlockStateAstralMixin} decide em que ela bate (so na terra).
 */
@Mixin(Player.class)
public abstract class PlayerAstralMixin {

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Player;noPhysics:Z",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void nexus$astralCannotPhase(CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (AstralProjection.collides(player)) {
            ((Entity) player).noPhysics = false;
        }
    }
}
