package com.nexussmp.nexuscore.mixin.morph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Para a criatura do anel pisar do jeito dela: a aranha, a galinha e o golem tem passo proprio. */
@Mixin(Entity.class)
public interface EntityInvoker {

    @Invoker("playStepSound")
    void nexus$playStepSound(BlockPos pos, BlockState state);
}
