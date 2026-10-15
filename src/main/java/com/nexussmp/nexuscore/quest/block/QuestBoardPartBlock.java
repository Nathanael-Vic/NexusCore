package com.nexussmp.nexuscore.quest.block;

import com.mojang.serialization.MapCodec;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Um pedaco do mural: o que da corpo ao resto da estrutura.
 *
 * <p>Nao desenha nada — quem desenha o mural inteiro e o bloco colocado. Estes aqui existem para o
 * mural ter a colisao do tamanho que aparenta, e para clicar em qualquer parte dele abrir a tela.
 * Quebrar um pedaco quebra o mural todo, e nenhum deles solta item: o item sai do bloco principal.
 */
public final class QuestBoardPartBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<QuestBoardPartBlock> CODEC = simpleCodec(QuestBoardPartBlock::new);

    /** 0, 1, 2 para a coluna a esquerda, no meio e a direita de quem olha para o mural. */
    public static final IntegerProperty CELL_X = IntegerProperty.create("cell_x", 0, 2);
    /** 0 para a fiada de baixo, 1 para a de cima. */
    public static final IntegerProperty CELL_Y = IntegerProperty.create("cell_y", 0, 1);

    public QuestBoardPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CELL_X, 1)
                .setValue(CELL_Y, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CELL_X, CELL_Y);
    }

    /** De volta ao bloco que foi colocado: e nele que mora o comportamento do mural. */
    public static BlockPos core(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        int cellX = state.getValue(CELL_X) - 1;
        int cellY = state.getValue(CELL_Y);
        return QuestBoardShape.offset(pos, facing, -cellX, -cellY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return QuestBoardShape.of(state.getValue(FACING), state.getValue(CELL_X) - 1, state.getValue(CELL_Y));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos core = core(state, pos);
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof QuestBoardBlock board)) {
            return InteractionResult.PASS;
        }
        return board.useWithoutItem(coreState, level, core, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos core = level.isClientSide ? null : core(state, pos);
        if (core != null && level.getBlockState(core).getBlock() instanceof QuestBoardBlock) {
            // Quebrar pelo pedaco e quebrar o mural: o bloco principal cuida do item e do resto.
            level.destroyBlock(core, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            QuestBoardBlock.removeStructure(level, core(state, pos), state.getValue(FACING), pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Botao do meio num pedaco pega o mural, e nao um pedaco invisivel. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
                                      Player player) {
        return new ItemStack(NexusRegistries.QUEST_BOARD_ITEM.get());
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
