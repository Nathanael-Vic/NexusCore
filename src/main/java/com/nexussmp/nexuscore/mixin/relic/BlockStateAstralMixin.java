package com.nexussmp.nexuscore.mixin.relic;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.nexussmp.nexuscore.relic.astral.AstralProjection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fora do corpo, a alma atravessa o que foi construido e bate na terra: para ela, todo bloco que
 * nao for chao natural ({@link AstralProjection#SOLID_TO_SOUL}) nao tem colisao. Assim da para
 * entrar numa casa pela parede, mas nao descer pelo chao ate as cavernas.
 *
 * <p>Esta e a pergunta de colisao que o jogo (e o Lithium) faz bloco a bloco ao mover alguem, por
 * isso o caminho comum sai no primeiro {@code instanceof}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateAstralMixin {

    @ModifyReturnValue(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"))
    private VoxelShape nexus$soulPassesBuildings(VoxelShape shape, @Local(argsOnly = true) CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
                && !shape.isEmpty() && AstralProjection.collides(player)
                && !((BlockBehaviour.BlockStateBase) (Object) this).is(AstralProjection.SOLID_TO_SOUL)) {
            return Shapes.empty();
        }
        return shape;
    }
}
