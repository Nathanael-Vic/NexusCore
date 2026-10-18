package com.nexussmp.nexuscore.monolith;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

import java.util.List;

/**
 * Os monolitos sao intocaveis: ninguem quebra, coloca ou usa nada dentro da caixa da estrutura, nem
 * explosao, pistao, fluido ou criatura. So operador no criativo mexe (para consertar alguma coisa).
 *
 * <p>Toda pergunta comeca barata: longe de qualquer monolito ({@link #near}), nada aqui faz conta.
 */
public final class MonolithGuard {

    private static boolean near(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || server.dimension() != Level.OVERWORLD) {
            return false;
        }
        for (MonolithSites.Site site : MonolithSites.of(server)) {
            if (site.horizontalDistance(pos.getX(), pos.getZ()) < site.coreRadius() * 3.0) {
                return true;
            }
        }
        return false;
    }

    private static boolean protectedAt(LevelAccessor accessor, BlockPos pos) {
        return accessor instanceof ServerLevel level && near(level, pos) && MonolithSites.insideStructure(level, pos);
    }

    private static boolean exempt(Entity entity) {
        return entity instanceof Player player && player.isCreative() && player.hasPermissions(2);
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!exempt(event.getPlayer()) && protectedAt(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if ((event.getEntity() == null || !exempt(event.getEntity())) && protectedAt(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Nada de usar bloco nem item em bloco: porta, balde, isqueiro, enxada, farinha de osso. */
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (exempt(event.getEntity())) {
            return;
        }
        BlockPos pos = event.getPos();
        if (protectedAt(event.getLevel(), pos)
                || (event.getFace() != null && protectedAt(event.getLevel(), pos.relative(event.getFace())))) {
            event.setCanceled(true);
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        BlockPos center = BlockPos.containing(event.getExplosion().center());
        if (near(level, center)) {
            event.getAffectedBlocks().removeIf(pos -> protectedAt(level, pos));
        }
    }

    public static void onPiston(PistonEvent.Pre event) {
        LevelAccessor level = event.getLevel();
        if (protectedAt(level, event.getPos()) || protectedAt(level, event.getFaceOffsetPos())) {
            event.setCanceled(true);
            return;
        }
        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver != null && resolver.resolve()) {
            if (anyProtected(level, resolver.getToPush()) || anyProtected(level, resolver.getToDestroy())) {
                event.setCanceled(true);
            }
        }
    }

    private static boolean anyProtected(LevelAccessor level, List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (protectedAt(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** Lava encontrando agua (pedregulho, obsidiana) dentro da estrutura nao vira nada. */
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (protectedAt(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (protectedAt(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** Enderman, creeper, wither: dentro da estrutura, nenhuma criatura mexe em bloco. */
    public static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (entity != null && protectedAt(entity.level(), entity.blockPosition())) {
            event.setCanGrief(false);
        }
    }

    private MonolithGuard() {}
}
