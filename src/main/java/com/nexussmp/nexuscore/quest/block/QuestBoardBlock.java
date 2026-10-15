package com.nexussmp.nexuscore.quest.block;

import com.mojang.serialization.MapCodec;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.quest.QuestBoards;
import com.nexussmp.nexuscore.quest.QuestSettings;
import com.nexussmp.nexuscore.quest.QuestSync;
import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * O mural de quests. Clicar com o botao direito abre a tela.
 *
 * <p>Quem abre fica "no mural" por um tempo: assumir e criar quest so valem com o bloco por perto.
 * O resto se resolve pelo livro do inventario, de qualquer lugar.
 *
 * <p>O mural e maior que um bloco: tres de largura por dois de altura. Este e o bloco que a pessoa
 * coloca e o que desenha o modelo inteiro; os outros cinco lugares recebem
 * {@link QuestBoardPartBlock}, invisiveis, so para a colisao bater com o que se ve. Colocar e
 * quebrar cuidam dos seis juntos.
 */
public final class QuestBoardBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<QuestBoardBlock> CODEC = simpleCodec(QuestBoardBlock::new);

    /** Evita que a remocao em cadeia volte para quem comecou. */
    private static boolean removing;

    public QuestBoardBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return QuestBoardShape.of(state.getValue(FACING), 0, 0);
    }

    // ---- colocar e quebrar os seis blocos juntos ----

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos part : parts(context.getClickedPos(), facing)) {
            if (!context.getLevel().getBlockState(part).canBeReplaced(context)) {
                // Sem espaco para o mural inteiro: nao coloca nada e nao gasta o item.
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        BlockState part = NexusRegistries.QUEST_BOARD_PART.get().defaultBlockState().setValue(FACING, facing);
        for (int cellX = -QuestBoardShape.HALF_WIDTH; cellX <= QuestBoardShape.HALF_WIDTH; cellX++) {
            for (int cellY = 0; cellY <= QuestBoardShape.EXTRA_HEIGHT; cellY++) {
                if (cellX == 0 && cellY == 0) {
                    continue;
                }
                level.setBlock(QuestBoardShape.offset(pos, facing, cellX, cellY),
                        part.setValue(QuestBoardPartBlock.CELL_X, cellX + 1)
                                .setValue(QuestBoardPartBlock.CELL_Y, cellY),
                        Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            removeStructure(level, pos, state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Tira o que sobrou do mural: os pedacos e, se ainda estiver la, o bloco principal. */
    static void removeStructure(Level level, BlockPos core, Direction facing, @Nullable BlockPos skip) {
        if (removing || level.isClientSide) {
            return;
        }
        removing = true;
        try {
            for (BlockPos part : parts(core, facing)) {
                if (!part.equals(skip) && level.getBlockState(part).getBlock() instanceof QuestBoardPartBlock) {
                    level.removeBlock(part, false);
                }
            }
            if (!core.equals(skip) && level.getBlockState(core).getBlock() instanceof QuestBoardBlock) {
                level.removeBlock(core, false);
            }
        } finally {
            removing = false;
        }
    }

    /** Os cinco lugares em volta do bloco colocado. */
    private static java.util.List<BlockPos> parts(BlockPos core, Direction facing) {
        java.util.List<BlockPos> positions = new java.util.ArrayList<>(5);
        for (int cellX = -QuestBoardShape.HALF_WIDTH; cellX <= QuestBoardShape.HALF_WIDTH; cellX++) {
            for (int cellY = 0; cellY <= QuestBoardShape.EXTRA_HEIGHT; cellY++) {
                if (cellX != 0 || cellY != 0) {
                    positions.add(QuestBoardShape.offset(core, facing, cellX, cellY));
                }
            }
        }
        return positions;
    }

    // ---- a tela ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!QuestSettings.current().enabled()) {
            player.displayClientMessage(Component.translatable("nexuscore.quest.disabled"), true);
            return InteractionResult.CONSUME;
        }
        QuestBoards.opened(serverPlayer, pos);
        QuestSync.send(serverPlayer);
        PacketDistributor.sendToPlayer(serverPlayer, new QuestPayloads.OpenBoard());
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
