package com.nexussmp.nexuscore.quest;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda em qual quest board cada jogador esta mexendo.
 *
 * <p>Assumir e criar so valem com o mural na frente: o servidor confere a dimensao, a distancia e se
 * o bloco ainda existe. Sem isso, qualquer um poderia mandar o pacote de longe. O resto — desistir,
 * dizer que fez, conferir, apagar o que e seu — e coisa de diario e vale em qualquer lugar, pelo
 * livro do inventario (ver {@code QuestServerHandler}).
 */
public final class QuestBoards {
    /** Depois disto o "estou no mural" vence, mesmo com a tela aberta esquecida. */
    private static final long VALID_MILLIS = 10 * 60 * 1000L;

    private record OpenBoard(ResourceKey<Level> dimension, BlockPos pos, long time) {}

    private static final Map<UUID, OpenBoard> OPEN = new HashMap<>();

    public static void opened(ServerPlayer player, BlockPos pos) {
        OPEN.put(player.getUUID(), new OpenBoard(player.level().dimension(), pos, System.currentTimeMillis()));
    }

    public static boolean atBoard(ServerPlayer player) {
        OpenBoard board = OPEN.get(player.getUUID());
        if (board == null || board.dimension() != player.level().dimension()) {
            return false;
        }
        if (System.currentTimeMillis() - board.time() > VALID_MILLIS) {
            OPEN.remove(player.getUUID());
            return false;
        }
        int range = QuestSettings.current().boardRange();
        if (!player.blockPosition().closerThan(board.pos(), range)) {
            return false;
        }
        return player.level().getBlockState(board.pos()).is(NexusRegistries.QUEST_BOARD.get());
    }

    public static void forget(UUID player) {
        OPEN.remove(player);
    }

    public static void clear() {
        OPEN.clear();
    }

    private QuestBoards() {}
}
