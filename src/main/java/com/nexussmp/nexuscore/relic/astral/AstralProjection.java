package com.nexussmp.nexuscore.relic.astral;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.relic.Relics;
import com.nexussmp.nexuscore.relic.refuge.Refuges;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A mascara astral: sair do proprio corpo por um tempo.
 *
 * <p><b>Sair.</b> Clique direito com a mascara. O corpo fica onde estava ({@link AstralBody}) e a
 * pessoa vira espectadora: voa, ninguem a ve, e ela enxerga o proprio corpo e os outros jogadores.
 * Os outros veem so o corpo parado.
 *
 * <p><b>Presa ao mundo.</b> A alma nao atravessa nada: bate em parede, chao e teto como o corpo
 * bateria ({@link #collides}, usado pelo {@code PlayerAstralMixin}). Caverna, so entrando pela boca
 * dela. Os mapas do Xaero tambem se fecham enquanto ela esta fora.
 *
 * <p><b>O fio.</b> A alma nao vai longe: passou de {@value #LEASH} blocos do corpo, volta para o
 * ultimo lugar em que ainda estava dentro do fio. Nao abre bau, nao entra na cabeca de bicho, nao se
 * teletransporta para outros jogadores pelo menu de espectador: ela esta em outro plano, so olhando.
 * E nao enxerga longe: uma nevoa escura fecha a visao, e fecha mais com o tempo ({@code AstralClient}).
 *
 * <p><b>Voltar.</b> Encostando no proprio corpo, ou pela tecla de voltar (B, por padrao). Voltar
 * tonteia, mais quanto mais tempo ficou fora. Se alguem bate no corpo, a pessoa e arrancada de volta
 * na hora e leva o golpe com {@value #BODY_PAIN} vezes a forca: o corpo estava indefeso.
 *
 * <p><b>A loucura.</b> Fora do corpo, a cabeca vai cedendo (o que a pessoa ve e ouve esta no cliente,
 * {@code AstralClient}). Passado o limite de {@value #MAX_TICKS} ticks (meio minuto), ela comeca a se
 * desfazer: dano a cada meio segundo, cada vez maior, que atravessa armadura e qualquer protecao, ate
 * voltar — ou morrer. Morrendo assim, ela cai no lugar do corpo, nao onde a alma estava.
 *
 * <p>Depois de usar, a mascara descansa {@value #COOLDOWN_TICKS} ticks (oito minutos).
 */
public final class AstralProjection {
    public static final int MAX_TICKS = 30 * 20;
    public static final int COOLDOWN_TICKS = 8 * 60 * 20;
    private static final double LEASH = 32.0;
    /** Passado o limite, um golpe a cada tantos ticks: meio segundo. */
    private static final int UNRAVEL_INTERVAL_TICKS = 10;
    /** Os primeiros instantes nao contam como "encostou no corpo" — a alma sai de dentro dele. */
    private static final int RETURN_GRACE_TICKS = 40;
    private static final float BODY_PAIN = 1.5F;
    /** Quanto tempo de tontura por tick fora, e o teto. */
    private static final int NAUSEA_DIVISOR = 4;
    private static final int NAUSEA_MAX_TICKS = 12 * 20;
    /** Quem apanhou ha menos que isto nao consegue se concentrar para sair. */
    private static final int CALM_TICKS = 5 * 20;

    public static final ResourceKey<DamageType> LOST = ResourceKey.create(Registries.DAMAGE_TYPE, NexusCore.id("lost_in_astral"));

    /** Quem esta fora do corpo agora, para as perguntas rapidas dos mixins (so servidor). */
    private static final Set<UUID> PROJECTING = new HashSet<>();
    /** O ultimo ponto de cada alma ainda dentro do fio: e para la que o fio puxa. */
    private static final Map<UUID, Vec3> LAST_INSIDE = new HashMap<>();
    /** Do lado do cliente: o jogador local esta fora do corpo? O servidor avisa pelo {@code State}. */
    private static boolean localProjecting;

    public static boolean isProjecting(Player player) {
        return !player.level().isClientSide() && PROJECTING.contains(player.getUUID());
    }

    /**
     * Esta alma bate nas coisas? O espectador comum atravessa tudo; fora do corpo pela mascara, nao.
     * Vale dos dois lados, porque quem move o jogador e o cliente: no cliente so o jogador local
     * importa, no servidor quem esta na lista.
     */
    public static boolean collides(Player player) {
        if (player.level().isClientSide()) {
            return localProjecting && player.isLocalPlayer();
        }
        return PROJECTING.contains(player.getUUID());
    }

    public static void setLocalProjecting(boolean projecting) {
        localProjecting = projecting;
    }

    /** Esta pessoa ainda esta fora do corpo? (o corpo pergunta, para saber se ainda tem por que existir) */
    static boolean isProjecting(UUID owner) {
        return PROJECTING.contains(owner);
    }

    // ---- sair ----

    static void start(ServerPlayer player, ItemStack mask) {
        ServerLevel level = player.serverLevel();
        if (PROJECTING.contains(player.getUUID())) {
            return;
        }
        if (level.dimension().equals(BackroomsDimensions.BACKROOMS) || level.dimension().equals(Refuges.REFUGE)) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.astral_mask.blocked"));
            return;
        }
        if (!player.gameMode.getGameModeForPlayer().isSurvival() || player.isPassenger() || player.isSleeping()) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.astral_mask.cannot"));
            return;
        }
        if (player.getLastHurtByMob() != null && player.tickCount - player.getLastHurtByMobTimestamp() < CALM_TICKS) {
            Relics.refuse(player, Component.translatable("nexuscore.relic.astral_mask.restless"));
            return;
        }
        if (!Relics.ready(level, mask)) {
            Relics.refuse(player, Relics.restingFor(level, mask));
            return;
        }
        AstralBody body = NexusRegistries.ASTRAL_BODY.get().create(level);
        if (body == null) {
            return;
        }
        body.copyOf(player);
        if (!level.addFreshEntity(body)) {
            return;
        }
        AstralSessions.get(player.server).put(player.getUUID(), new AstralSessions.Session(level.dimension(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
                player.gameMode.getGameModeForPlayer(), level.getGameTime(), body.getUUID()));
        PROJECTING.add(player.getUUID());
        // Enquanto esta fora, a mascara tambem nao serve; a recarga de verdade comeca na volta.
        Relics.startCooldown(player, mask, MAX_TICKS + COOLDOWN_TICKS);

        player.setGameMode(GameType.SPECTATOR);
        PacketDistributor.sendToPlayer(player, new AstralPayloads.State(true, MAX_TICKS));
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.2, player.getZ(), 14, 0.3, 0.5, 0.3, 0.03);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    // ---- la fora ----

    public static void tick(MinecraftServer server) {
        if (PROJECTING.isEmpty()) {
            return;
        }
        AstralSessions sessions = AstralSessions.get(server);
        for (UUID id : Set.copyOf(PROJECTING)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            AstralSessions.Session session = sessions.get(id);
            if (player == null || session == null) {
                PROJECTING.remove(id);
                LAST_INSIDE.remove(id);
                continue;
            }
            tickOne(player, session);
        }
    }

    private static void tickOne(ServerPlayer player, AstralSessions.Session session) {
        // Alguem trocou o modo de jogo dela por comando: a viagem acabou ali, sem devolver nada.
        if (!player.isSpectator()) {
            forget(player);
            return;
        }
        ServerLevel level = player.server.getLevel(session.dimension());
        Entity body = level == null ? null : level.getEntity(session.body());
        if (body == null || player.level() != level) {
            end(player);
            return;
        }
        long elapsed = level.getGameTime() - session.startedAt();

        Vec3 anchor = body.position().add(0.0, 1.0, 0.0);
        if (player.position().distanceTo(anchor) > LEASH) {
            // Volta para onde ja esteve, e nao em linha reta ate o corpo: a linha reta pode cruzar
            // a terra, e a alma acabaria enfiada dentro dela.
            Vec3 back = LAST_INSIDE.getOrDefault(player.getUUID(), new Vec3(session.x(), session.y(), session.z()));
            player.teleportTo(level, back.x, back.y, back.z, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.translatable("nexuscore.relic.astral_mask.leash")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        } else {
            LAST_INSIDE.put(player.getUUID(), player.position());
        }

        if (elapsed > RETURN_GRACE_TICKS && player.getBoundingBox().intersects(body.getBoundingBox())) {
            end(player);
            return;
        }

        long over = elapsed - MAX_TICKS;
        if (over >= 0 && over % UNRAVEL_INTERVAL_TICKS == 0) {
            // 3, 4.5, 6, 7.5...: de vida cheia, uns dois segundos ate o fim.
            float damage = 3.0F + over / (float) UNRAVEL_INTERVAL_TICKS * 1.5F;
            DamageSource lost = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(LOST));
            player.hurt(lost, damage);
            player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
    }

    // ---- voltar ----

    /** Volta para o corpo: posicao, modo de jogo, e a tontura. */
    static void end(ServerPlayer player) {
        PROJECTING.remove(player.getUUID());
        LAST_INSIDE.remove(player.getUUID());
        AstralSessions.Session session = AstralSessions.get(player.server).remove(player.getUUID());
        if (session == null) {
            return;
        }
        ServerLevel level = player.server.getLevel(session.dimension());
        if (level == null) {
            level = player.server.overworld();
        }
        Entity body = level.getEntity(session.body());
        if (body != null) {
            body.discard();
        }
        player.teleportTo(level, session.x(), session.y(), session.z(), session.yaw(), session.pitch());
        GameType previous = session.previous() == GameType.SPECTATOR ? GameType.SURVIVAL : session.previous();
        player.setGameMode(previous);
        player.resetFallDistance();
        PacketDistributor.sendToPlayer(player, new AstralPayloads.State(false, 0));

        long elapsed = Math.max(0, level.getGameTime() - session.startedAt());
        int nausea = (int) Math.min(NAUSEA_MAX_TICKS, elapsed / NAUSEA_DIVISOR);
        if (nausea > 20) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0, false, false, true));
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(NexusRegistries.ASTRAL_MASK.get())) {
                Relics.startCooldown(player, stack, COOLDOWN_TICKS);
            }
        }
        level.sendParticles(ParticleTypes.SOUL, session.x(), session.y() + 1.2, session.z(), 10, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Sem devolver nada: o modo de jogo ja foi mudado por fora. So apaga o corpo e o registro. */
    private static void forget(ServerPlayer player) {
        PROJECTING.remove(player.getUUID());
        LAST_INSIDE.remove(player.getUUID());
        AstralSessions.Session session = AstralSessions.get(player.server).remove(player.getUUID());
        if (session != null) {
            ServerLevel level = player.server.getLevel(session.dimension());
            Entity body = level == null ? null : level.getEntity(session.body());
            if (body != null) {
                body.discard();
            }
        }
        PacketDistributor.sendToPlayer(player, new AstralPayloads.State(false, 0));
    }

    /** Bateram no corpo: a alma volta na marra e leva o golpe, mais forte. /kill so devolve. */
    static void onBodyHurt(AstralBody body, DamageSource source, float amount, boolean killCommand) {
        UUID owner = body.owner();
        ServerPlayer player = owner == null || body.getServer() == null ? null : body.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !PROJECTING.contains(owner)) {
            body.discard();
            return;
        }
        end(player);
        if (!killCommand) {
            player.hurt(source, amount * BODY_PAIN);
            player.displayClientMessage(Component.translatable("nexuscore.relic.astral_mask.pulled_back")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /** Apertou a tecla de voltar. */
    public static void onReturnRequest(ServerPlayer player) {
        if (PROJECTING.contains(player.getUUID())) {
            end(player);
        }
    }

    // ---- as bordas ----

    /** Morreu fora do corpo (a loucura passou do limite): cai no lugar do corpo, no modo de antes. */
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PROJECTING.contains(player.getUUID())) {
            end(player);
        }
    }

    /** Saindo do servidor no meio: volta antes, para nao ser salvo espectador e longe do corpo. */
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PROJECTING.contains(player.getUUID())) {
            end(player);
        }
    }

    /** Entrou com uma viagem pendurada (o servidor caiu no meio): volta para o corpo agora. */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && AstralSessions.get(player.server).get(player.getUUID()) != null) {
            end(player);
        }
    }

    /** Fora do corpo nao se abre nada: nem bau, nem porta, nem alavanca. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isProjecting(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void clear() {
        PROJECTING.clear();
        LAST_INSIDE.clear();
    }

    private AstralProjection() {}
}
