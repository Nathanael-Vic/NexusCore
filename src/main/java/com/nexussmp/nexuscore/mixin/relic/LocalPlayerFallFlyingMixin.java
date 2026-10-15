package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.flight.WindFeather;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Apertar pulo no ar abre o voo — do lado do cliente, que e quem decide e avisa o servidor. Aqui o
 * jogo pergunta ao peito antes de tentar; com a pena, a resposta e sim.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerFallFlyingMixin {

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean nexus$featherCanOpen(ItemStack chest, LivingEntity entity) {
        return chest.canElytraFly(entity) || WindFeather.canGlide(entity);
    }
}
