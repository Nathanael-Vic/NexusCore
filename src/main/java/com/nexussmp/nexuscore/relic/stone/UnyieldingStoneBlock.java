package com.nexussmp.nexuscore.relic.stone;

import com.mojang.serialization.MapCodec;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A pedra inabalavel: um bloco so, que so quem colocou tira.
 *
 * <p><b>O dono</b> e quem coloca: na mao ela e de ninguem; no chao, fica presa a quem a pos. Para
 * tirar, o dono agacha e da clique direito com a mao vazia — ela sai inteira e volta direto para o
 * inventario. Para todo o resto ela e mais dura que a bedrock: picareta nenhuma, explosao nenhuma,
 * pistao, Wither, dragao, enderman — nada mexe. A unica excecao e admin no criativo, para uma pedra
 * esquecida nao ficar para sempre num lugar errado.
 *
 * <p>Quem esta olhando nao sabe de quem ela e: o nome do dono nao sai do servidor, a nao ser para
 * admin (no Jade). E ai mora a graca de uma pedra no meio da porta de alguem.
 */
public final class UnyieldingStoneBlock extends Block implements EntityBlock {
    public static final MapCodec<UnyieldingStoneBlock> CODEC = simpleCodec(UnyieldingStoneBlock::new);
    /** Admin, para o jogo: o nivel de operador. */
    private static final int ADMIN_LEVEL = 2;

    public UnyieldingStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /** Colocou: a pedra e sua. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof UnyieldingStoneBlockEntity stone) {
            stone.bind(player.getUUID(), player.getGameProfile().getName());
        }
    }

    /** Agachado e de mao vazia: se for o dono, a pedra volta para o inventario. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer owner)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof UnyieldingStoneBlockEntity stone) || !stone.isOwner(owner.getUUID())) {
            return InteractionResult.PASS;
        }
        server.removeBlock(pos, false);
        ItemStack item = new ItemStack(NexusRegistries.UNYIELDING_STONE_ITEM.get());
        if (!owner.getInventory().add(item)) {
            owner.drop(item, false);
        }
        server.playSound(null, pos, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.9F);
        server.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.6);
        return InteractionResult.SUCCESS;
    }

    /** Quebrar, so admin no criativo. Nem o dono quebra — o dono recolhe. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getState().is(NexusRegistries.UNYIELDING_STONE.get())) {
            Player player = event.getPlayer();
            if (!player.isCreative() || !player.hasPermissions(ADMIN_LEVEL)) {
                event.setCanceled(true);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UnyieldingStoneBlockEntity(pos, state);
    }
}
