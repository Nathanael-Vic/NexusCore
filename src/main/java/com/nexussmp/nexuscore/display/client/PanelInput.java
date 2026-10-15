package com.nexussmp.nexuscore.display.client;

import com.nexussmp.nexuscore.display.block.PanelBlock;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.display.client.render.PanelClientCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * Clique na tela do painel para navegar entre as paginas.
 *
 * <p>Tudo acontece no cliente: a posicao do clique vira uma coordenada da tela e muda so a visao deste
 * jogador. Nao existe pacote de rede para abusar, e ninguem muda a pagina de outro.
 *
 * <p>So com a mao vazia e sem agachar: com item na mao o clique continua colocando bloco, e agachado
 * e a configuracao do OP (essa sim vai para o servidor).
 */
public final class PanelInput {

    static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || !DisplayClientSettings.enabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof PanelBlock) || hit.getDirection() != state.getValue(PanelBlock.FACING)) {
            return;
        }
        PanelBlockEntity controller = PanelBlockEntity.controllerOf(level, hit.getBlockPos());
        if (controller == null || !controller.isController()) {
            return;
        }
        PanelClientCache.of(controller).click(controller, horizontalFraction(controller, state.getValue(PanelBlock.FACING), hit));
        event.setSwingHand(true);
        event.setCanceled(true);
    }

    /** 0 na borda esquerda da tela, 1 na direita (do ponto de vista de quem olha). */
    private static double horizontalFraction(PanelBlockEntity controller, Direction facing, BlockHitResult hit) {
        Direction right = PanelBlock.screenRight(facing);
        BlockPos origin = controller.getBlockPos();
        Vec3 point = hit.getLocation();
        double along = (point.x - origin.getX()) * right.getStepX() + (point.z - origin.getZ()) * right.getStepZ();
        if (right.getStepX() < 0 || right.getStepZ() < 0) {
            // Com o eixo apontando para -X/-Z, o bloco do controlador vai de -1 a 0.
            along += 1.0;
        }
        return Mth.clamp(along / controller.width(), 0.0, 1.0);
    }

    private PanelInput() {}
}
