package com.nexussmp.nexuscore.compat.envelope;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Onde um pombo pode surgir e sumir sem que ninguem veja.
 *
 * <p>"Ninguem ve" e medido do jeito que uma pessoa nota as coisas: o ponto tem de estar na frente
 * dela (ate 75 graus do centro da visao, um pouco alem do que a tela mostra), perto o bastante para
 * nao ser um pontinho, e sem bloco no caminho. Quem esta de costas, longe ou do outro lado de uma
 * parede nao conta.
 */
final class CourierSky {
    /** Cosseno do angulo a partir do centro da visao em que ainda se nota um pombo. */
    private static final double NOTICE_COS = Math.cos(Math.toRadians(75.0));
    /** Mais longe que isso, um pombo e um pontinho no ceu: nao conta como visto. */
    private static final double NOTICE_DISTANCE = 96.0;
    /** Direcoes tentadas em volta do ponto de referencia. */
    private static final int DIRECTIONS = 16;
    /** Espaco de um pombo, para nao surgir dentro de uma arvore. */
    private static final AABB POCKET = new AABB(-0.4, 0.0, -0.4, 0.4, 0.7, 0.4);
    /** Nunca rente ao chao: pelo menos isto acima do que tiver embaixo. */
    private static final int ABOVE_GROUND = 8;

    /**
     * A distancia em que o Envelope poe o pombo para surgir e sumir ao sair e chegar numa caixa. Ele
     * usa 16 para fora e 16 para cima: perto o bastante para ver o pombo virar fumaca no ar. Com o
     * modulo ligado, vale a da configuracao, se for maior.
     */
    static int ascendDistance(int envelopeDistance) {
        if (!CommonConfig.isModuleEnabled(ModuleIds.COMPAT_ENVELOPE)) {
            return envelopeDistance;
        }
        return Math.max(envelopeDistance, ServerConfig.get(ServerConfig.ENVELOPE_ASCEND_DISTANCE));
    }

    /**
     * Um ponto no ceu, {@code radius} blocos em volta de {@code anchor} e pelo menos {@code lift}
     * acima dele, que ninguem esteja olhando. Se nenhum servir, o menos visto. {@code null} se nao
     * houver nenhum com o mundo carregado e ativo — um pombo ali nem se mexeria.
     */
    @Nullable
    static Vec3 hiddenPoint(ServerLevel level, Vec3 anchor, double radius, double lift, RandomSource random) {
        double start = random.nextDouble() * Math.PI * 2.0;
        Vec3 best = null;
        int bestSeen = Integer.MAX_VALUE;
        for (int i = 0; i < DIRECTIONS; i++) {
            double angle = start + i * (Math.PI * 2.0 / DIRECTIONS);
            double x = anchor.x + Math.cos(angle) * radius;
            double z = anchor.z + Math.sin(angle) * radius;
            BlockPos column = BlockPos.containing(x, anchor.y, z);
            if (!level.isPositionEntityTicking(column)) {
                continue;
            }
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            double y = Mth.clamp(Math.max(anchor.y + lift, ground + ABOVE_GROUND),
                    level.getMinBuildHeight(), level.getMaxBuildHeight() - 2.0);
            Vec3 point = new Vec3(x, y, z);
            if (!level.noCollision(POCKET.move(point))) {
                continue;
            }
            int seen = watchers(level, point);
            if (seen == 0) {
                return point;
            }
            if (seen < bestSeen) {
                best = point;
                bestSeen = seen;
            }
        }
        return best;
    }

    /** Quantas pessoas estao olhando para este ponto agora. */
    static int watchers(ServerLevel level, Vec3 point) {
        int seen = 0;
        for (ServerPlayer player : level.players()) {
            Vec3 eye = player.getEyePosition();
            Vec3 toward = point.subtract(eye);
            double distance = toward.length();
            if (distance > NOTICE_DISTANCE) {
                continue;
            }
            if (distance > 1.0E-3 && player.getViewVector(1.0F).dot(toward.scale(1.0 / distance)) < NOTICE_COS) {
                continue;
            }
            ClipContext sight = new ClipContext(eye, point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);
            if (level.clip(sight).getType() == HitResult.Type.MISS) {
                seen++;
            }
        }
        return seen;
    }

    /** Ninguem olhando e ninguem colado: da para sumir sem ser notado. */
    static boolean unnoticed(ServerLevel level, Vec3 point, double closeDistance) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(point) < closeDistance * closeDistance) {
                return false;
            }
        }
        return watchers(level, point) == 0;
    }

    private CourierSky() {}
}
