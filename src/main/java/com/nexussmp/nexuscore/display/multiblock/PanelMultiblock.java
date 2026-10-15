package com.nexussmp.nexuscore.display.multiblock;

import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.display.block.PanelBlock;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Junta paineis vizinhos (mesmo plano, mesma direcao) em telas retangulares.
 *
 * <p>Roda so quando um painel e colocado ou removido. Acha o grupo conectado (busca limitada), e divide em
 * retangulos de forma deterministica: a partir do canto inferior esquerdo, cresce para a direita e depois
 * para cima, ate o tamanho maximo da config. Assim qualquer bloco do grupo que recalcule chega no mesmo resultado.
 */
public final class PanelMultiblock {
    /** Teto da busca, para uma parede enorme de paineis nao travar o servidor. */
    private static final int MAX_GROUP = 1024;

    private record Cell(int u, int v, BlockPos pos, PanelBlockEntity panel) {}

    public static void rebuild(ServerLevel level, BlockPos start) {
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof PanelBlock)) {
            return;
        }
        Direction facing = startState.getValue(PanelBlock.FACING);
        Direction right = PanelBlock.screenRight(facing);
        int maxWidth = ServerConfig.get(ServerConfig.PANEL_MAX_WIDTH);
        int maxHeight = ServerConfig.get(ServerConfig.PANEL_MAX_HEIGHT);

        Long2ObjectOpenHashMap<Cell> cells = collectGroup(level, start, facing, right);
        List<Cell> ordered = new ArrayList<>(cells.values());
        ordered.sort(Comparator.comparingInt(Cell::v).thenComparingInt(Cell::u));

        Long2ObjectOpenHashMap<Cell> free = new Long2ObjectOpenHashMap<>(cells);
        for (Cell corner : ordered) {
            if (!free.containsKey(key(corner.u, corner.v))) {
                continue;
            }
            int width = 1;
            while (width < maxWidth && free.containsKey(key(corner.u + width, corner.v))) {
                width++;
            }
            int height = 1;
            while (height < maxHeight && rowFree(free, corner.u, corner.v + height, width)) {
                height++;
            }
            assign(level, free, corner, width, height);
        }
    }

    /** Agenda recalculo dos paineis vizinhos no plano (usado quando um bloco some). */
    public static void scheduleNeighbors(Level level, BlockPos pos, Direction facing) {
        Direction right = PanelBlock.screenRight(facing);
        for (BlockPos neighbor : new BlockPos[] {pos.relative(right), pos.relative(right.getOpposite()), pos.above(), pos.below()}) {
            BlockState state = level.getBlockState(neighbor);
            if (state.getBlock() instanceof PanelBlock && state.getValue(PanelBlock.FACING) == facing) {
                level.scheduleTick(neighbor, state.getBlock(), 1);
            }
        }
    }

    private static Long2ObjectOpenHashMap<Cell> collectGroup(ServerLevel level, BlockPos start, Direction facing, Direction right) {
        Long2ObjectOpenHashMap<Cell> cells = new Long2ObjectOpenHashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && cells.size() < MAX_GROUP) {
            BlockPos pos = queue.poll();
            int u = (pos.getX() - start.getX()) * right.getStepX() + (pos.getZ() - start.getZ()) * right.getStepZ();
            int v = pos.getY() - start.getY();
            long key = key(u, v);
            if (cells.containsKey(key) || !level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof PanelBlock) || state.getValue(PanelBlock.FACING) != facing
                    || !(level.getBlockEntity(pos) instanceof PanelBlockEntity panel)) {
                continue;
            }
            cells.put(key, new Cell(u, v, pos, panel));
            queue.add(pos.relative(right));
            queue.add(pos.relative(right.getOpposite()));
            queue.add(pos.above());
            queue.add(pos.below());
        }
        return cells;
    }

    private static boolean rowFree(Long2ObjectOpenHashMap<Cell> free, int u, int v, int width) {
        for (int i = 0; i < width; i++) {
            if (!free.containsKey(key(u + i, v))) {
                return false;
            }
        }
        return true;
    }

    private static void assign(ServerLevel level, Long2ObjectOpenHashMap<Cell> free, Cell corner, int width, int height) {
        // A tela nova herda pagina/dimensao da maior tela antiga que ocupava este retangulo.
        PanelBlockEntity previous = null;
        int previousArea = 0;
        List<Cell> rect = new ArrayList<>(width * height);
        for (int dv = 0; dv < height; dv++) {
            for (int du = 0; du < width; du++) {
                Cell cell = free.remove(key(corner.u + du, corner.v + dv));
                rect.add(cell);
                PanelBlockEntity panel = cell.panel;
                int area = panel.width() * panel.height();
                if (panel.isController() && area > previousArea) {
                    previous = panel;
                    previousArea = area;
                }
            }
        }

        PanelBlockEntity controller = corner.panel;
        if (previous != null && previous != controller) {
            controller.copySettingsFrom(previous);
        }
        BlockPos controllerPos = corner.pos;
        for (Cell cell : rect) {
            boolean isController = cell == corner;
            connect(level, cell, cell.u - corner.u, cell.v - corner.v, width, height);
            cell.panel.applyMultiblock(
                    controllerPos.getX() - cell.pos.getX(),
                    controllerPos.getY() - cell.pos.getY(),
                    controllerPos.getZ() - cell.pos.getZ(),
                    isController ? width : 1,
                    isController ? height : 1);
        }
    }

    /** Liga os lados que continuam na mesma tela, para a moldura ficar so no contorno externo. */
    private static void connect(ServerLevel level, Cell cell, int du, int dv, int width, int height) {
        BlockState state = level.getBlockState(cell.pos);
        BlockState connected = state
                .setValue(PanelBlock.CONNECTED_LEFT, du > 0)
                .setValue(PanelBlock.CONNECTED_RIGHT, du < width - 1)
                .setValue(PanelBlock.CONNECTED_DOWN, dv > 0)
                .setValue(PanelBlock.CONNECTED_UP, dv < height - 1);
        if (connected != state) {
            // UPDATE_KNOWN_SHAPE: nenhum vizinho recalcula a propria forma por causa desta troca.
            level.setBlock(cell.pos, connected, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static long key(int u, int v) {
        return ((long) u << 32) | (v & 0xFFFFFFFFL);
    }

    private PanelMultiblock() {}
}
