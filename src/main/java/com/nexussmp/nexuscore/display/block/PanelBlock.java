package com.nexussmp.nexuscore.display.block;

import com.mojang.serialization.MapCodec;
import com.nexussmp.nexuscore.display.PanelPage;
import com.nexussmp.nexuscore.display.PanelPermissions;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.display.multiblock.PanelMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Painel de exploracao: mostra as estatisticas na face do bloco. Paineis lado a lado formam uma tela maior.
 * Agachar + clicar com a mao vazia troca a pagina (quem tiver permissao).
 */
public final class PanelBlock extends BaseEntityBlock {
    public static final MapCodec<PanelBlock> CODEC = simpleCodec(PanelBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * Lados que continuam na mesma tela, do ponto de vista de quem olha. Ligados so quando o vizinho
     * pertence ao mesmo multibloco, nunca so por ser um painel encostado. O modelo multipart usa isto
     * para desenhar a moldura apenas no contorno externo: sem isso, 2x2 blocos mostram uma cruz no meio.
     */
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.create("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.create("connected_down");
    public static final BooleanProperty CONNECTED_LEFT = BooleanProperty.create("connected_left");
    public static final BooleanProperty CONNECTED_RIGHT = BooleanProperty.create("connected_right");

    public PanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTED_UP, false)
                .setValue(CONNECTED_DOWN, false)
                .setValue(CONNECTED_LEFT, false)
                .setValue(CONNECTED_RIGHT, false));
    }

    /** Direita de quem olha a tela de frente. A tela cresce para a direita e para cima a partir do controlador. */
    public static Direction screenRight(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    protected MapCodec<PanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED_UP, CONNECTED_DOWN, CONNECTED_LEFT, CONNECTED_RIGHT);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (!PanelPermissions.canPlace(player)) {
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.translatable("nexuscore.panel.no_permission"), true);
            }
            return null;
        }
        // A tela fica virada para quem colocou.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PanelBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        boolean changed = !oldState.is(this) || oldState.getValue(FACING) != state.getValue(FACING);
        if (!level.isClientSide && changed) {
            // Um tick depois: a block entity ja existe e vizinhos colocados no mesmo tick entram juntos.
            level.scheduleTick(pos, this, 1);
            if (oldState.is(this)) {
                PanelMultiblock.scheduleNeighbors(level, pos, oldState.getValue(FACING));
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !newState.is(this);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && removed) {
            PanelMultiblock.scheduleNeighbors(level, pos, state.getValue(FACING));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        PanelMultiblock.rebuild(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !PanelPermissions.canConfigure(player)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        PanelBlockEntity controller = PanelBlockEntity.controllerOf(level, pos);
        if (controller == null) {
            return InteractionResult.PASS;
        }
        PanelPage next = PanelPage.next(controller.page(), controller.enabledPages(), 1);
        controller.setPage(next);
        player.displayClientMessage(Component.translatable("nexuscore.panel.page_changed", next.displayName()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        // Espelhar troca os lados da tela; o recalculo do multibloco corrige depois, isto so evita
        // um quadro errado no instante em que a estrutura e colada.
        BlockState mirrored = state.rotate(mirror.getRotation(state.getValue(FACING)));
        return mirrored
                .setValue(CONNECTED_LEFT, state.getValue(CONNECTED_RIGHT))
                .setValue(CONNECTED_RIGHT, state.getValue(CONNECTED_LEFT));
    }
}
