package com.nexussmp.nexuscore.relic.refuge;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.relic.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * O refugio: uma sala branca, so sua, numa dimensao de bolso.
 *
 * <p><b>A chave</b> abre uma porta na frente de quem a usa, por {@value #DOOR_SECONDS} segundos.
 * Qualquer um que atravessar enquanto ela esta ali vai parar na sala do dono da chave — da para levar
 * amigos. Cada pessoa volta para onde entrou, pela porta da parede de la de dentro.
 *
 * <p><b>A sala</b> tem 13x13x13 livres, paredes, chao e teto de um branco liso que nao quebra, e sua
 * propria luz. Dentro, da para construir e guardar o que quiser — so as paredes sao intocaveis.
 * Cada dono tem a sua, enfileiradas a {@value #ROOM_SPACING} blocos uma da outra; a sala e erguida na
 * primeira visita e fica para sempre.
 *
 * <p><b>Ninguem sai pelo lado.</b> Quem aparecer fora da sala — por fruta do coro, bug, ou o que for —
 * volta para dentro, na frente da porta.
 */
public final class Refuges {
    public static final ResourceKey<Level> REFUGE = ResourceKey.create(Registries.DIMENSION, NexusCore.id("refuge"));

    static final int DOOR_SECONDS = 10;
    private static final int KEY_COOLDOWN_TICKS = 30 * 20;
    static final int ROOM_SPACING = 512;
    /** O chao da sala; as paredes sobem dali ate o teto, 14 blocos acima. */
    private static final int FLOOR_Y = 16;
    /** Metade do vao livre: 6 para cada lado do meio da 13 blocos. */
    private static final int HALF = 6;
    private static final int HEIGHT = 13;
    private static final int CONTAIN_INTERVAL_TICKS = 10;
    /** Admin, para o jogo: o nivel de operador. */
    private static final int ADMIN_LEVEL = 2;

    // ---- a chave ----

    /** Usou a chave: abre a porta na frente, se der. */
    public static void useKey(ServerPlayer player, ItemStack key) {
        ServerLevel level = player.serverLevel();
        if (level.dimension().equals(REFUGE)) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.refuge_key.inside"));
            return;
        }
        if (level.dimension().equals(BackroomsDimensions.BACKROOMS)) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.refuge_key.blocked"));
            return;
        }
        if (!Relics.ready(level, key)) {
            Relics.refuse(player, Relics.restingFor(level, key));
            return;
        }
        Direction facing = player.getDirection();
        BlockPos lower = findDoorSpot(level, player.blockPosition(), facing);
        if (lower == null) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.refuge_key.no_room"));
            return;
        }
        placeDoor(level, lower, facing);
        if (level.getBlockEntity(lower) instanceof RefugeDoorBlockEntity door) {
            door.open(player.getUUID(), level.getGameTime() + DOOR_SECONDS * 20L);
        }
        Relics.startCooldown(player, key, KEY_COOLDOWN_TICKS);
        double x = lower.getX() + 0.5;
        double z = lower.getZ() + 0.5;
        level.playSound(null, lower, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.playSound(null, lower, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, lower.getY() + 1.0, z, 40, 0.35, 0.8, 0.35, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, x, lower.getY() + 1.0, z, 12, 0.3, 0.8, 0.3, 0.01);
    }

    /**
     * Onde cabe a porta: a dois passos na frente (ou um, ou tres), no nivel dos pes, um degrau acima
     * ou abaixo. Precisa de dois blocos livres e chao firme embaixo.
     */
    private static BlockPos findDoorSpot(ServerLevel level, BlockPos feet, Direction facing) {
        for (int distance : new int[] {2, 1, 3}) {
            BlockPos ahead = feet.relative(facing, distance);
            for (int dy : new int[] {0, 1, -1}) {
                BlockPos lower = ahead.above(dy);
                BlockPos below = lower.below();
                if (level.getBlockState(lower).canBeReplaced() && level.getBlockState(lower.above()).canBeReplaced()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    return lower;
                }
            }
        }
        return null;
    }

    private static void placeDoor(ServerLevel level, BlockPos lower, Direction facing) {
        BlockState door = NexusRegistries.REFUGE_DOOR.get().defaultBlockState().setValue(RefugeDoorBlock.FACING, facing);
        level.setBlock(lower, door.setValue(RefugeDoorBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS);
        level.setBlock(lower.above(), door.setValue(RefugeDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** O prazo da porta passou: ela fecha num sopro. */
    static void closeDoor(Level level, BlockPos lower) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        server.setBlock(lower.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        server.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        server.playSound(null, lower, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.5F);
        server.playSound(null, lower, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 0.7F);
        server.sendParticles(ParticleTypes.POOF, lower.getX() + 0.5, lower.getY() + 1.0, lower.getZ() + 0.5,
                12, 0.3, 0.6, 0.3, 0.02);
    }

    // ---- atravessar ----

    /** Alguem usou uma porta: a de fora leva para a sala do dono; a de dentro, de volta. */
    static void useDoor(ServerPlayer player, BlockPos lower) {
        if (!(player.level().getBlockEntity(lower) instanceof RefugeDoorBlockEntity door)) {
            return;
        }
        if (door.owner() != null) {
            enter(player, RefugeData.get(player.server).roomOf(door.owner()));
        } else {
            leave(player);
        }
    }

    private static void enter(ServerPlayer player, int room) {
        MinecraftServer server = player.server;
        ServerLevel refuge = server.getLevel(REFUGE);
        if (refuge == null) {
            NexusCore.LOGGER.warn("[NexusCore] refugio: a dimensao nexuscore:refuge nao existe neste mundo");
            return;
        }
        RefugeData data = RefugeData.get(server);
        if (!data.isBuilt(room)) {
            build(refuge, room);
            data.markBuilt(room);
        }
        data.rememberReturn(player.getUUID(), new RefugeData.ReturnPoint(player.level().dimension(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, 0.8F);
        BlockPos center = center(room);
        player.teleportTo(refuge, center.getX() + 0.5, FLOOR_Y + 1, center.getZ() - HALF + 1.5, 0.0F, 0.0F);
        refuge.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void leave(ServerPlayer player) {
        MinecraftServer server = player.server;
        RefugeData.ReturnPoint point = RefugeData.get(server).takeReturn(player.getUUID());
        ServerLevel target = point == null ? null : server.getLevel(point.dimension());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.8F, 1.2F);
        if (target == null) {
            // Nao ha de onde voltar (a pessoa nasceu aqui, ou o mundo de antes sumiu): o ponto de
            // nascer dela, como quem acorda.
            ServerLevel overworld = server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), 0.0F);
        } else {
            player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        }
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // ---- a sala ----

    static BlockPos center(int room) {
        return new BlockPos(room * ROOM_SPACING, FLOOR_Y, 0);
    }

    /** Ergue a sala: piso, teto e quatro paredes, e a porta de saida no meio da parede norte. */
    private static void build(ServerLevel level, int room) {
        BlockPos center = center(room);
        BlockState wall = NexusRegistries.REFUGE_WALL.get().defaultBlockState();
        int edge = HALF + 1;
        for (int dx = -edge; dx <= edge; dx++) {
            for (int dz = -edge; dz <= edge; dz++) {
                for (int dy = 0; dy <= HEIGHT + 1; dy++) {
                    boolean shell = dy == 0 || dy == HEIGHT + 1 || Math.abs(dx) == edge || Math.abs(dz) == edge;
                    if (shell) {
                        level.setBlock(center.offset(dx, dy, dz), wall, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // A porta fica no plano da parede, com a face para dentro da sala.
        placeDoor(level, center.offset(0, 1, -edge), Direction.NORTH);
    }

    /** O vao livre de uma sala, onde quem esta la dentro precisa estar. */
    private static AABB inside(int room) {
        BlockPos center = center(room);
        return new AABB(center.getX() - HALF, FLOOR_Y + 1, center.getZ() - HALF,
                center.getX() + HALF + 1, FLOOR_Y + 1 + HEIGHT, center.getZ() + HALF + 1);
    }

    /**
     * Parede e porta nao quebram para ninguem. Sobrevivencia ja nao quebrava (sao duras como a bedrock,
     * e explosao, pistao, Wither e dragao tambem nao passam); isto fecha o criativo — que quebra ate
     * bedrock. So admin no criativo passa, para consertar uma sala se precisar.
     */
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (state.is(NexusRegistries.REFUGE_WALL.get()) || state.is(NexusRegistries.REFUGE_DOOR.get())) {
            Player player = event.getPlayer();
            if (!player.isCreative() || !player.hasPermissions(ADMIN_LEVEL)) {
                event.setCanceled(true);
            }
        }
    }

    /** Ninguem fica fora da sala: quem escapou volta, na frente da porta. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CONTAIN_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel refuge = server.getLevel(REFUGE);
        if (refuge == null) {
            return;
        }
        for (ServerPlayer player : refuge.players()) {
            if (player.isSpectator()) {
                continue;
            }
            int room = Math.max(0, (int) Math.round(player.getX() / ROOM_SPACING));
            if (!inside(room).contains(player.position())) {
                BlockPos center = center(room);
                player.teleportTo(refuge, center.getX() + 0.5, FLOOR_Y + 1, center.getZ() - HALF + 1.5,
                        player.getYRot(), player.getXRot());
                player.resetFallDistance();
            }
        }
    }

    private Refuges() {}
}
