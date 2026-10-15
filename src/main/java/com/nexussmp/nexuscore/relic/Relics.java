package com.nexussmp.nexuscore.relic;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * O que as reliquias compartilham: quem e reliquia, a recarga, e o resgate do vazio.
 *
 * <p><b>A recarga mora no item</b> ({@code relic_ready_at}), em tempo de jogo, e nao no jogador: o
 * contador de recarga do jogo zera ao relogar ou morrer, e uma mascara que se recarrega relogando nao
 * tem recarga nenhuma. O contador do jogo ainda e usado, mas so como desenho — e ele que escurece o
 * icone na barra enquanto espera.
 */
public final class Relics {
    /** As reliquias. A lista mora na tag ({@code data/nexuscore/tags/item/relics.json}). */
    public static final TagKey<Item> TAG = ItemTags.create(NexusCore.id("relics"));
    /** No chao, nao somem nunca. */
    public static final int GROUND_LIFESPAN = Integer.MAX_VALUE;
    /** Abaixo disto do fundo do mundo, o item ja caiu no vazio. */
    private static final int VOID_MARGIN = 8;

    public static boolean isRelic(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TAG);
    }

    /** Agora, no relogio que todas as dimensoes compartilham. */
    public static long now(Level level) {
        return level.getGameTime();
    }

    public static boolean ready(Level level, ItemStack stack) {
        return remaining(level, stack) <= 0;
    }

    /** Ticks ate a reliquia voltar a funcionar; zero ou menos se ja esta pronta. */
    public static long remaining(Level level, ItemStack stack) {
        Long readyAt = stack.get(NexusRegistries.RELIC_READY_AT.get());
        return readyAt == null ? 0 : readyAt - now(level);
    }

    /** Poe a reliquia para descansar {@code ticks} — no item, e no desenho da barra. */
    public static void startCooldown(ServerPlayer player, ItemStack stack, int ticks) {
        stack.set(NexusRegistries.RELIC_READY_AT.get(), now(player.level()) + ticks);
        player.getCooldowns().addCooldown(stack.getItem(), ticks);
    }

    /** Ao entrar no servidor, o desenho da recarga volta para o que o item diz. */
    public static void syncCooldowns(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            long left = remaining(player.level(), stack);
            if (isRelic(stack) && left > 0 && stack.getItem() != NexusRegistries.EMBER_HEART.get()) {
                player.getCooldowns().addCooldown(stack.getItem(), (int) Math.min(Integer.MAX_VALUE, left));
            }
        }
    }

    /** Recusa com a frase na barra de acao e um estalo baixinho. */
    public static void refuse(ServerPlayer player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.PLAYERS, 0.5F, 0.6F);
    }

    /** "Ainda descansando: 12 s." */
    public static Component restingFor(Level level, ItemStack stack) {
        long seconds = (remaining(level, stack) + 19) / 20;
        return seconds >= 120
                ? Component.translatable("nexuscore.relic.resting_minutes", (seconds + 59) / 60)
                : Component.translatable("nexuscore.relic.resting_seconds", seconds);
    }

    /**
     * Caiu no vazio: volta para quem jogou, ou para o ponto de nascer do mundo.
     *
     * <p>Sem isto, jogar uma reliquia do fim do mundo seria o unico jeito de perde-la para sempre.
     */
    public static void rescueFromVoid(ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || item.getY() > level.getMinBuildHeight() - VOID_MARGIN) {
            return;
        }
        Entity owner = item.getOwner();
        Vec3 target;
        if (owner != null && owner.level() == level && owner.isAlive()) {
            target = owner.position().add(0.0, 0.5, 0.0);
        } else {
            BlockPos spawn = level.getSharedSpawnPos();
            target = Vec3.atBottomCenterOf(level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, spawn));
        }
        item.teleportTo(target.x, target.y, target.z);
        item.setDeltaMovement(Vec3.ZERO);
        level.playSound(null, target.x, target.y, target.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private Relics() {}
}
