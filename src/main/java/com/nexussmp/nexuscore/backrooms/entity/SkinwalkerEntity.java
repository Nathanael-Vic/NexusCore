package com.nexussmp.nexuscore.backrooms.entity;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.backrooms.BackroomsData;
import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.backrooms.BackroomsManager;
import com.nexussmp.nexuscore.backrooms.BackroomsSettings;
import com.nexussmp.nexuscore.backrooms.block.BackroomsLampBlock;
import com.nexussmp.nexuscore.backrooms.network.SkinwalkerPayload;
import com.nexussmp.nexuscore.backrooms.voice.BackroomsVoices;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import com.nexussmp.nexuscore.core.util.Profiles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O skinwalker: alguem que parece um jogador conhecido, ate nao parecer mais.
 *
 * <p>Ele vive em tres tempos. <b>Rondando</b>, usa a skin e o nome de uma pessoa do servidor, anda
 * como gente, para e observa de longe, funga, e de vez em quando solta um pedaco de fala dessa
 * pessoa gravado do chat de voz. <b>Se revelando</b>, trava no lugar, estala osso, e as luzes em
 * volta comecam a falhar. <b>Cacando</b>, fica preto, mais rapido que alguem correndo, e vai direto.
 *
 * <p>Ele nao causa dano: chegar perto o bastante termina a brincadeira e devolve a pessoa ao mundo,
 * com as coisas de volta. A saida de verdade e a porta — quem chega numa antes some daqui.
 *
 * <p>Nao usa os {@code Goal} do jogo de proposito: o comportamento e um roteiro com tempo marcado, e
 * um roteiro fica mais claro (e mais facil de ajustar) escrito de cima para baixo do que espalhado
 * em metas que competem entre si.
 */
public final class SkinwalkerEntity extends Monster {
    public static final byte PHASE_STALK = 0;
    public static final byte PHASE_REVEAL = 1;
    public static final byte PHASE_HUNT = 2;

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BYTE);
    /**
     * O perfil inteiro de quem ele esta imitando — nome, id e a textura.
     *
     * <p>Mandar so o id obrigaria o cliente a descobrir a skin sozinho, perguntando a Mojang. Isso
     * da a skin errada em servidor que troca skins (SkinRestorer, Ely.by) e skin nenhuma em servidor
     * offline, onde o id nem existe la fora. O servidor ja sabe a resposta certa: e ela que viaja.
     */
    private static final EntityDataAccessor<CompoundTag> DATA_SKIN =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** 0 a 100: o quanto a revelacao ja andou. O cliente usa isso para falhar a imagem. */
    private static final EntityDataAccessor<Byte> DATA_REVEAL =
            SynchedEntityData.defineId(SkinwalkerEntity.class, EntityDataSerializers.BYTE);

    /**
     * Passo de gente. O mod original usa 0.32 e e por isso que o dele convence: no 0.16 de antes ele
     * arrastava o pe, e nada arrasta o pe num corredor vazio sem parecer errado.
     */
    private static final double WALK_SPEED = 0.30;
    /** Cacada: mais rapido que alguem correndo (um jogador esprintando faz ~5,6 blocos por segundo). */
    private static final double HUNT_SPEED = 0.40;
    /** Depois de um minuto de perseguicao ele para de brincar. */
    private static final int HUNT_PATIENCE_TICKS = 1200;
    /** Quanto a velocidade cresce por segundo depois disso — 6% ao segundo dobra em uns doze. */
    private static final double HUNT_RAMP = 1.06;
    /** Quantos blocos a cabeca desce procurando o chao (um pulo nao passa disso). */
    private static final int HEAD_MAX_FALL = 4;
    /** Teto: rapido o bastante para acabar, nao para atravessar parede. */
    private static final double HUNT_SPEED_MAX = 0.95;
    /** Distancia em que ele encosta e a brincadeira acaba. */
    private static final double CATCH_DISTANCE = 1.9;
    /** Longe demais da vitima: some, para nao ficar entidade solta no andar. */
    private static final double DESPAWN_DISTANCE = 96.0;
    /** Onde ele fica enquanto ronda: colado o bastante para ser visto, longe o bastante para nao encostar. */
    private static final double STALK_NEAR = 5.5;
    private static final double STALK_FAR = 14.0;
    /** As luzes em volta que participam do susto. */
    private static final int LIGHT_RADIUS = 22;

    @Nullable
    private UUID victimId;
    private int phaseTicks;
    private int nextVoice;
    private final List<BlockPos> lamps = new ArrayList<>();
    /**
     * As lampadas que ele mexe nao vao para o disco. Se o servidor desligou no meio da revelacao ou
     * da cacada, elas ficaram apagadas no mundo: na volta, ele as procura de novo antes de qualquer
     * coisa, para devolver a luz quando sumir.
     */
    private boolean lampsUnknown;

    /** O perfil ja lido, e o tag de onde ele saiu — para nao reler NBT a cada quadro desenhado. */
    @Nullable
    private CompoundTag skinTag;
    @Nullable
    private GameProfile skin;

    public SkinwalkerEntity(EntityType<? extends SkinwalkerEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, PHASE_STALK);
        builder.define(DATA_SKIN, new CompoundTag());
        builder.define(DATA_REVEAL, (byte) 0);
    }

    /** Quem ele esta imitando e de quem ele vai atras. */
    public void impersonate(ServerPlayer victim, GameProfile skin) {
        this.victimId = victim.getUUID();
        setSkin(skin);
        String name = skin.getName();
        if (name != null && !name.isEmpty()) {
            setCustomName(net.minecraft.network.chat.Component.literal(name));
            // Nome por cima, como qualquer jogador. De longe, no fim do corredor, e so mais alguem
            // -- e o nome e justamente o que faz voce ir ate la.
            setCustomNameVisible(true);
        }
    }

    public byte phase() {
        return entityData.get(DATA_PHASE);
    }

    /**
     * O perfil de quem ele imita, pronto para desenhar.
     *
     * <p>A leitura do NBT acontece uma vez por atualizacao: enquanto o objeto do tag for o mesmo, o
     * perfil ja lido serve. O desenho chama isto a cada quadro.
     */
    @Nullable
    public GameProfile skinProfile() {
        CompoundTag tag = entityData.get(DATA_SKIN);
        if (tag != skinTag) {
            skinTag = tag;
            skin = Profiles.read(tag);
        }
        return skin;
    }

    @Nullable
    public UUID skinId() {
        GameProfile profile = skinProfile();
        return profile == null ? null : profile.getId();
    }


    private void setSkin(GameProfile profile) {
        CompoundTag tag = Profiles.write(profile);
        skinTag = tag;
        skin = profile;
        entityData.set(DATA_SKIN, tag);
    }

    /**
     * Se, neste tick, a imagem dele deve aparecer preta.
     *
     * <p>Em vez de trocar a textura de uma vez, as duas se alternam: quanto mais a revelacao anda,
     * mais ticks de cada seis saem pretos. E lampada queimando, nao animacao.
     */
    public boolean revealFlicker() {
        int dark = Math.round(entityData.get(DATA_REVEAL) / 100.0F * 6.0F);
        return tickCount % 6 < dark;
    }

    // ---- roteiro ----

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (lampsUnknown) {
            lampsUnknown = false;
            collectLamps();
        }
        ServerPlayer victim = victim();
        if (victim == null) {
            discard();
            return;
        }
        phaseTicks++;
        switch (phase()) {
            case PHASE_REVEAL -> reveal(victim);
            case PHASE_HUNT -> hunt(victim);
            default -> stalk(victim);
        }
    }

    @Nullable
    private ServerPlayer victim() {
        if (victimId == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer victim = level.getServer().getPlayerList().getPlayer(victimId);
        if (victim == null
                || !victim.level().dimension().equals(BackroomsDimensions.BACKROOMS)
                || victim.isCreative()
                || victim.isSpectator()
                || distanceToSqr(victim) > DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
            return null;
        }
        return victim;
    }

    /**
     * Rondando: ele <b>segue</b> a pessoa.
     *
     * <p>Ficar de longe so funciona em corredor reto; num labirinto vira "nunca ninguem apareceu".
     * Entao ele vem atras mesmo, apressa o passo quando fica para tras e, chegando a alguns blocos,
     * para e fica ali olhando — que e a parte que incomoda.
     */
    private void stalk(ServerPlayer victim) {
        getLookControl().setLookAt(victim, 30.0F, 30.0F);
        double distance = distanceTo(victim);
        if (distance <= STALK_NEAR) {
            getNavigation().stop();
        } else if (tickCount % 10 == 0) {
            // Rota refeita de meio em meio segundo: a pessoa anda, e no labirinto o caminho muda.
            getNavigation().moveTo(victim, distance > STALK_FAR ? 1.15 : 0.8);
        }

        if (--nextVoice <= 0) {
            BackroomsVoices.speak(this, skinId(), BackroomsVoices.PITCH_HUMAN);
            nextVoice = 240 + random.nextInt(360);
        }

        // A transformacao e espontanea: chegar perto dele nao antecipa nada. Ele fica ali, andando
        // atras, falando com a voz de alguem — e ai, do nada, para de fingir.
        if (phaseTicks >= BackroomsSettings.skinwalkerStalkTicks()) {
            startReveal(victim);
        }
    }

    private void startReveal(ServerPlayer victim) {
        setPhase(PHASE_REVEAL);
        // O nome some junto com o disfarce.
        setCustomNameVisible(false);
        getNavigation().stop();
        // A primeira coisa que deixa de ser humana e o cheiro que ele sente.
        playSound(NexusSounds.SKINWALKER_SNIFF.get(), 1.0F, 0.8F);
        playSound(NexusSounds.SKINWALKER_NOTICE.get(), 1.0F, 1.0F);
        playSound(NexusSounds.SKINWALKER_REVEAL.get(), 1.0F, 1.0F);
        collectLamps();
        PacketDistributor.sendToPlayer(victim, SkinwalkerPayload.reveal(BackroomsSettings.skinwalkerRevealTicks()));
    }

    /** Se revelando: parado, estalando osso, com as luzes falhando em volta. */
    private void reveal(ServerPlayer victim) {
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        getLookControl().setLookAt(victim, 60.0F, 60.0F);

        if (phaseTicks % 20 == 0) {
            playSound(NexusSounds.SKINWALKER_BONE_CRACK.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
        if (phaseTicks % 3 == 0) {
            flickerLamps(random.nextFloat() < 0.65F, 3);
        }
        entityData.set(DATA_REVEAL,
                (byte) Math.min(100, phaseTicks * 100 / Math.max(1, BackroomsSettings.skinwalkerRevealTicks())));

        if (phaseTicks >= BackroomsSettings.skinwalkerRevealTicks()) {
            startHunt(victim);
        }
    }

    private void startHunt(ServerPlayer victim) {
        setPhase(PHASE_HUNT);
        phaseTicks = 0;
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNT_SPEED);
        PacketDistributor.sendToPlayer(victim, SkinwalkerPayload.hunt());
        // Apagao: daqui para frente a perseguicao acontece no escuro, e achar a saida deixa de ser
        // so questao de correr. As luzes voltam quando ele some.
        flickerLamps(false, 1);
    }

    /**
     * Cacando: linha reta, rapido, sem som de gente.
     *
     * <p>Passado um minuto, ele acelera de segundo em segundo. Perseguicao que nao termina vira
     * passeio: a partir dai a conta e exponencial e alcancar deixa de ser questao de sorte.
     */
    private void hunt(ServerPlayer victim) {
        getLookControl().setLookAt(victim, 90.0F, 90.0F);
        if (tickCount % 2 == 0) {
            getNavigation().moveTo(victim, 1.0);
        }
        if (phaseTicks > HUNT_PATIENCE_TICKS && phaseTicks % 20 == 0) {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            speed.setBaseValue(Math.min(HUNT_SPEED_MAX, speed.getBaseValue() * HUNT_RAMP));
        }
        if (phaseTicks % 6 == 0) {
            playSound(NexusSounds.SKINWALKER_STEP.get(), 0.9F, 0.8F);
        }
        // Rosnado de vez em quando: aqui ele ja nao tem o que disfarcar.
        if (phaseTicks % 70 == 0) {
            playSound(NexusSounds.SKINWALKER_AMBIENCE.get(), 1.0F, 0.7F);
        }
        // Ele continua falando na cacada — com a voz da pessoa, so que fundo demais para ser ela.
        if (--nextVoice <= 0) {
            BackroomsVoices.speak(this, skinId(), BackroomsVoices.PITCH_BEAST);
            nextVoice = 100 + random.nextInt(140);
        }
        // No escuro, uma lampada ou outra ainda estremece: o suficiente para voce achar que viu algo.
        if (phaseTicks % 40 == 0) {
            flickerLamps(random.nextInt(4) == 0, 8);
        }
        if (distanceTo(victim) <= CATCH_DISTANCE) {
            catchVictim(victim);
        }
    }

    private void catchVictim(ServerPlayer victim) {
        PacketDistributor.sendToPlayer(victim, SkinwalkerPayload.caught());
        BackroomsManager.startCatch(victim);
        dropHead(victim);
        restoreLamps();
        discard();
    }

    /**
     * A cabeca de quem foi pego fica no chao, onde aconteceu.
     *
     * <p>E o rastro que faz o lugar acumular historia: quem cair ali depois vai encontrar a cara de
     * alguem conhecido no meio do corredor. Some quando o andar e refeito, ou pelo comando de limpar.
     *
     * <p>A cabeca leva o perfil que o servidor tem da pessoa, textura inclusive — e por isso ela
     * mostra a cara certa tambem em servidor offline ou com mod de skin. Corpo nao fica: la dentro
     * o unico rastro e este.
     */
    private void dropHead(ServerPlayer victim) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // Pego no meio de um pulo, a pessoa esta no ar: a cabeca cai ate o chao, nao fica boiando.
        BlockPos pos = victim.blockPosition();
        for (int fall = 0; fall < HEAD_MAX_FALL && level.getBlockState(pos.below()).isAir(); fall++) {
            pos = pos.below();
        }
        if (!level.getBlockState(pos).isAir() || level.getBlockState(pos.below()).isAir()) {
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.PLAYER_HEAD.defaultBlockState()
                .setValue(SkullBlock.ROTATION, random.nextInt(16)));
        if (level.getBlockEntity(pos) instanceof SkullBlockEntity skull) {
            skull.setOwner(new ResolvableProfile(victim.getGameProfile()));
        }
        BackroomsData.get(level.getServer()).rememberHead(pos);
    }

    private void setPhase(byte phase) {
        if (phase != PHASE_REVEAL) {
            entityData.set(DATA_REVEAL, phase == PHASE_HUNT ? (byte) 100 : (byte) 0);
        }
        entityData.set(DATA_PHASE, phase);
        phaseTicks = 0;
    }

    // ---- as luzes ----

    /** As lampadas ficam todas na mesma altura, entao procurar por elas e varrer um plano so. */
    private void collectLamps() {
        lamps.clear();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dz = -LIGHT_RADIUS; dz <= LIGHT_RADIUS; dz++) {
            for (int dx = -LIGHT_RADIUS; dx <= LIGHT_RADIUS; dx++) {
                cursor.set(getBlockX() + dx, BackroomsDimensions.CEILING_Y, getBlockZ() + dz);
                if (level.getBlockState(cursor).getBlock() instanceof BackroomsLampBlock) {
                    lamps.add(cursor.immutable());
                }
            }
        }
    }

    /** Apaga (ou acende) parte das lampadas em volta. {@code fraction} 1 = todas, 3 = um terco. */
    private void flickerLamps(boolean lit, int fraction) {
        if (lamps.isEmpty() || !(level() instanceof ServerLevel level)) {
            return;
        }
        int touched = Math.max(1, lamps.size() / Math.max(1, fraction));
        for (int i = 0; i < touched; i++) {
            BlockPos pos = lamps.get(random.nextInt(lamps.size()));
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BackroomsLampBlock && state.getValue(BackroomsLampBlock.LIT) != lit) {
                level.setBlock(pos, state.setValue(BackroomsLampBlock.LIT, lit),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
        level.playSound(null, blockPosition(),
                lit ? NexusSounds.LIGHT_ON.get() : NexusSounds.LIGHT_BLINK.get(),
                SoundSource.AMBIENT, 0.8F, 1.0F);
    }

    /** Ninguem fica no escuro para sempre: ao sumir, a luz volta. */
    private void restoreLamps() {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        for (BlockPos pos : lamps) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BackroomsLampBlock && !state.getValue(BackroomsLampBlock.LIT)) {
                level.setBlock(pos, state.setValue(BackroomsLampBlock.LIT, true),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
        }
        lamps.clear();
    }

    /** Ele nao morre nem apanha: e um susto, nao um chefe. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) {
            restoreLamps();
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (victimId != null) {
            tag.putUUID("victim", victimId);
        }
        tag.put("skin", entityData.get(DATA_SKIN));
        tag.putByte("phase", phase());
        tag.putInt("phaseTicks", phaseTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        victimId = tag.hasUUID("victim") ? tag.getUUID("victim") : null;
        entityData.set(DATA_SKIN, savedSkin(tag));
        entityData.set(DATA_PHASE, tag.getByte("phase"));
        phaseTicks = tag.getInt("phaseTicks");
        if (phase() == PHASE_HUNT) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HUNT_SPEED);
        }
        // Rondando ele ainda nao apagou nada. Depois disso, as luzes em volta sao dele.
        lampsUnknown = phase() != PHASE_STALK;
    }

    /**
     * O perfil salvo em disco.
     *
     * <p>Skinwalkers de mundos feitos antes disto guardavam so id e nome; nesse caso o perfil e
     * remontado a partir deles, e a textura volta a ser procurada como antes.
     */
    private static CompoundTag savedSkin(CompoundTag tag) {
        if (tag.contains("skin", Tag.TAG_COMPOUND)) {
            return tag.getCompound("skin");
        }
        if (!tag.hasUUID("skinId")) {
            return new CompoundTag();
        }
        return Profiles.write(new GameProfile(tag.getUUID("skinId"), tag.getString("skinName")));
    }

    /** Onde nasce: perto o bastante para aparecer, longe o bastante para nao brotar na cara. */
    public static SkinwalkerEntity spawn(ServerLevel level, ServerPlayer victim, BlockPos where,
                                         GameProfile skin) {
        SkinwalkerEntity skinwalker = NexusRegistries.SKINWALKER.get().create(level);
        if (skinwalker == null) {
            return null;
        }
        skinwalker.moveTo(where.getX() + 0.5, where.getY(), where.getZ() + 0.5,
                victim.getYRot(), 0.0F);
        skinwalker.impersonate(victim, skin);
        level.addFreshEntity(skinwalker);
        return skinwalker;
    }
}
