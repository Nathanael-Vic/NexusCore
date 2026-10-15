package com.nexussmp.nexuscore.mixin.backrooms;

import com.nexussmp.nexuscore.backrooms.client.BackroomsTransition;
import com.nexussmp.nexuscore.backrooms.client.SkinwalkerEffects;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Afunda a camera durante a travessia para as backrooms.
 *
 * <p>E o unico jeito honesto de mostrar alguem atravessando o chao: a entidade fica onde esta (mexer
 * nela geraria dano de sufocamento e briga com o servidor) e so o ponto de vista desce. Fora da
 * animacao o metodo devolve zero e nada aqui acontece.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Vec3 position;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("RETURN"))
    private void nexus$sinkThroughFloor(BlockGetter level, Entity entity, boolean detached,
                                        boolean reversed, float partialTick, CallbackInfo callback) {
        double drop = BackroomsTransition.cameraDrop(partialTick);
        double shake = SkinwalkerEffects.shake();
        if (drop <= 0.0 && shake <= 0.0) {
            return;
        }
        // O tremor e pequeno e sem ritmo: o suficiente para a mao nao parecer firme.
        double time = entity == null ? 0.0 : entity.tickCount + partialTick;
        double shakeX = Math.sin(time * 2.7) * Math.cos(time * 1.3) * shake;
        double shakeY = Math.sin(time * 3.9) * shake;
        setPosition(position.x + shakeX, position.y - drop + shakeY, position.z);
    }
}
