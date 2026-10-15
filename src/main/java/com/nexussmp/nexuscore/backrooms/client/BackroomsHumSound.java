package com.nexussmp.nexuscore.backrooms.client;

import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.backrooms.block.BackroomsLampBlock;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O zumbido do forro: sempre presente enquanto houver lampada acesa por perto.
 *
 * <p>Antes isso era um som solto que o servidor tocava de vez em quando na posicao da pessoa. Som
 * do mundo fica onde nasceu: bastava andar dez blocos para ele ficar para tras, e quem chegava
 * ouvia silencio ate o proximo. Zumbido de lampada nao funciona assim — ele e o barulho do lugar, e
 * lugar nenhum fica em silencio porque voce andou.
 *
 * <p>Entao ele virou um loop do cliente, sem posicao, que acompanha a pessoa. O que muda e o
 * <b>volume</b>: a cada meio segundo ele olha as lampadas acesas em volta e mira no volume da mais
 * perto, subindo e descendo aos poucos. Debaixo de um painel aceso ele enche o corredor; longe de
 * tudo, quase some; e quando o skinwalker apaga o andar inteiro, o zumbido morre junto — o silencio
 * na perseguicao passa a ser parte do susto, sem ninguem escrever isso em lugar nenhum.
 */
final class BackroomsHumSound extends AbstractTickableSoundInstance {
    /** Ate onde uma lampada ainda se faz ouvir, em blocos. */
    private static final int RADIUS = 10;
    /** Volume debaixo de uma lampada. */
    private static final float MAX_VOLUME = 0.9F;
    /** Piso enquanto houver qualquer lampada acesa por perto: o forro nunca fica mudo de todo. */
    private static final float FLOOR_VOLUME = 0.25F;
    /** De quanto em quanto tempo as lampadas em volta sao conferidas. */
    private static final int SCAN_INTERVAL_TICKS = 10;
    /** Quanto o volume anda por tick ate o alvo: meio segundo para sumir, nao um corte seco. */
    private static final float FADE_STEP = 0.02F;

    private float target;
    private int ticks;

    BackroomsHumSound() {
        super(NexusSounds.LIGHT_HUM.get(), SoundSource.AMBIENT, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        // Sem posicao e sem atenuacao: o zumbido esta em volta da pessoa, nao num ponto do mapa.
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    @Override
    public boolean canPlaySound() {
        return true;
    }

    /**
     * Nascer calado e permitido.
     *
     * <p>Este era o motivo de nao se ouvir lampada nenhuma: o som comeca em volume zero (ele sobe
     * conforme a lampada mais perto), e o jogo joga fora, antes de tocar, todo som que comeca em
     * zero — a menos que ele diga, aqui, que a intencao e essa.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.level().dimension().equals(BackroomsDimensions.BACKROOMS)) {
            stop();
            return;
        }
        if (ticks++ % SCAN_INTERVAL_TICKS == 0) {
            target = nearestLamp(player);
        }
        volume = Mth.approach(volume, target, FADE_STEP);
    }

    /**
     * O volume que a lampada acesa mais perto pede.
     *
     * <p>As lampadas ficam todas na mesma altura, entao a busca e um plano so — um quadrado de 17
     * por 17 a cada meio segundo, que e barato e nao precisa de lista guardada em lugar nenhum.
     */
    private static float nearestLamp(LocalPlayer player) {
        Level level = player.level();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int originX = player.getBlockX();
        int originZ = player.getBlockZ();
        float loudest = 0.0F;
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                cursor.set(originX + dx, BackroomsDimensions.CEILING_Y, originZ + dz);
                BlockState state = level.getBlockState(cursor);
                if (!(state.getBlock() instanceof BackroomsLampBlock) || !state.getValue(BackroomsLampBlock.LIT)) {
                    continue;
                }
                float distance = Mth.sqrt(dx * dx + dz * dz);
                loudest = Math.max(loudest, 1.0F - Math.min(1.0F, distance / RADIUS));
            }
        }
        if (loudest <= 0.0F) {
            return 0.0F;
        }
        // Com lampada acesa por perto, o zumbido nunca cai abaixo do piso: no corredor ele varia,
        // mas esta sempre la. Sem nenhuma acesa (o apagao da cacada), ai sim e silencio.
        return Math.max(FLOOR_VOLUME, loudest * MAX_VOLUME);
    }
}
