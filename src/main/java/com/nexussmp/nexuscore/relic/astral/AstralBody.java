package com.nexussmp.nexuscore.relic.astral;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.core.util.Profiles;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * O corpo que fica para tras enquanto a pessoa anda fora dele.
 *
 * <p>Para quem olha, e a pessoa: a skin dela, o nome dela em cima, a armadura e o que ela segurava,
 * parada de cabeca baixa. Mas e uma casca — nao tem vida propria. Tudo o que acerta o corpo chega a
 * alma: a pessoa e puxada de volta na hora e leva o golpe, mais forte, porque estava indefesa
 * ({@link AstralProjection#onBodyHurt}).
 *
 * <p>Nunca e salvo. O que importa — onde o corpo estava — fica no {@link AstralSessions}; se o
 * servidor cair, a pessoa volta para la ao entrar, e o corpo nem precisa existir.
 */
public final class AstralBody extends LivingEntity {
    /** O perfil da pessoa, com a textura: e o que faz o corpo ter a cara dela em qualquer cliente. */
    private static final EntityDataAccessor<CompoundTag> DATA_PROFILE =
            SynchedEntityData.defineId(AstralBody.class, EntityDataSerializers.COMPOUND_TAG);
    /** Cabeca baixa: o corpo vazio olha para o chao. */
    private static final float HEAD_DOWN = 28.0F;

    private final NonNullList<ItemStack> hands = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    @Nullable
    private UUID owner;

    public AstralBody(EntityType<? extends AstralBody> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    /** Copia a cara, o nome e o que a pessoa veste e segura — so a aparencia, nada disso cai. */
    void copyOf(ServerPlayer player) {
        owner = player.getUUID();
        entityData.set(DATA_PROFILE, Profiles.write(player.getGameProfile()));
        setCustomName(Component.literal(player.getGameProfile().getName()));
        setCustomNameVisible(true);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HAND || slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }
        moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), HEAD_DOWN);
        setYHeadRot(player.getYRot());
        yBodyRot = player.yBodyRot;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Nullable
    public GameProfile profile() {
        return Profiles.read(entityData.get(DATA_PROFILE));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, new CompoundTag());
    }

    /** Nada machuca a casca: o golpe vai para a alma, que volta para leva-lo. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved()) {
            return false;
        }
        AstralProjection.onBodyHurt(this, source, amount, source.is(DamageTypes.GENERIC_KILL));
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        setXRot(HEAD_DOWN);
        // Sem dono esperando, nao ha alma para voltar: o corpo nao tem por que ficar.
        if (!level().isClientSide() && tickCount % 20 == 0 && (owner == null || !AstralProjection.isProjecting(owner))) {
            discard();
        }
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return armor;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot.getType()) {
            case HAND -> hands.get(slot.getIndex());
            case HUMANOID_ARMOR -> armor.get(slot.getIndex());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        switch (slot.getType()) {
            case HAND -> hands.set(slot.getIndex(), stack);
            case HUMANOID_ARMOR -> armor.set(slot.getIndex(), stack);
            default -> { }
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }
}
