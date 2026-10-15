package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.flight.WindFeather;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Abrir o voo no ar: o servidor confere o mesmo que o cliente — o peito, ou a pena. */
@Mixin(Player.class)
public abstract class PlayerFallFlyingMixin {

    @Redirect(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean nexus$featherStartsFlying(ItemStack chest, LivingEntity entity) {
        return chest.canElytraFly(entity) || WindFeather.canGlide(entity);
    }
}
