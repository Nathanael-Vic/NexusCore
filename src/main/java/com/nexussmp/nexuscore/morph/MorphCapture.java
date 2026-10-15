package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Bater numa criatura com o anel na mao guarda a forma dela no anel.
 *
 * <p>O golpe nao acontece: nem dano, nem empurrao, nem acerto critico. Da para copiar um bicho manso
 * sem machucar ele. Isso vale para criatura viva; num quadro, num barco ou num suporte de armadura
 * o anel bate como qualquer outra coisa na mao.
 *
 * <p><b>O que vai junto.</b> O NBT inteiro da criatura, menos o que nao e aparencia. Aparencia e
 * coisa de cada mod — a cor da ovelha, a variante do sapo, o tipo do aldeao, o que o esqueleto
 * segura, o tamanho do slime, e o que os mods de bicho inventarem —, entao nao da para listar o que
 * fica; lista-se o que sai: onde ela esta, quanta vida tem, de quem ela e, o que ela carrega, o que
 * ela lembra. O resto vai, com um teto de tamanho: um bicho com NBT gigante perde primeiro o que
 * segura e veste, e se ainda nao couber vira so o tipo, com a cara padrao.
 */
public final class MorphCapture {
    /** Teto do que o anel guarda. Vai no item, e item anda pela rede. */
    private static final int MAX_BYTES = 16 * 1024;

    /** Nao e aparencia: posicao, estado, dono, memoria, inventario, e dados que so o servidor usa. */
    private static final Set<String> DROPPED = Set.of(
            // onde e como ela esta agora
            "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround", "Invulnerable",
            "PortalCooldown", "UUID", "TicksFrozen", "HasVisualFire", "Glowing", "NoGravity", "Silent",
            "Passengers", "leash", "Leash", "Tags", "CustomName", "CustomNameVisible", "Team",
            // vida e o que ela sofreu
            "Health", "HurtTime", "HurtByTimestamp", "DeathTime", "AbsorptionAmount", "FallFlying",
            "SleepingX", "SleepingY", "SleepingZ", "active_effects", "attributes",
            // cabeca e memoria
            "Brain", "AngerTime", "AngryAt", "InLove", "LoveCause", "ForcedAge", "Sitting", "OrderedToSit",
            "CanPickUpLoot", "PersistenceRequired", "DeathLootTable", "DeathLootTableSeed",
            "HandDropChances", "ArmorDropChances", "body_armor_drop_chance",
            // o que ela carrega e negocia
            "Inventory", "Items", "Offers", "Gossips", "Xp", "LastRestock", "LastGossipDecay",
            "RestocksToday", "FoodLevel",
            // lugares do mundo que ela conhece
            "HivePos", "FlowerPos", "HomePos", "PatrolTarget", "Patrolling", "Wave", "CanJoinRaid",
            // dados que outros mods penduram na entidade
            "neoforge:attachments", "NeoForgeData", "ForgeData", "ForgeCaps");

    /** O que sai primeiro quando a forma passa do teto: e o que costuma ser grande. */
    private static final Set<String> EQUIPMENT = Set.of("HandItems", "ArmorItems", "body_armor_item", "SaddleItem");

    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.getMainHandItem().is(NexusRegistries.MORPH_RING.get())
                || !CommonConfig.isModuleEnabled(ModuleIds.MORPH)) {
            return;
        }
        // O dragao se acerta pelas partes dele; a forma e a do dono das partes.
        Entity target = event.getTarget() instanceof PartEntity<?> part ? part.getParent() : event.getTarget();
        if (!(target instanceof LivingEntity living) || target instanceof ArmorStand) {
            return;
        }
        // Cancelado dos dois lados: o cliente tambem nao mostra um acerto que nao aconteceu.
        event.setCanceled(true);
        if (!(player instanceof ServerPlayer server)) {
            return;
        }

        MorphForm form = formOf(living);
        if (form == null) {
            server.displayClientMessage(Component.translatable("nexuscore.morph.blocked")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        player.getMainHandItem().set(NexusRegistries.MORPH_FORM.get(), form);
        server.displayClientMessage(Component.translatable("nexuscore.morph.copied", form.displayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        ServerLevel level = server.serverLevel();
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getBbHeight() / 2.0, target.getZ(),
                16, target.getBbWidth() / 2.0, target.getBbHeight() / 2.0, target.getBbWidth() / 2.0, 0.02);
    }

    /** A forma daquela criatura, ou {@code null} se a configuracao nao deixa copiar. */
    @Nullable
    static MorphForm formOf(LivingEntity target) {
        if (target instanceof Player other) {
            return MorphSettings.allowPlayers() ? MorphForm.ofPlayer(other.getGameProfile()) : null;
        }
        MorphForm form = new MorphForm(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), appearanceOf(target));
        return MorphSettings.allowed(form) ? form : null;
    }

    private static CompoundTag appearanceOf(LivingEntity target) {
        CompoundTag tag = new CompoundTag();
        try {
            target.saveWithoutId(tag);
        } catch (RuntimeException error) {
            // Um mod que nao sabe se salvar fora da hora: fica a cara padrao do tipo.
            return new CompoundTag();
        }
        DROPPED.forEach(tag::remove);
        if (tag.sizeInBytes() > MAX_BYTES) {
            EQUIPMENT.forEach(tag::remove);
        }
        return tag.sizeInBytes() > MAX_BYTES ? new CompoundTag() : tag;
    }

    private MorphCapture() {}
}
