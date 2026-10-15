package com.nexussmp.nexuscore.compat.envelope.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.compat.envelope.EnvelopePayloads;
import com.nexussmp.nexuscore.core.client.StandMirror;
import com.nexussmp.nexuscore.morph.MorphEntities;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Desenha um morcego no lugar do pombo que leva carta de lorde.
 *
 * <p>O servidor avisa quais pombos sao morcegos ({@link EnvelopePayloads.BatCourier}). Para cada um,
 * um morcego do jogo — o mesmo que voa nas cavernas, com o modelo e as animacoes de sempre — e
 * criado so aqui, nunca entra no mundo, e e desenhado onde o pombo estaria. A cada tick ele copia do
 * pombo a posicao, a direcao e a idade, e bate as asas no mesmo ritmo.
 *
 * <p>Se o desenho do morcego quebrar por qualquer motivo, o pombo aparece normal dali em diante: um
 * pombo no lugar do morcego e melhor do que um buraco no ceu.
 */
public final class BatCourierClient {
    private static final Int2ObjectMap<Bat> BATS = new Int2ObjectOpenHashMap<>();
    private static boolean broken;

    /** Chega do servidor: este pombo passou a ser morcego, ou deixou de ser. */
    public static void apply(EnvelopePayloads.BatCourier payload) {
        if (!payload.bat()) {
            BATS.remove(payload.entity());
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(payload.entity()) instanceof Pigeon)) {
            return;
        }
        Bat bat = EntityType.BAT.create(level);
        if (bat == null) {
            return;
        }
        bat.setResting(false);
        bat.setSilent(true);
        bat.addTag(MorphEntities.STAND_TAG);
        BATS.put(payload.entity(), bat);
    }

    static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (broken || !(event.getEntity() instanceof Pigeon pigeon)) {
            return;
        }
        Bat bat = batFor(pigeon);
        if (bat == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        StandMirror.mirror(pigeon, bat);
        EntityRenderer<? super Bat> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bat);

        // Uma pilha so nossa, copiada da do jogo, como no anel da metamorfose: se o desenho quebrar no
        // meio, a pilha do jogo continua inteira.
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(event.getPoseStack().last().pose());
        poseStack.last().normal().set(event.getPoseStack().last().normal());
        Vec3 offset = renderer.getRenderOffset(bat, partialTick)
                .subtract(pigeonOffset(event, pigeon, partialTick));
        poseStack.translate(offset.x, offset.y, offset.z);

        float yaw = Mth.rotLerp(partialTick, pigeon.yRotO, pigeon.getYRot());
        try {
            renderer.render(bat, yaw, partialTick, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException | LinkageError error) {
            broken = true;
            NexusCore.LOGGER.warn("[NexusCore] envelope: o desenho do morcego carteiro quebrou; "
                    + "os carteiros do lorde aparecem como pombo", error);
            return;
        }
        event.setCanceled(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Vec3 pigeonOffset(RenderLivingEvent.Pre<?, ?> event, Pigeon pigeon, float partialTick) {
        return ((EntityRenderer) event.getRenderer()).getRenderOffset(pigeon, partialTick);
    }

    /** O morcego bate as asas no ritmo do pombo: a animacao de voo do morcego anda a cada tick. */
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (broken || BATS.isEmpty() || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (Int2ObjectMap.Entry<Bat> entry : BATS.int2ObjectEntrySet()) {
            if (minecraft.level.getEntity(entry.getIntKey()) instanceof Pigeon pigeon) {
                Bat bat = entry.getValue();
                StandMirror.mirror(pigeon, bat);
                try {
                    bat.tick();
                } catch (RuntimeException | LinkageError error) {
                    broken = true;
                    NexusCore.LOGGER.warn("[NexusCore] envelope: o tick do morcego carteiro quebrou; "
                            + "os carteiros do lorde aparecem como pombo", error);
                    return;
                }
                // O tick mexe um pouco no morcego; quem manda na posicao continua sendo o pombo.
                StandMirror.mirror(pigeon, bat);
            }
        }
    }

    /**
     * Esta entidade esta sendo desenhada como morcego agora? E o que o Jade pergunta para mostrar
     * "Morcego" no lugar de "Pombo" — se o desenho quebrou e ele aparece como pombo, continua pombo.
     */
    public static boolean isBat(Entity entity) {
        return !broken && entity instanceof Pigeon pigeon && batFor(pigeon) != null;
    }

    @Nullable
    private static Bat batFor(Pigeon pigeon) {
        Bat bat = BATS.get(pigeon.getId());
        return bat != null && bat.level() == pigeon.level() ? bat : null;
    }

    /** O pombo saiu de vista (ou do mundo): o morcego dele vai junto. */
    static void onLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Pigeon pigeon) {
            BATS.remove(pigeon.getId());
        }
    }

    /** Trocou de mundo ou saiu do servidor: os numeros das entidades de antes nao valem mais. */
    static void clear() {
        BATS.clear();
        broken = false;
    }

    private BatCourierClient() {}
}
