package com.nexussmp.nexuscore.parrot;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * O papagaio que mora no ombro de alguem.
 *
 * <p><b>Por que passageiro.</b> Ele "monta" na pessoa, como alguem monta num cavalo. E o unico jeito
 * de o jogo levar uma entidade grudada em outra sem atraso: o cliente reposiciona o passageiro a
 * cada tick, logo depois de mover o dono, e o desenho interpola os dois juntos. Uma entidade que
 * seguisse a pessoa por conta propria chegaria sempre um passo atrasada. O preco disso — o jogo
 * achar que quem leva passageiro e "montaria" e por isso esconder o nome dela e nao deixar ninguem
 * empurrar — e desfeito no {@code EntityShoulderParrotMixin}.
 *
 * <p><b>Ele nunca sai dali.</b> Nao toma dano, nao se afoga, nao pega fogo, nao some com a distancia,
 * nao aceita coleira, biscoito nem etiqueta. Nao e salvo: quem cuida dele e o {@link ParrotKeeper},
 * que poe um novo no ombro sempre que a pessoa aparece no mundo. Se por qualquer motivo ele se
 * soltar do dono, ele mesmo se apaga no tick seguinte, e o guardiao poe outro.
 *
 * <p><b>Os efeitos.</b> Tres sinais vao para o cliente: como ele chegou (com festa, pelo comando, ou
 * discreto, ao renascer junto com o dono), se esta indo embora, e se esta falando. O desenho
 * ({@code ShoulderParrotRenderer}) anima com eles; as particulas saem daqui mesmo, do lado do cliente.
 */
public final class ShoulderParrot extends Parrot {
    public static final byte ARRIVAL_NONE = 0;
    public static final byte ARRIVAL_QUIET = 1;
    public static final byte ARRIVAL_GRAND = 2;

    /** Quanto dura a chegada. Com festa, ele desce girando e da um pulinho ao pousar. */
    public static final int ARRIVAL_TICKS = 18;
    /** Quanto dura a saida: encolhe girando e some num estalo de penas. */
    public static final int LEAVING_TICKS = 12;

    private static final EntityDataAccessor<Byte> DATA_ARRIVAL =
            SynchedEntityData.defineId(ShoulderParrot.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_LEAVING =
            SynchedEntityData.defineId(ShoulderParrot.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TALKING =
            SynchedEntityData.defineId(ShoulderParrot.class, EntityDataSerializers.BOOLEAN);

    /** Largura do jogador: e por ela que se sabe se o dono ainda tem ombro (ou virou bicho no anel). */
    private static final float PLAYER_WIDTH = 0.6F;
    /** Onde fica o ombro, em blocos a partir do pe: de lado, e a altura em pe e agachado. */
    private static final double SHOULDER_SIDE = 0.38;
    private static final double SHOULDER_STANDING = 1.40;
    private static final double SHOULDER_CROUCHING = 1.10;
    /** Um papagaio no ombro o dia inteiro nao pode piar a cada cinco segundos como um solto. */
    private static final int AMBIENT_INTERVAL_TICKS = 1200;
    /** Depois de repetir alguem, as vezes ele solta um pio — como quem gostou do que disse. */
    private static final float SQUAWK_AFTER_TALK = 0.35F;
    /** Ate onde ele repara em alguem para olhar. */
    private static final double LOOK_RANGE = 8.0;
    /** Graus por tick ao virar a cabeca, e o quanto ela inclina para cima e para baixo. */
    private static final float LOOK_TURN_SPEED = 12.0F;
    private static final float LOOK_MAX_PITCH = 40.0F;

    // ---- servidor ----
    @Nullable
    private UUID owner;
    @Nullable
    private Player lookTarget;
    @Nullable
    private Vec3 lookSpot;
    private int lookLeft;
    private int arrivalLeft;
    private int leavingLeft;
    private int talkLeft;

    // ---- cliente: quando cada animacao comecou, no relogio desta entidade ----
    private int leavingSince = -1;
    private int talkingSince = -1;

    public ShoulderParrot(EntityType<? extends ShoulderParrot> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(true);
        setInSittingPose(true);
    }

    /**
     * Poe um papagaio no ombro de alguem.
     *
     * @return o papagaio, ou {@code null} se o mundo recusou a entidade
     */
    @Nullable
    static ShoulderParrot perchOn(ServerPlayer player, ParrotOwners.Perch perch, byte arrival) {
        ServerLevel level = player.serverLevel();
        ShoulderParrot parrot = NexusRegistries.SHOULDER_PARROT.get().create(level);
        if (parrot == null) {
            return null;
        }
        parrot.owner = player.getUUID();
        parrot.setVariant(perch.color().variant());
        parrot.setCustomName(Component.literal(perch.name()));
        parrot.arrivalLeft = ARRIVAL_TICKS;
        parrot.entityData.set(DATA_ARRIVAL, arrival);
        Vec3 spot = player.position().add(perchOffset(player));
        parrot.moveTo(spot.x, spot.y, spot.z, player.yBodyRot, 0.0F);
        if (!level.addFreshEntity(parrot)) {
            return null;
        }
        parrot.startRiding(player, true);
        // O jogo avisa quem monta em quem so para quem esta em volta, nunca para a propria montaria —
        // e o papagaio chegou ao cliente do dono ainda solto, ao entrar no mundo, um instante antes de
        // montar. Sem este aviso, para o dono ele ficava parado no ar onde nasceu.
        player.connection.send(new ClientboundSetPassengersPacket(player));
        if (arrival == ARRIVAL_GRAND) {
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.25F);
            level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2F, 1.4F);
        }
        return parrot;
    }

    /** Comeca a ir embora. Ele termina sozinho, encolhendo, e some no fim. */
    public void leave() {
        if (isLeaving()) {
            return;
        }
        leavingLeft = LEAVING_TICKS;
        entityData.set(DATA_LEAVING, true);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.0F, 1.3F);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 0.8F);
    }

    /** Ele esta repetindo alguem por {@code ticks}: o desenho mexe junto com a fala. */
    public void startTalking(int ticks) {
        talkLeft = Math.max(ticks, 1);
        entityData.set(DATA_TALKING, true);
    }

    public byte arrival() {
        return entityData.get(DATA_ARRIVAL);
    }

    public boolean isLeaving() {
        return entityData.get(DATA_LEAVING);
    }

    public boolean isTalking() {
        return entityData.get(DATA_TALKING);
    }

    /** Ha quantos ticks ele comecou a ir embora, so no cliente; -1 se nao esta indo. */
    public int leavingSince() {
        return leavingSince;
    }

    /** Ha quantos ticks ele comecou a falar, so no cliente; -1 se esta calado. */
    public int talkingSince() {
        return talkingSince;
    }

    public ParrotColor color() {
        return ParrotColor.of(getVariant());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ARRIVAL, ARRIVAL_NONE);
        builder.define(DATA_LEAVING, false);
        builder.define(DATA_TALKING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (!level().isClientSide()) {
            return;
        }
        if (DATA_LEAVING.equals(accessor)) {
            leavingSince = isLeaving() ? tickCount : -1;
        } else if (DATA_TALKING.equals(accessor)) {
            talkingSince = isTalking() ? tickCount : -1;
        }
    }

    // ---- o ombro ----

    /**
     * Onde ele fica, a partir do pe do dono.
     *
     * <p>No ombro esquerdo, girando com o corpo. Quem nao tem ombro no momento — virou bicho no anel,
     * esta nadando, planando ou deitado — leva o papagaio nas costas, em cima.
     */
    static Vec3 perchOffset(Entity vehicle) {
        float scale = vehicle instanceof LivingEntity living ? living.getScale() : 1.0F;
        if (vehicle instanceof Player player && Math.abs(player.getBbWidth() - PLAYER_WIDTH * scale) < 0.01F) {
            Pose pose = player.getPose();
            if (pose == Pose.STANDING || pose == Pose.CROUCHING) {
                double height = pose == Pose.CROUCHING ? SHOULDER_CROUCHING : SHOULDER_STANDING;
                float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
                return new Vec3(Mth.cos(yaw) * SHOULDER_SIDE, height, Mth.sin(yaw) * SHOULDER_SIDE).scale(scale);
            }
        }
        return new Vec3(0.0, vehicle.getBbHeight(), 0.0);
    }

    /**
     * O jogo poe o passageiro onde o dono manda (o alto da cabeca, no caso de gente) menos este ponto.
     * Devolvendo a diferenca entre os dois, ele vai parar no ombro.
     */
    @Override
    public Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        Vec3 seat = vehicle.getPassengerRidingPosition(this).subtract(vehicle.position());
        return seat.subtract(perchOffset(vehicle));
    }

    /**
     * O corpo aponta para onde o corpo do dono aponta. A cabeca nao: ela e dele, e quem decide para
     * onde olha e o {@link #lookAround}.
     */
    @Override
    public void rideTick() {
        super.rideTick();
        if (getVehicle() instanceof LivingEntity vehicle) {
            setYRot(vehicle.yBodyRot);
            yBodyRot = vehicle.yBodyRot;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientEffects();
        } else {
            serverTick();
        }
    }

    private void serverTick() {
        Entity vehicle = getVehicle();
        if (!(vehicle instanceof ServerPlayer player) || !player.getUUID().equals(owner)
                || player.isRemoved() || player.level() != level()) {
            // Soltou do dono: nao existe papagaio solto. O guardiao poe outro no ombro.
            discard();
            return;
        }
        setInvisible(player.isInvisible());
        lookAround(player);
        if (arrivalLeft > 0 && --arrivalLeft == 0) {
            entityData.set(DATA_ARRIVAL, ARRIVAL_NONE);
        }
        if (talkLeft > 0 && --talkLeft == 0) {
            entityData.set(DATA_TALKING, false);
            if (random.nextFloat() < SQUAWK_AFTER_TALK) {
                playSound(SoundEvents.PARROT_AMBIENT, 0.8F, getVoicePitch());
            }
        }
        if (isLeaving() && --leavingLeft <= 0) {
            discard();
        }
    }

    // ---- para onde ele olha (servidor) ----

    /**
     * A cabeca de um papagaio de verdade: de tempos em tempos ele escolhe o que olhar e fica ali um
     * pouco. Se tem alguem perto, quase sempre e essa pessoa; senao, um ponto qualquer em volta, ou
     * so para a frente. Falando, olha para quem esta ouvindo. Para o dono, nunca: grudado na cara
     * dele, ficava encarando a pessoa o tempo todo.
     *
     * <p>Sem IA (ele nao tem metas), o controle de olhar do jogo e tocado aqui na mao — e o mesmo de
     * qualquer mob, com a mesma velocidade de virar, e o jogo manda a cabeca para os clientes.
     */
    private void lookAround(ServerPlayer owner) {
        Player nearest = nearestOther(owner);
        if (lookTarget != null && (lookTarget.isRemoved() || lookTarget.level() != level()
                || distanceToSqr(lookTarget) > LOOK_RANGE * LOOK_RANGE)) {
            lookTarget = null;
            lookLeft = 0;
        }

        if (isTalking() && nearest != null) {
            lookTarget = nearest;
            lookSpot = null;
            lookLeft = Math.max(lookLeft, 10);
        } else if (--lookLeft <= 0) {
            float roll = random.nextFloat();
            lookTarget = null;
            lookSpot = null;
            if (nearest != null && roll < 0.6F) {
                lookTarget = nearest;
                lookLeft = 40 + random.nextInt(80);
            } else if (roll < 0.85F) {
                float yaw = owner.yBodyRot + (random.nextFloat() * 2.0F - 1.0F) * 80.0F;
                float pitch = random.nextFloat() * 45.0F - 25.0F;
                lookSpot = getEyePosition().add(Vec3.directionFromRotation(pitch, yaw).scale(3.0));
                lookLeft = 20 + random.nextInt(40);
            } else {
                lookLeft = 30 + random.nextInt(60);
            }
        }

        if (lookTarget != null) {
            getLookControl().setLookAt(lookTarget, LOOK_TURN_SPEED, LOOK_MAX_PITCH);
        } else if (lookSpot != null) {
            getLookControl().setLookAt(lookSpot.x, lookSpot.y, lookSpot.z, LOOK_TURN_SPEED, LOOK_MAX_PITCH);
        }
        getLookControl().tick();
    }

    @Nullable
    private Player nearestOther(ServerPlayer owner) {
        Player best = null;
        double bestDistance = LOOK_RANGE * LOOK_RANGE;
        for (Player other : level().players()) {
            double distance = distanceToSqr(other);
            if (other != owner && !other.isSpectator() && distance <= bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---- particulas (so cliente) ----

    private void clientEffects() {
        if (isInvisible()) {
            return;
        }
        DustParticleOptions feather = new DustParticleOptions(color().particleColor(), 0.9F);
        if (arrival() == ARRIVAL_GRAND && tickCount < ARRIVAL_TICKS) {
            // Uma espiral de penas que desce junto com ele ate o ombro.
            float progress = tickCount / (float) ARRIVAL_TICKS;
            double height = (1.0 - progress) * 1.6 + 0.2;
            double radius = 0.55 * (1.0 - progress) + 0.15;
            for (int i = 0; i < 2; i++) {
                double angle = tickCount * 0.9 + i * Math.PI;
                level().addParticle(feather, getX() + Math.cos(angle) * radius, getY() + height,
                        getZ() + Math.sin(angle) * radius, 0.0, 0.0, 0.0);
            }
            if (tickCount == ARRIVAL_TICKS - 6) {
                burst(feather, 10);
            }
        }
        if (isLeaving() && leavingSince >= 0) {
            // Subindo em espiral enquanto encolhe.
            int age = tickCount - leavingSince;
            double angle = age * 1.1;
            level().addParticle(feather, getX() + Math.cos(angle) * 0.35, getY() + 0.2 + age * 0.06,
                    getZ() + Math.sin(angle) * 0.35, 0.0, 0.02, 0.0);
        }
    }

    /** Sumiu: se estava indo embora, sai um estalo de penas no lugar. */
    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        if (isLeaving() && !isInvisible()) {
            burst(new DustParticleOptions(color().particleColor(), 1.1F), 14);
            for (int i = 0; i < 5; i++) {
                level().addParticle(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(),
                        random.nextGaussian() * 0.04, random.nextGaussian() * 0.04 + 0.02, random.nextGaussian() * 0.04);
            }
        }
    }

    private void burst(DustParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            level().addParticle(particle, getX() + random.nextGaussian() * 0.2, getY() + 0.3 + random.nextGaussian() * 0.15,
                    getZ() + random.nextGaussian() * 0.2, 0.0, 0.0, 0.0);
        }
    }

    // ---- o que um papagaio solto faz e este nao ----

    @Override
    protected void registerGoals() {
        // Nada de voar atras do dono, sentar, passear: ele ja esta onde tem de estar.
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    /**
     * Para quem o carrega, ele nao entra na mira: esta grudado na cara da pessoa, e pegaria todo
     * clique que ela desse para o lado. Para os outros, da para mirar nele — e o Jade mostra o nome.
     * (O Jade nao pergunta isto; ele e tirado da mira do dono no {@code NexusJadePlugin}.)
     */
    @Override
    public boolean isPickable() {
        return !isOnLocalShoulder() && super.isPickable();
    }

    /** No cliente: este papagaio esta no ombro de quem esta jogando aqui? */
    public boolean isOnLocalShoulder() {
        return getVehicle() instanceof Player player && player.isLocalPlayer();
    }

    /** Flecha mirada no ombro de alguem acerta a pessoa, nao quica no papagaio. */
    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return AMBIENT_INTERVAL_TICKS;
    }

    @Override
    protected boolean isFlapping() {
        return false;
    }
}
