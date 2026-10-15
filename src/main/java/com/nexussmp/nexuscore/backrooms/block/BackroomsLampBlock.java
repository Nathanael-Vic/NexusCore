package com.nexussmp.nexuscore.backrooms.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * O painel de luz do teto — e o unico bloco das backrooms que faz alguma coisa: ele apaga.
 *
 * <p>Apagar e acender e o que da o nervoso do lugar quando o skinwalker se revela. Por isso a luz e
 * um estado do bloco e nao uma propriedade fixa: trocar o estado e barato e o jogo cuida da luz.
 */
public final class BackroomsLampBlock extends Block {
    public static final MapCodec<BackroomsLampBlock> CODEC = simpleCodec(BackroomsLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BackroomsLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
