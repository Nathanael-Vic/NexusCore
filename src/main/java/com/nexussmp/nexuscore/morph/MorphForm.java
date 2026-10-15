package com.nexussmp.nexuscore.morph;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Uma forma copiada pelo anel: o tipo da criatura e o que faz ela ter a cara que tem.
 *
 * <p>Para bicho, {@link #data} e o NBT dele ja limpo pelo {@link MorphCapture} — sobra o que muda a
 * aparencia (filhote, cor, variante, o que ele segura) e sai o que e de onde ele esta, de quem ele
 * e ou do que ele carrega. Para jogador, e o id, o nome e a textura da skin: com a textura junto, a
 * skin aparece certa mesmo com a pessoa offline e em servidor sem conta original.
 *
 * <p>O NBT nunca e alterado depois de criado: e o valor de um componente de item, e o jogo compara
 * componentes por igualdade.
 */
public record MorphForm(ResourceLocation type, CompoundTag data) {
    public static final ResourceLocation PLAYER = ResourceLocation.withDefaultNamespace("player");

    private static final String PLAYER_ID = "Id";
    private static final String PLAYER_NAME = "Name";
    private static final String SKIN = "Textures";
    private static final String SKIN_SIGNATURE = "Signature";
    private static final String TEXTURES_PROPERTY = "textures";

    public static final Codec<MorphForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(MorphForm::type),
            CompoundTag.CODEC.optionalFieldOf("data", new CompoundTag()).forGetter(MorphForm::data)
    ).apply(instance, MorphForm::new));

    public static final StreamCodec<ByteBuf, MorphForm> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MorphForm::type,
            ByteBufCodecs.COMPOUND_TAG, MorphForm::data,
            MorphForm::new);

    /** A forma de outro jogador: quem ele e e a skin dele, tirados do perfil que o servidor conhece. */
    public static MorphForm ofPlayer(GameProfile profile) {
        CompoundTag data = new CompoundTag();
        data.putUUID(PLAYER_ID, profile.getId());
        data.putString(PLAYER_NAME, profile.getName());
        profile.getProperties().get(TEXTURES_PROPERTY).stream().findFirst().ifPresent(textures -> {
            data.putString(SKIN, textures.value());
            if (textures.signature() != null) {
                data.putString(SKIN_SIGNATURE, textures.signature());
            }
        });
        return new MorphForm(PLAYER, data);
    }

    public boolean isPlayer() {
        return PLAYER.equals(type);
    }

    /** O perfil do jogador copiado, com a skin; {@code null} se esta forma e de bicho. */
    @Nullable
    public GameProfile playerProfile() {
        if (!isPlayer() || !data.hasUUID(PLAYER_ID)) {
            return null;
        }
        GameProfile profile = new GameProfile(data.getUUID(PLAYER_ID), data.getString(PLAYER_NAME));
        if (data.contains(SKIN)) {
            String signature = data.contains(SKIN_SIGNATURE) ? data.getString(SKIN_SIGNATURE) : null;
            profile.getProperties().put(TEXTURES_PROPERTY, new Property(TEXTURES_PROPERTY, data.getString(SKIN), signature));
        }
        return profile;
    }

    public Optional<EntityType<?>> entityType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(type);
    }

    /** O nome que aparece na dica do anel: o do jogador, ou o nome traduzido da criatura. */
    public Component displayName() {
        if (isPlayer()) {
            return Component.literal(data.getString(PLAYER_NAME));
        }
        return entityType().map(EntityType::getDescription)
                .orElseGet(() -> Component.literal(type.toString()));
    }
}
