package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.flight.WindFeather;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A cada tick o jogo confere se quem esta planando ainda pode: "o peito deixa voar?" Com a pena dos
 * ventos nas costas, deixa. Sem isto o voo pela pena acabaria no tick seguinte ao comeco.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallFlyingMixin {

    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean nexus$featherKeepsFlying(ItemStack chest, LivingEntity entity) {
        return chest.canElytraFly(entity) || WindFeather.canGlide(entity);
    }

    /**
     * O tick de voo da elytra gasta durabilidade dela. Pela pena nao ha o que gastar: so continua.
     */
    @Redirect(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;elytraFlightTick(Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean nexus$featherFlightTick(ItemStack chest, LivingEntity entity, int flightTicks) {
        return chest.canElytraFly(entity) ? chest.elytraFlightTick(entity, flightTicks) : WindFeather.canGlide(entity);
    }
}
