package com.nexussmp.nexuscore.hologram.entity;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.core.util.Profiles;
import com.nexussmp.nexuscore.hologram.HologramRecording;
import com.nexussmp.nexuscore.hologram.client.HologramClientSettings;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

/**
 * O holograma: uma entidade que refaz, tick a tick, o que o jogador gravou.
 *
 * <p>Quem manda e o servidor: ele le a gravacao e move a entidade, e o jogo sincroniza posicao, rotacao,
 * pose e itens para todo mundo pelos mesmos pacotes de sempre. Por isso todos os jogadores veem a mesma
 * apresentacao, e quem chega no meio ve o holograma no ponto certo, sem nada de especial.
 *
 * <p>Nao colide, nao empurra, nao leva dano, nao e salva no mundo: se o servidor cair, nao sobra
 * holograma perdido por ai.
 */
public final class HologramEntity extends LivingEntity {
    /**
     * O perfil de quem gravou — nome, id e textura.
     *
     * <p>Mandar so o id nao basta: o cliente nao sabe buscar skin por id, so desembrulhar a textura
     * que vem junto do perfil. Sem isto, um holograma de alguem que nao esta online aparece com a
     * cara padrao, mesmo com a skin certa existindo no servidor.
     */
    private static final EntityDataAccessor<CompoundTag> DATA_AUTHOR =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DATA_LEFT_HANDED =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> items = NonNullList.withSize(EquipmentSlot.values().length, ItemStack.EMPTY);

    @Nullable
    private HologramRecording recording;
    /** O perfil do autor ja lido, e o tag de onde ele saiu. */
    @Nullable
    private CompoundTag authorTag;
    @Nullable
    private GameProfile author;
    private Vec3 playbackOrigin = Vec3.ZERO;
    private float yawOffset;
    private boolean loop;
    private int frame;
    private int nextEquipmentChange;

    public HologramEntity(EntityType<? extends HologramEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
        noPhysics = true;
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 20.0);
    }

    /** Preparado no servidor logo depois de nascer. */
    public void startPlayback(HologramRecording recording, Vec3 origin, float yawOffset, float scale, boolean loop) {
        this.recording = recording;
        this.playbackOrigin = origin;
        this.yawOffset = yawOffset;
        this.loop = loop;
        this.frame = 0;
        this.nextEquipmentChange = 0;
        setScale(scale);
        // Gravacao antiga nao guardava o perfil; ai vale o que da para montar com id e nome.
        CompoundTag author = recording.authorProfile();
        setAuthor(author != null && !author.isEmpty() ? author
                : Profiles.write(new GameProfile(recording.authorId(), recording.authorName())));
        entityData.set(DATA_LEFT_HANDED, recording.mainArm() == HumanoidArm.LEFT);
        refreshDimensions();
        applyFrame(0);
    }

    @Nullable
    public String recordingName() {
        return recording == null ? null : recording.name();
    }

    public int frame() {
        return frame;
    }

    /**
     * O tamanho passa pelo atributo {@code minecraft:generic.scale}, o mesmo que o jogo usa para
     * qualquer mob gigante: com ele, modelo, hitbox e altura dos olhos crescem juntos e na mesma
     * conta do jogo. Os pes continuam no ponto onde o holograma foi posto; ele cresce para cima.
     */
    public void setScale(float scale) {
        AttributeInstance attribute = getAttribute(Attributes.SCALE);
        if (attribute != null) {
            attribute.setBaseValue(scale);
        }
    }

    public float scale() {
        return getScale();
    }

    /**
     * O perfil do autor, lido uma vez por atualizacao: enquanto o tag for o mesmo objeto, o perfil
     * ja lido serve. O desenho pede isto a cada quadro.
     */
    @Nullable
    public GameProfile authorProfile() {
        CompoundTag tag = entityData.get(DATA_AUTHOR);
        if (tag != authorTag) {
            authorTag = tag;
            author = Profiles.read(tag);
        }
        return author;
    }


    private void setAuthor(CompoundTag profile) {
        authorTag = profile;
        author = Profiles.read(profile);
        entityData.set(DATA_AUTHOR, profile);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AUTHOR, new CompoundTag());
        builder.define(DATA_LEFT_HANDED, false);
    }

    @Override
    public void tick() {
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide) {
            if (recording == null) {
                discard();
                return;
            }
            advance();
        }
        // super.tick() cuida da interpolacao no cliente e da animacao de andar e de bracos.
        super.tick();
        if (level().isClientSide) {
            spawnProjectionParticles();
        }
    }

    /**
     * Poeira subindo em volta, como a luz da projecao. So no cliente, e so a cada dois ticks.
     *
     * <p>Liga, desliga e cor vem do {@link HologramClientSettings}, o mesmo lugar de onde o shader
     * tira os dele: a config e lida uma vez, quando carrega, e nao aqui a cada particula.
     */
    private void spawnProjectionParticles() {
        if (tickCount % 2 != 0 || !HologramClientSettings.particles()) {
            return;
        }
        RandomSource random = level().random;
        float scale = scale();
        Vector3f color = new Vector3f(HologramClientSettings.red(), HologramClientSettings.green(),
                HologramClientSettings.blue());
        DustParticleOptions dust = new DustParticleOptions(color, 0.6F + random.nextFloat() * 0.4F);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = (0.30 + random.nextDouble() * 0.25) * scale;
        level().addParticle(dust,
                getX() + Math.cos(angle) * radius,
                getY() + random.nextDouble() * 1.9 * scale,
                getZ() + Math.sin(angle) * radius,
                0.0, 0.015 + random.nextDouble() * 0.02, 0.0);
        if (random.nextInt(16) == 0) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    getX() + (random.nextDouble() - 0.5) * 0.8 * scale,
                    getY() + random.nextDouble() * 1.8 * scale,
                    getZ() + (random.nextDouble() - 0.5) * 0.8 * scale,
                    0.0, 0.0, 0.0);
        }
    }

    private void advance() {
        if (recording == null) {
            return;
        }
        if (frame >= recording.tickCount()) {
            if (!loop) {
                discard();
                return;
            }
            frame = 0;
            nextEquipmentChange = 0;
            items.replaceAll(stack -> ItemStack.EMPTY);
        }
        applyFrame(frame);
        frame++;
    }

    private void applyFrame(int tick) {
        if (recording == null || tick >= recording.tickCount()) {
            return;
        }
        double dx = recording.x(tick);
        double dz = recording.z(tick);
        if (yawOffset != 0.0F) {
            float radians = yawOffset * Mth.DEG_TO_RAD;
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            double rotatedX = dx * cos - dz * sin;
            dz = dx * sin + dz * cos;
            dx = rotatedX;
        }
        setPos(playbackOrigin.x + dx, playbackOrigin.y + recording.y(tick), playbackOrigin.z + dz);

        float body = Mth.wrapDegrees(recording.bodyYaw(tick) + yawOffset);
        float head = Mth.wrapDegrees(recording.headYaw(tick) + yawOffset);
        setYRot(body);
        yBodyRot = body;
        setYHeadRot(head);
        setXRot(recording.pitch(tick));
        if (tick == 0) {
            // Sem isto, o primeiro quadro interpola a partir de uma rotacao qualquer.
            yRotO = body;
            yBodyRotO = body;
            yHeadRotO = head;
            xRotO = getXRot();
            setOldPosAndRot();
        }

        setPose(recording.pose(tick));
        setSprinting(recording.flag(tick, HologramRecording.FLAG_SPRINTING));
        if (recording.flag(tick, HologramRecording.FLAG_SWING_MAIN)) {
            swing(InteractionHand.MAIN_HAND, true);
        }
        if (recording.flag(tick, HologramRecording.FLAG_SWING_OFF)) {
            swing(InteractionHand.OFF_HAND, true);
        }

        List<HologramRecording.EquipmentChange> changes = recording.equipment();
        while (nextEquipmentChange < changes.size() && changes.get(nextEquipmentChange).tick() <= tick) {
            HologramRecording.EquipmentChange change = changes.get(nextEquipmentChange++);
            items.set(change.slot().ordinal(), change.stack().copy());
        }
    }

    // ---- equipamento ----

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of(getItemBySlot(EquipmentSlot.FEET), getItemBySlot(EquipmentSlot.LEGS),
                getItemBySlot(EquipmentSlot.CHEST), getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return items.get(slot.ordinal());
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        items.set(slot.ordinal(), stack);
    }

    @Override
    public HumanoidArm getMainArm() {
        return entityData.get(DATA_LEFT_HANDED) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    // ---- e so uma projecao: nada de fisica, dano ou persistencia ----

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
