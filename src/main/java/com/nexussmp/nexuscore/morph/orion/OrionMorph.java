package com.nexussmp.nexuscore.morph.orion;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.morph.MorphForm;
import com.nexussmp.nexuscore.morph.MorphState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * O ORION como forma: quem um operador vira ({@link OrionCommands}) e o que muda nessa pessoa.
 *
 * <p><b>A marca.</b> Fica nos dados do jogador que o jogo guarda e leva junto na morte, entao a forma
 * continua depois de morrer e de sair e voltar. O {@code MorphEquip} le a marca a cada meio segundo e
 * poe a forma, acima de qualquer anel. So o {@code desvirar} tira.
 *
 * <ul>
 *   <li><b>Chat vira bip:</b> cada mensagem no chat sai com uma sequencia de bips de onde a pessoa
 *       esta, para todo mundo em volta ouvir. Mensagem maior, mais bips.</li>
 *   <li><b>Metade da velocidade</b> de um jogador, andando e correndo. As pernas dele tem 5 px (uns
 *       0,19 bloco, contra 0,75 de gente): na velocidade de jogador, o passo cobria um quarto do
 *       chao que ele andava, e ele deslizava. Mais devagar, e com as pernas no ritmo delas
 *       ({@code OrionRenderer}), o pe acompanha o chao. A camera nao "da zoom" por isso
 *       ({@link #correctFov}).</li>
 *   <li><b>Sem chat de voz:</b> o ORION nao fala. O microfone de quem esta nessa forma e cortado no
 *       servidor ({@link OrionVoicechatPlugin}), e a pessoa recebe um aviso de vez em quando.</li>
 * </ul>
 */
public final class OrionMorph {
    public static final ResourceLocation ID = NexusCore.id("orion");
    public static final MorphForm FORM = new MorphForm(ID, new CompoundTag());
    private static final String MARK = "nexuscore_orion";
    /** Um bip a cada 3 ticks, como o robozinho. */
    private static final int BIP_INTERVAL = 3;
    private static final int MIN_BIPS = 2;
    private static final int MAX_BIPS = 6;
    private static final long WARNING_MS = 8_000;
    /** Quanto da velocidade de jogador ele perde (multiplica tudo: andar, correr, poção). */
    public static final double SLOWDOWN = -0.5;
    private static final ResourceLocation SPEED = NexusCore.id("orion_speed");

    /** Bips que ainda faltam tocar, por pessoa. So na thread do servidor. */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    /** Quem esta na forma agora. Lido pela thread do chat de voz, por isso concorrente. */
    private static final Set<UUID> MUTED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LAST_WARNING = new ConcurrentHashMap<>();
    private static long ticks;

    /** Se um operador virou esta pessoa ORION. */
    public static boolean forced(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(MARK);
    }

    static void setForced(Player player, boolean orion) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (orion) {
            persisted.putBoolean(MARK, true);
        } else {
            persisted.remove(MARK);
        }
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static boolean isOrion(Player player) {
        MorphForm form = MorphState.form(player);
        return form != null && ID.equals(form.type());
    }

    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (isOrion(player)) {
            int bips = Math.clamp(MIN_BIPS + event.getRawText().length() / 15, MIN_BIPS, MAX_BIPS);
            // O evento do chat pode vir fora da thread do servidor; os bips tocam nela.
            player.server.execute(() -> PENDING.merge(player.getUUID(), bips, Math::max));
        }
    }

    public static void onServerTick(MinecraftServer server) {
        ticks++;
        if (ticks % 10 == 0) {
            MUTED.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean orion = isOrion(player);
                if (orion) {
                    MUTED.add(player.getUUID());
                }
                slow(player, orion);
            }
        }
        if (ticks % BIP_INTERVAL != 0 || PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !isOrion(player) || entry.getValue() <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(entry.getValue() - 1);
            float pitch = 1.25F + (player.getRandom().nextFloat() - 0.5F) * 0.5F;
            player.level().playSound(null, player.getX(), player.getY() + player.getBbHeight() * 0.7, player.getZ(),
                    SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.55F, pitch);
        }
    }

    /**
     * Poe ou tira a lentidao. Modificador temporario: nao vai para o disco, e a entidade nova (depois de
     * morrer ou trocar de dimensao) recebe de novo aqui, em ate meio segundo.
     */
    private static void slow(ServerPlayer player, boolean orion) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null || speed.hasModifier(SPEED) == orion) {
            return;
        }
        if (orion) {
            speed.addTransientModifier(new AttributeModifier(SPEED, SLOWDOWN, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(SPEED);
        }
    }

    /**
     * O jogo aperta a camera de quem esta mais lento, como numa pocao de lentidao. Aqui a conta do
     * jogo e refeita sem a lentidao do ORION: a camera fica como a de qualquer pessoa andando.
     *
     * @param fov o modificador que o jogo calculou
     * @return o modificador sem o efeito da lentidao
     */
    public static float correctFov(Player player, float fov) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        float walking = player.getAbilities().getWalkingSpeed();
        if (speed == null || !speed.hasModifier(SPEED) || walking == 0.0F) {
            return fov;
        }
        float slowed = (float) speed.getValue() / walking;
        float normal = slowed / (float) (1.0 + SLOWDOWN);
        return fov * (normal + 1.0F) / (slowed + 1.0F);
    }

    /** Chamado pela thread do chat de voz, a cada pacote de microfone. */
    static boolean muted(UUID player) {
        return MUTED.contains(player);
    }

    /** Avisa (sem encher) quem tenta falar no chat de voz como ORION. Na thread do servidor. */
    static void warn(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = LAST_WARNING.get(player.getUUID());
        if (last == null || now - last > WARNING_MS) {
            LAST_WARNING.put(player.getUUID(), now);
            player.displayClientMessage(Component.translatable("nexuscore.morph.orion.no_voice"), true);
        }
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
        MUTED.remove(player);
        LAST_WARNING.remove(player);
    }

    public static void clear() {
        PENDING.clear();
        MUTED.clear();
        LAST_WARNING.clear();
        ticks = 0;
    }

    private OrionMorph() {}
}
